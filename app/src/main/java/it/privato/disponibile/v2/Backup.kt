package it.privato.disponibile.v2

import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate

object Backup {
    private fun JSONObject.longStrict(name:String):Long {
        val value=get(name)
        require(value is Int || value is Long) {"Campo numerico non intero: $name"}
        return (value as Number).toLong()
    }
    private fun JSONObject.intStrict(name:String):Int {
        val value=longStrict(name);require(value in Int.MIN_VALUE..Int.MAX_VALUE);return value.toInt()
    }
    const val LIMIT=5*1024*1024
    fun encode(l:Ledger):String {
        val s=l.settings
        val root=JSONObject().put("format","posso-spenderlo").put("version",2).put("settings",JSONObject().put("opening",s.opening).put("reserve",s.reserve).put("horizon",s.horizon).put("theme",s.theme).put("reminders",s.reminders))
        root.put("categories",JSONArray().apply {l.categories.forEach {put(JSONObject().put("id",it.id).put("name",it.name).put("color",it.color))}})
        root.put("series",JSONArray().apply {l.series.forEach {s->put(JSONObject().put("id",s.id).put("title",s.title).put("cents",s.cents).put("kind",s.kind.name).put("category",s.category).put("start",s.start).put("frequency",s.frequency.name).put("count",s.count).put("uncertain",s.uncertain).put("active",s.active))}})
        root.put("operations",JSONArray().apply {l.operations.forEach {o->put(JSONObject().put("id",o.id).put("title",o.title).put("cents",o.cents).put("kind",o.kind.name).put("status",o.status.name).put("date",o.date).put("category",o.category).put("uncertain",o.uncertain).put("series",o.series ?: JSONObject.NULL).put("index",o.index ?: JSONObject.NULL))}})
        return root.toString(2)
    }
    fun decode(text:String):Ledger {
        require(text.toByteArray(Charsets.UTF_8).size<=LIMIT) {"Backup troppo grande (massimo 5 MB)."}
        val r=JSONObject(text)
        require(r.getString("format")=="posso-spenderlo" && r.intStrict("version")==2) {"Formato backup non supportato."}
        val j=r.getJSONObject("settings")
        val settings=Settings(j.longStrict("opening"),j.longStrict("reserve"),LocalDate.parse(j.getString("horizon")),j.getString("theme"),j.getBoolean("reminders"))
        require(settings.opening in -Money.MAX..Money.MAX && settings.reserve in 0..Money.MAX && settings.theme in listOf("light","dark","system") && settings.horizon.year in 1900..2200 && settings.horizon<=LocalDate.now().plusYears(10))
        fun array(key:String,max:Int):List<JSONObject> {val a=r.getJSONArray(key);require(a.length()<=max);return (0 until a.length()).map {a.getJSONObject(it)}}
        val categories=array("categories",100).map {Category(it.longStrict("id"),it.getString("name"),it.intStrict("color"))}
        require(categories.isNotEmpty() && categories.all {it.id>0 && it.name.trim().length in 1..40} && categories.map {it.id}.distinct().size==categories.size && categories.map {it.name}.distinct().size==categories.size)
        val series=array("series",2000).map {s->Series(s.longStrict("id"),s.getString("title"),s.longStrict("cents"),Kind.valueOf(s.getString("kind")),s.longStrict("category"),LocalDate.parse(s.getString("start")),Frequency.valueOf(s.getString("frequency")),s.intStrict("count"),s.getBoolean("uncertain"),s.getBoolean("active"))}
        require(series.map {it.id}.distinct().size==series.size)
        require(series.all {it.id>0 && it.title.trim().length in 1..64 && it.cents in 1..Money.MAX && it.kind!=Kind.ADJUSTMENT && it.count in 0..120 && categories.any {c->c.id==it.category} && (!it.uncertain || it.kind==Kind.INCOME) && it.start.year in 1900..2200 && it.start<=LocalDate.now().plusYears(10) && (it.count==0 || it.date(it.count-1).year<=2200)}) {"Ricorrenze del backup non valide o troppo vecchie."}
        val generationHorizon=maxOf(settings.horizon,LocalDate.now().plusYears(1))
        val generated=series.filter {it.active}.sumOf {s->
            if(s.count>0)s.count.toLong() else when(s.frequency) {
                Frequency.WEEKLY->maxOf(0L,java.time.temporal.ChronoUnit.WEEKS.between(s.start,generationHorizon)+1)
                Frequency.MONTHLY->maxOf(0L,java.time.temporal.ChronoUnit.MONTHS.between(s.start,generationHorizon)+2)
                Frequency.YEARLY->maxOf(0L,java.time.temporal.ChronoUnit.YEARS.between(s.start,generationHorizon)+2)
            }
        }
        require(generated<=20000) {"Il backup genererebbe troppe scadenze: massimo 20.000."}
        val operations=array("operations",20000).map {o->Operation(o.longStrict("id"),o.getString("title"),o.longStrict("cents"),Kind.valueOf(o.getString("kind")),Status.valueOf(o.getString("status")),LocalDate.parse(o.getString("date")),o.longStrict("category"),o.getBoolean("uncertain"),if(o.isNull("series"))null else o.longStrict("series"),if(o.isNull("index"))null else o.intStrict("index"))}
        operations.forEach {Store.validate(it);require(it.id>0 && categories.any {c->c.id==it.category} && (it.series==null || series.any {s->s.id==it.series && (s.count==0 || (it.index ?: -1)<s.count)}))}
        require(operations.map {it.id}.distinct().size==operations.size)
        val occurrences=operations.filter {it.series!=null}.map {it.series to it.index}
        require(occurrences.distinct().size==occurrences.size) {"Scadenze duplicate nel backup."}
        return Ledger(settings,categories,operations,series)
    }
}
