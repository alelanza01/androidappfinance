package it.privato.disponibile.v2
import android.app.*
import android.os.Bundle
import android.view.*
import android.widget.*
import android.content.Intent
import android.graphics.Color
import android.text.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject
import it.privato.disponibile.v2.MainActivity.Draft
import it.privato.disponibile.v2.MainActivity.HistoryItem

/** Native screen composition and its UI events; financial writes remain in Store. */
internal fun MainActivity.createMenu(planned:Boolean=false,advance:Boolean=false) {
        if(advance){openForm("advance");return}
        dialog?.dismiss()
        dialog=RegisterSheet(this,ui).show(planned){type,defaults->openForm(type,defaults=defaults)}
    }

internal fun MainActivity.manage(o:Operation) {
        val actions=mutableListOf<Pair<String,()->Unit>>()
        if(o.status==Status.PLANNED)actions.add((if(o.kind==Kind.INCOME)"Conferma incasso oggi" else "Conferma pagamento oggi") to {confirm("Registrare ${Money.format(o.cents)}?","Il movimento diventerà effettivo oggi e cambierà la liquidità una sola volta.",if(o.kind==Kind.INCOME)"Registra incasso" else "Registra pagamento",false){store.settle(o.id)}})
        actions.add("Modifica" to {openForm("operation",o.id)})
        if(o.series==null)actions.add("Elimina movimento" to {confirm("Eliminare il movimento?",if(o.status==Status.ACTUAL)"Il saldo cambierà di ${Money.format(-o.signed)}. Il movimento verrà rimosso da storico, grafici e backup." else "La previsione sarà aggiornata; la liquidità attuale non cambia.",if(o.kind==Kind.EXPENSE)"Elimina spesa" else "Elimina movimento"){store.delete(o.id)}})
        else {
            actions.add("Elimina…" to {deleteChoices(o)})
            l.series.firstOrNull {it.id==o.series && it.active}?.let {s->actions.add("Modifica serie" to {openForm("series",s.id)})}
        }
        sheet(l.operationTitle(o),actions,"${o.date.format(dates)} · ${Money.format(o.cents)} · ${if(o.status==Status.ACTUAL)"già effettuato" else "da effettuare"}")
    }

internal fun MainActivity.deleteChoices(o:Operation) {
        sheet("Cosa vuoi eliminare?",listOf(
            "Soltanto questa occorrenza" to {confirm("Eliminare questa occorrenza?",if(o.status==Status.ACTUAL)"È già contabilizzata: il saldo cambierà di ${Money.format(-o.signed)}. Le altre occorrenze resteranno." else "Le altre occorrenze della serie resteranno previste.","Elimina questa occorrenza"){store.delete(o.id)}},
            "Questa e tutte le future" to {confirm("Interrompere da qui?","Verrà interrotta la programmazione da questa occorrenza in poi. I movimenti già effettuati, incluso questo se pagato, resteranno nello storico.","Interrompi le prossime"){store.removeSeries(o.series!!,o.index!!)}},
            "Intera serie" to {stopSeries(o.series!!)}
        ))
    }

internal fun MainActivity.stopSeries(id:Long) {
        val paid=l.actual.filter {it.series==id};val effect=-paid.sumOf {it.signed}
        sheet("Elimina intera serie",listOf(
            "Interrompi, mantieni lo storico" to {confirm("Interrompere la serie?","Tutti gli impegni non effettuati verranno eliminati. ${paid.size} movimenti effettivi resteranno: la liquidità non cambia.","Interrompi la serie"){store.removeSeries(id)}},
            "Elimina anche i movimenti effettuati" to {confirm("Rettificare anche lo storico?","Questa è una correzione contabile distinta: eliminerà ${paid.size} movimenti già effettuati. La liquidità passerà da ${Money.format(l.balance)} a ${Money.format(l.balance+effect)}. Conferma solo se quei movimenti erano errati.","Elimina l’intera serie"){store.removeSeries(id,removeActual=true)}}
        ),"L'interruzione della programmazione non deve cancellare pagamenti reali.")
    }

internal fun MainActivity.seriesMenu(s:Series)=sheet(s.title,listOf("Modifica serie" to {openForm("series",s.id)},"Elimina intera serie" to {stopSeries(s.id)}),"${frequencyName(s.frequency)} · ${Money.format(s.cents)}")
