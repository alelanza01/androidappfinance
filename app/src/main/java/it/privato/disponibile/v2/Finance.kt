package it.privato.disponibile.v2

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

object Money {
    const val MAX = 999_999_999L
    fun parse(value: String, signed: Boolean=false): Long {
        val s=value.trim().replace(',','.')
        require(Regex("-?\\d{1,7}(\\.\\d{1,2})?").matches(s)) {"Importo non valido: usa ad esempio 125,50, senza migliaia e con massimo 2 decimali."}
        val n=BigDecimal(s).movePointRight(2).longValueExact()
        require(n in -MAX..MAX && (signed || n>=0)) {"Importo fuori limite."}
        return n
    }
    fun format(n:Long)=NumberFormat.getCurrencyInstance(Locale.ITALY).format(BigDecimal.valueOf(n,2))
    fun input(n:Long)=BigDecimal.valueOf(n,2).toPlainString().replace('.',',')
}
enum class Kind { INCOME, EXPENSE, ADJUSTMENT }
enum class Status { PLANNED, ACTUAL, DELETED }
enum class Frequency { MONTHLY, WEEKLY, YEARLY }
data class Category(val id:Long,val name:String,val color:Int)
data class Operation(val id:Long=0,val title:String,val cents:Long,val kind:Kind,val status:Status,val date:LocalDate,val category:Long,val uncertain:Boolean=false,val series:Long?=null,val index:Int?=null) {
    val signed:Long get()=if(kind==Kind.EXPENSE) -cents else cents
}
data class Series(val id:Long=0,val title:String,val cents:Long,val kind:Kind,val category:Long,val start:LocalDate,val frequency:Frequency,val count:Int,val uncertain:Boolean,val active:Boolean=true,val endIndex:Int?=null) {
    fun date(index:Int):LocalDate=when(frequency) {Frequency.MONTHLY->start.plusMonths(index.toLong());Frequency.WEEKLY->start.plusWeeks(index.toLong());Frequency.YEARLY->start.plusYears(index.toLong())}
}
data class Settings(val opening:Long=0,val reserve:Long=0,val horizon:LocalDate=LocalDate.now().plusDays(30),val theme:String="system",val reminders:Boolean=false)
data class Advance(val id:Long=0,val cents:Long,val date:LocalDate,val description:String,val debtor:String,val project:String="",val cause:String="",val due:LocalDate?=null,val notes:String="")
data class Refund(val id:Long=0,val advance:Long,val cents:Long,val date:LocalDate,val note:String="")
data class CashEvent(val date:LocalDate,val signed:Long)
data class Ledger(val settings:Settings,val categories:List<Category>,val operations:List<Operation>,val series:List<Series>,val advances:List<Advance> = emptyList(),val refunds:List<Refund> = emptyList()) {
    val persistedSeries get():List<Series> {
        val historical=operations.filter {it.status==Status.ACTUAL}.mapNotNull {it.series}.toSet()
        return series.filter {it.active || it.id in historical}
    }
    val persistedOperations get():List<Operation> {
        val definitions=series.associateBy {it.id}
        return operations.filter {o->o.status!=Status.DELETED || definitions[o.series]?.let {it.active && (it.endIndex==null || (o.index ?: Int.MAX_VALUE)<it.endIndex)}==true}
    }
    val actual get()=operations.filter {it.status==Status.ACTUAL}
    val pending get()=operations.filter {it.status==Status.PLANNED}.sortedWith(compareBy({it.date},{it.id}))
    val cashEvents get()=actual.map {CashEvent(it.date,it.signed)}+advances.map {CashEvent(it.date,-it.cents)}+refunds.map {CashEvent(it.date,it.cents)}
    val balance get()=settings.opening+cashEvents.sumOf {it.signed}
    private val refundTotals by lazy {refunds.groupBy {it.advance}.mapValues {(_,items)->items.sumOf {it.cents}}}
    fun recovered(id:Long)=refundTotals[id] ?: 0L
    fun remaining(a:Advance)=a.cents-recovered(a.id)
    val outstanding get()=advances.sumOf {remaining(it)}
    fun advanceStatus(a:Advance)=when {remaining(a)==0L->"Recuperato";a.due!=null && a.due<LocalDate.now()->"Scaduto";recovered(a.id)>0->"Parziale";else->"Aperto"}
    val commitments get()=pending.filter {it.kind==Kind.EXPENSE && !it.date.isAfter(settings.horizon)}.sumOf {it.cents}
    val expectedIncome get()=pending.filter {it.kind==Kind.INCOME && !it.date.isAfter(settings.horizon)}.sumOf {it.cents}
    val spendable get()=balance-settings.reserve-commitments
    fun simulated(cents:Long)=spendable-cents
    fun month(month:YearMonth,kind:Kind)=actual.filter {YearMonth.from(it.date)==month && it.kind==kind}.sumOf {it.cents}
    fun categoryName(id:Long)=categories.firstOrNull {it.id==id}?.name ?: "Senza categoria"
    fun history():List<Pair<LocalDate,Long>> {
        var b=settings.opening
        val events=cashEvents.groupBy {it.date}.toSortedMap()
        val start=events.keys.firstOrNull() ?: LocalDate.now()
        val points=mutableListOf(start.minusDays(1) to b)
        events.forEach {(date,items)->b+=items.sumOf {it.signed};points.add(date to b)}
        if(points.last().first<LocalDate.now())points.add(LocalDate.now() to b)
        return points
    }
    fun projection(includeIncome:Boolean):List<Pair<LocalDate,Long>> {
        var b=balance
        val today=LocalDate.now();val points=mutableListOf(today to b)
        pending.filter { !it.date.isAfter(settings.horizon) && (it.kind==Kind.EXPENSE || includeIncome) }.groupBy {maxOf(it.date,today)}.toSortedMap().forEach {(d,items)->
            b+=items.sumOf {it.signed};points.add(d to b)
        }
        if(points.last().first<settings.horizon)points.add(settings.horizon to b)
        return points
    }
}
