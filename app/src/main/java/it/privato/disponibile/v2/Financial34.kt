package it.privato.disponibile.v2

import java.time.LocalDate

/** Integer-only allocation; the first remainder installments receive one extra cent. */
object Installments {
    fun split(total:Long,count:Int):List<Long> {
        require(count in 1..120 && total in count.toLong()..Money.MAX){"Il totale deve coprire almeno un centesimo per rata (1–120 rate)."}
        return List(count){total/count+if(it<total%count)1 else 0}
    }
    fun validate(s:Series) {
        require(s.amountMode!=AmountMode.TOTAL || (s.count in 1..120 && s.totalCents!=null && s.totalCents in 1..Money.MAX && s.installments.size==s.count && s.installments.all {it in 1..Money.MAX} && s.installments.sum()==s.totalCents)){"Piano totale privo di quote valide."}
        require(s.amountMode==AmountMode.TOTAL || s.totalCents==null)
        require(s.installments.isEmpty() || (s.count>0 && s.installments.size==s.count && s.installments.all {it in 1..Money.MAX}))
    }
    fun edit(s:Series,operations:List<Operation>):Series {
        val paid=operations.filter {it.series==s.id && it.status==Status.ACTUAL}
        if(s.amountMode==AmountMode.EACH)return s.copy(installments=emptyList())
        require(paid.all {(it.index ?: -1) in 0 until s.count || s.count==0}){"Il numero di rate eliminerebbe pagamenti già registrati."}
        val total=requireNotNull(s.totalCents);val fixed=paid.associate {requireNotNull(it.index) to it.cents};val free=(0 until s.count).filter {it !in fixed}
        val left=total-fixed.values.sum()
        require(if(free.isEmpty())left==0L else left>=free.size){"Il totale non copre i pagamenti già effettuati e le rate residue."}
        val portions=if(free.isEmpty())emptyList() else split(left,free.size)
        val byIndex=free.zip(portions).toMap()
        return s.copy(installments=List(s.count){fixed[it] ?: byIndex.getValue(it)})
    }
}

data class SimulationPoint(val date:LocalDate,val cash:Long,val titles:List<String>)
data class Simulation(val margin:Long,val start:Long,val reserve:Long,val points:List<SimulationPoint>,val scenario:List<SimulationPoint>)
object Simulator {
    /** Pure read model. It receives a ledger and cannot access SQLite or materialize recurrences. */
    fun calculate(l:Ledger,expense:Long,end:LocalDate,today:LocalDate=AppClock.today(),includeIncome:Boolean=false):Simulation {
        require(expense in 0..Money.MAX && end>=today && end<=today.plusYears(10))
        val planned=l.pending.filter {it.date<=end}
        fun line(income:Boolean):List<SimulationPoint> {
            var cash=l.balance-expense;val result=mutableListOf(SimulationPoint(today,cash,listOf("Dopo la spesa simulata")))
            planned.filter {it.kind==Kind.EXPENSE || income && it.kind==Kind.INCOME}.groupBy {maxOf(it.date,today)}.toSortedMap().forEach {(date,ops)->
                cash+=ops.sumOf {it.signed};result.add(SimulationPoint(date,cash,ops.map {l.operationTitle(it)}))
            }
            if(result.last().date<end)result.add(SimulationPoint(end,cash,listOf("Fine simulazione")))
            return result
        }
        return Simulation(l.spendable-expense,l.balance-expense,l.settings.reserve,line(false),if(includeIncome)line(true)else emptyList())
    }
}
object QuickCategories {
    fun ordered(l:Ledger):List<Category> {
        val usage=l.actual.filter {it.kind==Kind.EXPENSE}.groupingBy {it.category}.eachCount()
        return l.categories.sortedWith(compareBy<Category> {if(it.quickRank!=null)0 else if(it.favorite)1 else if((usage[it.id] ?: 0)>0)2 else 3}.thenBy {it.quickRank ?: Int.MAX_VALUE}.thenByDescending {if(it.quickRank==null && !it.favorite)usage[it.id] ?: 0 else 0}.thenBy {listOf("basket","home","car","travel","wine","smoke","vape","health","bag","holiday","work","other").indexOf(it.icon).let {rank->if(rank<0)100 else rank}}.thenBy {it.id})
    }
}
