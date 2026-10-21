package it.privato.disponibile.v2

import android.database.sqlite.SQLiteDatabase
import android.graphics.*
import android.graphics.drawable.Drawable

/** Stable names: migration adds choices without changing existing category identities. */
object CategoryCatalog {
    data class Entry(val name:String,val icon:String,val group:String)
    val entries=listOf(
        Entry("Spesa","basket","Quotidiano"),Entry("Bar e ristoranti","cup","Quotidiano"),Entry("Sport e palestra","sport","Quotidiano"),
        Entry("Alcolici","wine","Quotidiano"),Entry("Sigarette","smoke","Quotidiano"),Entry("Svapo","vape","Quotidiano"),
        Entry("Casa","home","Casa e mobilità"),Entry("Casa vacanza","holiday","Casa e mobilità"),Entry("Automobile","car","Casa e mobilità"),
        Entry("Carburante","fuel","Casa e mobilità"),Entry("Trasporti","bus","Casa e mobilità"),Entry("Viaggi","travel","Casa e mobilità"),
        Entry("Salute","health","Persona"),Entry("Farmacia","pharmacy","Persona"),Entry("Abbigliamento","shirt","Persona"),
        Entry("Shopping","bag","Persona"),Entry("Tecnologia","screen","Servizi e tempo libero"),Entry("Abbonamenti","repeat","Servizi e tempo libero"),
        Entry("Intrattenimento","play","Servizi e tempo libero"),Entry("Utenze","bolt","Servizi e tempo libero"),Entry("Regali","gift","Persona"),
        Entry("Animali","paw","Persona"),Entry("Formazione","book","Lavoro e altro"),Entry("Lavoro","work","Lavoro e altro"),
        Entry("Personale","person","Persona"),Entry("Entrate","income","Lavoro e altro"),Entry("Altro","other","Lavoro e altro"))
    val icons=entries.map {it.icon}.toSet()
    fun iconFor(name:String)=entries.firstOrNull {it.name.equals(name,true)}?.icon ?: "other"
    fun group(c:Category)=entries.firstOrNull {it.icon==c.icon}?.group ?: "Lavoro e altro"
    fun completeLegacy(categories:List<Category>):List<Category> {
        val result=categories.toMutableList();var id=(categories.maxOfOrNull {it.id} ?: 0)+1
        entries.filter {e->categories.none {it.name.equals(e.name,true)}}.forEachIndexed {i,e->
            result.add(Category(id++,e.name,Color.parseColor(listOf("#779B91","#DBBF8B","#7392B7","#AC91BB","#C66B65")[i%5]),e.icon))
        };return result
    }
    fun ensure(db:SQLiteDatabase) {
        entries.forEachIndexed {i,e->
            val color=Color.parseColor(listOf("#779B91","#DBBF8B","#7392B7","#AC91BB","#C66B65")[i%5])
            db.execSQL("INSERT OR IGNORE INTO categories(name,color,icon,favorite) VALUES(?,?,?,0)",arrayOf<Any?>(e.name,color,e.icon))
            db.execSQL("UPDATE categories SET icon=? WHERE name=? AND icon='other'",arrayOf<Any?>(e.icon,e.name))
        }
    }
}

