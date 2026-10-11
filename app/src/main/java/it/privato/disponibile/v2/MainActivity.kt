package it.privato.disponibile.v2

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import android.content.Intent
import android.graphics.Color
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.ByteArrayOutputStream

class MainActivity:Activity() {
    private lateinit var store:Store
    private lateinit var l:Ledger
    private lateinit var ui:Design
    private var page=0
    private var month=YearMonth.now()
    private var period=6
    private var query=""
    private var filter=0
    private var categoryFilter:Long?=null
    private var selectedDay:LocalDate?=null
    private var demo=false
    private var exportPrevious=false
    private val dates=DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ITALY)
    private var dialog:AlertDialog?=null
    override fun onCreate(state:Bundle?) {
        store=Store(this)
        val theme=store.snapshot().settings.theme
        val palette=Palette.of(this,theme)
        setTheme(if(palette.dark) R.style.AppThemeDark else R.style.AppTheme)
        super.onCreate(state)
        page=state?.getInt("page") ?: 0;demo=state?.getBoolean("demo") ?: false
        if(state?.containsKey("month")==true)month=YearMonth.parse(state.getString("month"))
        exportPrevious=state?.getBoolean("exportPrevious") ?: false
        render()
    }
    override fun onSaveInstanceState(state:Bundle) {super.onSaveInstanceState(state);state.putInt("page",page);state.putBoolean("demo",demo);state.putString("month",month.toString());state.putBoolean("exportPrevious",exportPrevious)}
    override fun onDestroy() {dialog?.dismiss();store.close();super.onDestroy()}
    override fun onConfigurationChanged(config:android.content.res.Configuration) {super.onConfigurationChanged(config);recreate()}
    private fun fail(e:Exception) {AlertDialog.Builder(this).setTitle("Operazione non completata").setMessage(e.message ?: "Riprova: i dati non sono stati salvati.").setPositiveButton("OK",null).show()}
    private fun change(action:()->Unit) {
        if(demo) {Toast.makeText(this,"Esci dalla demo per modificare i tuoi dati.",Toast.LENGTH_LONG).show();return}
        try {action();render()} catch(e:Exception) {fail(e)}
    }
    private fun confirm(title:String,message:String,action:()->Unit) {dialog=AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Annulla",null).setPositiveButton("Conferma") {_,_->change(action)}.create();dialog!!.show()}
    private fun render() {
        try {l=if(demo) Demo.ledger(store.snapshot().settings.theme) else store.snapshot()} catch(e:Exception) {fail(e);return}
        ui=Design(this,Palette.of(this,l.settings.theme))
        val c=ui.colors
        window.statusBarColor=c.background;window.navigationBarColor=c.card
        window.decorView.systemUiVisibility=if(c.dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if(android.os.Build.VERSION.SDK_INT>=27) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        val root=ui.column().apply {setBackgroundColor(c.background)}
        root.setOnApplyWindowInsetsListener {v,insets->
            if(android.os.Build.VERSION.SDK_INT>=30) {val i=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom)}
            else v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        val header=ui.column().apply {setPadding(ui.dp(20),ui.dp(18),ui.dp(20),ui.dp(8))}
        header.addView(ui.label(if(demo) "DEMO · DATI DIMOSTRATIVI" else "POSSO SPENDERLO?",11f,c.muted,true))
        header.addView(ui.label(listOf("Il tuo margine","Una vista più chiara","Le prossime scadenze","I tuoi movimenti","Il tuo spazio")[page],27f,c.text,true))
        root.addView(header)
        val scroll=ScrollView(this).apply {isFillViewport=true;clipToPadding=false}
        val content=ui.column().apply {setPadding(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(18))}
        when(page) {0->dashboard(content);1->insights(content);2->calendar(content);3->history(content);4->settings(content)}
        scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(page!=4)root.addView(ui.button(if(demo) "Esci dalla demo" else "+ Nuova operazione",true) {if(demo){demo=false;render()}else editor(null)}.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(20),0,ui.dp(20),ui.dp(10))}})
        val nav=ui.row().apply {setBackgroundColor(c.card);setPadding(ui.dp(4),ui.dp(4),ui.dp(4),ui.dp(4))}
        listOf("Oggi","Grafici","Scadenze","Storico","Altro").forEachIndexed {i,name->
            nav.addView(ui.button(name) {page=i;render()}.apply {textSize=11f;setPadding(0,0,0,0);setTextColor(if(page==i)c.sage else c.muted);layoutParams=LinearLayout.LayoutParams(0,ui.dp(52),1f);background=ui.shape(if(page==i)c.background else c.card,12)})
        }
        root.addView(nav);setContentView(root);root.requestApplyInsets()
    }
    private fun metric(card:LinearLayout,title:String,value:String,color:Int=ui.colors.text) {
        card.addView(ui.label(title,13f,ui.colors.muted));card.addView(ui.label(value,23f,color,true))
    }
    private fun dashboard(body:LinearLayout) {
        val c=ui.colors
        val hero=ui.card(c.navy)
        hero.addView(ui.label("REALMENTE SPENDIBILE",11f,c.gold,true))
        hero.addView(ui.label(Money.format(l.spendable),39f,if(l.spendable<0) Color.parseColor("#F3AAA3") else Color.WHITE,true))
        hero.addView(ui.label("Fino al ${l.settings.horizon.format(dates)}",14f,Color.parseColor("#C1CBDA")))
        hero.addView(ui.label("Liquidità − uscite previste − riserva. Le entrate future non sono denaro già disponibile.",14f,Color.parseColor("#D5DCE8")))
        body.addView(hero)
        val cash=ui.card();metric(cash,"Liquidità attuale",Money.format(l.balance));metric(cash,"Uscite ancora da pagare",Money.format(l.commitments),c.coral);metric(cash,"Riserva personale protetta",Money.format(l.settings.reserve),c.sage)
        cash.addView(ui.button("Come viene calcolato?") {AlertDialog.Builder(this).setTitle("Un margine prudente").setMessage("Liquidità: saldo iniziale più entrate incassate meno uscite pagate, con eventuali rettifiche.\n\nSpendibile: liquidità meno tutte le uscite ancora da pagare entro ${l.settings.horizon.format(dates)}, comprese quelle arretrate, meno la riserva.\n\nEntrate previste: ${Money.format(l.expectedIncome)}. Sono mostrate solo nella proiezione, anche se le ritieni probabili.").setPositiveButton("Chiaro",null).show()})
        body.addView(cash)
        val next=l.pending.firstOrNull {it.kind==Kind.EXPENSE}
        val warning=ui.card()
        warning.addView(ui.label(if(l.spendable<0) "Attenzione al margine" else "La prossima cosa da ricordare",18f,if(l.spendable<0)c.coral else c.text,true))
        if(l.spendable<0)warning.addView(ui.label("Mancano ${Money.format(-l.spendable)} per coprire impegni e riserva entro la previsione.",14f,c.coral))
        warning.addView(ui.label(next?.let {"${it.title}\n${it.date.format(dates)} · ${Money.format(it.cents)}"} ?: "Nessuna scadenza registrata. Aggiungi i prossimi pagamenti per rendere utile la previsione.",15f,c.muted))
        if(next!=null)warning.addView(ui.button("Apri scadenza") {manage(next)})
        val critical=l.projection(false).firstOrNull {it.second<l.settings.reserve}
        if(critical!=null)warning.addView(ui.label("Senza nuove entrate, sotto la riserva dal ${critical.first.format(dates)}.",13f,c.coral))
        body.addView(warning)
        val simulation=ui.card();simulation.addView(ui.label("E se spendessi…",21f,c.text,true));simulation.addView(ui.label("Una simulazione, senza salvare un movimento.",13f,c.muted))
        val field=ui.input("Importo in euro",money=true);simulation.addView(field)
        val result=ui.label("Inserisci un importo per vedere quanto resterebbe.",17f,c.muted,true);simulation.addView(result)
        field.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {
                if(s.isNullOrBlank()){result.text="Inserisci un importo per vedere quanto resterebbe.";return}
                try {val value=l.simulated(Money.parse(s.toString()));result.text="Resterebbero ${Money.format(value)}";result.setTextColor(if(value<0)c.coral else c.sage);field.error=null} catch(e:Exception){field.error=e.message;result.text="Controlla l'importo."}
            }
            override fun afterTextChanged(s:Editable?)=Unit
        })
        body.addView(simulation)
        val actions=ui.card();actions.addView(ui.button("Aggiorna liquidità") {moneyDialog("Liquidità attuale",l.balance,true) {store.reconcile(it)}})
        actions.addView(ui.button("Riserva e data di previsione") {reserveDialog()});body.addView(actions)
    }
    private fun monthControls(body:LinearLayout,onChange:()->Unit={render()}) {
        val row=ui.row();row.addView(ui.button("‹") {month=month.minusMonths(1);selectedDay=null;onChange()}.apply {layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        row.addView(ui.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ITALY)),16f,ui.colors.text,true).apply {gravity=android.view.Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(0,-2,3f)})
        row.addView(ui.button("›") {month=month.plusMonths(1);selectedDay=null;onChange()}.apply {layoutParams=LinearLayout.LayoutParams(0,-2,1f)});body.addView(row)
    }
    private fun chart(body:LinearLayout,title:String,subtitle:String,mode:Int,legend:String) {
        val card=ui.card();card.addView(ui.label(title,21f,ui.colors.text,true));card.addView(ui.label(subtitle,13f,ui.colors.muted))
        val selected=ui.label("Tocca il grafico per i dettagli.",13f,ui.colors.text)
        val view=Charts(this,ui.colors,l,mode,month,period) {selected.text=it}
        view.contentDescription=title+". "+legend;view.isFocusable=true
        card.addView(view,LinearLayout.LayoutParams(-1,ui.dp(if(mode==0)220 else 230)));card.addView(ui.label(legend,12f,ui.colors.muted));card.addView(selected)
        if(mode==0) {
            val categories=l.categories.map {it to l.actual.filter {o->o.kind==Kind.EXPENSE && YearMonth.from(o.date)==month && o.category==it.id}.sumOf {o->o.cents}}.filter {it.second>0}
            val total=categories.sumOf {it.second}
            categories.forEach {(cat,n)->card.addView(ui.button("● ${cat.name} · ${Money.format(n)}") {selected.text="${cat.name} · ${Money.format(n)} · ${String.format(Locale.ITALY,"%.1f",n*100.0/total)}%"}.apply {
                setTextColor(ui.colors.text)
                text=android.text.SpannableString(text).apply {setSpan(android.text.style.ForegroundColorSpan(cat.color),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            })}
        }
        body.addView(card)
    }
    private fun insights(body:LinearLayout) {
        monthControls(body)
        chart(body,"Dove va il denaro","Uscite effettuate nel mese selezionato",0,"Le categorie contano solo le spese già pagate.")
        chart(body,"La strada della liquidità","Dalle operazioni registrate alla previsione scelta",1,"Verde: storico effettivo · Corallo tratteggiato: solo uscite note · Oro tratteggiato: incluse entrate previste, anche non garantite. Le linee future sono scenari, non certezze.")
        val selector=ui.row();listOf(3,6,12).forEach {n->selector.addView(ui.button("$n mesi") {period=n;render()}.apply {layoutParams=LinearLayout.LayoutParams(0,-2,1f);setTextColor(if(period==n)ui.colors.sage else ui.colors.muted)})};body.addView(selector)
        chart(body,"Entrate e uscite","Operazioni effettuate · ultimo mese: ${month.format(DateTimeFormatter.ofPattern("MMM yyyy",Locale.ITALY))}",2,"Verde: entrate incassate · Corallo: uscite pagate. Rettifiche di saldo escluse.")
        if(l.actual.isEmpty())body.addView(ui.label("Non hai ancora registrato movimenti effettivi: nessun dato storico viene inventato.",14f,ui.colors.muted))
    }
    private fun calendar(body:LinearLayout) {
        monthControls(body)
        val calendar=ui.card();calendar.addView(ui.label("L  M  M  G  V  S  D",15f,ui.colors.muted,true).apply {gravity=android.view.Gravity.CENTER})
        val start=month.atDay(1);val offset=start.dayOfWeek.value-1;var row=ui.row()
        val count=((offset+month.lengthOfMonth()+6)/7)*7
        for(i in 0 until count) {
            if(i%7==0){row=ui.row();calendar.addView(row)}
            val day=i-offset+1
            if(day !in 1..month.lengthOfMonth()){row.addView(ui.label("").apply {layoutParams=LinearLayout.LayoutParams(0,ui.dp(46),1f)});continue}
            val date=month.atDay(day);val has=l.pending.any {it.date==date}
            row.addView(ui.button(day.toString()+if(has) "•" else "") {selectedDay=if(selectedDay==date)null else date;render()}.apply {textSize=12f;setPadding(0,0,0,0);minWidth=0;layoutParams=LinearLayout.LayoutParams(0,ui.dp(46),1f);background=ui.shape(if(selectedDay==date)ui.colors.line else ui.colors.card,10);setTextColor(if(has)ui.colors.sage else ui.colors.text)})
        }
        body.addView(calendar)
        body.addView(ui.label(selectedDay?.let {"Scadenze del ${it.format(dates)}"} ?: "Scadenze del mese",20f,ui.colors.text,true))
        if(selectedDay!=null)body.addView(ui.button("Mostra tutto il mese") {selectedDay=null;render()})
        val pending=l.pending.filter {if(selectedDay!=null)it.date==selectedDay else YearMonth.from(it.date)==month}
        if(pending.isEmpty())empty(body,"Un po' di respiro","Nessuna scadenza in questo periodo.") else pending.forEach {operationCard(body,it)}
        val overdue=l.pending.filter {it.kind==Kind.EXPENSE && it.date<LocalDate.now() && YearMonth.from(it.date)!=month}
        if(overdue.isNotEmpty()) {body.addView(ui.label("Arretrati fuori dal mese",18f,ui.colors.coral,true));overdue.forEach {operationCard(body,it)}}
        val series=l.series.filter {it.active}
        if(series.isNotEmpty()) {
            body.addView(ui.label("Ricorrenze e rate attive",20f,ui.colors.text,true))
            series.forEach {s->val c=ui.card();c.addView(ui.label(s.title,17f,ui.colors.text,true));c.addView(ui.label("${Money.format(s.cents)} · ${when(s.frequency){Frequency.MONTHLY->"mensile";Frequency.WEEKLY->"settimanale";Frequency.YEARLY->"annuale"}} · ${if(s.count==0) "continua" else "${s.count} rate"}",13f,ui.colors.muted));c.addView(ui.button("Interrompi e annulla le scadenze non effettuate") {confirm("Interrompere ${s.title}?","I movimenti già effettuati resteranno nello storico; tutte le scadenze ancora previste verranno annullate."){store.cancelSeries(s.id)}});body.addView(c)}
        }
    }
    private fun empty(body:LinearLayout,title:String,message:String) {val card=ui.card();card.addView(ui.label(title,21f,ui.colors.text,true));card.addView(ui.label(message,15f,ui.colors.muted));body.addView(card)}
    private fun operationCard(body:LinearLayout,o:Operation) {
        val card=ui.card();val color=if(o.kind==Kind.EXPENSE)ui.colors.coral else if(o.kind==Kind.INCOME)ui.colors.sage else ui.colors.muted
        card.addView(ui.label(o.title,17f,ui.colors.text,true));card.addView(ui.label((if(o.kind==Kind.EXPENSE) "− " else if(o.signed>=0) "+ " else "")+Money.format(o.cents),23f,color,true))
        val status=if(o.status==Status.ACTUAL) "effettuata" else if(o.date<LocalDate.now()) "prevista · arretrata" else "prevista"
        card.addView(ui.label("${o.date.format(dates)} · ${l.categoryName(o.category)}\n$status${if(o.uncertain) " · entrata non garantita" else ""}",13f,ui.colors.muted))
        card.addView(ui.button("Gestisci") {manage(o)});body.addView(card)
    }
    private fun history(body:LinearLayout) {
        val search=ui.input("Cerca descrizione o categoria",query);body.addView(search)
        val controls=ui.card();val filters=listOf("Tutte","Effettuate","Previste","Entrate","Uscite")
        val status=ui.spinner(filters,filter);controls.addView(status)
        val cats=listOf("Tutte le categorie")+l.categories.map {it.name};val cat=ui.spinner(cats,if(categoryFilter==null)0 else l.categories.indexOfFirst {it.id==categoryFilter}+1);controls.addView(cat)
        body.addView(controls)
        val results=ui.column();body.addView(results)
        fun update() {
            results.removeAllViews()
            val list=l.operations.filter {it.status!=Status.DELETED && (query.isBlank() || (it.title+" "+l.categoryName(it.category)).contains(query,true)) && (categoryFilter==null || it.category==categoryFilter) && when(filter){1->it.status==Status.ACTUAL;2->it.status==Status.PLANNED;3->it.kind==Kind.INCOME;4->it.kind==Kind.EXPENSE;else->true}}.sortedWith(compareByDescending<Operation>{it.date}.thenByDescending {it.id})
            results.addView(ui.label("${list.size} operazioni",13f,ui.colors.muted))
            if(list.isEmpty())empty(results,"Qui inizia la tua storia","Nessuna operazione corrisponde alla ricerca.")
            list.take(300).forEach {operationCard(results,it)}
            if(list.size>300)results.addView(ui.label("Mostrate le prime 300 operazioni: restringi ricerca o filtri.",13f,ui.colors.muted))
        }
        search.addTextChangedListener(object:TextWatcher {override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){query=s.toString();update()}})
        status.onItemSelectedListener=selection {i->filter=i;update()};cat.onItemSelectedListener=selection {i->categoryFilter=if(i==0)null else l.categories[i-1].id;update()};update()
    }
    private fun selection(action:(Int)->Unit)=object:AdapterView.OnItemSelectedListener {override fun onNothingSelected(parent:AdapterView<*>?)=Unit;override fun onItemSelected(parent:AdapterView<*>?,v:View?,position:Int,id:Long){action(position)}}
    private fun settings(body:LinearLayout) {
        val money=ui.card();money.addView(ui.label("Le tue regole",21f,ui.colors.text,true));metric(money,"Liquidità iniziale, prima dei movimenti",Money.format(l.settings.opening));metric(money,"Riserva minima",Money.format(l.settings.reserve));money.addView(ui.button("Modifica liquidità iniziale") {moneyDialog("Liquidità iniziale",l.settings.opening,true){store.settings(l.settings.copy(opening=it))}});money.addView(ui.button("Riserva e data di previsione") {reserveDialog()});body.addView(money)
        val look=ui.card();look.addView(ui.label("Un tema per ogni momento",21f,ui.colors.text,true));listOf("system" to "Come il telefono","light" to "Chiaro","dark" to "Scuro").forEach {(theme,title)->look.addView(ui.button((if(l.settings.theme==theme)"✓ " else "")+title) {change {store.settings(l.settings.copy(theme=theme))};if(!demo)recreate()})};body.addView(look)
        val categories=ui.card();categories.addView(ui.label("Le tue categorie",21f,ui.colors.text,true));l.categories.forEach {cat->categories.addView(ui.button("● ${cat.name}") {categoryDialog(cat)}.apply {
                setTextColor(ui.colors.text)
                text=android.text.SpannableString(text).apply {setSpan(android.text.style.ForegroundColorSpan(cat.color),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            })};categories.addView(ui.button("+ Crea categoria") {categoryDialog(null)});body.addView(categories)
        val backup=ui.card();backup.addView(ui.label("I dati sono tuoi",21f,ui.colors.text,true));backup.addView(ui.label("Backup JSON sul file che scegli. Non cifrato: contiene dati personali. Usa una cartella locale sicura; evita destinazioni cloud se vuoi mantenere i dati sul dispositivo.",13f,ui.colors.muted));backup.addView(ui.button("Esporta backup") {export(false)});backup.addView(ui.button("Importa backup") {if(!demo)startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),2)})
        if(java.io.File(filesDir,"pre-import.json").exists())backup.addView(ui.button("Esporta copia precedente all'importazione") {export(true)})
        body.addView(backup)
        val reminders=ui.card();reminders.addView(ui.label("Un promemoria discreto",21f,ui.colors.text,true));reminders.addView(ui.label("Riepilogo locale delle spese entro domani e degli arretrati, controllato circa una volta al giorno. Android può ritardarlo per risparmiare batteria; non è un allarme a un orario preciso.",13f,ui.colors.muted));if(l.settings.reminders && !getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled()) {
            reminders.addView(ui.label("Avvisi richiesti, ma Android ha disabilitato le notifiche.",13f,ui.colors.coral))
            reminders.addView(ui.button("Apri autorizzazioni Android") {startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))})
        }
        reminders.addView(ui.button(if(l.settings.reminders) "Disattiva avvisi" else "Attiva avvisi") {
            if(demo)return@button
            if(!l.settings.reminders && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),3)
            else change {ReminderService.schedule(this,!l.settings.reminders);store.settings(l.settings.copy(reminders=!l.settings.reminders))}
        });body.addView(reminders)
        val info=ui.card();info.addView(ui.label("Privata, per davvero",21f,ui.colors.text,true));info.addView(ui.label("V2 · nessun account, server, analytics o costo ricorrente. L'app non ha il permesso Internet. Le rettifiche aggiornano il saldo ma non sono contate come entrate o uscite nei grafici. Le ricorrenze continue vengono preparate almeno per i prossimi 12 mesi e fino alla data di previsione.",13f,ui.colors.muted));info.addView(ui.button(if(demo) "Esci dalla demo" else "Esplora la demo separata") {demo=!demo;page=0;render()});body.addView(info)
    }
    private fun dialogBody():LinearLayout=ui.column().apply {setPadding(ui.dp(22),ui.dp(8),ui.dp(22),ui.dp(8))}
    private fun showForm(title:String,body:LinearLayout,save:()->Unit) {
        val scroll=ScrollView(this);scroll.addView(body)
        val d=AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("Annulla",null).setPositiveButton("Salva",null).create()
        dialog=d;d.setOnShowListener {d.getButton(-1).setOnClickListener {
            if(demo){Toast.makeText(this,"Esci dalla demo per salvare.",Toast.LENGTH_LONG).show();return@setOnClickListener}
            try {save();d.dismiss();render()}catch(e:Exception){fail(e)}
        }};d.show()
    }
    private fun pick(initial:LocalDate,action:(LocalDate)->Unit) {DatePickerDialog(this,{_,y,m,d->action(LocalDate.of(y,m+1,d))},initial.year,initial.monthValue-1,initial.dayOfMonth).show()}
    private fun moneyDialog(title:String,amount:Long,signed:Boolean,save:(Long)->Unit) {
        val body=dialogBody();body.addView(ui.label(if(title=="Liquidità iniziale") "Saldo prima delle operazioni registrate. Modificarlo cambia anche lo storico; per correggere il saldo di oggi usa Aggiorna liquidità." else "Il saldo di oggi verrà riconciliato con una rettifica, senza inventare un'entrata o una spesa.",13f,ui.colors.muted))
        val field=ui.input("Euro",Money.input(amount),true);body.addView(field)
        showForm(title,body) {save(Money.parse(field.text.toString(),signed))}
    }
    private fun reserveDialog() {
        val body=dialogBody();body.addView(ui.label("Somma da non spendere",14f,ui.colors.muted));val reserve=ui.input("Riserva in euro",Money.input(l.settings.reserve),true);body.addView(reserve)
        var date=l.settings.horizon;val button=ui.button("Previsione: ${date.format(dates)}") {}
        button.setOnClickListener {pick(date){date=it;button.text="Previsione: ${date.format(dates)}"}};body.addView(button)
        showForm("Riserva e previsione",body) {store.settings(l.settings.copy(reserve=Money.parse(reserve.text.toString()),horizon=date))}
    }
    private fun editor(existing:Operation?) {
        val body=dialogBody();val title=ui.input("Descrizione",existing?.title ?: "");body.addView(title)
        body.addView(ui.label("Importo in euro",13f,ui.colors.muted));val amount=ui.input("Es. 125,50",existing?.let {Money.input(it.cents)} ?: "",true);body.addView(amount)
        val adjustment=existing?.kind==Kind.ADJUSTMENT
        val types=if(adjustment)listOf("Rettifica saldo")else listOf("Uscita","Entrata")
        val type=ui.spinner(types,if(existing?.kind==Kind.INCOME)1 else 0);body.addView(type)
        val status=ui.spinner(listOf("Prevista, ancora da effettuare","Effettuata, già pagata/incassata"),if(existing?.status==Status.ACTUAL)1 else 0);body.addView(status)
        if(adjustment) {status.setSelection(1);status.isEnabled=false}
        var date=existing?.date ?: LocalDate.now();val dateButton=ui.button("Data: ${date.format(dates)}") {}
        dateButton.setOnClickListener {pick(date) {date=it;dateButton.text="Data: ${date.format(dates)}"}};body.addView(dateButton)
        body.addView(ui.label("Categoria",13f,ui.colors.muted));val cat=ui.spinner(l.categories.map {it.name},l.categories.indexOfFirst {it.id==existing?.category}.coerceAtLeast(0));body.addView(cat)
        val uncertain=CheckBox(this).apply {text="Entrata prevista non garantita";setTextColor(ui.colors.text);isChecked=existing?.uncertain ?: false};body.addView(uncertain)
        fun updateUncertain() {val allowed=type.selectedItemPosition==1 && status.selectedItemPosition==0 && !adjustment;uncertain.visibility=if(allowed)View.VISIBLE else View.GONE;if(!allowed)uncertain.isChecked=false}
        type.onItemSelectedListener=selection {updateUncertain()};status.onItemSelectedListener=selection {updateUncertain()}
        body.addView(ui.label("Ripetizione",13f,ui.colors.muted))
        val repetition=ui.spinner(listOf("Una sola operazione","Ogni mese","Ogni settimana","Ogni anno","Rate mensili (numero definito)"));body.addView(repetition)
        val count=ui.input("Numero rate (1–120)","12").apply {inputType=android.text.InputType.TYPE_CLASS_NUMBER;visibility=View.GONE};body.addView(count)
        repetition.onItemSelectedListener=selection {i->count.visibility=if(i==4)View.VISIBLE else View.GONE}
        if(existing!=null){repetition.visibility=View.GONE;body.addView(ui.label(if(existing.series!=null) "Modifichi solo questa occorrenza. Per cambiare la ricorrenza, interrompila nel calendario e creane una nuova." else "Modifica o elimina questo movimento senza creare duplicati.",13f,ui.colors.muted))}
        body.addView(ui.label("Una previsione non cambia il saldo. Se scegli Effettuata, il movimento entra subito nella liquidità e nello storico.",13f,ui.colors.muted))
        showForm(if(existing==null) "Nuova operazione" else "Modifica operazione",body) {
            val name=title.text.toString().trim();require(name.length in 1..80) {"Inserisci una descrizione."}
            val value=Money.parse(amount.text.toString(),adjustment);require(value!=0L) {"L'importo non può essere zero."}
            val kind=if(adjustment)Kind.ADJUSTMENT else if(type.selectedItemPosition==0)Kind.EXPENSE else Kind.INCOME
            val state=if(status.selectedItemPosition==1)Status.ACTUAL else Status.PLANNED
            val category=l.categories[cat.selectedItemPosition].id;val irregular=uncertain.isChecked && kind==Kind.INCOME && state==Status.PLANNED
            val repeat=if(existing!=null)0 else repetition.selectedItemPosition
            if(repeat>0) {
                require(state==Status.PLANNED) {"Una ricorrenza nasce come prevista. Conferma ogni pagamento o incasso quando avviene."}
                val freq=when(repeat){2->Frequency.WEEKLY;3->Frequency.YEARLY;else->Frequency.MONTHLY}
                val n=if(repeat==4)count.text.toString().toIntOrNull() ?: error("Numero rate non valido.") else 0
                require(repeat!=4 || n in 1..120) {"Scegli da 1 a 120 rate. L'importo inserito è quello di ogni rata."}
                store.saveSeries(Series(title=name,cents=value,kind=kind,category=category,start=date,frequency=freq,count=n,uncertain=irregular))
            } else store.save(Operation(existing?.id ?: 0,name,value,kind,state,date,category,irregular,existing?.series,existing?.index))
        }
    }
    private fun manage(o:Operation) {
        val labels=if(o.status==Status.PLANNED) listOf(if(o.kind==Kind.INCOME) "Conferma incasso oggi" else "Conferma pagamento oggi","Modifica","Elimina") else listOf("Modifica","Elimina")
        AlertDialog.Builder(this).setTitle(o.title).setItems(labels.toTypedArray()) {_,position->
            when(labels[position]) {
                "Modifica"->editor(o)
                "Elimina"->confirm("Eliminare questa operazione?",if(o.status==Status.ACTUAL) "Il suo effetto sul saldo verrà rimosso. Una singola occorrenza eliminata non verrà rigenerata." else "Il saldo attuale non cambia; verrà eliminata solo questa scadenza.") {store.delete(o.id)}
                else->confirm("Confermi ${Money.format(o.cents)}?","Registra il movimento come effettuato oggi, ${LocalDate.now().format(dates)}. La liquidità ${if(o.kind==Kind.INCOME)"aumenterà" else "diminuirà"} una sola volta. Puoi correggere la data nello storico.") {store.settle(o.id)}
            }
        }.show()
    }
    private fun categoryDialog(category:Category?) {
        val body=dialogBody();val name=ui.input("Nome categoria",category?.name ?: "");body.addView(name)
        val colors=listOf("#779B91","#DBBF8B","#7392B7","#AC91BB","#C66B65","#B09060").map {Color.parseColor(it)}
        val picker=ui.spinner(listOf("Salvia","Oro","Blu","Lavanda","Corallo","Sabbia"),colors.indexOf(category?.color).coerceAtLeast(0));body.addView(picker)
        if(category!=null)body.addView(ui.button("Elimina categoria") {val parentDialog=dialog;confirm("Eliminare ${category.name}?","È possibile solo se non è usata da operazioni o ricorrenze attive.") {store.deleteCategory(category.id);parentDialog?.dismiss()}})
        showForm(if(category==null) "Nuova categoria" else "Modifica categoria",body) {store.category(category?.id,name.text.toString(),colors[picker.selectedItemPosition])}
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
                confirm("Sostituire i dati attuali?","Il backup contiene ${imported.operations.count {it.status!=Status.DELETED}} operazioni. Prima di sostituire i dati verrà salvata sul dispositivo una copia dell'archivio attuale, esportabile da Altro. Il backup deve provenire da una fonte fidata.") {
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
