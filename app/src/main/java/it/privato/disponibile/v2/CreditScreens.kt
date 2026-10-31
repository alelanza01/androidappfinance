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
        body.addView(ui.label("Tieni traccia dei soldi che hai anticipato e di quanto ti devono ancora restituire.",13f,ui.colors.muted));body.addView(ui.space(16))
        body.addView(ui.hero("Totale da recuperare",DisplayMoney.amount(l.outstanding),"Totale generale · non spendibile"));body.addView(ui.space(12))
        val received=l.refunds.filter {YearMonth.from(it.date)==AppClock.month()}.sumOf {it.cents}
        ui.pair(body,"Aperti · tutti",l.advances.count {l.remaining(it)>0}.toString(),"Rimborsi ${AppClock.month().format(DateTimeFormatter.ofPattern("MMMM",Locale.ITALY))} · tutti",DisplayMoney.amount(received))
        val filters=ui.column();body.addView(filters)
        val search=ui.input("Cerca motivo o persona",creditQuery).apply {contentDescription="Cerca anticipi";textSize=14f;background=ui.shape(ui.colors.hero,22).apply {setStroke(ui.dp(1),ui.colors.line)};setCompoundDrawablesWithIntrinsicBounds(SearchIcon(ui.colors.cool,ui.dp(18)),null,null,null);compoundDrawablePadding=ui.dp(10)}
        val rows=ui.column()
        fun scope(a:Advance)=
            (creditQuery.isBlank() || (a.description+" "+a.debtor+" "+a.cause).contains(creditQuery,true)) &&
            (creditSubject.isBlank() || a.debtor.contains(creditSubject,true)) && (creditProject.isBlank() || a.project.contains(creditProject,true)) && (!creditMonth || YearMonth.from(a.date)==month)
        fun update(){
            val scoped=l.advances.filter {scope(it)}
            filters.removeAllViews();filters.addView(ui.segments(listOf("Aperti","Recuperati","Scaduti"),when(creditStatus){1,2->0;3->1;4->2;else->-1}){creditStatus=listOf(1,3,4)[it];render()})
            val counts=listOf(scoped.count {l.remaining(it)>0},scoped.count {l.remaining(it)==0L},scoped.count {l.remaining(it)>0 && it.due!=null && it.due<AppClock.today()})
            filters.addView(ui.label("Aperti ${counts[0]} · Recuperati ${counts[1]} · Scaduti ${counts[2]}",11f,ui.colors.muted))
            filters.addView(ui.label("Gli scaduti sono già compresi negli aperti. I conteggi seguono i filtri.",11f,ui.colors.muted))
            val found=scoped.filter {a->when(creditStatus){1->l.remaining(a)>0;2->l.remaining(a)>0 && l.recovered(a.id)>0;3->l.remaining(a)==0L;4->l.remaining(a)>0 && a.due!=null && a.due<AppClock.today();else->true}}.sortedByDescending {it.date}
            rows.removeAllViews();rows.addView(ui.label("${found.size} ${if(found.size==1)"anticipo mostrato"else "anticipi mostrati"}",13f,ui.colors.text,true));rows.addView(ui.label("Residuo dei risultati: ${DisplayMoney.amount(found.sumOf {l.remaining(it)})}",12f,ui.colors.muted));rows.addView(ui.divider())
            if(found.isEmpty())ui.empty(rows,"Nessun anticipo in questa selezione","Rimuovi un filtro o mostra gli aperti: gli altri anticipi restano conservati.")
            found.take(300).forEach {creditRow(rows,it)}
            if(found.size>300)rows.addView(ui.label("Mostrati 300 risultati: restringi i filtri.",12f,ui.colors.muted))
        }
        body.addView(search);body.addView(ui.button("Filtra per persona, progetto o periodo"){creditFilters()}.apply {contentDescription="Filtri anticipi";gravity=Gravity.START;textSize=12f;setTextColor(ui.colors.cool);setPadding(0,ui.dp(8),0,ui.dp(8))})
        if(creditMonth)monthControls(body)
        if(creditQuery.isNotBlank() || creditSubject.isNotBlank() || creditProject.isNotBlank() || creditMonth)body.addView(ui.button("Rimuovi filtri anticipi"){creditQuery="";creditSubject="";creditProject="";creditMonth=false;render()})
        body.addView(rows);watch(search){creditQuery=it;update()};update()
        body.addView(ui.space(12));body.addView(ui.reviewCta("Registra anticipo"){openForm("advance")}.apply {tag="advance-cta"})
    }

internal fun MainActivity.creditFilters()=openForm("creditFilter",defaults=mapOf("subject" to creditSubject,"project" to creditProject,"state" to creditStatus.toString(),"month" to creditMonth.toString()))

internal fun MainActivity.creditRow(body:LinearLayout,a:Advance) {
        val outer=ui.column().apply {setPadding(0,ui.dp(12),0,ui.dp(12));minimumHeight=ui.dp(76)}
        val top=ui.row();top.addView(View(this).apply {background=ui.symbol("advance",36)},LinearLayout.LayoutParams(ui.dp(36),ui.dp(36)).apply {marginEnd=ui.dp(12)})
        val copy=ui.column();copy.addView(ui.label(a.description,14f,ui.colors.text,true));copy.addView(ui.label(a.debtor,12f,ui.colors.muted));top.addView(copy,LinearLayout.LayoutParams(0,-2,1.7f))
        top.addView(ui.amount(DisplayMoney.amount(l.remaining(a)),16f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));outer.addView(top)
        val large=resources.configuration.fontScale>=1.5f
        val badges=ui.row().apply {orientation=if(large)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL;setPadding(ui.dp(if(large)0 else 48),ui.dp(4),0,0)}
        val overdue=l.remaining(a)>0 && a.due!=null && a.due<AppClock.today()
        badges.addView(ui.badge(if(overdue)"Aperto · scaduto"else l.advanceStatus(a),overdue));if(l.recovered(a.id)>0 && l.remaining(a)>0)badges.addView(ui.badge("Rimborso parziale"),LinearLayout.LayoutParams(-2,-2).apply {if(!large)marginStart=ui.dp(5)else topMargin=ui.dp(5)})
        outer.addView(badges);outer.isFocusable=true;outer.contentDescription=a.description;outer.setOnClickListener {openCredit(a)};body.addView(outer);body.addView(ui.divider())
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
