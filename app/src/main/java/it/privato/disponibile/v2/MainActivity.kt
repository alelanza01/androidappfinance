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
import java.io.ByteArrayOutputStream
import org.json.JSONObject

/** Four daily destinations; complex edits are pages, short decisions are bottom sheets. */
class MainActivity:Activity() {
    private lateinit var store:Store
    private lateinit var l:Ledger
    private lateinit var ui:Design
    private var page=0
    private var previous=0
    private var demo=false
    private var month=YearMonth.now()
    private var chartMode=0
    private var period=6
    private var calendarGrid=false
    private var selectedDay:LocalDate?=null
    private var moveQuery=""
    private var creditQuery=""
    private var moveFilter=1
    private var categoryFilter:Long?=null
    private var creditSubject=""
    private var creditProject=""
    private var creditStatus=0
    private var creditMonth=false
    private var detailId=0L
    private var exportPrevious=false
    private var dialog:Dialog?=null
    private var draft:Draft?=null
    private var saveDraft:(()->Unit)?=null
    private val dates=DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ITALY)
    private data class Draft(val type:String,val id:Long=0,val values:MutableMap<String,String> = mutableMapOf()) {
        fun json()=JSONObject().put("type",type).put("id",id).put("values",JSONObject(values)).toString()
        companion object {fun parse(text:String):Draft {val j=JSONObject(text);val v=j.getJSONObject("values");return Draft(j.getString("type"),j.getLong("id"),v.keys().asSequence().associateWith {v.getString(it)}.toMutableMap())}}
    }
    override fun onCreate(state:Bundle?) {
        store=Store(this)
        val palette=Palette.of(this,store.snapshot().settings.theme)
        setTheme(if(palette.dark)R.style.AppThemeDark else R.style.AppTheme)
        super.onCreate(state)
        page=state?.getInt("page") ?: 0;previous=state?.getInt("previous") ?: 0;demo=state?.getBoolean("demo") ?: false
        month=state?.getString("month")?.let {YearMonth.parse(it)} ?: YearMonth.now()
        chartMode=state?.getInt("chartMode") ?: 0;calendarGrid=state?.getBoolean("grid") ?: false
        detailId=state?.getLong("detailId") ?: 0;draft=state?.getString("draft")?.let {Draft.parse(it)}
        exportPrevious=state?.getBoolean("exportPrevious") ?: false
        if(android.os.Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false)
        window.setSoftInputMode(if(android.os.Build.VERSION.SDK_INT>=30)WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        render()
    }
    override fun onSaveInstanceState(s:Bundle) {
        super.onSaveInstanceState(s);s.putInt("page",page);s.putInt("previous",previous);s.putBoolean("demo",demo);s.putString("month",month.toString());s.putInt("chartMode",chartMode);s.putBoolean("grid",calendarGrid);s.putLong("detailId",detailId);draft?.let {s.putString("draft",it.json())};s.putBoolean("exportPrevious",exportPrevious)
    }
    override fun onDestroy(){dialog?.dismiss();store.close();super.onDestroy()}
    @Deprecated("Android 8 compatible back navigation") override fun onBackPressed() {
        if(page==20) {sheet("Uscire dall'inserimento?",listOf("Conferma" to {draft=null;go(previous)}),"Le informazioni non salvate verranno scartate.")}
        else if(page==13)go(3) else if(page>=4)go(if(page in 7..11)6 else 0) else if(page!=0)go(0) else super.onBackPressed()
    }
    private fun go(destination:Int){hideKeyboard();dialog?.dismiss();page=destination;render()}
    private fun hideKeyboard(){currentFocus?.let {getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(it.windowToken,0)}}
    private fun fail(e:Exception) {sheet("Controlla i dati",listOf("Ho capito" to {}),e.message ?: "Operazione non completata: i dati precedenti sono conservati.")}
    private fun change(action:()->Unit) {
        if(demo){Toast.makeText(this,"La demo non modifica i tuoi dati.",Toast.LENGTH_SHORT).show();return}
        try {action();render();Toast.makeText(this,"Aggiornato",Toast.LENGTH_SHORT).show()}catch(e:Exception){fail(e)}
    }
    private fun sheet(title:String,actions:List<Pair<String,()->Unit>>,message:String="") {
        dialog?.dismiss()
        val d=Dialog(this);val body=ui.column().apply {setPadding(ui.dp(24),ui.dp(18),ui.dp(24),ui.dp(16));background=ui.shape(ui.colors.card,20)}
        body.addView(View(this).apply {background=ui.shape(ui.colors.line,2);layoutParams=LinearLayout.LayoutParams(ui.dp(34),ui.dp(4)).apply {gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=ui.dp(14)}})
        body.addView(ui.label(title,22f,ui.colors.text,true));if(message.isNotBlank())body.addView(ui.label(message,14f,ui.colors.muted))
        actions.forEach {(name,action)->body.addView(ui.listRow(name){d.dismiss();action()});body.addView(ui.divider())}
        body.addView(ui.button("Chiudi") {d.dismiss()});d.setContentView(ScrollView(this).apply {addView(body)})
        dialog=d;d.show();d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,-2);setGravity(Gravity.BOTTOM);addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);setDimAmount(.4f);setWindowAnimations(android.R.style.Animation_Dialog)}
    }
    private fun confirm(title:String,message:String,action:()->Unit)=sheet(title,listOf("Conferma" to {change(action)}),message)
    private fun render() {
        l=if(demo)Demo.ledger(store.snapshot().settings.theme) else store.snapshot()
        ui=Design(this,Palette.of(this,l.settings.theme));val c=ui.colors
        window.statusBarColor=c.background;window.navigationBarColor=c.background
        window.decorView.systemUiVisibility=if(c.dark)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if(android.os.Build.VERSION.SDK_INT>=27)View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        val root=ui.column().apply {setBackgroundColor(c.background);isFocusableInTouchMode=true}
        root.setOnApplyWindowInsetsListener {v,insets->
            if(android.os.Build.VERSION.SDK_INT>=30){val i=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom)}
            else v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        val titles=mapOf(0 to "Posso spenderlo?",1 to "Movimenti",2 to "Impegni",3 to "Crediti",4 to "Analisi",5 to "Simulatore",6 to "Impostazioni",7 to "Regole del saldo",8 to "Aspetto",9 to "Categorie",10 to "Backup",11 to "Avvisi",12 to "Come si calcola",13 to "Anticipo",20 to formTitle())
        if(demo)root.addView(ui.label("DEMO · DATI DIMOSTRATIVI",10f,c.muted,true).apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(12).toFloat(),10f*resources.displayMetrics.scaledDensity));setPadding(ui.dp(24),ui.dp(6),ui.dp(24),0)})
        val header=ui.row().apply {setPadding(ui.dp(20),ui.dp(12),ui.dp(20),ui.dp(12))}
        if(page>=4)header.addView(ui.button("‹"){onBackPressed()}.apply {contentDescription="Indietro"},LinearLayout.LayoutParams(ui.dp(40),ui.dp(48)))
        val name=ui.column()
        name.addView(ui.label(titles[page] ?: "Posso spenderlo?",if(page==0)22f else 24f,c.text,true).apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(if(page==0)23 else 25).toFloat(),(if(page==0)22f else 24f)*resources.displayMetrics.scaledDensity));maxLines=2});header.addView(name,LinearLayout.LayoutParams(0,-2,1f))
        if(page in 0..3)header.addView(ui.button("+"){createMenu(page==2,page==3)}.apply {contentDescription="Aggiungi";textSize=26f},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)))
        if(page==0)header.addView(ui.button(""){go(6)}.apply {setCompoundDrawablesWithIntrinsicBounds(NavIcon(6,c.muted,ui.dp(22)),null,null,null);contentDescription="Impostazioni"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)))
        root.addView(header)
        val scroll=ScrollView(this).apply {isFillViewport=false;clipToPadding=true}
        val body=ui.column().apply {setPadding(ui.dp(24),0,ui.dp(24),ui.dp(24))}
        when(page){0->home(body);1->movements(body);2->commitments(body);3->credits(body);4->analysis(body);5->simulator(body);6->settings(body);7->rules(body);8->appearance(body);9->categories(body);10->backups(body);11->reminders(body);12->calculation(body);13->creditDetail(body);20->form(body)}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(page==20)root.addView(ui.button(if(draft?.type=="creditFilter")"Applica filtri" else "Salva",true){
            if(page!=20 || draft==null)return@button
            if(demo && draft?.type!="creditFilter"){Toast.makeText(this,"Esci dalla demo per salvare.",Toast.LENGTH_SHORT).show();return@button}
            try {saveDraft?.invoke();draft=null;hideKeyboard();page=previous;render();Toast.makeText(this,"Salvato",Toast.LENGTH_SHORT).show()}catch(e:Exception){fail(e)}
        }.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(12))}})
        if(page in 0..3) {
            root.addView(ui.divider());val nav=ui.row().apply {setPadding(ui.dp(8),ui.dp(6),ui.dp(8),ui.dp(6))}
            listOf("Home","Movimenti","Impegni","Crediti").forEachIndexed {i,title->nav.addView(ui.button(title){go(i)}.apply {
                textSize=11f;setTextColor(if(page==i)c.text else c.muted);isSelected=page==i
                setCompoundDrawablesWithIntrinsicBounds(null,NavIcon(listOf(0,3,2,5)[i],if(page==i)c.sage else c.muted,ui.dp(22)),null,null);compoundDrawablePadding=ui.dp(5)
                background=ui.shape(if(page==i)c.card else Color.TRANSPARENT,12);maxLines=1;setPadding(ui.dp(3),ui.dp(6),ui.dp(3),ui.dp(6))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(13).toFloat(),11f*resources.displayMetrics.scaledDensity));ellipsize=android.text.TextUtils.TruncateAt.END;contentDescription="$title${if(page==i) ", sezione attiva" else ""}"
            },LinearLayout.LayoutParams(0,-2,1f))};root.addView(nav)
        }
        setContentView(root);root.requestApplyInsets()
    }
    private fun home(body:LinearLayout) {
        body.addView(ui.space(12));body.addView(ui.label("DISPONIBILE DA SPENDERE",11f,ui.colors.muted,true).apply {letterSpacing=.08f})
        body.addView(ui.label(Money.format(l.spendable),43f,if(l.spendable<0)ui.colors.coral else ui.colors.text,true).apply {setAutoSizeTextTypeUniformWithConfiguration(14,43,1,android.util.TypedValue.COMPLEX_UNIT_SP);maxLines=1})
        body.addView(ui.label("Fino al ${l.settings.horizon.format(dates)} · impegni e riserva già sottratti",13f,ui.colors.muted))
        body.addView(ui.button("Dettaglio del calcolo"){go(12)}.apply {gravity=Gravity.START;setPadding(0,ui.dp(8),0,ui.dp(8))})
        if(l.spendable<0)body.addView(ui.label("Mancano ${Money.format(-l.spendable)} per coprire gli impegni e proteggere la riserva.",14f,ui.colors.coral))
        body.addView(ui.space(8));val actions=ui.row()
        actions.addView(ui.button("Simula spesa"){go(5)}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity));background=ui.shape(ui.colors.card)},LinearLayout.LayoutParams(0,-2,1f).apply {marginEnd=ui.dp(8)})
        actions.addView(ui.button("Analisi"){go(4)}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity));background=ui.shape(ui.colors.card)},LinearLayout.LayoutParams(0,-2,1f));body.addView(actions)
        body.addView(ui.section("Da tenere d'occhio"))
        val next=l.pending.firstOrNull {it.kind==Kind.EXPENSE}
        body.addView(ui.listRow(next?.title ?: "Nessun pagamento previsto",next?.date?.format(dates) ?: "Aggiungi i prossimi impegni",next?.let {Money.format(it.cents)} ?: "",ui.colors.coral){if(next==null)go(2)else manage(next)});body.addView(ui.divider())
        body.addView(ui.listRow("Da recuperare","${l.advances.count {l.remaining(it)>0}} anticipi aperti",Money.format(l.outstanding),ui.colors.sage){go(3)})
        body.addView(ui.space(16));body.addView(ui.button("Registra",true){createMenu()})
        if(demo)body.addView(ui.button("Esci dalla demo"){demo=false;go(0)})
    }
    private fun calculation(body:LinearLayout) {
        body.addView(ui.label("Solo denaro realmente disponibile",22f,ui.colors.text,true));ui.metric(body,"Liquidità",Money.format(l.balance));ui.metric(body,"− Impegni entro la previsione",Money.format(l.commitments),ui.colors.coral);ui.metric(body,"− Riserva protetta",Money.format(l.settings.reserve));body.addView(ui.divider());ui.metric(body,"Puoi spendere",Money.format(l.spendable),ui.colors.sage)
        body.addView(ui.section("La regola"));body.addView(ui.label("Il saldo include soltanto incassi, pagamenti, anticipi e rimborsi già effettuati. I crediti aperti e le entrate previste non sono denaro spendibile.",15f,ui.colors.muted))
        body.addView(ui.listRow("Aggiorna liquidità","Riconcilia il saldo reale senza inventare guadagni"){moneyForm("reconcile",l.balance)})
        body.addView(ui.listRow("Riserva e previsione","Modifica le regole del calcolo"){go(7)})
    }
    private fun simulator(body:LinearLayout) {
        body.addView(ui.label("Quanto vorresti spendere?",22f,ui.colors.text,true));val input=ui.field(body,"Importo in euro",money=true).apply {textSize=30f};val result=ui.label("Inserisci un importo",25f,ui.colors.muted,true);body.addView(ui.space(20));body.addView(result)
        body.addView(ui.label("Questa prova non registra movimenti né modifica il saldo.",14f,ui.colors.muted))
        watch(input){value->if(value.isBlank()){result.text="Inserisci un importo";input.error=null}else try {val n=l.simulated(Money.parse(value));result.text="Resterebbero ${Money.format(n)}";result.setTextColor(if(n<0)ui.colors.coral else ui.colors.sage);input.error=null}catch(e:Exception){input.error=e.message;result.text="Controlla l'importo"}}
    }
    private fun monthControls(body:LinearLayout) {
        val row=ui.row();row.addView(ui.button("‹"){month=month.minusMonths(1);selectedDay=null;render()}.apply {contentDescription="Mese precedente"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)))
        row.addView(ui.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ITALY)),15f,ui.colors.text,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f))
        row.addView(ui.button("›"){month=month.plusMonths(1);selectedDay=null;render()}.apply {contentDescription="Mese successivo"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)));body.addView(row)
    }
    private fun analysis(body:LinearLayout) {
        body.addView(ui.segments(listOf("Spese","Liquidità","Mesi"),chartMode){chartMode=it;render()});body.addView(ui.space(12))
        if(chartMode!=1)monthControls(body)
        val title=listOf("Dove va il denaro","Saldo e scenari","Entrate e spese")[chartMode]
        body.addView(ui.label(title,21f,ui.colors.text,true))
        val detail=ui.label("Tocca il grafico per un dettaglio",13f,ui.colors.muted)
        val view=Charts(this,ui.colors,l,chartMode,month,period){detail.text=it};view.contentDescription=title;view.isFocusable=true
        body.addView(view,LinearLayout.LayoutParams(-1,ui.dp(250)));body.addView(detail)
        if(chartMode==0) {
            val entries=l.categories.map {it to l.actual.filter {o->o.kind==Kind.EXPENSE && YearMonth.from(o.date)==month && o.category==it.id}.sumOf {o->o.cents}}.filter {it.second>0}
            entries.forEach {(cat,n)->body.addView(ui.listRow(cat.name,"Spese personali",Money.format(n),ui.colors.text){detail.text="${cat.name} · ${Money.format(n)}"});body.addView(ui.divider())}
        } else if(chartMode==1) {
            body.addView(ui.label("Verde: saldo reale · Corallo: uscite note · Oro: incluse entrate attese",12f,ui.colors.muted))
            body.addView(ui.button("Leggere la previsione"){sheet("Scenari, non disponibilità",listOf("Ho capito" to {}),"Il saldo storico comprende anticipi e rimborsi. Le linee tratteggiate sono ipotesi future: il corallo sottrae gli impegni, l'oro aggiunge entrate previste, anche non garantite. Le restituzioni attese non sono incluse automaticamente.")})
        } else {body.addView(ui.segments(listOf("3 mesi","6 mesi","12 mesi"),listOf(3,6,12).indexOf(period)){period=listOf(3,6,12)[it];render()});body.addView(ui.label("Verde: guadagni · Corallo: spese personali. Rettifiche, anticipi e rimborsi esclusi.",12f,ui.colors.muted))}
    }
    private data class HistoryItem(val date:LocalDate,val title:String,val subtitle:String,val signed:Long,val action:()->Unit)
    private fun movements(body:LinearLayout) {
        val search=ui.input("Cerca movimenti",moveQuery);body.addView(search)
        val filters=ui.row();filters.addView(ui.button("Filtri"){sheet("Mostra movimenti",listOf("Tutti" to {moveFilter=0;render()},"Effettuati" to {moveFilter=1;render()},"Programmati" to {moveFilter=2;render()},"Entrate" to {moveFilter=3;render()},"Spese" to {moveFilter=4;render()},"Anticipi e rimborsi" to {moveFilter=5;render()}))}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity))},LinearLayout.LayoutParams(0,-2,1f))
        filters.addView(ui.button("Categoria"){sheet("Categoria",listOf("Tutte le categorie" to {categoryFilter=null;render()})+l.categories.map {it.name to {categoryFilter=it.id;render()}})}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity))},LinearLayout.LayoutParams(0,-2,1f));body.addView(filters)
        val rows=ui.column();body.addView(rows)
        fun update() {
            rows.removeAllViews()
            val personal=l.operations.filter {it.status!=Status.DELETED && (categoryFilter==null || it.category==categoryFilter) && when(moveFilter){1->it.status==Status.ACTUAL;2->it.status==Status.PLANNED;3->it.kind==Kind.INCOME;4->it.kind==Kind.EXPENSE;5->false;else->true}}.map {o->HistoryItem(o.date,o.title,"${l.categoryName(o.category)} · ${if(o.status==Status.ACTUAL)"effettuato" else "programmato"}",o.signed){manage(o)}}
            val credit=if(moveFilter in listOf(0,1,5) && categoryFilter==null)l.advances.flatMap {a->listOf(HistoryItem(a.date,"Anticipo · ${a.description}","${a.debtor} · credito da recuperare",-a.cents){openCredit(a)})+l.refunds.filter {it.advance==a.id}.map {r->HistoryItem(r.date,"Rimborso · ${a.description}","${a.debtor} · restituzione, non guadagno",r.cents){openCredit(a)}}}else emptyList()
            val found=(personal+credit).filter {moveQuery.isBlank() || (it.title+" "+it.subtitle).contains(moveQuery,true)}.sortedByDescending {it.date}
            rows.addView(ui.label("${found.size} movimenti · ${listOf("tutti","effettuati","programmati","entrate","spese","anticipi")[moveFilter]}",12f,ui.colors.muted))
            if(found.isEmpty())ui.empty(rows,"Nessun movimento","Registra una spesa o un incasso per iniziare.")
            var day:LocalDate?=null
            found.take(300).forEach {item->if(day!=item.date){rows.addView(ui.section(item.date.format(dates)));day=item.date};rows.addView(ui.listRow(item.title,item.subtitle,(if(item.signed<0)"− " else "+ ")+Money.format(kotlin.math.abs(item.signed)),if(item.signed<0)ui.colors.coral else ui.colors.sage,item.action));rows.addView(ui.divider())}
            if(found.size>300)rows.addView(ui.label("Mostrati 300 risultati: restringi i filtri.",12f,ui.colors.muted))
        };watch(search){moveQuery=it;update()};update()
    }
    private fun commitments(body:LinearLayout) {
        body.addView(ui.segments(listOf("Lista","Calendario"),if(calendarGrid)1 else 0){calendarGrid=it==1;selectedDay=null;render()});monthControls(body)
        if(calendarGrid)calendar(body)
        val found=l.pending.filter {if(selectedDay!=null)it.date==selectedDay else YearMonth.from(it.date)==month}.sortedBy {it.date}
        val overdue=if(selectedDay==null)l.pending.filter {it.kind==Kind.EXPENSE && it.date<LocalDate.now() && YearMonth.from(it.date)!=month}else emptyList()
        if(found.isEmpty() && overdue.isEmpty())ui.empty(body,"Nessun impegno","Qui trovi ciò che devi ancora pagare o incassare.")
        if(overdue.isNotEmpty()){body.addView(ui.section("Arretrati"));overdue.forEach {operationRow(body,it)}}
        var day:LocalDate?=null
        found.take(300).forEach {o->if(day!=o.date){body.addView(ui.section(o.date.format(dates)));day=o.date};operationRow(body,o)}
        val expected=l.advances.filter {l.remaining(it)>0 && it.due!=null && (if(selectedDay!=null)it.due==selectedDay else YearMonth.from(it.due)==month)}
        if(expected.isNotEmpty()){body.addView(ui.section("Restituzioni attese · non garantite"));expected.forEach {a->creditRow(body,a)}}
        body.addView(ui.section("Programmazioni"))
        l.series.filter {it.active}.forEach {s->body.addView(ui.listRow(s.title,"${frequencyName(s.frequency)} · ${if(s.count==0)"continua" else "${s.count} rate"}",Money.format(s.cents)){seriesMenu(s)})}
        body.addView(ui.button("Nuovo impegno",true){createMenu(planned=true)})
    }
    private fun operationRow(body:LinearLayout,o:Operation) {body.addView(ui.listRow(o.title,"${l.categoryName(o.category)}${if(o.uncertain)" · non garantita" else ""}",(if(o.kind==Kind.EXPENSE)"− " else "+ ")+Money.format(o.cents),if(o.kind==Kind.EXPENSE)ui.colors.coral else ui.colors.sage){manage(o)});body.addView(ui.divider())}
    private fun calendar(body:LinearLayout) {
        val week=ui.row();listOf("L","M","M","G","V","S","D").forEach {name->week.addView(ui.label(name,11f,ui.colors.muted,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,ui.dp(28),1f))};body.addView(week)
        val offset=month.atDay(1).dayOfWeek.value-1;val count=((offset+month.lengthOfMonth()+6)/7)*7;var row=ui.row()
        for(i in 0 until count){if(i%7==0){row=ui.row();body.addView(row)};val day=i-offset+1
            if(day !in 1..month.lengthOfMonth()){row.addView(View(this),LinearLayout.LayoutParams(0,ui.dp(44),1f));continue}
            val date=month.atDay(day);val has=l.pending.any {it.date==date};val credit=l.advances.any {it.due==date && l.remaining(it)>0};val selected=date==selectedDay
            val cell=object:Button(this){private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);if(has || credit){paint.color=if(has)ui.colors.sage else ui.colors.chartGold;c.drawCircle(width/2f,height-ui.dp(5).toFloat(),ui.dp(2).toFloat(),paint)}}}.apply {
                text=day.toString();textSize=13f;includeFontPadding=false;gravity=Gravity.CENTER;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(0,0,0,ui.dp(4));maxLines=1
                background=ui.shape(if(selected)ui.colors.card else Color.TRANSPARENT,8);setTextColor(if(selected)ui.colors.sage else ui.colors.text);isSelected=selected;contentDescription="${date.format(dates)}${if(has) ", impegni" else ""}${if(credit) ", restituzione attesa" else ""}"
                setOnClickListener {selectedDay=if(selected)null else date;render()}
            };row.addView(cell,LinearLayout.LayoutParams(0,ui.dp(44),1f))
        };body.addView(ui.space(8));if(selectedDay!=null)body.addView(ui.button("Tutto il mese"){selectedDay=null;render()})
    }
    private fun frequencyName(f:Frequency)=when(f){Frequency.WEEKLY->"Ogni settimana";Frequency.MONTHLY->"Ogni mese";Frequency.YEARLY->"Ogni anno"}
    private fun credits(body:LinearLayout) {
        body.addView(ui.label("ANCORA DA RECUPERARE",11f,ui.colors.muted,true));body.addView(ui.amount(Money.format(l.outstanding)))
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
            if(found.isEmpty())ui.empty(rows,"Nessun credito","Registra il denaro che anticipi per conto di altri. Non sarà confuso con una spesa personale.")
            found.take(300).forEach {creditRow(rows,it)}
            if(found.size>300)rows.addView(ui.label("Mostrati 300 risultati: restringi i filtri.",12f,ui.colors.muted))
        };watch(search){creditQuery=it;update()};update();body.addView(ui.space(12));body.addView(ui.button("Nuovo anticipo",true){openForm("advance")})
    }
    private fun creditFilters()=openForm("creditFilter",defaults=mapOf("subject" to creditSubject,"project" to creditProject,"state" to creditStatus.toString(),"month" to creditMonth.toString()))
    private fun creditRow(body:LinearLayout,a:Advance) {
        body.addView(ui.listRow(a.description,"${a.debtor}${if(a.project.isNotBlank()) " · ${a.project}" else ""}\n${l.advanceStatus(a)}",Money.format(l.remaining(a)),if(a.due!=null && a.due<LocalDate.now() && l.remaining(a)>0)ui.colors.coral else ui.colors.text){openCredit(a)});body.addView(ui.divider())
    }
    private fun openCredit(a:Advance){detailId=a.id;go(13)}
    private fun creditDetail(body:LinearLayout) {
        val a=l.advances.firstOrNull {it.id==detailId} ?: run {ui.empty(body,"Anticipo non disponibile","La registrazione è stata eliminata.");return}
        body.addView(ui.label(a.description,21f,ui.colors.text,true));body.addView(ui.label(a.debtor,14f,ui.colors.muted));body.addView(ui.section(l.advanceStatus(a)));body.addView(ui.amount(Money.format(l.remaining(a))));body.addView(ui.label("Da recuperare",13f,ui.colors.muted))
        if(l.remaining(a)>0)body.addView(ui.button("Registra rimborso",true){openForm("refund",a.id)})
        body.addView(ui.section("Dettagli"));ui.metric(body,"Anticipato",Money.format(a.cents));ui.metric(body,"Restituito",Money.format(l.recovered(a.id)),ui.colors.sage)
        body.addView(ui.label("${a.date.format(dates)}${a.due?.let {" · atteso ${it.format(dates)}"} ?: ""}",13f,ui.colors.muted))
        if(a.project.isNotBlank())body.addView(ui.label(a.project,14f,ui.colors.muted));if(a.cause.isNotBlank())body.addView(ui.label(a.cause,14f,ui.colors.muted));if(a.notes.isNotBlank())body.addView(ui.label(a.notes,14f,ui.colors.muted))
        body.addView(ui.section("Rimborsi ricevuti"));val receipts=l.refunds.filter {it.advance==a.id}.sortedByDescending {it.date}
        if(receipts.isEmpty())body.addView(ui.label("Nessun rimborso ricevuto",14f,ui.colors.muted))
        receipts.forEach {r->body.addView(ui.listRow(r.date.format(dates),r.note,Money.format(r.cents),ui.colors.sage){sheet("Rimborso",listOf("Modifica rimborso" to {openForm("refundEdit",r.id)},"Elimina rimborso" to {confirm("Eliminare il rimborso?","La liquidità diminuirà di ${Money.format(r.cents)} e il credito residuo aumenterà della stessa somma."){store.deleteRefund(r.id)}}))});body.addView(ui.divider())}
        body.addView(ui.button("Gestisci anticipo"){sheet("Gestisci anticipo",listOf("Modifica anticipo" to {openForm("advance",a.id)},"Elimina anticipo" to {confirm("Eliminare l'anticipo?","Verranno eliminati anche i suoi rimborsi. Il saldo aumenterà di ${Money.format(l.remaining(a))}. Usa questa azione per correggere una registrazione errata."){store.deleteAdvance(a.id);page=3}}))})
    }
    private fun createMenu(planned:Boolean=false,advance:Boolean=false) {
        if(advance){openForm("advance");return}
        sheet(if(planned)"Programma un impegno" else "Registra",listOf(
            (if(planned)"Spesa prevista" else "Spesa") to {openForm("operation",defaults=mapOf("kind" to "EXPENSE","status" to if(planned)"PLANNED" else "ACTUAL"))},
            (if(planned)"Entrata prevista" else "Entrata") to {openForm("operation",defaults=mapOf("kind" to "INCOME","status" to if(planned)"PLANNED" else "ACTUAL"))},
            "Anticipo" to {openForm("advance")},"Ricorrenza o rate" to {openForm("series")}
        ))
    }
    private fun manage(o:Operation) {
        val actions=mutableListOf<Pair<String,()->Unit>>()
        if(o.status==Status.PLANNED)actions.add((if(o.kind==Kind.INCOME)"Conferma incasso oggi" else "Conferma pagamento oggi") to {confirm("Registrare ${Money.format(o.cents)}?","Il movimento diventerà effettivo oggi e cambierà la liquidità una sola volta."){store.settle(o.id)}})
        actions.add("Modifica" to {openForm("operation",o.id)})
        if(o.series==null)actions.add("Elimina movimento" to {confirm("Eliminare il movimento?",if(o.status==Status.ACTUAL)"Il saldo cambierà di ${Money.format(-o.signed)}. Il movimento verrà rimosso da storico, grafici e backup." else "La previsione sarà aggiornata; la liquidità attuale non cambia."){store.delete(o.id)}})
        else {
            actions.add("Elimina…" to {deleteChoices(o)})
            l.series.firstOrNull {it.id==o.series && it.active}?.let {s->actions.add("Modifica serie" to {openForm("series",s.id)})}
        }
        sheet(o.title,actions,"${o.date.format(dates)} · ${Money.format(o.cents)} · ${if(o.status==Status.ACTUAL)"già effettuato" else "da effettuare"}")
    }
    private fun deleteChoices(o:Operation) {
        sheet("Cosa vuoi eliminare?",listOf(
            "Soltanto questa occorrenza" to {confirm("Eliminare questa occorrenza?",if(o.status==Status.ACTUAL)"È già contabilizzata: il saldo cambierà di ${Money.format(-o.signed)}. Le altre occorrenze resteranno." else "Le altre occorrenze della serie resteranno previste."){store.delete(o.id)}},
            "Questa e tutte le future" to {confirm("Interrompere da qui?","Verrà interrotta la programmazione da questa occorrenza in poi. I movimenti già effettuati, incluso questo se pagato, resteranno nello storico."){store.removeSeries(o.series!!,o.index!!)}},
            "Intera serie" to {stopSeries(o.series!!)}
        ))
    }
    private fun stopSeries(id:Long) {
        val paid=l.actual.filter {it.series==id};val effect=-paid.sumOf {it.signed}
        sheet("Elimina intera serie",listOf(
            "Interrompi, mantieni lo storico" to {confirm("Interrompere la serie?","Tutti gli impegni non effettuati verranno eliminati. ${paid.size} movimenti effettivi resteranno: la liquidità non cambia."){store.removeSeries(id)}},
            "Elimina anche i movimenti effettuati" to {confirm("Rettificare anche lo storico?","Questa è una correzione contabile distinta: eliminerà ${paid.size} movimenti già effettuati. La liquidità passerà da ${Money.format(l.balance)} a ${Money.format(l.balance+effect)}. Conferma solo se quei movimenti erano errati."){store.removeSeries(id,removeActual=true)}}
        ),"L'interruzione della programmazione non deve cancellare pagamenti reali.")
    }
    private fun seriesMenu(s:Series)=sheet(s.title,listOf("Modifica serie" to {openForm("series",s.id)},"Elimina intera serie" to {stopSeries(s.id)}),"${frequencyName(s.frequency)} · ${Money.format(s.cents)}")
    private fun settings(body:LinearLayout) {
        body.addView(ui.section("Preferenze"))
        body.addView(ui.listRow("Regole del saldo","Liquidità iniziale, riserva e previsione"){go(7)});body.addView(ui.divider())
        body.addView(ui.listRow("Aspetto","Tema chiaro, scuro o di sistema"){go(8)});body.addView(ui.divider())
        body.addView(ui.listRow("Avvisi","Promemoria locali facoltativi"){go(11)})
        body.addView(ui.section("Archivio"));body.addView(ui.listRow("Categorie","Organizza le spese e le entrate"){go(9)});body.addView(ui.divider());body.addView(ui.listRow("Backup","Esporta e recupera i tuoi dati"){go(10)})
        body.addView(ui.section("App privata"));body.addView(ui.label("3.0 · offline · nessun account o costo ricorrente",13f,ui.colors.muted));body.addView(ui.button(if(demo)"Esci dalla demo" else "Esplora la demo separata"){demo=!demo;go(0)})
    }
    private fun rules(body:LinearLayout) {
        body.addView(ui.listRow("Liquidità iniziale","Prima dei movimenti registrati",Money.format(l.settings.opening)){moneyForm("opening",l.settings.opening)});body.addView(ui.divider())
        body.addView(ui.listRow("Riserva protetta","Somma da non spendere",Money.format(l.settings.reserve)){moneyForm("reserve",l.settings.reserve)});body.addView(ui.divider())
        body.addView(ui.listRow("Previsione fino al",l.settings.horizon.format(dates)){pick(l.settings.horizon){date->change {store.settings(l.settings.copy(horizon=date))}}})
        body.addView(ui.section("Correggi il saldo di oggi"));body.addView(ui.label("La base iniziale modifica anche lo storico. Per allineare solo la liquidità attuale, registra una rettifica.",14f,ui.colors.muted));body.addView(ui.button("Aggiorna liquidità"){moneyForm("reconcile",l.balance)})
    }
    private fun appearance(body:LinearLayout) {
        body.addView(ui.label("Un tema per ogni momento",22f,ui.colors.text,true));body.addView(ui.space(16))
        body.addView(ui.segments(listOf("Sistema","Chiaro","Scuro"),listOf("system","light","dark").indexOf(l.settings.theme)){i->change {store.settings(l.settings.copy(theme=listOf("system","light","dark")[i]))};if(!demo)recreate()})
        body.addView(ui.space(16));body.addView(ui.label("La dimensione del testo segue le preferenze di accessibilità del telefono.",14f,ui.colors.muted))
    }
    private fun categories(body:LinearLayout) {l.categories.forEach {cat->body.addView(ui.listRow(cat.name,"Categoria",color=ui.colors.text){openForm("category",cat.id)});body.addView(ui.divider())};body.addView(ui.space());body.addView(ui.button("Nuova categoria",true){openForm("category")})}
    private fun backups(body:LinearLayout) {
        body.addView(ui.label("I dati restano tuoi",22f,ui.colors.text,true));body.addView(ui.label("Il backup JSON contiene dati personali e non è cifrato. Scegli una cartella locale sicura. L'importazione verifica il file e conserva prima una copia dell'archivio attuale.",14f,ui.colors.muted));body.addView(ui.space())
        body.addView(ui.button("Esporta backup",true){export(false)});body.addView(ui.button("Importa backup"){if(!demo)startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),2)})
        if(java.io.File(filesDir,"pre-import.json").exists())body.addView(ui.button("Esporta copia precedente all'importazione"){export(true)})
        body.addView(ui.section("Compatibilità"));body.addView(ui.label("Include movimenti, regole, categorie, ricorrenze, anticipi e rimborsi. Accetta i backup V2 e V2.1; le interruzioni delle serie sono conservate nei nuovi backup.",14f,ui.colors.muted))
    }
    private fun reminders(body:LinearLayout) {
        body.addView(ui.label(if(l.settings.reminders)"Avvisi attivi" else "Avvisi disattivati",22f,ui.colors.text,true));body.addView(ui.label("Un riepilogo locale delle spese entro domani e degli arretrati, controllato circa una volta al giorno. Android può ritardarlo per risparmiare batteria: non è un allarme puntuale.",14f,ui.colors.muted));body.addView(ui.space())
        body.addView(ui.button(if(l.settings.reminders)"Disattiva avvisi" else "Attiva avvisi",true){
            if(demo)return@button
            if(!l.settings.reminders && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),3)
            else change {ReminderService.schedule(this,!l.settings.reminders);store.settings(l.settings.copy(reminders=!l.settings.reminders))}
        })
        if(l.settings.reminders && !getSystemService(NotificationManager::class.java).areNotificationsEnabled())body.addView(ui.button("Apri autorizzazioni Android"){startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))})
    }
    private fun formTitle()=when(draft?.type){"operation"->if(draft?.id==0L)"Nuovo movimento" else "Modifica movimento";"series"->if(draft?.id==0L)"Ricorrenza o rate" else "Modifica serie";"advance"->if(draft?.id==0L)"Nuovo anticipo" else "Modifica anticipo";"refund"->"Registra rimborso";"refundEdit"->"Modifica rimborso";"category"->"Categoria";"opening"->"Liquidità iniziale";"reserve"->"Riserva protetta";"reconcile"->"Liquidità attuale";"creditFilter"->"Filtri crediti";else->"Inserimento"}
    private fun openForm(type:String,id:Long=0,defaults:Map<String,String> = emptyMap()) {
        previous=page;val v=mutableMapOf<String,String>();v.putAll(defaults)
        if(type=="operation" && id!=0L)l.operations.first {it.id==id}.let {o->v.putAll(mapOf("amount" to Money.input(o.cents),"description" to o.title,"kind" to o.kind.name,"status" to o.status.name,"date" to o.date.toString(),"category" to o.category.toString(),"uncertain" to o.uncertain.toString()))}
        if(type=="series" && id!=0L)l.series.first {it.id==id}.let {s->v.putAll(mapOf("amount" to Money.input(s.cents),"description" to s.title,"kind" to s.kind.name,"date" to s.start.toString(),"category" to s.category.toString(),"uncertain" to s.uncertain.toString(),"frequency" to s.frequency.name,"count" to if(s.count==0)"" else s.count.toString()))}
        if(type=="advance" && id!=0L)l.advances.first {it.id==id}.let {a->v.putAll(mapOf("amount" to Money.input(a.cents),"description" to a.description,"debtor" to a.debtor,"date" to a.date.toString(),"project" to a.project,"cause" to a.cause,"due" to (a.due?.toString() ?: ""),"notes" to a.notes,"advanced" to "true"))}
        if(type=="refundEdit")l.refunds.first {it.id==id}.let {r->v.putAll(mapOf("amount" to Money.input(r.cents),"date" to r.date.toString(),"notes" to r.note,"advance" to r.advance.toString()))}
        if(type=="refund")v["advance"]=id.toString()
        if(type=="category" && id!=0L)l.categories.first {it.id==id}.let {c->v["description"]=c.name;v["color"]=c.color.toString()}
        draft=Draft(type,id,v);go(20)
    }
    private fun moneyForm(type:String,value:Long)=openForm(type,defaults=mapOf("amount" to Money.input(value)))
    private fun watch(field:EditText,action:(String)->Unit){field.addTextChangedListener(object:TextWatcher {override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){action(s.toString())}})}
    private fun pick(initial:LocalDate,action:(LocalDate)->Unit){DatePickerDialog(this,{_,y,m,d->action(LocalDate.of(y,m+1,d))},initial.year,initial.monthValue-1,initial.dayOfMonth).show()}
    private fun form(body:LinearLayout) {
        val d=draft ?: return
        fun value(key:String,default:String="")=d.values.getOrPut(key){default}
        fun field(key:String,title:String,money:Boolean=false,default:String=""):EditText {
            val e=ui.field(body,title,value(key,default),money);e.tag=key
            if(money && d.type in listOf("opening","reconcile"))e.inputType=e.inputType or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            if(key=="notes"){e.setSingleLine(false);e.maxLines=4;e.filters=arrayOf(android.text.InputFilter.LengthFilter(500))}
            watch(e){d.values[key]=it};return e
        }
        fun option(key:String,title:String,choices:List<Pair<String,String>>,default:String) {
            val selected=value(key,default);val label=choices.firstOrNull {it.first==selected}?.second ?: selected
            body.addView(ui.listRow(title,label){sheet(title,choices.map {(code,name)->name to {d.values[key]=code;render()}})})
        }
        fun date(key:String,title:String,optional:Boolean=false) {
            val text=value(key,if(optional)"" else LocalDate.now().toString())
            body.addView(ui.listRow(title,if(text.isBlank())"Facoltativa" else LocalDate.parse(text).format(dates)){pick(if(text.isBlank())LocalDate.now() else LocalDate.parse(text)){d.values[key]=it.toString();render()}})
            if(optional && text.isNotBlank())body.addView(ui.button("Rimuovi data prevista"){d.values[key]="";render()})
        }
        fun category()=option("category","Categoria",l.categories.map {it.id.toString() to it.name},l.categories.first().id.toString())
        fun advanced(action:()->Unit) {
            body.addView(ui.button(if(value("advanced","false")=="true")"Meno opzioni" else "Opzioni avanzate"){d.values["advanced"]=(value("advanced")=="false").toString();render()})
            if(value("advanced")=="true")action()
        }
        when(d.type) {
            "operation","series"->{
                val series=d.type=="series";val adjustment=value("kind","EXPENSE")=="ADJUSTMENT"
                if(!adjustment)body.addView(ui.segments(listOf("Spesa","Entrata"),if(value("kind","EXPENSE")=="INCOME")1 else 0){d.values["kind"]=if(it==0)"EXPENSE" else "INCOME";if(it==0)d.values["uncertain"]="false";render()})
                val amount=field("amount","Importo in euro",true).apply {textSize=30f}
                if(adjustment)amount.inputType=amount.inputType or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                field("description","Descrizione")
                if(!series && !adjustment)option("status","Quando incide sul saldo?",listOf("ACTUAL" to "Già effettuato","PLANNED" to "Previsto, ancora da effettuare"),"ACTUAL")
                category();date("date",if(series)"Prima scadenza" else "Data")
                val effect=ui.label(if(series || value("status","ACTUAL")=="PLANNED")"È una previsione: il saldo cambierà solo alla conferma del pagamento o incasso." else "Il movimento cambia subito la liquidità.",13f,ui.colors.muted);body.addView(effect)
                if(series){
                    option("frequency","Ripetizione",Frequency.entries.map {it.name to frequencyName(it)},"MONTHLY")
                    field("count","Numero rate (vuoto = continua)").apply {inputType=android.text.InputType.TYPE_CLASS_NUMBER}
                    body.addView(ui.label(if(d.id!=0L)"Modifichi solo le scadenze non effettuate. I movimenti pagati e le singole occorrenze eliminate restano invariati." else "L'importo è quello di ogni rata. Conferma il pagamento o incasso soltanto quando avviene.",13f,ui.colors.muted))
                }
                advanced {
                    if(value("kind")=="INCOME" && (series || value("status")=="PLANNED"))option("uncertain","Entrata non garantita",listOf("false" to "No","true" to "Sì, solo uno scenario"),"false")
                    if(!series && d.id==0L && !adjustment)body.addView(ui.button("Trasforma in ricorrenza o rate"){draft=Draft("series",values=d.values.toMutableMap().apply {put("status","PLANNED")});render()})
                }
                saveDraft={
                    val kind=Kind.valueOf(value("kind"));val cents=Money.parse(value("amount"),kind==Kind.ADJUSTMENT);val name=value("description").trim();val day=LocalDate.parse(value("date"));val cat=value("category").toLong()
                    val uncertain=value("uncertain","false")=="true" && kind==Kind.INCOME && (series || value("status")=="PLANNED")
                    if(series){val count=if(value("count").isBlank())0 else value("count").toIntOrNull() ?: error("Numero rate non valido.");val s=Series(d.id,name,cents,kind,cat,day,Frequency.valueOf(value("frequency")),count,uncertain);if(d.id==0L)store.saveSeries(s) else store.updateSeries(s)}
                    else {val old=l.operations.firstOrNull {it.id==d.id};store.save(Operation(d.id,name,cents,kind,if(adjustment)Status.ACTUAL else Status.valueOf(value("status")),day,cat,uncertain,old?.series,old?.index))}
                }
            }
            "advance"->{
                body.addView(ui.label("Il denaro è già uscito. Diventerà un credito da recuperare, distinto dalle spese personali.",13f,ui.colors.muted))
                field("amount","Importo anticipato in euro",true).textSize=30f;field("description","Descrizione");field("debtor","Da chi recuperare?");date("date","Data dell'anticipo")
                advanced {field("project","Immobile / progetto");field("cause","Categoria / causale");date("due","Restituzione prevista",true);field("notes","Note")}
                saveDraft={store.saveAdvance(Advance(d.id,Money.parse(value("amount")),LocalDate.parse(value("date")),value("description"),value("debtor"),value("project"),value("cause"),value("due").takeIf {it.isNotBlank()}?.let {LocalDate.parse(it)},value("notes")))}
            }
            "refund","refundEdit"->{
                val a=l.advances.first {it.id==value("advance").toLong()};body.addView(ui.label(a.description,20f,ui.colors.text,true));body.addView(ui.label("Da recuperare ${Money.format(l.remaining(a))} · ${a.debtor}",14f,ui.colors.muted))
                field("amount","Importo ricevuto in euro",true).textSize=30f;date("date","Data del rimborso");field("notes","Nota facoltativa")
                body.addView(ui.label("Registri soltanto denaro già ricevuto. Aumenterà la liquidità, senza diventare un guadagno.",13f,ui.colors.muted))
                saveDraft={store.saveRefund(Refund(if(d.type=="refundEdit")d.id else 0,a.id,Money.parse(value("amount")),LocalDate.parse(value("date")),value("notes")))}
            }
            "opening","reserve","reconcile"->{
                body.addView(ui.label(when(d.type){"opening"->"È la base prima dei movimenti: cambia anche lo storico. Per allineare solo il saldo di oggi usa Aggiorna liquidità.";"reserve"->"La somma protetta viene sottratta dallo spendibile.";else->"Verrà registrata una rettifica: nessun guadagno o spesa inventati."},14f,ui.colors.muted))
                field("amount","Euro",true).textSize=30f
                saveDraft={val n=Money.parse(value("amount"),d.type!="reserve");when(d.type){"opening"->store.settings(l.settings.copy(opening=n));"reserve"->store.settings(l.settings.copy(reserve=n));else->store.reconcile(n)}}
            }
            "category"->{
                field("description","Nome categoria")
                val colors=listOf("Salvia" to "#779B91","Oro" to "#DBBF8B","Blu" to "#7392B7","Lavanda" to "#AC91BB","Corallo" to "#C66B65","Sabbia" to "#B09060")
                option("color","Colore",colors.map {(name,hex)->Color.parseColor(hex).toString() to name},Color.parseColor("#779B91").toString())
                if(d.id!=0L)body.addView(ui.button("Elimina categoria"){confirm("Eliminare la categoria?","È possibile soltanto se non è utilizzata da movimenti o ricorrenze attive."){store.deleteCategory(d.id);draft=null;page=previous}})
                saveDraft={store.category(d.id.takeIf {it!=0L},value("description"),value("color").toInt())}
            }
            "creditFilter"->{
                field("subject","Soggetto");field("project","Immobile / progetto")
                option("state","Stato",listOf("Tutti","Aperti","Parziali","Recuperati","Scaduti").mapIndexed {i,name->i.toString() to name},"0")
                option("month","Periodo",listOf("false" to "Tutto il periodo","true" to "Mese selezionato"),"false")
                body.addView(ui.button("Azzera filtri"){d.values.clear();render()})
                saveDraft={creditSubject=value("subject");creditProject=value("project");creditStatus=value("state").toInt();creditMonth=value("month")=="true"}
            }
        }
    }

    private fun export(previous:Boolean) {
        if(demo){Toast.makeText(this,"Esci dalla demo per esportare i tuoi dati.",Toast.LENGTH_LONG).show();return}
        exportPrevious=previous
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"posso-spenderlo-${LocalDate.now()}${if(previous)"-precedente" else ""}.json"),1)
    }
    @Deprecated("Uses system document picker for Android 8+ compatibility")
    override fun onActivityResult(request:Int,result:Int,data:Intent?) {
        super.onActivityResult(request,result,data)
        if(result!=RESULT_OK)return
        val uri=data?.data ?: return
        try {
            if(request==1) {
                val text=if(exportPrevious)java.io.File(filesDir,"pre-import.json").readText() else Backup.encode(store.snapshot())
                val stream=contentResolver.openOutputStream(uri,"wt") ?: error("Impossibile aprire il file di destinazione.")
                stream.use {it.write(text.toByteArray(Charsets.UTF_8));it.flush()}
                Toast.makeText(this,"Backup esportato",Toast.LENGTH_LONG).show()
            } else if(request==2) {
                val stream=contentResolver.openInputStream(uri) ?: error("Impossibile leggere il backup.")
                val text=stream.use {input->val output=ByteArrayOutputStream();val buffer=ByteArray(8192);var size=0
                    while(true){val n=input.read(buffer);if(n<0)break;size+=n;require(size<=Backup.LIMIT) {"Backup troppo grande."};output.write(buffer,0,n)}
                    output.toString("UTF-8")
                }
                val imported=Backup.decode(text)
                confirm("Sostituire i dati attuali?","Il backup contiene ${imported.operations.count {it.status!=Status.DELETED}} operazioni e ${imported.advances.size} anticipi. Prima di sostituire i dati verrà salvata sul dispositivo una copia dell'archivio attuale, esportabile da Altro. Il backup deve provenire da una fonte fidata.") {
                    val previous=Backup.encode(store.snapshot());val tmp=java.io.File(filesDir,"pre-import.tmp");tmp.writeText(previous)
                    check(tmp.renameTo(java.io.File(filesDir,"pre-import.json"))) {"Impossibile conservare la copia precedente: importazione annullata."}
                    store.replace(imported)
                    try {ReminderService.schedule(this,imported.settings.reminders)} catch(_:Exception) {
                        store.settings(store.snapshot().settings.copy(reminders=false))
                        Toast.makeText(this,"Dati importati; avvisi non attivati da Android.",Toast.LENGTH_LONG).show()
                    }
                    recreate()
                }
            }
        } catch(e:Exception){fail(e)}
    }
    override fun onRequestPermissionsResult(request:Int,permissions:Array<out String>,results:IntArray) {
        super.onRequestPermissionsResult(request,permissions,results)
        if(request==3 && results.isNotEmpty() && results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)change {ReminderService.schedule(this,true);store.settings(l.settings.copy(reminders=true))}
        else if(request==3)Toast.makeText(this,"Avvisi non attivati. Puoi concedere il permesso dalle impostazioni Android.",Toast.LENGTH_LONG).show()
    }
}
