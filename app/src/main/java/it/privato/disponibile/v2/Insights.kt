package it.privato.disponibile.v2

import java.time.LocalDate
import java.time.YearMonth

/** Descriptive observations, never forecasts or classification of refunds as income. */
class Insights(val ledger:Ledger,val month:YearMonth,val today:LocalDate=AppClock.today()) {
    val previous=month.minusMonths(1)
    val day=if(month==YearMonth.from(today))today.dayOfMonth else month.lengthOfMonth()
    private fun expenses(m:YearMonth,limit:Int)=ledger.actual.filter {it.kind==Kind.EXPENSE && YearMonth.from(it.date)==m && it.date.dayOfMonth<=limit}
    val current=expenses(month,day)
    val prior=expenses(previous,if(month==YearMonth.from(today))minOf(day,previous.lengthOfMonth()) else previous.lengthOfMonth())
    val total=current.sumOf {it.cents}
    val previousTotal=prior.sumOf {it.cents}
    val comparable=current.isNotEmpty() && prior.isNotEmpty() && month<=YearMonth.from(today)
    val delta=total-previousTotal
    val percent=if(comparable && previousTotal>0)delta.toDouble()*100/previousTotal else null
    val ranking=current.groupBy {it.category}.map { (id,ops)->ledger.categories.first {it.id==id} to ops.sumOf {it.cents}}.sortedByDescending {it.second}
    val recurring=current.filter {it.series!=null}.sumOf {it.cents}
    val recurringCount=current.count {it.series!=null}
    val monthCash=ledger.cashEvents.filter {YearMonth.from(it.date)==month}.sumOf {it.signed}
}
