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
internal fun MainActivity.movements(body:LinearLayout) {
        val search=ui.input("Cerca nei movimenti",moveQuery).apply {contentDescription="Cerca movimenti";textSize=14f;background=ui.shape(ui.colors.hero,22).apply {setStroke(ui.dp(1),ui.colors.line)};setCompoundDrawablesWithIntrinsicBounds(SearchIcon(ui.colors.cool,ui.dp(18)),null,null,null);compoundDrawablePadding=ui.dp(10)};body.addView(search)
        val filters=ui.row()
        filters.addView(ui.button("Filtri"){sheet("Mostra movimenti",listOf("Tutti" to {moveFilter=0;render()},"Effettuati" to {moveFilter=1;render()},"Programmati" to {moveFilter=2;render()},"Entrate" to {moveFilter=3;render()},"Spese" to {moveFilter=4;render()},"Anticipi e rimborsi" to {moveFilter=5;render()}))}.apply {textSize=12f;setTextColor(ui.colors.cool);gravity=Gravity.START},LinearLayout.LayoutParams(0,-2,1f))
        filters.addView(ui.button("Categoria"){pickingFilter=true;categoryQuery="";categoryTab=2;go(21)}.apply {textSize=12f;setTextColor(ui.colors.cool)},LinearLayout.LayoutParams(0,-2,1f));body.addView(filters)
        if(moveFilter!=0 || categoryFilter!=null)body.addView(ui.button("${listOf("Tutti","Effettuati","Programmati","Entrate","Spese","Anticipi e rimborsi")[moveFilter]}${categoryFilter?.let {" · ${l.categoryName(it)}"} ?: ""}  ×"){moveFilter=0;categoryFilter=null;render()}.apply {contentDescription="Rimuovi filtri movimenti";textSize=11f;background=ui.shape(ui.colors.hero,16);gravity=Gravity.START})
        val rows=ui.column();body.addView(rows)
        fun update() {
            rows.removeAllViews()
            val personal=l.operations.filter {it.status!=Status.DELETED && (categoryFilter==null || it.category==categoryFilter) && when(moveFilter){1->it.status==Status.ACTUAL;2->it.status==Status.PLANNED;3->it.kind==Kind.INCOME;4->it.kind==Kind.EXPENSE;5->false;else->true}}.map {o->HistoryItem(o.date,l.operationTitle(o),"${l.categoryName(o.category)} · ${if(o.status==Status.ACTUAL)"effettuato" else "programmato"}",o.signed,FinancialState.label(o),l.categories.first {it.id==o.category}.icon){manage(o)}}
            val credit=if(moveFilter in listOf(0,1,5) && categoryFilter==null)l.advances.flatMap {a->listOf(HistoryItem(a.date,"Anticipo · ${a.description}","${a.debtor} · credito da recuperare",-a.cents,"Anticipo versato","advance"){openCredit(a)})+l.refunds.filter {it.advance==a.id}.map {r->HistoryItem(r.date,"Rimborso · ${a.description}","${a.debtor} · restituzione, non guadagno",r.cents,"Rimborso ricevuto","income"){openCredit(a)}}}else emptyList()
            val found=(personal+credit).filter {moveQuery.isBlank() || (it.title+" "+it.subtitle).contains(moveQuery,true)}.sortedByDescending {it.date}
            rows.addView(ui.label("${found.size} movimenti · ${listOf("tutti","effettuati","programmati","entrate","spese","anticipi")[moveFilter]}",12f,ui.colors.muted))
            if(found.isEmpty())ui.empty(rows,"Nessun movimento","Registra una spesa o un incasso per iniziare.")
            var day:LocalDate?=null
            found.take(300).forEach {item->if(day!=item.date){rows.addView(ui.label(item.date.format(dates),11f,ui.colors.muted,true).apply {setPadding(0,ui.dp(18),0,ui.dp(8))});day=item.date};val row=ui.row().apply {gravity=Gravity.CENTER_VERTICAL};row.addView(View(this).apply {background=ui.symbol(item.icon,36)},LinearLayout.LayoutParams(ui.dp(36),ui.dp(36)).apply {marginEnd=ui.dp(10)});row.addView(FinancialRows.row(ui,item.title,item.subtitle,item.state,(if(item.signed<0)"− " else "+ ")+DisplayMoney.amount(kotlin.math.abs(item.signed)),if(item.signed<0)ui.colors.coral else ui.colors.sage,item.action),LinearLayout.LayoutParams(0,-2,1f));rows.addView(row);rows.addView(ui.space(5))}
            if(found.size>300)rows.addView(ui.label("Mostrati 300 risultati: restringi i filtri.",12f,ui.colors.muted))
        };watch(search){moveQuery=it;update()};update()
    }

