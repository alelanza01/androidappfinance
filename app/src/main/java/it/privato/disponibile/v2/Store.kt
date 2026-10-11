package it.privato.disponibile.v2

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import android.graphics.Color

class Store(context:Context):SQLiteOpenHelper(context,"finance-v2.db",null,2) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE settings(id INTEGER PRIMARY KEY CHECK(id=1),opening INTEGER NOT NULL,reserve INTEGER NOT NULL,horizon TEXT NOT NULL,theme TEXT NOT NULL,reminders INTEGER NOT NULL)")
        db.execSQL("INSERT INTO settings VALUES(1,0,0,?,'system',0)",arrayOf<Any?>(LocalDate.now().plusDays(30).toString()))
        db.execSQL("CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,color INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,cents INTEGER NOT NULL,kind TEXT NOT NULL,category INTEGER NOT NULL,start TEXT NOT NULL,frequency TEXT NOT NULL,count INTEGER NOT NULL,uncertain INTEGER NOT NULL,active INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE operations(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,cents INTEGER NOT NULL,kind TEXT NOT NULL,status TEXT NOT NULL,date TEXT NOT NULL,category INTEGER NOT NULL,uncertain INTEGER NOT NULL,series INTEGER,occurrence INTEGER,UNIQUE(series,occurrence))")
        db.execSQL("CREATE INDEX operation_dates ON operations(date,status)")
        createAdvances(db)
        listOf("Casa" to "#779B91","Spesa" to "#DBBF8B","Trasporti" to "#7392B7","Personale" to "#AC91BB","Entrate" to "#60977B","Altro" to "#C66B65").forEach {(name,color)->db.execSQL("INSERT INTO categories(name,color) VALUES(?,?)",arrayOf<Any?>(name,Color.parseColor(color)))}
    }
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int) { if(old<2)createAdvances(db) }
    private fun createAdvances(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE advances(id INTEGER PRIMARY KEY AUTOINCREMENT,cents INTEGER NOT NULL CHECK(cents>0),date TEXT NOT NULL,description TEXT NOT NULL,debtor TEXT NOT NULL,project TEXT NOT NULL,cause TEXT NOT NULL,due TEXT,notes TEXT NOT NULL)")
        db.execSQL("CREATE TABLE refunds(id INTEGER PRIMARY KEY AUTOINCREMENT,advance INTEGER NOT NULL,cents INTEGER NOT NULL CHECK(cents>0),date TEXT NOT NULL,note TEXT NOT NULL)")
        db.execSQL("CREATE INDEX refund_advance ON refunds(advance)")
    }
    private fun transaction(action:(SQLiteDatabase)->Unit) {
        val db=writableDatabase;db.beginTransaction()
        try {action(db);db.setTransactionSuccessful()} finally {db.endTransaction()}
    }
    private fun read():Ledger {
        val settings=readableDatabase.rawQuery("SELECT opening,reserve,horizon,theme,reminders FROM settings WHERE id=1",null).use {it.moveToFirst();Settings(it.getLong(0),it.getLong(1),maxOf(LocalDate.parse(it.getString(2)),LocalDate.now()),it.getString(3),it.getInt(4)==1)}
        val categories=readableDatabase.rawQuery("SELECT id,name,color FROM categories ORDER BY id",null).use {c->buildList {while(c.moveToNext())add(Category(c.getLong(0),c.getString(1),c.getInt(2)))}}
        val series=readableDatabase.rawQuery("SELECT id,title,cents,kind,category,start,frequency,count,uncertain,active FROM series",null).use {c->buildList {while(c.moveToNext())add(Series(c.getLong(0),c.getString(1),c.getLong(2),Kind.valueOf(c.getString(3)),c.getLong(4),LocalDate.parse(c.getString(5)),Frequency.valueOf(c.getString(6)),c.getInt(7),c.getInt(8)==1,c.getInt(9)==1))}}
        val operations=readableDatabase.rawQuery("SELECT id,title,cents,kind,status,date,category,uncertain,series,occurrence FROM operations ORDER BY date,id",null).use {c->buildList {while(c.moveToNext())add(Operation(c.getLong(0),c.getString(1),c.getLong(2),Kind.valueOf(c.getString(3)),Status.valueOf(c.getString(4)),LocalDate.parse(c.getString(5)),c.getLong(6),c.getInt(7)==1,if(c.isNull(8))null else c.getLong(8),if(c.isNull(9))null else c.getInt(9)))}}
        val advances=readableDatabase.rawQuery("SELECT id,cents,date,description,debtor,project,cause,due,notes FROM advances ORDER BY date,id",null).use {c->buildList {while(c.moveToNext())add(Advance(c.getLong(0),c.getLong(1),LocalDate.parse(c.getString(2)),c.getString(3),c.getString(4),c.getString(5),c.getString(6),if(c.isNull(7))null else LocalDate.parse(c.getString(7)),c.getString(8)))}}
        val refunds=readableDatabase.rawQuery("SELECT id,advance,cents,date,note FROM refunds ORDER BY date,id",null).use {c->buildList {while(c.moveToNext())add(Refund(c.getLong(0),c.getLong(1),c.getLong(2),LocalDate.parse(c.getString(3)),c.getString(4)))}}
        return Ledger(settings,categories,operations,series,advances,refunds)
    }
    fun snapshot(expand:Boolean=true):Ledger {
        var ledger=read()
        if(expand) {
            val until=maxOf(ledger.settings.horizon,LocalDate.now().plusYears(1))
            transaction {db->ledger.series.filter {it.active}.forEach {s->
                var i=0
                while((s.count==0 || i<s.count) && (s.count>0 || s.date(i)<=until)) {
                    require(i<20000) {"Ricorrenza troppo lunga. Riduci l'intervallo."}
                    val op=Operation(title=if(s.count>0) "${s.title} · ${i+1}/${s.count}" else s.title,cents=s.cents,kind=s.kind,status=Status.PLANNED,date=s.date(i),category=s.category,uncertain=s.uncertain,series=s.id,index=i)
                    db.insertWithOnConflict("operations",null,values(op),SQLiteDatabase.CONFLICT_IGNORE);i++
                }
            }}
            ledger=read()
        }
        return ledger
    }
    private fun values(op:Operation)=ContentValues().apply {
        put("title",op.title.trim());put("cents",op.cents);put("kind",op.kind.name);put("status",op.status.name);put("date",op.date.toString());put("category",op.category);put("uncertain",if(op.uncertain)1 else 0)
        if(op.series==null)putNull("series") else put("series",op.series)
        if(op.index==null)putNull("occurrence") else put("occurrence",op.index)
    }
    fun save(op:Operation) {
        validate(op)
        require(read().categories.any {it.id==op.category}) {"Categoria non disponibile."}
        if(op.id==0L)check(writableDatabase.insertOrThrow("operations",null,values(op))>0)
        else check(writableDatabase.update("operations",values(op),"id=? AND status!='DELETED'",arrayOf(op.id.toString()))==1) {"Operazione non disponibile."}
    }
    fun settle(id:Long) {transaction {db->
        val op=read().operations.firstOrNull {it.id==id && it.status==Status.PLANNED} ?: error("Operazione già registrata.")
        val v=values(op.copy(status=Status.ACTUAL,date=LocalDate.now(),uncertain=false))
        check(db.update("operations",v,"id=? AND status='PLANNED'",arrayOf(id.toString()))==1)
    }}
    fun delete(id:Long) {check(writableDatabase.update("operations",ContentValues().apply {put("status",Status.DELETED.name)},"id=? AND status!='DELETED'",arrayOf(id.toString()))==1)}
    fun cancelSeries(id:Long) {transaction {db->db.execSQL("UPDATE series SET active=0 WHERE id=?",arrayOf<Any?>(id));db.execSQL("UPDATE operations SET status='DELETED' WHERE series=? AND status='PLANNED'",arrayOf<Any?>(id))}}
    fun saveSeries(s:Series) {
        require(s.title.isNotBlank() && s.title.length<=64 && s.cents in 1..Money.MAX && s.count in 0..120 && s.kind!=Kind.ADJUSTMENT) {"Ricorrenza: titolo fino a 64 caratteri, importo positivo e massimo 120 rate."}
        require(s.start>=LocalDate.now().minusYears(2) && s.start<=LocalDate.now().plusYears(10)) {"Scegli una data entro 2 anni fa e 10 anni futuri."}
        require(read().categories.any {it.id==s.category})
        val v=ContentValues().apply {put("title",s.title.trim());put("cents",s.cents);put("kind",s.kind.name);put("category",s.category);put("start",s.start.toString());put("frequency",s.frequency.name);put("count",s.count);put("uncertain",if(s.uncertain)1 else 0);put("active",1)}
        transaction {db->check(db.insertOrThrow("series",null,v)>0)}
        snapshot()
    }
    fun settings(s:Settings) {
        require(s.opening in -Money.MAX..Money.MAX && s.reserve in 0..Money.MAX)
        require(s.horizon>=LocalDate.now() && s.horizon<=LocalDate.now().plusYears(10)) {"La previsione deve essere da oggi a 10 anni."}
        require(s.theme in listOf("system","light","dark"))
        writableDatabase.execSQL("UPDATE settings SET opening=?,reserve=?,horizon=?,theme=?,reminders=? WHERE id=1",arrayOf<Any?>(s.opening,s.reserve,s.horizon.toString(),s.theme,if(s.reminders)1 else 0))
    }
    fun reconcile(target:Long) {
        require(target in -Money.MAX..Money.MAX)
        val l=snapshot();val diff=target-l.balance
        if(diff==0L)return
        require(diff in -Money.MAX..Money.MAX) {"Rettifica troppo grande."}
        save(Operation(title="Rettifica liquidità",cents=diff,kind=Kind.ADJUSTMENT,status=Status.ACTUAL,date=LocalDate.now(),category=l.categories.last().id))
    }
    fun category(id:Long?,name:String,color:Int) {
        require(name.trim().length in 1..40) {"Nome categoria: da 1 a 40 caratteri."}
        val v=ContentValues().apply {put("name",name.trim());put("color",color)}
        if(id==null)check(writableDatabase.insertOrThrow("categories",null,v)>0) else check(writableDatabase.update("categories",v,"id=?",arrayOf(id.toString()))==1)
    }
    fun deleteCategory(id:Long) {transaction {db->
        val l=read();require(l.categories.size>1) {"Mantieni almeno una categoria."}
        require(l.operations.none {it.category==id && it.status!=Status.DELETED} && l.series.none {it.category==id && it.active}) {"Categoria utilizzata: modifica prima le operazioni e le ricorrenze."}
        val fallback=l.categories.first {it.id!=id}.id
        db.execSQL("UPDATE operations SET category=? WHERE category=?",arrayOf<Any?>(fallback,id));db.execSQL("UPDATE series SET category=? WHERE category=?",arrayOf<Any?>(fallback,id));db.delete("categories","id=?",arrayOf(id.toString()))
    }}
    fun replace(ledger:Ledger) {
        Backup.decode(Backup.encode(ledger))
        transaction {db->
        db.delete("refunds",null,null);db.delete("advances",null,null);db.delete("operations",null,null);db.delete("series",null,null);db.delete("categories",null,null)
        ledger.categories.forEach {c->db.execSQL("INSERT INTO categories(id,name,color) VALUES(?,?,?)",arrayOf<Any?>(c.id,c.name,c.color))}
        ledger.series.forEach {s->db.execSQL("INSERT INTO series VALUES(?,?,?,?,?,?,?,?,?,?)",arrayOf<Any?>(s.id,s.title,s.cents,s.kind.name,s.category,s.start.toString(),s.frequency.name,s.count,if(s.uncertain)1 else 0,if(s.active)1 else 0))}
        ledger.operations.forEach {op->val v=values(op);v.put("id",op.id);db.insertOrThrow("operations",null,v)}
        ledger.advances.forEach {a->val v=advanceValues(a);v.put("id",a.id);db.insertOrThrow("advances",null,v)}
        ledger.refunds.forEach {r->val v=refundValues(r);v.put("id",r.id);db.insertOrThrow("refunds",null,v)}
        val s=ledger.settings
        db.execSQL("UPDATE settings SET opening=?,reserve=?,horizon=?,theme=?,reminders=? WHERE id=1",arrayOf<Any?>(s.opening,s.reserve,maxOf(s.horizon,LocalDate.now()).toString(),s.theme,if(s.reminders)1 else 0))
    }}
    private fun advanceValues(a:Advance)=ContentValues().apply {
        put("cents",a.cents);put("date",a.date.toString());put("description",a.description.trim());put("debtor",a.debtor.trim());put("project",a.project.trim());put("cause",a.cause.trim());put("due",a.due?.toString());put("notes",a.notes.trim())
    }
    private fun refundValues(r:Refund)=ContentValues().apply {put("advance",r.advance);put("cents",r.cents);put("date",r.date.toString());put("note",r.note.trim())}
    fun saveAdvance(a:Advance) {transaction {db->
        validateAdvance(a)
        val l=read();val receipts=l.refunds.filter {it.advance==a.id}
        require(a.cents>=receipts.sumOf {it.cents}) {"L'anticipo non può essere inferiore ai rimborsi già ricevuti."}
        require(receipts.all {it.date>=a.date}) {"La data dell'anticipo deve precedere tutti i rimborsi."}
        if(a.id==0L)db.insertOrThrow("advances",null,advanceValues(a))
        else check(db.update("advances",advanceValues(a),"id=?",arrayOf(a.id.toString()))==1) {"Anticipo non disponibile."}
    }}
    fun saveRefund(r:Refund) {transaction {db->
        val l=read();val a=l.advances.firstOrNull {it.id==r.advance} ?: error("Anticipo non disponibile.")
        if(r.id!=0L)require(l.refunds.any {it.id==r.id && it.advance==r.advance}) {"Rimborso non disponibile."}
        validateRefund(r,a)
        val available=a.cents-l.refunds.filter {it.advance==a.id && it.id!=r.id}.sumOf {it.cents}
        require(r.cents<=available) {"Rimborso superiore al credito residuo (${Money.format(available)}). Verifica l'importo."}
        if(r.id==0L)db.insertOrThrow("refunds",null,refundValues(r))
        else check(db.update("refunds",refundValues(r),"id=? AND advance=?",arrayOf(r.id.toString(),r.advance.toString()))==1)
    }}
    fun deleteRefund(id:Long) {check(writableDatabase.delete("refunds","id=?",arrayOf(id.toString()))==1) {"Rimborso non disponibile."}}
    fun deleteAdvance(id:Long) {transaction {db->
        check(db.delete("advances","id=?",arrayOf(id.toString()))==1) {"Anticipo non disponibile."}
        db.delete("refunds","advance=?",arrayOf(id.toString()))
    }}
    companion object {
        fun validateAdvance(a:Advance) {
            require(a.cents in 1..Money.MAX) {"Inserisci un importo positivo."}
            require(a.description.trim().length in 1..80 && a.debtor.trim().length in 1..80) {"Descrizione e soggetto sono richiesti (massimo 80 caratteri)."}
            require(a.project.length<=80 && a.cause.length<=80 && a.notes.length<=500) {"Dettagli troppo lunghi."}
            require(a.date.year in 1900..2200 && a.date<=LocalDate.now()) {"L'anticipo deve essere già effettuato, con una data non futura."}
            require(a.due==null || (a.due>=a.date && a.due.year in 1900..2200)) {"La restituzione prevista deve seguire la data dell'anticipo."}
        }
        fun validateRefund(r:Refund,a:Advance) {
            require(r.cents in 1..Money.MAX) {"Inserisci un rimborso positivo."}
            require(r.date>=a.date && r.date<=LocalDate.now()) {"Il rimborso deve essere già ricevuto e successivo all'anticipo."}
            require(r.note.length<=500) {"Nota troppo lunga (massimo 500 caratteri)."}
        }
        fun validate(op:Operation) {
            require(op.title.trim().length in 1..80) {"Descrizione richiesta, massimo 80 caratteri."}
            require(if(op.kind==Kind.ADJUSTMENT) op.cents!=0L && op.cents in -Money.MAX..Money.MAX else op.cents in 1..Money.MAX) {"Importo non valido."}
            require(op.date.year in 1900..2200) {"Data fuori intervallo."}
            require(op.status!=Status.ACTUAL || op.date<=LocalDate.now()) {"Una data futura deve restare prevista."}
            require(op.kind!=Kind.ADJUSTMENT || op.status!=Status.PLANNED) {"Le rettifiche sono effettive."}
            require(!op.uncertain || (op.kind==Kind.INCOME && op.status!=Status.ACTUAL)) {"Solo un'entrata prevista può essere non garantita."}
            require((op.series==null)==(op.index==null) && (op.index==null || op.index>=0))
        }
    }
}
