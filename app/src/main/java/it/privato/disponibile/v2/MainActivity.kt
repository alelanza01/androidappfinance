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
    private var analysisSpan=1
    private var rateChoice="none"
    private var rateCount="12"
    private var ratesAdvanced=false
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
    private var draftBaseline:String?=null
    private var categoryQuery=""
    private var categoryTab=2
    private var pickingFilter=false
    private var parentDraft:Draft?=null
    private var parentPrevious=0
    private var parentBaseline:String?=null
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
        draftBaseline=state?.getString("baseline");parentDraft=state?.getString("parentDraft")?.let {Draft.parse(it)}
        parentBaseline=state?.getString("parentBaseline");parentPrevious=state?.getInt("parentPrevious") ?: 0
        pickingFilter=state?.getBoolean("pickingFilter") ?: false
        ratesAdvanced=state?.getBoolean("ratesAdvanced") ?: false;categoryTab=state?.getInt("categoryTab") ?: 2;categoryQuery=state?.getString("categoryQuery") ?: ""
        analysisSpan=state?.getInt("analysisSpan") ?: 1;rateChoice=state?.getString("rateChoice") ?: "none";rateCount=state?.getString("rateCount") ?: "12"
        exportPrevious=state?.getBoolean("exportPrevious") ?: false
        if(android.os.Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false)
        window.setSoftInputMode(if(android.os.Build.VERSION.SDK_INT>=30)WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        render()
    }
    override fun onSaveInstanceState(s:Bundle) {
        super.onSaveInstanceState(s);s.putBoolean("ratesAdvanced",ratesAdvanced);s.putInt("categoryTab",categoryTab);s.putString("categoryQuery",categoryQuery);s.putInt("analysisSpan",analysisSpan);s.putString("rateChoice",rateChoice);s.putString("rateCount",rateCount);s.putInt("page",page);s.putInt("previous",previous);s.putBoolean("demo",demo);s.putString("month",month.toString());s.putInt("chartMode",chartMode);s.putBoolean("grid",calendarGrid);s.putLong("detailId",detailId);draft?.let {s.putString("draft",it.json())};s.putBoolean("exportPrevious",exportPrevious);s.putString("baseline",draftBaseline);s.putString("parentBaseline",parentBaseline);s.putInt("parentPrevious",parentPrevious);s.putBoolean("pickingFilter",pickingFilter);parentDraft?.let {s.putString("parentDraft",it.json())}
    }
    override fun onDestroy(){dialog?.dismiss();store.close();super.onDestroy()}
    @Deprecated("Android 8 compatible back navigation") override fun onBackPressed() {
        if(page==22)go(20)
        else if(page==21)go(if(pickingFilter)1 else 20)
        else if(page==20) {
            if(draftFingerprint()==draftBaseline)finishForm(false)
            else decision("Uscire senza salvare?","Le modifiche che hai inserito andranno perse.","Esci senza salvare","Continua a modificare",true){finishForm(false)}
        }
        else if(page==13)go(3) else if(page>=4)go(if(page in 7..11)6 else 0) else if(page!=0)go(0) else super.onBackPressed()
    }
    private fun go(destination:Int){if(destination==9){categoryQuery="";categoryTab=2};hideKeyboard();dialog?.dismiss();page=destination;render();if(android.animation.ValueAnimator.areAnimatorsEnabled()){val v=findViewById<View>(android.R.id.content);v.alpha=.75f;v.animate().alpha(1f).setDuration(140).start()}}
    private fun draftFingerprint():String?=draft?.let {d->d.type+":"+d.values.filter {it.key!="advanced" && it.value.isNotBlank() && it.value!="false"}.toSortedMap().toString()}
    private fun finishForm(saved:Boolean) {
        if(previous==21 && draft?.type=="category") {
            val created=if(saved)draft?.id?.takeIf {it>0} ?: store.snapshot().categories.lastOrNull()?.id else null
            draft=parentDraft;previous=parentPrevious;draftBaseline=parentBaseline;parentDraft=null;parentBaseline=null
            val available=store.snapshot().categories
            if(draft?.values?.get("category")?.toLongOrNull()?.let {id->available.none {it.id==id}}==true)draft?.values?.set("category",(available.firstOrNull {it.name=="Altro"} ?: available.first()).id.toString())
            if(created!=null && !pickingFilter)draft?.values?.set("category",created.toString())
            go(21)
        } else {draft=null;draftBaseline=null;go(previous)}
    }
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
        body.addView(ui.label(title,22f,ui.colors.text,true));if(message.isNotBlank())body.addView(ui.label(message,13f,ui.colors.muted))
        actions.forEach {(name,action)->body.addView(ui.listRow(name){d.dismiss();action()});body.addView(ui.divider())}
        if(actions.size!=1 || actions[0].first!="Ho capito")body.addView(ui.button("Annulla") {d.dismiss()});d.setContentView(ScrollView(this).apply {addView(body)})
        dialog=d;d.show();animateSheet(body);d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,-2);setGravity(Gravity.BOTTOM);addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);setDimAmount(.4f);setWindowAnimations(0);ui.flatten(decorView);decorView.setPadding(0,0,0,0)}
    }
    private fun decision(title:String,message:String,accept:String,cancel:String="Annulla",destructive:Boolean=false,action:()->Unit) {
        dialog?.dismiss();hideKeyboard();val d=Dialog(this)
        val body=ui.column().apply {setPadding(ui.dp(22),ui.dp(22),ui.dp(22),ui.dp(22));background=ui.surface(ui.colors.card,24)}
        body.addView(ui.label("",12f).apply {setCompoundDrawablesWithIntrinsicBounds(null,UnsavedIcon(ui.dp(64)),null,null);gravity=Gravity.CENTER;setPadding(0,0,0,ui.dp(12))})
        body.addView(ui.label(title,18f,ui.colors.text,true).apply {gravity=Gravity.CENTER});body.addView(ui.space(10));body.addView(ui.label(message,14f,ui.colors.muted).apply {gravity=Gravity.CENTER});body.addView(ui.space(18))
        body.addView(ui.button(cancel,destructive){d.dismiss()}.apply {tag="cancel-action"})
        body.addView(ui.space(8));body.addView((if(destructive)ui.button(accept){d.dismiss();action()}.apply {background=ui.shape(ui.colors.line,24)} else ui.button(accept,true){d.dismiss();action()}).apply {tag="confirm-action"})
        val pageRoot=findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val clearBackdrop=DialogBackdrop.attach(pageRoot,ui.colors.dark);d.setOnDismissListener {clearBackdrop()}
        d.setContentView(ScrollView(this).apply {addView(body)});dialog=d;d.show()
        d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(minOf(resources.displayMetrics.widthPixels-ui.dp(48),ui.dp(380)),-2);setGravity(Gravity.CENTER);setDimAmount(.32f);setWindowAnimations(0);ui.flatten(decorView);decorView.setPadding(0,0,0,0)}
        animateSheet(body)
    }
    private fun animateSheet(body:View){if(android.animation.ValueAnimator.areAnimatorsEnabled()){body.alpha=0f;body.translationY=ui.dp(12).toFloat();body.animate().alpha(1f).translationY(0f).setDuration(160).start()}}
    private fun confirm(title:String,message:String,accept:String,destructive:Boolean=true,action:()->Unit)=decision(title,message,accept,destructive=destructive){change(action)}
    private fun render() {
        l=if(demo)Demo.ledger(store.snapshot().settings.theme) else store.snapshot()
        ui=Design(this,Palette.of(this,l.settings.theme));val c=ui.colors
        window.statusBarColor=c.background;window.navigationBarColor=c.background
        window.decorView.systemUiVisibility=if(c.dark)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if(android.os.Build.VERSION.SDK_INT>=27)View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        val root=ui.column().apply {background=ui.pageBackground();isFocusableInTouchMode=true}
        root.setOnApplyWindowInsetsListener {v,insets->
            if(android.os.Build.VERSION.SDK_INT>=30){val i=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom)}
            else v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        val titles=mapOf(0 to "Posso spenderlo?",1 to "Movimenti",2 to "Impegni",3 to "Crediti",4 to "Analisi",5 to "Simulatore",6 to "Impostazioni",7 to "Regole del saldo",8 to "Aspetto",9 to "Categorie",10 to "Backup",11 to "Avvisi",12 to "Come si calcola",13 to "Anticipo",20 to formTitle(),21 to "Scegli categoria",22 to "Numero di rate")
        if(demo)root.addView(ui.label("DEMO · DATI DIMOSTRATIVI",10f,c.muted,true).apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(12).toFloat(),10f*resources.displayMetrics.scaledDensity));setPadding(ui.dp(24),ui.dp(6),ui.dp(24),0)})
        val header=ui.row().apply {setPadding(ui.dp(18),ui.dp(if(page==0)24 else 4),ui.dp(18),ui.dp(4))}
        if(page!=0)header.addView(ui.button("‹"){onBackPressed()}.apply {contentDescription="Indietro";textSize=25f;background=android.graphics.drawable.InsetDrawable(ui.shape(c.hero,20),ui.dp(9));setPadding(0,0,0,0)},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)))
        val name=ui.column();name.addView(ui.label(if(page==0)"Ciao!" else titles[page] ?: "Posso spenderlo?",if(page==0)27f else 18f,c.text,true).apply {gravity=if(page==0)Gravity.START else Gravity.CENTER;maxLines=2})
        if(page==0)name.addView(ui.label("Ecco la tua situazione attuale",12f,c.muted))
        header.addView(name,LinearLayout.LayoutParams(0,-2,1f))
        if(page==0)header.addView(ui.button(""){go(6)}.apply {setCompoundDrawablesWithIntrinsicBounds(NavIcon(6,c.text,ui.dp(22)),null,null,null);contentDescription="Impostazioni";background=ui.shape(c.hero,24)},LinearLayout.LayoutParams(ui.dp(44),ui.dp(44)))
        else header.addView(View(this),LinearLayout.LayoutParams(ui.dp(48),1))
        root.addView(header)
        val scroll=ScrollView(this).apply {isFillViewport=false;clipToPadding=true}
        val body=ui.column().apply {setPadding(ui.dp(20),0,ui.dp(20),ui.dp(18))}
        when(page){0->home(body);1->movements(body);2->commitments(body);3->credits(body);4->analysis(body);5->simulator(body);6->settings(body);7->rules(body);8->appearance(body);9->categories(body);10->backups(body);11->reminders(body);12->calculation(body);13->creditDetail(body);20->form(body);21->categoryGrid(body,true);22->rates(body)}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(page==20)root.addView(ui.button(saveLabel(),true){
            if(page!=20 || draft==null)return@button
            if(demo && draft?.type!="creditFilter"){Toast.makeText(this,"Esci dalla demo per salvare.",Toast.LENGTH_SHORT).show();return@button}
            try {saveDraft?.invoke();finishForm(true);Toast.makeText(this,"Salvato",Toast.LENGTH_SHORT).show()}catch(e:Exception){fail(e)}
        }.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(12))}})
        if(page==22)root.addView(ui.button("Applica rate",true){applyRates()}.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(12))}})
        if(page in listOf(0,1,2,4))root.addView(navigation())
        setContentView(root);root.requestApplyInsets()
    }
    private fun navigation():View {
        val frame=FrameLayout(this);frame.layoutParams=LinearLayout.LayoutParams(-1,ui.dp(94))
        val nav=ui.row().apply {setPadding(ui.dp(8),ui.dp(7),ui.dp(8),ui.dp(2));background=ui.shape(ui.colors.card,22)}
        listOf(0 to "Home",1 to "Movimenti",4 to "Analisi",2 to "Impegni").forEachIndexed {index,(destination,label)->
            if(index==2)nav.addView(View(this),LinearLayout.LayoutParams(ui.dp(64),1))
            nav.addView(ui.button(label){go(destination)}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(11).toFloat(),10f*resources.displayMetrics.scaledDensity));setTextColor(if(page==destination)ui.colors.primary else ui.colors.muted);isSelected=page==destination;maxLines=1;setPadding(ui.dp(1),ui.dp(3),ui.dp(1),ui.dp(3));setCompoundDrawablesWithIntrinsicBounds(null,NavIcon(when(destination){0->0;1->3;4->1;else->2},if(page==destination)ui.colors.primary else ui.colors.muted,ui.dp(21)),null,null);compoundDrawablePadding=ui.dp(3);contentDescription="$label${if(page==destination)", sezione attiva" else ""}"},LinearLayout.LayoutParams(0,ui.dp(54),1f))
        }
        frame.addView(nav,FrameLayout.LayoutParams(-1,ui.dp(54),Gravity.BOTTOM))
        val add=ui.button("+",true){createMenu(page==2)}.apply {textSize=30f;contentDescription="Aggiungi";setPadding(0,0,0,0);background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(ui.colors.hero),android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,intArrayOf(ui.colors.primary,Color.parseColor("#43A6FF"))).apply {cornerRadius=ui.dp(99).toFloat()},ui.shape(Color.WHITE,99))}
        frame.addView(View(this).apply {background=ui.shape(ui.colors.line,99)},FrameLayout.LayoutParams(ui.dp(64),ui.dp(64),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        frame.addView(add,FrameLayout.LayoutParams(ui.dp(56),ui.dp(56),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        return frame
    }
    private fun saveLabel()=when(draft?.type){"creditFilter"->"Applica filtri";"series"->"Salva piano";"operation"->if(draft?.values?.get("kind")=="EXPENSE")"Salva spesa" else "Salva entrata";else->"Salva"}
    private fun home(body:LinearLayout) {
        val ring=SpendableRing(this,ui.colors,l).apply {setOnClickListener {go(12)};isFocusable=true}
        body.addView(ring,LinearLayout.LayoutParams(-1,ui.dp(if(resources.configuration.fontScale>=1.5f)252 else 248)))
        val totals=ui.row();listOf("Entrate" to Kind.INCOME,"Uscite" to Kind.EXPENSE).forEachIndexed {i,(name,kind)->
            val cat=Category(0,name,Color.parseColor(if(i==0)"#74CFA3" else "#F49BA8"),if(i==0)"income" else "bag")
            val card=ui.row().apply {background=ui.surface(ui.colors.card,16);setPadding(ui.dp(12),ui.dp(10),ui.dp(12),ui.dp(14))}
            card.addView(ui.button(""){moveFilter=if(i==0)3 else 4;go(1)}.apply {setCompoundDrawablesWithIntrinsicBounds(CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(30),ui.categoryWash(cat)),null,null,null);setPadding(0,0,0,0);contentDescription="$name del mese"},LinearLayout.LayoutParams(ui.dp(34),ui.dp(44)))
            val text=ui.column();text.addView(ui.label(name,11f,ui.colors.muted));text.addView(ui.amount(DisplayMoney.amount(l.month(YearMonth.now(),kind)),16f));card.addView(text,LinearLayout.LayoutParams(0,-2,1f))
            totals.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply {if(i==0)marginEnd=ui.dp(10)})
        };body.addView(totals)
        body.addView(ui.label("Questo mese · spese personali, anticipi e rimborsi esclusi",10f,ui.colors.muted))
        body.addView(ui.section("Prossimi impegni"));val pending=l.pending.filter {it.kind==Kind.EXPENSE}.take(3)
        if(pending.isEmpty())body.addView(ui.label("Nessun pagamento previsto",13f,ui.colors.muted))
        pending.forEach {o->
            val cat=l.categories.first {it.id==o.category};val row=ui.row().apply {setPadding(ui.dp(8),ui.dp(7),ui.dp(8),ui.dp(7));background=ui.surface(if(ui.colors.dark)ui.colors.card else Color.parseColor("#F9FCFF"),12)}
            row.addView(View(this).apply {background=CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(28),ui.categoryWash(cat))},LinearLayout.LayoutParams(ui.dp(28),ui.dp(28)).apply {marginEnd=ui.dp(10)})
            val text=ui.column();text.addView(ui.label(l.operationTitle(o),12f,ui.colors.text,true));text.addView(ui.label(o.date.format(dates),10f,ui.colors.muted));row.addView(text,LinearLayout.LayoutParams(0,-2,1.5f));row.addView(ui.amount(DisplayMoney.amount(o.cents),14f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));row.isFocusable=true;row.contentDescription="${l.operationTitle(o)}, ${o.date.format(dates)}, ${Money.format(o.cents)}";row.setOnClickListener {manage(o)};body.addView(row);body.addView(ui.space(3))
        }
        body.addView(ui.space(10));val actions=ui.row().apply {gravity=Gravity.TOP}
        listOf(Triple("Simula spesa",1,{go(5)}),Triple("Crediti",5,{go(3)}),Triple("Dettaglio",3,{go(12)}),Triple("Spesa veloce",0,{openForm("operation",defaults=mapOf("kind" to "EXPENSE","status" to "ACTUAL"))})).forEach {(name,icon,action)->actions.addView(ui.chip(name,icon,action).apply {if(name=="Dettaglio")contentDescription="Dettaglio del calcolo"},LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(ui.dp(2),0,ui.dp(2),0)})};body.addView(actions)
        if(demo)body.addView(ui.button("Esci dalla demo"){demo=false;go(0)})
    }
    private fun calculation(body:LinearLayout) {
        body.addView(ui.label("Solo denaro realmente disponibile",22f,ui.colors.text,true));ui.metric(body,"Liquidità",Money.format(l.balance));ui.metric(body,"− Impegni entro la previsione",Money.format(l.commitments),ui.colors.coral);ui.metric(body,"− Riserva protetta",Money.format(l.settings.reserve));body.addView(ui.divider());ui.metric(body,"Puoi spendere",Money.format(l.spendable),ui.colors.sage)
        body.addView(ui.section("La regola"));body.addView(ui.label("Il saldo include soltanto incassi, pagamenti, anticipi e rimborsi già effettuati. I crediti aperti e le entrate previste non sono denaro spendibile.",15f,ui.colors.muted))
        body.addView(ui.listRow("Aggiorna liquidità","Riconcilia il saldo reale senza inventare guadagni"){moneyForm("reconcile",l.balance)})
        body.addView(ui.listRow("Riserva e previsione","Modifica le regole del calcolo"){go(7)})
    }
    private fun simulator(body:LinearLayout) {
        body.addView(ui.label("Prima di decidere, prova.",25f,ui.colors.text,true));body.addView(ui.label("La simulazione protegge già impegni e riserva.",14f,ui.colors.muted));body.addView(ui.space(10))
        val input=ui.field(body,"Importo in euro",money=true).apply {textSize=32f}
        val resultBody=ui.column().apply {background=ui.shape(ui.colors.hero,20);setPadding(ui.dp(20),ui.dp(22),ui.dp(20),ui.dp(22))}
        val result=ui.amount("Inserisci un importo",30f,ui.colors.muted);val interpretation=ui.label("Il tuo margine attuale è ${Money.format(l.spendable)}.",14f,ui.colors.muted)
        resultBody.addView(ui.label("DOPO QUESTA SPESA",11f,ui.colors.sage,true));resultBody.addView(ui.space(6));resultBody.addView(result);resultBody.addView(interpretation)
        body.addView(ui.space(20));body.addView(resultBody)
        ui.pair(body,"Impegni già coperti",Money.format(l.commitments),"Riserva protetta",Money.format(l.settings.reserve))
        body.addView(ui.label("Questa prova non registra movimenti né modifica il saldo.",13f,ui.colors.muted))
        watch(input){value->if(value.isBlank()){result.text="Inserisci un importo";result.setTextColor(ui.colors.muted);interpretation.text="Il tuo margine attuale è ${Money.format(l.spendable)}.";input.error=null}else try {
            val n=l.simulated(Money.parse(value));result.text="Resterebbero ${Money.format(n)}";result.setTextColor(if(n<0)ui.colors.coral else ui.colors.sage)
            interpretation.text=if(n<0)"Questa spesa supera il margine di ${Money.format(-n)}: impegni e riserva non sarebbero interamente coperti." else "La spesa rientra nel margine calcolato. Entrate attese e crediti non sono inclusi."
            input.error=null
        }catch(e:Exception){input.error=e.message;result.text="Controlla l'importo";interpretation.text="Usa un importo positivo con massimo due decimali."}}
    }
    private fun monthControls(body:LinearLayout,step:Int=1) {
        val row=ui.row();row.addView(ui.button("‹"){month=month.minusMonths(step.toLong());selectedDay=null;render()}.apply {contentDescription="Mese precedente"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)))
        row.addView(ui.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ITALY)),15f,ui.colors.text,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f))
        row.addView(ui.button("›"){month=month.plusMonths(step.toLong());selectedDay=null;render()}.apply {contentDescription="Mese successivo"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)));body.addView(row)
    }
    private fun analysis(body:LinearLayout) {
        body.addView(ui.compactSegments(listOf("Mese","Trimestre","Anno"),listOf(1,3,12).indexOf(analysisSpan)){analysisSpan=listOf(1,3,12)[it];render()});monthControls(body,analysisSpan)
        val data=PeriodInsights(l,month,analysisSpan)
        if(analysisSpan>1)body.addView(ui.label("${data.start.format(dates)} – ${(if(data.start>LocalDate.now())data.end else minOf(data.end,LocalDate.now())).format(dates)} · periodo mobile",12f,ui.colors.muted))
        val detail=ui.label("Tocca una categoria per esplorarla",12f,ui.colors.muted)
        if(chartMode==0){
            body.addView(Charts(this,ui.colors,l,0,month,period,analysisSpan){detail.text=it;detail.visibility=View.VISIBLE}.apply {contentDescription="Distribuzione delle spese personali"},LinearLayout.LayoutParams(-1,ui.dp(198)))
            detail.textSize=10f;detail.visibility=View.GONE;body.addView(detail)
            if(data.ranking.isEmpty())body.addView(ui.label("Le categorie compariranno dopo la prima spesa.",13f,ui.colors.muted))
            data.ranking.forEach {(cat,n)->
                val row=ui.row();row.addView(View(this).apply {background=ui.shape(CategoryVisual.chartColor(cat),20)},LinearLayout.LayoutParams(ui.dp(7),ui.dp(7)).apply {marginEnd=ui.dp(9)})
                row.addView(ui.label(cat.name,11f),LinearLayout.LayoutParams(0,-2,1.4f));row.addView(ui.label("${String.format(Locale.ITALY,"%.0f",n*100.0/data.total)}%",11f,ui.colors.muted).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,.5f));row.addView(ui.amount(DisplayMoney.amount(n),12f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));row.setPadding(0,ui.dp(1),0,ui.dp(1));for(i in 1 until row.childCount)row.getChildAt(i).setPadding(0,0,0,0);row.setOnClickListener {detail.visibility=View.VISIBLE;detail.text="${cat.name} · ${Money.format(n)}"};row.isFocusable=true;body.addView(row)
            }
            body.addView(ui.label("Andamento liquidità",13f,ui.colors.text,true).apply {setPadding(0,ui.dp(8),0,ui.dp(4))});body.addView(RecordedCashLine(this,ui.colors,l,data.start,data.end),LinearLayout.LayoutParams(-1,ui.dp(92)))
            body.addView(ui.label("Saldo registrato · anticipi e rimborsi inclusi",11f,ui.colors.muted))
        }else {
            body.addView(Charts(this,ui.colors,l,chartMode,month,period){detail.text=it},LinearLayout.LayoutParams(-1,ui.dp(250)));body.addView(detail)
            if(chartMode==1){body.addView(ui.label("Blu: saldo reale · Corallo: uscite note · Turchese: entrate attese",12f,ui.colors.muted));body.addView(ui.button("Leggere la previsione"){sheet("Scenari, non disponibilità",listOf("Ho capito" to {}),"Le previsioni non aumentano lo spendibile. Anticipi e rimborsi sono inclusi nel saldo storico, non nei guadagni o nelle spese personali.")})}
            else body.addView(ui.segments(listOf("3 mesi","6 mesi","12 mesi"),listOf(3,6,12).indexOf(period)){period=listOf(3,6,12)[it];render()})
        }
        body.addView(ui.button("Approfondimenti"){sheet("Confronti e altre viste",listOf(
            "Confronto del periodo" to {sheet("Confronto del periodo",listOf("Ho capito" to {}),if(data.comparable){val delta=data.total-data.priorTotal;"${if(delta>0)"+" else ""}${Money.format(delta)} rispetto al periodo precedente equivalente"}else "Confronto non disponibile: servono spese in entrambi i periodi.")},
            "Spese ricorrenti" to {sheet("Pagamenti ricorrenti effettuati",listOf("Ho capito" to {}),"${Money.format(data.recurring)} nel periodo. Anticipi e rimborsi esclusi dalle spese personali.")},
            "Spese" to {chartMode=0;render()},"Liquidità" to {chartMode=1;render()},"Mesi" to {chartMode=2;render()}))}.apply {textSize=12f;background=ui.shape(ui.colors.hero,18)})

    }
    private data class HistoryItem(val date:LocalDate,val title:String,val subtitle:String,val signed:Long,val action:()->Unit)
    private fun movements(body:LinearLayout) {
        body.addView(ui.label("Questo mese",12f,ui.colors.muted));ui.pair(body,"Entrate personali",Money.format(l.month(YearMonth.now(),Kind.INCOME)),"Spese personali",Money.format(l.month(YearMonth.now(),Kind.EXPENSE)));
        val search=ui.input("Cerca movimenti",moveQuery);body.addView(search)
        val filters=ui.row();filters.addView(ui.button("Filtri"){sheet("Mostra movimenti",listOf("Tutti" to {moveFilter=0;render()},"Effettuati" to {moveFilter=1;render()},"Programmati" to {moveFilter=2;render()},"Entrate" to {moveFilter=3;render()},"Spese" to {moveFilter=4;render()},"Anticipi e rimborsi" to {moveFilter=5;render()}))}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity))},LinearLayout.LayoutParams(0,-2,1f))
        filters.addView(ui.button("Categoria"){run {pickingFilter=true;categoryQuery="";categoryTab=2;go(21)}}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(16).toFloat(),15f*resources.displayMetrics.scaledDensity))},LinearLayout.LayoutParams(0,-2,1f));body.addView(filters)
        val rows=ui.column();body.addView(rows)
        fun update() {
            rows.removeAllViews()
            val personal=l.operations.filter {it.status!=Status.DELETED && (categoryFilter==null || it.category==categoryFilter) && when(moveFilter){1->it.status==Status.ACTUAL;2->it.status==Status.PLANNED;3->it.kind==Kind.INCOME;4->it.kind==Kind.EXPENSE;5->false;else->true}}.map {o->HistoryItem(o.date,l.operationTitle(o),"${l.categoryName(o.category)} · ${if(o.status==Status.ACTUAL)"effettuato" else "programmato"}",o.signed){manage(o)}}
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
        val due=l.pending.filter {it.kind==Kind.EXPENSE && YearMonth.from(it.date)==month}
        ui.pair(body,"Pagamenti nel mese",Money.format(due.sumOf {it.cents}),"Scadenze",due.size.toString())
        body.addView(ui.segments(listOf("Lista","Calendario"),if(calendarGrid)1 else 0){calendarGrid=it==1;selectedDay=null;render()});monthControls(body)
        if(calendarGrid)calendar(body)
        else if(l.pending.any {it.kind==Kind.EXPENSE && it.date<=l.settings.horizon}) {
            body.addView(ui.section("Il percorso della liquidità"));body.addView(ui.label("Da oggi al ${l.settings.horizon.format(dates)} · solo uscite note",12f,ui.colors.muted))
            body.addView(CashTimeline(this,ui.colors,l),LinearLayout.LayoutParams(-1,ui.dp(116)))
            body.addView(ui.label("Saldo dopo gli impegni: ${Money.format(l.balance-l.commitments)}. La linea tratteggiata indica la riserva; entrate previste e crediti sono esclusi.",12f,ui.colors.muted));body.addView(ui.space(12))
        }
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
    private fun operationRow(body:LinearLayout,o:Operation) {body.addView(ui.listRow(l.operationTitle(o),"${l.categoryName(o.category)}${if(o.uncertain)" · non garantita" else ""}",(if(o.kind==Kind.EXPENSE)"− " else "+ ")+Money.format(o.cents),if(o.kind==Kind.EXPENSE)ui.colors.coral else ui.colors.sage){manage(o)});body.addView(ui.divider())}
    private fun calendar(body:LinearLayout) {
        val week=ui.row();listOf("L","M","M","G","V","S","D").forEach {name->week.addView(ui.label(name,11f,ui.colors.muted,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,ui.dp(28),1f))};body.addView(week)
        val offset=month.atDay(1).dayOfWeek.value-1;val count=((offset+month.lengthOfMonth()+6)/7)*7;var row=ui.row()
        for(i in 0 until count){if(i%7==0){row=ui.row();body.addView(row)};val day=i-offset+1
            if(day !in 1..month.lengthOfMonth()){row.addView(View(this),LinearLayout.LayoutParams(0,ui.dp(44),1f));continue}
            val date=month.atDay(day);val has=l.pending.any {it.date==date};val credit=l.advances.any {it.due==date && l.remaining(it)>0};val selected=date==selectedDay
            val cell=object:Button(this,null,0){private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);override fun onDraw(c:android.graphics.Canvas){super.onDraw(c);if(has || credit){paint.color=if(has)ui.colors.sage else ui.colors.chartCool;c.drawCircle(width/2f,height-ui.dp(5).toFloat(),ui.dp(2).toFloat(),paint)}}}.apply {
                ui.flatten(this);text=day.toString();textSize=13f;includeFontPadding=false;gravity=Gravity.CENTER;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(0,0,0,ui.dp(4));maxLines=1
                background=ui.shape(if(selected)ui.colors.card else Color.TRANSPARENT,8);setTextColor(if(selected)ui.colors.sage else ui.colors.text);isSelected=selected;contentDescription="${date.format(dates)}${if(has) ", impegni" else ""}${if(credit) ", restituzione attesa" else ""}"
                setOnClickListener {selectedDay=if(selected)null else date;render()}
            };row.addView(cell,LinearLayout.LayoutParams(0,ui.dp(44),1f))
        };body.addView(ui.space(8));if(selectedDay!=null)body.addView(ui.button("Tutto il mese"){selectedDay=null;render()})
    }
    private fun frequencyName(f:Frequency)=when(f){Frequency.WEEKLY->"Ogni settimana";Frequency.MONTHLY->"Ogni mese";Frequency.YEARLY->"Ogni anno"}
    private fun credits(body:LinearLayout) {
        body.addView(ui.label("ANCORA DA RECUPERARE",11f,ui.colors.muted,true));body.addView(ui.amount(Money.format(l.outstanding)))
        val received=l.refunds.filter {YearMonth.from(it.date)==YearMonth.now()}.sumOf {it.cents}
        ui.pair(body,"Recuperati questo mese",Money.format(received),"Anticipi aperti",l.advances.count {l.remaining(it)>0}.toString())
        body.addView(ui.segments(listOf("Tutti","Aperti","Recuperati"),when(creditStatus){1->1;3->2;else->0}){creditStatus=listOf(0,1,3)[it];render()});body.addView(ui.space(12))
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
        receipts.forEach {r->body.addView(ui.listRow(r.date.format(dates),r.note,Money.format(r.cents),ui.colors.sage){sheet("Rimborso",listOf("Modifica rimborso" to {openForm("refundEdit",r.id)},"Elimina rimborso" to {confirm("Eliminare il rimborso?","La liquidità diminuirà di ${Money.format(r.cents)} e il credito residuo aumenterà della stessa somma.","Elimina rimborso"){store.deleteRefund(r.id)}}))});body.addView(ui.divider())}
        body.addView(ui.button("Gestisci anticipo"){sheet("Gestisci anticipo",listOf("Modifica anticipo" to {openForm("advance",a.id)},"Elimina anticipo" to {confirm("Eliminare l'anticipo?","Verranno eliminati anche i suoi rimborsi. Il saldo aumenterà di ${Money.format(l.remaining(a))}. Usa questa azione per correggere una registrazione errata.","Elimina anticipo"){store.deleteAdvance(a.id);page=3}}))})
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
        if(o.status==Status.PLANNED)actions.add((if(o.kind==Kind.INCOME)"Conferma incasso oggi" else "Conferma pagamento oggi") to {confirm("Registrare ${Money.format(o.cents)}?","Il movimento diventerà effettivo oggi e cambierà la liquidità una sola volta.",if(o.kind==Kind.INCOME)"Registra incasso" else "Registra pagamento",false){store.settle(o.id)}})
        actions.add("Modifica" to {openForm("operation",o.id)})
        if(o.series==null)actions.add("Elimina movimento" to {confirm("Eliminare il movimento?",if(o.status==Status.ACTUAL)"Il saldo cambierà di ${Money.format(-o.signed)}. Il movimento verrà rimosso da storico, grafici e backup." else "La previsione sarà aggiornata; la liquidità attuale non cambia.",if(o.kind==Kind.EXPENSE)"Elimina spesa" else "Elimina movimento"){store.delete(o.id)}})
        else {
            actions.add("Elimina…" to {deleteChoices(o)})
            l.series.firstOrNull {it.id==o.series && it.active}?.let {s->actions.add("Modifica serie" to {openForm("series",s.id)})}
        }
        sheet(l.operationTitle(o),actions,"${o.date.format(dates)} · ${Money.format(o.cents)} · ${if(o.status==Status.ACTUAL)"già effettuato" else "da effettuare"}")
    }
    private fun deleteChoices(o:Operation) {
        sheet("Cosa vuoi eliminare?",listOf(
            "Soltanto questa occorrenza" to {confirm("Eliminare questa occorrenza?",if(o.status==Status.ACTUAL)"È già contabilizzata: il saldo cambierà di ${Money.format(-o.signed)}. Le altre occorrenze resteranno." else "Le altre occorrenze della serie resteranno previste.","Elimina questa occorrenza"){store.delete(o.id)}},
            "Questa e tutte le future" to {confirm("Interrompere da qui?","Verrà interrotta la programmazione da questa occorrenza in poi. I movimenti già effettuati, incluso questo se pagato, resteranno nello storico.","Interrompi le prossime"){store.removeSeries(o.series!!,o.index!!)}},
            "Intera serie" to {stopSeries(o.series!!)}
        ))
    }
    private fun stopSeries(id:Long) {
        val paid=l.actual.filter {it.series==id};val effect=-paid.sumOf {it.signed}
        sheet("Elimina intera serie",listOf(
            "Interrompi, mantieni lo storico" to {confirm("Interrompere la serie?","Tutti gli impegni non effettuati verranno eliminati. ${paid.size} movimenti effettivi resteranno: la liquidità non cambia.","Interrompi la serie"){store.removeSeries(id)}},
            "Elimina anche i movimenti effettuati" to {confirm("Rettificare anche lo storico?","Questa è una correzione contabile distinta: eliminerà ${paid.size} movimenti già effettuati. La liquidità passerà da ${Money.format(l.balance)} a ${Money.format(l.balance+effect)}. Conferma solo se quei movimenti erano errati.","Elimina l’intera serie"){store.removeSeries(id,removeActual=true)}}
        ),"L'interruzione della programmazione non deve cancellare pagamenti reali.")
    }
    private fun seriesMenu(s:Series)=sheet(s.title,listOf("Modifica serie" to {openForm("series",s.id)},"Elimina intera serie" to {stopSeries(s.id)}),"${frequencyName(s.frequency)} · ${Money.format(s.cents)}")
    private fun settings(body:LinearLayout) {
        body.addView(ui.section("Preferenze"))
        body.addView(ui.listRow("Regole del saldo","Liquidità iniziale, riserva e previsione"){go(7)});body.addView(ui.divider())
        body.addView(ui.listRow("Aspetto","Tema chiaro, scuro o di sistema"){go(8)});body.addView(ui.divider())
        body.addView(ui.listRow("Avvisi","Promemoria locali facoltativi"){go(11)})
        body.addView(ui.section("Archivio"));body.addView(ui.listRow("Categorie","Organizza le spese e le entrate"){go(9)});body.addView(ui.divider());body.addView(ui.listRow("Backup","Esporta e recupera i tuoi dati"){go(10)})
        body.addView(ui.section("App privata"));body.addView(ui.label("3.3 · offline · nessun account o costo ricorrente",13f,ui.colors.muted));body.addView(ui.button(if(demo)"Esci dalla demo" else "Esplora la demo separata"){demo=!demo;go(0)})
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
    private fun categories(body:LinearLayout)=categoryGrid(body,false)
    private fun inlineCategories(body:LinearLayout,d:Draft) {
        val fallback=(l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()).id.toString()
        val selected=d.values.getOrPut("category"){fallback}.toLong()
        val keys=listOf("basket","home","car","travel","wine","smoke","vape","health","bag","holiday","work","other")
        val choices=keys.mapNotNull {key->l.categories.firstOrNull {it.icon==key}}.toMutableList()
        if(choices.none {it.id==selected})l.categories.firstOrNull {it.id==selected}?.let {if(choices.size==12)choices[11]=it else choices.add(it)}
        val columns=if(resources.configuration.fontScale>=1.5f)3 else 4
        choices.chunked(columns).forEach {chunk->val row=ui.row().apply {gravity=Gravity.TOP};chunk.forEach {cat->row.addView(ui.categoryTile(cat,cat.id==selected,true){d.values["category"]=cat.id.toString();render()},LinearLayout.LayoutParams(0,-2,1f))};repeat(columns-chunk.size){row.addView(View(this),LinearLayout.LayoutParams(0,1,1f))};body.addView(row)}
        body.addView(ui.button("Categoria"){pickingFilter=false;categoryQuery="";categoryTab=2;go(21)}.apply {text=l.categoryName(selected);contentDescription="Categoria";textSize=13f;gravity=Gravity.START;setPadding(ui.dp(12),ui.dp(6),ui.dp(12),ui.dp(6));l.categories.firstOrNull {it.id==selected}?.let {cat->setCompoundDrawablesWithIntrinsicBounds(CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(24),ui.categoryWash(cat)),null,null,null);compoundDrawablePadding=ui.dp(10)};minHeight=ui.dp(40);minimumHeight=ui.dp(40);background=ui.shape(ui.colors.hero,12)})
    }
    private fun categoryGrid(body:LinearLayout,picker:Boolean) {
        val search=ui.input("Cerca categoria…",categoryQuery).apply {contentDescription="Cerca categorie";textSize=13f;minHeight=ui.dp(44);setPadding(ui.dp(12),ui.dp(7),ui.dp(12),ui.dp(7));background=ui.shape(ui.colors.hero,20).apply {setStroke(ui.dp(1),ui.colors.line)};setCompoundDrawablesWithIntrinsicBounds(SearchIcon(ui.colors.cool,ui.dp(16)),null,null,null);compoundDrawablePadding=ui.dp(8)};body.addView(search)
        val results=ui.column();body.addView(results)
        fun choose(cat:Category){if(!picker){openForm("category",cat.id)}else if(pickingFilter){categoryFilter=cat.id;go(1)}else {draft?.values?.set("category",cat.id.toString());go(20)}}
        fun edit(cat:Category){if(picker){parentDraft=draft;parentPrevious=previous;parentBaseline=draftBaseline};openForm("category",cat.id)}
        fun update(){
            results.removeAllViews()
            results.addView(ui.section("Preferite"))
            val favorites=l.categories.filter {it.favorite};val horizontal=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false};val row=ui.row()
            favorites.forEach {cat->row.addView(ui.categoryTile(cat,false,true){choose(cat)},LinearLayout.LayoutParams(ui.dp(if(resources.configuration.fontScale>=1.5f)100 else 72),-2))};horizontal.addView(row);results.addView(horizontal)
            if(favorites.isEmpty())results.addView(ui.label("Tocca un cuore per ritrovare qui le tue categorie.",12f,ui.colors.muted))
            val heading=ui.row();heading.addView(ui.section(if(categoryTab==2)"Tutte le categorie" else if(categoryTab==1)"Categorie preferite" else "Usate di recente"),LinearLayout.LayoutParams(0,-2,1f));heading.addView(ui.button("Filtri"){sheet("Mostra categorie",listOf("Recenti","Preferite","Tutte").mapIndexed {i,name->name to {categoryTab=i;render()}})}.apply {contentDescription="Filtra categorie";textSize=11f;setTextColor(ui.colors.cool)},LinearLayout.LayoutParams(ui.dp(52),ui.dp(48)));results.addView(heading)
            val recent=l.operations.filter {it.status!=Status.DELETED}.sortedByDescending {it.id}.map {it.category}.distinct().take(9)
            val choices=when(categoryTab){0->recent.mapNotNull {id->l.categories.firstOrNull {it.id==id}};1->favorites;else->l.categories}
            val found=choices.filter {categoryQuery.isBlank() || it.name.contains(categoryQuery,true) || (it.icon=="basket" && "alimentari".contains(categoryQuery,true)) || (it.icon=="vape" && "sigaretta elettronica".contains(categoryQuery,true))}
            if(found.isEmpty())results.addView(ui.label("Nessuna categoria in questa selezione",13f,ui.colors.muted))
            found.forEach {cat->val line=ui.row();val select=ui.button(cat.name){choose(cat)}.apply {tag="category-choice";gravity=Gravity.START;textSize=13f;typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL);setPadding(0,ui.dp(5),0,ui.dp(5));setCompoundDrawablesWithIntrinsicBounds(CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(32),ui.categoryWash(cat)),null,null,null);compoundDrawablePadding=ui.dp(12);contentDescription=cat.name;setOnLongClickListener {edit(cat);true}}
                line.addView(select,LinearLayout.LayoutParams(0,-2,1f))
                line.addView(ui.button(""){if(demo){Toast.makeText(this,"La demo non modifica i tuoi dati.",Toast.LENGTH_SHORT).show()}else try {store.category(cat.id,cat.name,cat.color,cat.icon,!cat.favorite);l=store.snapshot();update()}catch(e:Exception){fail(e)}}.apply {setCompoundDrawablesWithIntrinsicBounds(HeartIcon(if(cat.favorite)ui.colors.primary else ui.colors.muted,cat.favorite,ui.dp(18)),null,null,null);contentDescription="${if(cat.favorite)"Rimuovi" else "Aggiungi"} ${cat.name} ${if(cat.favorite)"dai" else "ai"} preferiti";setPadding(ui.dp(12),0,ui.dp(12),0)},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)))
                results.addView(line)
            }
        };watch(search){categoryQuery=it;update()};update()
        if(picker && pickingFilter)body.addView(ui.button("Tutte le categorie"){categoryFilter=null;go(1)})
        body.addView(ui.label("Tieni premuto sul nome per modificare",11f,ui.colors.muted));body.addView(ui.button("Nuova categoria"){if(picker){parentDraft=draft;parentPrevious=previous;parentBaseline=draftBaseline};openForm("category")})
    }
    private fun openRates(){
        val d=draft ?: return
        rateChoice=if(d.type!="series")"none" else if(d.values["rateMode"]=="continuous")"continuous" else "finite"
        rateCount=d.values["count"]?.takeIf {it.isNotBlank()} ?: "12";ratesAdvanced=rateChoice=="finite" && rateCount !in listOf("2","3","6","12");go(22)
    }
    private fun rates(body:LinearLayout) {
        val d=draft ?: return
        val amount=d.values["amount"]?.let {runCatching {Money.parse(it)}.getOrNull()}
        body.addView(ui.amount(amount?.let {DisplayMoney.amount(it)} ?: "—",32f).apply {gravity=Gravity.CENTER;background=ui.shape(ui.colors.hero,20);setPadding(ui.dp(10),ui.dp(14),ui.dp(10),ui.dp(14))})
        body.addView(ui.label("Importo di ciascuna rata",12f,ui.colors.muted).apply {gravity=Gravity.CENTER});body.addView(ui.space(12))
        val radioUpdates=mutableListOf<()->Unit>()
        fun option(name:String,subtitle:String,choice:String,n:Int?=null,enabled:Boolean=true){val selected=rateChoice==choice && (n==null || rateCount==n.toString())
            val row=ui.row().apply {setPadding(ui.dp(14),ui.dp(9),ui.dp(12),ui.dp(9));background=ui.shape(if(selected)ui.colors.hero else ui.colors.card,16).apply {if(selected)setStroke(ui.dp(1),ui.colors.primary)};isEnabled=enabled;alpha=if(enabled)1f else .45f}
            row.addView(View(this).apply {background=RadioIcon(selected,if(selected)ui.colors.primary else ui.colors.muted,ui.dp(22))},LinearLayout.LayoutParams(ui.dp(22),ui.dp(22)).apply {marginEnd=ui.dp(12)})
            val text=ui.column();text.addView(ui.label(name,14f,ui.colors.text,true));text.addView(ui.label(subtitle,12f,ui.colors.muted));row.addView(text,LinearLayout.LayoutParams(0,-2,1f));row.contentDescription=name;row.isSelected=selected;row.isFocusable=true;row.minimumHeight=ui.dp(56)
            radioUpdates.add {val checked=rateChoice==choice && (n==null || rateCount==n.toString());row.isSelected=checked;row.background=ui.shape(if(checked)ui.colors.hero else ui.colors.card,16).apply {if(checked)setStroke(ui.dp(1),ui.colors.primary)};row.getChildAt(0).background=RadioIcon(checked,if(checked)ui.colors.primary else ui.colors.muted,ui.dp(22))}
            if(enabled)row.setOnClickListener {rateChoice=choice;if(n!=null)rateCount=n.toString();render()};body.addView(row);body.addView(ui.space(7))
        }
        option("Nessuna rata","Spesa singola","none",enabled=d.id==0L)
        listOf(2,3,6,12).forEach {n->option("$n rate",if(amount==null)"Importo per rata da inserire" else "${DisplayMoney.amount(amount)} per rata","finite",n)}
        val countValue=rateCount.toIntOrNull()
        val summary=ui.label(if(rateChoice=="finite" && amount!=null && countValue in 1..120)"$rateCount rate · totale ${DisplayMoney.amount(amount*countValue!!)}" else if(rateChoice=="continuous")"Piano: ricorrenza continua" else "Nessuna suddivisione",12f,ui.colors.muted);body.addView(summary)
        body.addView(ui.button(if(ratesAdvanced)"Meno opzioni" else "Altre opzioni"){ratesAdvanced=!ratesAdvanced;render()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
        if(ratesAdvanced){
        option("Ricorrenza continua","Senza numero finale, alla frequenza scelta","continuous")
        body.addView(ui.section("Numero personalizzato · da 1 a 120"))
        val row=ui.row();val count=ui.input("12",rateCount).apply {contentDescription="Numero rate";inputType=android.text.InputType.TYPE_CLASS_NUMBER;filters=arrayOf(InputFilter.LengthFilter(3));gravity=Gravity.CENTER}
        val minus=ui.button("−"){val n=count.text.toString().toIntOrNull() ?: 1;count.setText(maxOf(1,n-1).toString())}.apply {contentDescription="Riduci rate"}
        val plus=ui.button("+"){val n=count.text.toString().toIntOrNull() ?: 1;count.setText(minOf(120,n+1).toString())}.apply {contentDescription="Aumenta rate"}
        row.addView(minus,LinearLayout.LayoutParams(ui.dp(48),ui.dp(52)));row.addView(count,LinearLayout.LayoutParams(0,ui.dp(52),1f));row.addView(plus,LinearLayout.LayoutParams(ui.dp(48),ui.dp(52)));body.addView(row)
        fun update(){val n=count.text.toString().toIntOrNull();minus.isEnabled=n!=null && n>1;plus.isEnabled=n!=null && n<120;count.error=if(n==null || n !in 1..120)"Da 1 a 120" else null}
        watch(count){rateCount=it;rateChoice="finite";update();radioUpdates.forEach {refresh->refresh()};val n=it.toIntOrNull();summary.text=if(n!=null && n in 1..120 && amount!=null)"$n rate · totale ${DisplayMoney.amount(amount*n)}" else "Scegli da 1 a 120 rate"};update()
        }
        body.addView(ui.label("L’importo è di ogni rata. Applica prepara il piano, non registra pagamenti.",11f,ui.colors.muted))
    }
    private fun applyRates(){try {
        val d=draft ?: return
        if(rateChoice=="none"){require(d.id==0L);draft=Draft("operation",values=d.values.toMutableMap().apply {put("count","");put("rateMode","none")})}
        else {val n=if(rateChoice=="continuous")0 else rateCount.toIntOrNull()?.also {require(it in 1..120){"Scegli da 1 a 120 rate."}} ?: error("Numero rate non valido.")
            draft=Draft("series",d.id,d.values.toMutableMap().apply {put("count",if(n==0)"" else n.toString());put("rateMode",if(n==0)"continuous" else "finite");put("frequency",get("frequency") ?: "MONTHLY")})}
        go(20)
    }catch(e:Exception){fail(e)}}
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
    private fun formTitle()=when(draft?.type){"operation"->if(draft?.id==0L)if(draft?.values?.get("kind")!="INCOME")"Nuova spesa" else "Nuova entrata" else "Modifica movimento";"series"->if(draft?.id==0L)"Ricorrenza o rate" else "Modifica serie";"advance"->if(draft?.id==0L)"Nuovo anticipo" else "Modifica anticipo";"refund"->"Registra rimborso";"refundEdit"->"Modifica rimborso";"category"->"Categoria";"opening"->"Liquidità iniziale";"reserve"->"Riserva protetta";"reconcile"->"Liquidità attuale";"creditFilter"->"Filtri crediti";else->"Inserimento"}
    private fun openForm(type:String,id:Long=0,defaults:Map<String,String> = emptyMap()) {
        draftBaseline=null;previous=page;val v=mutableMapOf<String,String>();v.putAll(defaults)
        if(type=="operation" && id!=0L)l.operations.first {it.id==id}.let {o->v.putAll(mapOf("amount" to Money.input(o.cents),"description" to o.title,"kind" to o.kind.name,"status" to o.status.name,"date" to o.date.toString(),"category" to o.category.toString(),"uncertain" to o.uncertain.toString()))}
        if(type=="series" && id!=0L)l.series.first {it.id==id}.let {s->v.putAll(mapOf("amount" to Money.input(s.cents),"description" to s.title,"kind" to s.kind.name,"date" to s.start.toString(),"category" to s.category.toString(),"uncertain" to s.uncertain.toString(),"frequency" to s.frequency.name,"count" to if(s.count==0)"" else s.count.toString()))}
        if(type=="advance" && id!=0L)l.advances.first {it.id==id}.let {a->v.putAll(mapOf("amount" to Money.input(a.cents),"description" to a.description,"debtor" to a.debtor,"date" to a.date.toString(),"project" to a.project,"cause" to a.cause,"due" to (a.due?.toString() ?: ""),"notes" to a.notes,"advanced" to "true"))}
        if(type=="refundEdit")l.refunds.first {it.id==id}.let {r->v.putAll(mapOf("amount" to Money.input(r.cents),"date" to r.date.toString(),"notes" to r.note,"advance" to r.advance.toString()))}
        if(type=="refund")v["advance"]=id.toString()
        if(type=="category" && id!=0L)l.categories.first {it.id==id}.let {c->v["description"]=c.name;v["color"]=c.color.toString();v["icon"]=c.icon;v["favorite"]=c.favorite.toString()}
        draft=Draft(type,id,v);go(20)
    }
    private fun moneyForm(type:String,value:Long)=openForm(type,defaults=mapOf("amount" to Money.input(value)))
    private fun watch(field:EditText,action:(String)->Unit){field.addTextChangedListener(object:TextWatcher {override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){action(s.toString())}})}
    private fun pick(initial:LocalDate,action:(LocalDate)->Unit){
        val d=DatePickerDialog(this,{_,y,m,day->action(LocalDate.of(y,m+1,day))},initial.year,initial.monthValue-1,initial.dayOfMonth)
        dialog?.dismiss();dialog=d;d.show();d.getButton(Dialog.BUTTON_POSITIVE).text="Scegli data";d.getButton(Dialog.BUTTON_NEGATIVE).text="Annulla"
        listOf(Dialog.BUTTON_POSITIVE,Dialog.BUTTON_NEGATIVE).forEach {which->d.getButton(which)?.apply {ui.flatten(this);background=ui.button(text.toString(),which==Dialog.BUTTON_POSITIVE){}.background;setTextColor(if(which==Dialog.BUTTON_POSITIVE){ui.colors.onPrimary}else ui.colors.text);isAllCaps=false;minHeight=ui.dp(48)}}
        d.window?.apply {setBackgroundDrawable(ui.shape(ui.colors.card,20));setWindowAnimations(0);ui.flatten(decorView)}
    }
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
        fun category() {
            val default=l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()
            val cat=l.categories.first {it.id==value("category",default.id.toString()).toLong()}
            body.addView(ui.listRow("Categoria",cat.name){pickingFilter=false;categoryQuery="";categoryTab=2;go(21)})
        }
        fun advanced(action:()->Unit) {
            body.addView(ui.button(if(value("advanced","false")=="true")"Meno opzioni" else "Opzioni avanzate"){d.values["advanced"]=(value("advanced")=="false").toString();render()})
            if(value("advanced")=="true")action()
        }
        when(d.type) {
            "operation","series"->{
                val series=d.type=="series";val adjustment=value("kind","EXPENSE")=="ADJUSTMENT"
                val expense=value("kind","EXPENSE")=="EXPENSE"
                if(!expense && !adjustment)body.addView(ui.segments(listOf("Spesa","Entrata"),1){d.values["kind"]=if(it==0)"EXPENSE" else "INCOME";render()})
                val amount=ui.moneyEntry(body,"Importo in euro",value("amount")).apply {tag="amount"};watch(amount){d.values["amount"]=it}
                if(adjustment)amount.inputType=amount.inputType or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                if(series)body.addView(ui.label("Importo di ciascuna rata",12f,ui.colors.muted).apply {gravity=Gravity.CENTER})
                if(expense){body.addView(ui.section("Categorie"));inlineCategories(body,d)}else category()
                field("description","Descrizione").apply {hint=if(expense)"Facoltativa" else "Compenso ricevuto";textSize=14f;minHeight=ui.dp(38);setPadding(0,ui.dp(3),0,ui.dp(3));background=null}
                if(expense){
                    val day=value("date",LocalDate.now().toString());val fields=ui.row().apply {orientation=if(resources.configuration.fontScale>=1.5f)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL;background=ui.shape(ui.colors.hero,12)}
                    val dateView=ui.miniField(if(series)"Prima scadenza" else "Data",LocalDate.parse(day).format(dates)){pick(LocalDate.parse(day)){d.values["date"]=it.toString();render()}}
                    fields.addView(dateView,if(fields.orientation==LinearLayout.VERTICAL)LinearLayout.LayoutParams(-1,-2)else LinearLayout.LayoutParams(0,-2,1.2f))
                    if(!adjustment && (d.id==0L || series))fields.addView(ui.miniField("Rate",if(series){if(value("rateMode",if(value("count").isBlank())"continuous" else "finite")=="continuous")"Continua" else "${value("count")} rate"}else "Nessuna"){openRates()},if(fields.orientation==LinearLayout.VERTICAL)LinearLayout.LayoutParams(-1,-2)else LinearLayout.LayoutParams(0,-2,1f))
                    body.addView(ui.space(10));body.addView(fields)
                }else{date("date",if(series)"Prima scadenza" else "Data");if(!adjustment && (d.id==0L || series))body.addView(ui.listRow("Rate",if(series)"Piano rateale" else "Nessuna rata"){openRates()})}
                if(series)option("frequency","Ripetizione",Frequency.entries.map {it.name to frequencyName(it)},"MONTHLY")
                advanced {
                    if(!series && !adjustment)option("status","Quando incide sul saldo?",listOf("ACTUAL" to "Già effettuato","PLANNED" to "Previsto, ancora da effettuare"),"ACTUAL")
                    if(value("kind")=="INCOME" && (series || value("status","ACTUAL")=="PLANNED"))option("uncertain","Entrata non garantita",listOf("false" to "No","true" to "Sì, solo uno scenario"),"false")
                    if(!series && d.id==0L && !adjustment)body.addView(ui.button("Trasforma in ricorrenza o rate"){draft=Draft("series",values=d.values.toMutableMap().apply {put("status","PLANNED");put("rateMode","continuous")});render()})
                }
                value("status","ACTUAL");value("kind","EXPENSE");value("category",(l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()).id.toString());if(series){value("rateMode",if(value("count").isBlank())"continuous" else "finite");value("frequency","MONTHLY")}
                saveDraft={
                    val kind=Kind.valueOf(value("kind"));val cents=Money.parse(value("amount"),kind==Kind.ADJUSTMENT);val rawName=value("description").trim();val name=if(series && rawName.isBlank() && kind==Kind.EXPENSE)l.categoryName(value("category").toLong()) else rawName;val day=LocalDate.parse(value("date"));val cat=value("category").toLong()
                    val uncertain=value("uncertain","false")=="true" && kind==Kind.INCOME && (series || value("status")=="PLANNED")
                    if(series){val count=if(value("rateMode")=="continuous")0 else value("count").toIntOrNull()?.also {require(it in 1..120){"Scegli da 1 a 120 rate."}} ?: error("Numero rate non valido.");val s=Series(d.id,name,cents,kind,cat,day,Frequency.valueOf(value("frequency")),count,uncertain);if(d.id==0L)store.saveSeries(s) else store.updateSeries(s)}
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
                option("icon","Icona",CategoryCatalog.entries.map {it.icon to it.name},"other")
                option("favorite","Preferita",listOf("false" to "No","true" to "Sì"),"false")
                if(d.id!=0L)body.addView(ui.destructive("Elimina categoria"){confirm("Eliminare la categoria?","È possibile soltanto se non è utilizzata da movimenti o ricorrenze attive.","Elimina categoria"){store.deleteCategory(d.id);finishForm(false)}})
                saveDraft={store.category(d.id.takeIf {it!=0L},value("description"),value("color").toInt(),value("icon"),value("favorite")=="true")}
            }
            "creditFilter"->{
                field("subject","Soggetto");field("project","Immobile / progetto")
                option("state","Stato",listOf("Tutti","Aperti","Parziali","Recuperati","Scaduti").mapIndexed {i,name->i.toString() to name},"0")
                option("month","Periodo",listOf("false" to "Tutto il periodo","true" to "Mese selezionato"),"false")
                body.addView(ui.button("Azzera filtri"){d.values.clear();render()})
                saveDraft={creditSubject=value("subject");creditProject=value("project");creditStatus=value("state").toInt();creditMonth=value("month")=="true"}
            }
        }
        if(draftBaseline==null)draftBaseline=draftFingerprint()
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
                confirm("Sostituire i dati attuali?","Il backup contiene ${imported.operations.count {it.status!=Status.DELETED}} operazioni e ${imported.advances.size} anticipi. Prima di sostituire i dati verrà salvata sul dispositivo una copia dell'archivio attuale, esportabile da Altro. Il backup deve provenire da una fonte fidata.","Importa e sostituisci") {
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