internal fun MainActivity.commitments(body:LinearLayout) {
    body.addView(ui.label("Scadenze da pagare, incassi e restituzioni attese. Gli importi previsti non sono liquidità.",13f,ui.colors.muted))
    body.addView(ui.space(12));body.addView(ui.segments(listOf("Elenco","Calendario"),if(calendarGrid)1 else 0){calendarGrid=it==1;selectedDay=null;render()});monthControls(body)
    if(month!=AppClock.month())body.addView(ui.button("Torna al mese corrente"){month=AppClock.month();selectedDay=null;render()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
    val shown=l.pending.filter {if(calendarGrid && selectedDay!=null)it.date==selectedDay else YearMonth.from(it.date)==month}
    ui.pair(body,"Pagamenti mostrati",Money.format(shown.filter {it.kind==Kind.EXPENSE}.sumOf {it.cents}),"Incassi attesi",Money.format(shown.filter {it.kind==Kind.INCOME}.sumOf {it.cents}))
    if(calendarGrid)calendar(body)
    val overdue=if(selectedDay==null)l.pending.filter {it.kind==Kind.EXPENSE && it.date<AppClock.today() && YearMonth.from(it.date)!=month}else emptyList()
    if(overdue.isNotEmpty()){body.addView(ui.section("Arretrati"));overdue.forEach {operationRow(body,it)}}
    if(shown.isEmpty() && overdue.isEmpty())ui.empty(body,"Nessun impegno","Qui trovi ciò che devi ancora pagare o incassare.")
    listOf(Kind.EXPENSE to "Da pagare",Kind.INCOME to "Incassi attesi").forEach {(kind,label)->val items=shown.filter {it.kind==kind};if(items.isNotEmpty()){body.addView(ui.section(label));items.take(300).forEach {operationRow(body,it)}}}
    val expected=l.advances.filter {l.remaining(it)>0 && it.due!=null && (if(calendarGrid && selectedDay!=null)it.due==selectedDay else YearMonth.from(it.due)==month)}
    if(expected.isNotEmpty()){body.addView(ui.section("Restituzioni attese · non garantite"));expected.forEach {a->creditRow(body,a)}}
    if(l.series.any {it.active})body.addView(ui.section("Programmazioni"))
    l.series.filter {it.active}.forEach {plan->body.addView(ui.listRow(plan.title,"${frequencyName(plan.frequency)} · ${if(plan.count==0)"continua"else "${plan.count} rate"}",Money.format(plan.totalCents ?: plan.cents)){seriesMenu(plan)})}
    body.addView(ui.button("Nuovo impegno",true){createMenu(planned=true)})
    if(l.pending.any {it.kind==Kind.EXPENSE}) {
        body.addView(ui.button("Andamento previsto"){commitmentForecast=!commitmentForecast;render()})
        if(commitmentForecast){body.addView(CashTimeline(this,ui.colors,l),LinearLayout.LayoutParams(-1,ui.dp(116)));body.addView(ui.label("Saldo dopo le uscite note. Entrate attese e crediti esclusi; riserva tratteggiata.",12f,ui.colors.muted))}
    }
}

internal fun MainActivity.operationRow(body:LinearLayout,o:Operation) {body.addView(FinancialRows.row(ui,l.operationTitle(o),"${o.date.format(dates)} · ${l.categoryName(o.category)}${if(o.uncertain)" · non garantita" else ""}",FinancialState.label(o),(if(o.kind==Kind.EXPENSE)"− " else "+ ")+Money.format(o.cents),if(o.kind==Kind.EXPENSE)ui.colors.coral else ui.colors.sage){manage(o)});body.addView(ui.space(4))}

internal fun MainActivity.calendar(body:LinearLayout) {
        body.addView(ui.button("Oggi"){month=AppClock.month();selectedDay=AppClock.today();render();dayDetails(AppClock.today())}.apply {textSize=12f;setTextColor(ui.colors.cool)})
        val week=ui.row();listOf("L","M","M","G","V","S","D").forEach {name->week.addView(ui.label(name,11f,ui.colors.muted,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,ui.dp(28),1f))};body.addView(week)
        val offset=month.atDay(1).dayOfWeek.value-1;val count=((offset+month.lengthOfMonth()+6)/7)*7;var row=ui.row()
        for(i in 0 until count){if(i%7==0){row=ui.row();body.addView(row)};val day=i-offset+1
            if(day !in 1..month.lengthOfMonth()){row.addView(View(this),LinearLayout.LayoutParams(0,ui.dp(44),1f));continue}
            val date=month.atDay(day);val has=l.pending.any {it.date==date};val credit=l.advances.any {it.due==date && l.remaining(it)>0};val selected=date==selectedDay
            val cell=object:Button(this,null,0){private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);if(has || credit){paint.color=if(has)ui.colors.sage else ui.colors.chartCool;c.drawCircle(width/2f,height-ui.dp(5).toFloat(),ui.dp(2).toFloat(),paint)}}}.apply {
                ui.flatten(this);text=day.toString();textSize=13f;includeFontPadding=false;gravity=Gravity.CENTER;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(0,0,0,ui.dp(4));maxLines=1
                background=ui.shape(if(selected)ui.colors.primary else if(date==AppClock.today())ui.colors.hero else Color.TRANSPARENT,22);setTextColor(if(selected)ui.colors.onPrimary else ui.colors.text);isSelected=selected;contentDescription="${date.format(dates)}${if(has) ", impegni" else ""}${if(credit) ", restituzione attesa" else ""}"
                setOnClickListener {selectedDay=date;render();dayDetails(date)}
            };row.addView(cell,LinearLayout.LayoutParams(0,ui.dp(44),1f))
        };body.addView(ui.space(8));if(selectedDay!=null)body.addView(ui.button("Tutto il mese"){selectedDay=null;render()})
    }

