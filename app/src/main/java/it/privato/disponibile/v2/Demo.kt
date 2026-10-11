package it.privato.disponibile.v2
import android.graphics.Color
import java.time.LocalDate

object Demo {
    fun ledger(theme:String):Ledger {
        val today=LocalDate.now()
        val cats=listOf(Category(1,"Casa",Color.parseColor("#779B91")),Category(2,"Spesa",Color.parseColor("#DBBF8B")),Category(3,"Trasporti",Color.parseColor("#7392B7")),Category(4,"Entrate",Color.parseColor("#AC91BB")))
        val operations=listOf(
            Operation(1,"Compenso freelance",145000,Kind.INCOME,Status.ACTUAL,today.minusDays(12),4),
            Operation(2,"Affitto",60000,Kind.EXPENSE,Status.ACTUAL,today.minusDays(8),1),
            Operation(3,"Spesa settimanale",7800,Kind.EXPENSE,Status.ACTUAL,today.minusDays(3),2),
            Operation(4,"Abbonamento trasporti",3500,Kind.EXPENSE,Status.ACTUAL,today.minusDays(1),3),
            Operation(5,"Bolletta energia",9200,Kind.EXPENSE,Status.PLANNED,today.plusDays(4),1),
            Operation(6,"Rata computer",8500,Kind.EXPENSE,Status.PLANNED,today.plusDays(12),1),
            Operation(7,"Possibile incarico",45000,Kind.INCOME,Status.PLANNED,today.plusDays(18),4,true),
            Operation(8,"Compenso precedente",110000,Kind.INCOME,Status.ACTUAL,today.minusMonths(1),4),
            Operation(9,"Spese precedenti",82000,Kind.EXPENSE,Status.ACTUAL,today.minusMonths(1).plusDays(3),1)
        )
        val advances=listOf(Advance(1,5000,today.minusDays(7),"Pulizie Casa A","Proprietario Casa A","Casa A","Pulizie",today.minusDays(1)),Advance(2,8400,today.minusDays(2),"Materiali e detergenti","Studio Rossi","Progetto studio","Materiali",today.plusDays(5)),Advance(3,3500,today.minusDays(10),"Spesa condivisa","Marco",cause="Spesa"))
        val refunds=listOf(Refund(1,1,2000,today.minusDays(3),"Primo rimborso"),Refund(2,3,3500,today.minusDays(4),"Recuperato tutto"))
        return Ledger(Settings(50000,30000,today.plusDays(30),theme),cats,operations,emptyList(),advances,refunds)
    }
}
