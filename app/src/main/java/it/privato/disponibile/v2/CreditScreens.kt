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
internal fun MainActivity.credits(body:LinearLayout) {
        body.addView(ui.label("ANCORA DA RECUPERARE · TOTALE GLOBALE",11f,ui.colors.muted,true));body.addView(ui.amount(Money.format(l.outstanding)))
        val received=l.refunds.filter {YearMonth.from(it.date)==YearMonth.now()}.sumOf {it.cents}
        ui.pair(body,"Recuperati questo mese",Money.format(received),"Anticipi aperti",l.advances.count {l.remaining(it)>0}.toString())
        body.addView(ui.segments(listOf("Aperti","Recuperati","Scaduti"),when(creditStatus){1,2->0;3->1;4->2;else->-1}){creditStatus=listOf(1,3,4)[it];render()});body.addView(ui.space(12))
        body.addView(ui.label("Aperti ${l.advances.count {l.remaining(it)>0}} · Recuperati ${l.advances.count {l.remaining(it)==0L}} · Scaduti ${l.advances.count {l.remaining(it)>0 && it.due!=null && it.due<LocalDate.now()}}",12f,ui.colors.muted))
        val search=ui.input("Cerca crediti",creditQuery);body.addView(search)
        body.addView(ui.button("Filtri crediti"){creditFilters()}.apply {gravity=Gravity.START;setPadding(0,ui.dp(8),0,ui.dp(8))})
        if(creditMonth)monthControls(body)
        val rows=ui.column();body.addView(rows)
        fun update(){rows.removeAllViews();val found=l.advances.filter {a->
            (creditQuery.isBlank() || (a.description+" "+a.debtor+" "+a.cause).contains(creditQuery,true)) &&
            (creditSubject.isBlank() || a.debtor.contains(creditSubject,true)) && (creditProject.isBlank() || a.project.contains(creditProject,true)) &&
            (!creditMonth || YearMonth.from(a.date)==month) && when(creditStatus){1->l.remaining(a)>0;2->l.remaining(a)>0 && l.recovered(a.id)>0;3->l.remaining(a)==0L;4->l.remaining(a)>0 && a.due!=null && a.due<LocalDate.now();else->true}
        }.sortedByDescending {it.date}
            rows.addView(ui.label("${found.size} anticipi · ${listOf("tutti","aperti","parziali","recuperati","scaduti")[creditStatus]}",12f,ui.colors.muted))
            rows.addView(ui.label("Residuo della selezione: ${Money.format(found.sumOf {l.remaining(it)})}. Gli scaduti sono inclusi negli aperti.",12f,ui.colors.muted))
            if(found.isEmpty())ui.empty(rows,"Nessun credito","Registra il denaro che anticipi per conto di altri. Non sarà confuso con una spesa personale.")
            found.take(300).forEach {creditRow(rows,it)}
            if(found.size>300)rows.addView(ui.label("Mostrati 300 risultati: restringi i filtri.",12f,ui.colors.muted))
        };watch(search){creditQuery=it;update()};update();body.addView(ui.space(12));body.addView(ui.button("Nuovo anticipo",true){openForm("advance")})
    }

internal fun MainActivity.creditFilters()=openForm("creditFilter",defaults=mapOf("subject" to creditSubject,"project" to creditProject,"state" to creditStatus.toString(),"month" to creditMonth.toString()))

internal fun MainActivity.creditRow(body:LinearLayout,a:Advance) {
        body.addView(ui.listRow(a.description,"${a.debtor}${if(a.project.isNotBlank()) " · ${a.project}" else ""}\n${l.advanceStatus(a)}",Money.format(l.remaining(a)),if(a.due!=null && a.due<LocalDate.now() && l.remaining(a)>0)ui.colors.coral else ui.colors.text){openCredit(a)});body.addView(ui.divider())
    }

internal fun MainActivity.openCredit(a:Advance){detailId=a.id;go(13)}

internal fun MainActivity.creditDetail(body:LinearLayout) {
        val a=l.advances.firstOrNull {it.id==detailId} ?: run {ui.empty(body,"Anticipo non disponibile","La registrazione è stata eliminata.");return}
        body.addView(ui.label(a.description,21f,ui.colors.text,true));body.addView(ui.label(a.debtor,14f,ui.colors.muted));body.addView(ui.section(l.advanceStatus(a)));body.addView(ui.amount(Money.format(l.remaining(a))));body.addView(ui.label("Da recuperare",13f,ui.colors.muted))
        if(l.remaining(a)>0)body.addView(ui.button("Registra rimborso",true){openForm("refund",a.id)})
        body.addView(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {max=100;progress=(l.recovered(a.id)*100/a.cents).toInt();progressTintList=android.content.res.ColorStateList.valueOf(ui.colors.sage);contentDescription="Recuperato $progress per cento"},LinearLayout.LayoutParams(-1,ui.dp(8)))
        body.addView(ui.section("Dettagli"));ui.metric(body,"Anticipato",Money.format(a.cents));ui.metric(body,"Restituito",Money.format(l.recovered(a.id)),ui.colors.sage)
        body.addView(ui.label("${a.date.format(dates)}${a.due?.let {" · atteso ${it.format(dates)}"} ?: ""}",13f,ui.colors.muted))
        if(a.project.isNotBlank())body.addView(ui.label(a.project,14f,ui.colors.muted));if(a.cause.isNotBlank())body.addView(ui.label(a.cause,14f,ui.colors.muted));if(a.notes.isNotBlank())body.addView(ui.label(a.notes,14f,ui.colors.muted))
        body.addView(ui.section("Rimborsi ricevuti"));val receipts=l.refunds.filter {it.advance==a.id}.sortedByDescending {it.date}
        if(receipts.isEmpty())body.addView(ui.label("Nessun rimborso ricevuto",14f,ui.colors.muted))
        receipts.forEach {r->body.addView(ui.listRow(r.date.format(dates),r.note,Money.format(r.cents),ui.colors.sage){sheet("Rimborso",listOf("Modifica rimborso" to {openForm("refundEdit",r.id)},"Elimina rimborso" to {confirm("Eliminare il rimborso?","La liquidità diminuirà di ${Money.format(r.cents)} e il credito residuo aumenterà della stessa somma.","Elimina rimborso"){store.deleteRefund(r.id)}}))});body.addView(ui.divider())}
        body.addView(ui.button("Gestisci anticipo"){sheet("Gestisci anticipo",listOf("Modifica anticipo" to {openForm("advance",a.id)},"Elimina anticipo" to {confirm("Eliminare l'anticipo?","Verranno eliminati anche i suoi rimborsi. Il saldo aumenterà di ${Money.format(l.remaining(a))}. Usa questa azione per correggere una registrazione errata.","Elimina anticipo"){store.deleteAdvance(a.id);page=3}}))})
    }