/** One 24 px vector vocabulary, consistent stroke and optical size; no font emoji. */
class CategoryIcon(private val key:String,private val tint:Int,private val px:Int):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun getIntrinsicWidth()=px
    override fun getIntrinsicHeight()=px
    override fun draw(c:Canvas) {
        c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f)
        fun line(x:Float,y:Float,a:Float,b:Float)=c.drawLine(x,y,a,b,p)
        fun rect(x:Float,y:Float,a:Float,b:Float)=c.drawRoundRect(x,y,a,b,2f,2f,p)
        fun path(vararg xy:Float){val v=Path();v.moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)v.lineTo(xy[i],xy[i+1]);c.drawPath(v,p)}
        when(key) {
            "home","holiday"->{path(3f,11f,12f,4f,21f,11f);rect(6f,11f,18f,21f);line(11f,15f,11f,21f);if(key=="holiday")c.drawCircle(20f,4f,2f,p)}
            "basket"->{path(3f,10f,6f,21f,18f,21f,21f,10f,3f,10f);line(7f,10f,10f,4f);line(17f,10f,14f,4f);line(9f,14f,9f,18f);line(15f,14f,15f,18f)}
            "cup"->{rect(4f,8f,16f,19f);c.drawArc(14f,9f,22f,17f,-90f,180f,false,p);line(3f,22f,19f,22f);line(8f,3f,8f,5f);line(13f,3f,13f,5f)}
            "wine"->{path(7f,3f,6f,10f,9f,14f,15f,14f,18f,10f,17f,3f,7f,3f);line(12f,14f,12f,21f);line(8f,21f,16f,21f);line(7f,8f,17f,8f)}
            "smoke"->{rect(3f,13f,21f,18f);line(16f,13f,16f,18f);path(17f,3f,15f,6f,18f,9f)}
            "vape"->{rect(8f,9f,16f,22f);rect(10f,5f,14f,9f);line(12f,1f,15f,2f);c.drawCircle(12f,15f,1f,p)}
            "sport"->{line(5f,12f,19f,12f);rect(3f,8f,7f,16f);rect(17f,8f,21f,16f)}
            "car","bus"->{rect(4f,7f,20f,18f);line(4f,12f,20f,12f);line(6f,18f,6f,21f);line(18f,18f,18f,21f);line(7f,15f,9f,15f);line(15f,15f,17f,15f);if(key=="car")path(5f,7f,8f,3f,16f,3f,19f,7f)}
            "fuel"->{rect(4f,3f,14f,21f);rect(6f,5f,12f,10f);path(14f,12f,18f,12f,18f,18f,21f,18f,21f,8f,17f,4f)}
            "travel"->{path(12f,2f,14f,9f,21f,13f,21f,15f,14f,13f,14f,19f,17f,21f,7f,21f,10f,19f,10f,13f,3f,15f,3f,13f,10f,9f,12f,2f)}
            "health","pharmacy"->{if(key=="health")c.drawCircle(12f,12f,9f,p)else rect(3f,3f,21f,21f);line(12f,7f,12f,17f);line(7f,12f,17f,12f)}
            "shirt"->{path(8f,3f,3f,7f,6f,11f,8f,9f,8f,21f,16f,21f,16f,9f,18f,11f,21f,7f,16f,3f);c.drawArc(8f,1f,16f,7f,0f,180f,false,p)}
            "bag"->{rect(4f,8f,20f,21f);c.drawArc(8f,2f,16f,13f,180f,180f,false,p)}
            "screen"->{rect(3f,4f,21f,17f);line(12f,17f,12f,21f);line(7f,21f,17f,21f)}
            "repeat"->{c.drawArc(4f,4f,20f,20f,-60f,145f,false,p);c.drawArc(4f,4f,20f,20f,120f,145f,false,p);path(18f,3f,18f,7f,22f,7f);path(6f,21f,6f,17f,2f,17f)}
            "play"->{c.drawCircle(12f,12f,9f,p);path(10f,7f,17f,12f,10f,17f,10f,7f)}
            "bolt"->path(14f,2f,5f,14f,11f,14f,10f,22f,19f,10f,13f,10f,14f,2f)
            "gift"->{rect(4f,9f,20f,21f);line(12f,9f,12f,21f);line(4f,13f,20f,13f);c.drawOval(5f,3f,12f,9f,p);c.drawOval(12f,3f,19f,9f,p)}
            "paw"->{c.drawOval(6f,12f,18f,21f,p);listOf(5f to 8f,10f to 4f,16f to 4f,21f to 8f).forEach {(x,y)->c.drawOval(x-2f,y-2f,x+1f,y+2f,p)}}
            "book"->{rect(3f,4f,21f,20f);line(12f,4f,12f,20f);line(6f,8f,9f,8f);line(15f,8f,18f,8f)}
            "work"->{rect(3f,7f,21f,21f);rect(8f,3f,16f,7f);line(3f,13f,21f,13f);line(12f,11f,12f,15f)}
            "person"->{c.drawCircle(12f,7f,4f,p);c.drawArc(4f,13f,20f,27f,180f,180f,false,p)}
            "income"->{rect(3f,5f,21f,19f);line(12f,15f,12f,8f);path(8f,11f,12f,7f,16f,11f)}
            else->{c.drawCircle(12f,12f,9f,p);listOf(7f,12f,17f).forEach {c.drawCircle(it,12f,.8f,p)}}
        };c.restore()
    }
    override fun setAlpha(a:Int){p.alpha=a}
    override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