internal fun MainActivity.dayDetails(date:LocalDate) {
        val payments=l.pending.filter {it.date==date && it.kind==Kind.EXPENSE};val incomes=l.pending.filter {it.date==date && it.kind==Kind.INCOME};val refunds=l.advances.filter {it.due==date && l.remaining(it)>0}
        val actions=mutableListOf<Pair<String,()->Unit>>()
        payments.forEach {o->actions.add("Pagamento · ${l.operationTitle(o)} · ${Money.format(o.cents)}" to {manage(o)})}
        incomes.forEach {o->actions.add("Incasso previsto · ${l.operationTitle(o)} · ${Money.format(o.cents)}" to {manage(o)})}
        refunds.forEach {a->actions.add("Restituzione attesa · ${a.description} · ${Money.format(l.remaining(a))}" to {openCredit(a)})}
        actions.add("Nuova scadenza in questa data" to {openForm("operation",defaults=mapOf("kind" to "EXPENSE","status" to "PLANNED","date" to date.toString()))})
        sheet(date.format(dates),actions,"${payments.size} pagamenti · ${incomes.size} incassi previsti · ${refunds.size} restituzioni. Nessuna registrazione automatica.")
    }

internal fun MainActivity.frequencyName(f:Frequency)=when(f){Frequency.WEEKLY->"Ogni settimana";Frequency.MONTHLY->"Ogni mese";Frequency.YEARLY->"Ogni anno"}
