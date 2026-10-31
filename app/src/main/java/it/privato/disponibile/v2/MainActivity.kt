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
    internal lateinit var store:Store
    internal lateinit var l:Ledger
    internal lateinit var ui:Design
    internal var page=0
    internal var previous=0
    internal var demo=false
    internal var month=YearMonth.now()
    internal var chartMode=0
    internal var period=6
    internal var analysisSpan=1
    internal var rateChoice="none"
    internal var rateCount="12"
    internal var ratesAdvanced=false
    internal var rateBaseline:String?=null
    internal var calendarGrid=false
    internal var commitmentForecast=false
    internal var renderedDay=LocalDate.now()
    internal var selectedDay:LocalDate?=null
    internal var moveQuery=""
    internal var creditQuery=""
    internal var moveFilter=1
    internal var categoryFilter:Long?=null
    internal var creditSubject=""
    internal var creditProject=""
    internal var creditStatus=1
    internal var creditMonth=false
    internal var detailId=0L
    internal var exportPrevious=false
    internal var dialog:Dialog?=null
    internal var draft:Draft?=null
    internal var draftBaseline:String?=null
    internal var categoryQuery=""
    internal var categoryTab=2
    internal var pickingFilter=false
    internal var parentDraft:Draft?=null
    internal var parentPrevious=0
    internal var parentBaseline:String?=null
    internal var pickerState:DateSelection?=null
    internal var pickerTarget=""
    internal lateinit var onboarding:OnboardingState
    internal var simulationDraft=SimulationDraft()
    internal var saving=false
    internal var saveDraft:(()->Unit)?=null
    internal val dates=DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ITALY)
    internal data class Draft(val type:String,val id:Long=0,val values:MutableMap<String,String> = mutableMapOf()) {
        fun json()=JSONObject().put("type",type).put("id",id).put("values",JSONObject(values)).toString()
        companion object {fun parse(text:String):Draft {val j=JSONObject(text);val v=j.getJSONObject("values");return Draft(j.getString("type"),j.getLong("id"),v.keys().asSequence().associateWith {v.getString(it)}.toMutableMap())}}
    }
    override fun onCreate(state:Bundle?) {
        store=Store(this)
        val palette=Palette.of(this,store.snapshot().settings.theme)
        setTheme(if(palette.dark)R.style.AppThemeDark else R.style.AppTheme)
        super.onCreate(state)
        rateBaseline=state?.getString("rateBaseline")
        page=state?.getInt("page") ?: 0;previous=state?.getInt("previous") ?: 0;demo=state?.getBoolean("demo") ?: false
        selectedDay=state?.getString("selectedDay")?.let {LocalDate.parse(it)}
        month=state?.getString("month")?.let {YearMonth.parse(it)} ?: YearMonth.now()
        chartMode=state?.getInt("chartMode") ?: 0;calendarGrid=state?.getBoolean("grid") ?: false
        detailId=state?.getLong("detailId") ?: 0;draft=state?.getString("draft")?.let {Draft.parse(it)}
        draftBaseline=state?.getString("baseline");parentDraft=state?.getString("parentDraft")?.let {Draft.parse(it)}
        parentBaseline=state?.getString("parentBaseline");parentPrevious=state?.getInt("parentPrevious") ?: 0
        pickingFilter=state?.getBoolean("pickingFilter") ?: false
        ratesAdvanced=state?.getBoolean("ratesAdvanced") ?: false;categoryTab=state?.getInt("categoryTab") ?: 2;categoryQuery=state?.getString("categoryQuery") ?: ""
        analysisSpan=state?.getInt("analysisSpan") ?: 1;rateChoice=state?.getString("rateChoice") ?: "none";rateCount=state?.getString("rateCount") ?: "12"
        moveQuery=state?.getString("moveQuery","") ?: "";creditQuery=state?.getString("creditQuery","") ?: ""
        moveFilter=state?.getInt("moveFilter",1) ?: 1;creditStatus=state?.getInt("creditStatus",1) ?: 1
        categoryFilter=state?.getLong("categoryFilter",0)?.takeIf {it>0};creditSubject=state?.getString("creditSubject","") ?: "";creditProject=state?.getString("creditProject","") ?: "";creditMonth=state?.getBoolean("creditMonth") ?: false
        commitmentForecast=state?.getBoolean("commitmentForecast") ?: false
        simulationDraft=state?.getString("simulation")?.let {SimulationDraft.decode(it)} ?: SimulationDraft()
        exportPrevious=state?.getBoolean("exportPrevious") ?: false
        if(android.os.Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false)
        window.setSoftInputMode(if(android.os.Build.VERSION.SDK_INT>=30)WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onboarding=OnboardingState(this)
        if(state==null && !store.snapshot(false).settings.onboardingCompleted && !onboarding.paused)page=30
        render()
        state?.getString("dateSelection")?.let {pickerState=DateSelection.decode(it);pickerTarget=state.getString("pickerTarget","");showPicker()}
    }
    override fun onSaveInstanceState(s:Bundle) {
        super.onSaveInstanceState(s);s.putString("moveQuery",moveQuery);s.putString("creditQuery",creditQuery);s.putInt("moveFilter",moveFilter);s.putInt("creditStatus",creditStatus);s.putLong("categoryFilter",categoryFilter ?: 0);s.putString("creditSubject",creditSubject);s.putString("creditProject",creditProject);s.putBoolean("creditMonth",creditMonth);s.putBoolean("commitmentForecast",commitmentForecast);s.putString("rateBaseline",rateBaseline);s.putString("simulation",simulationDraft.encode());s.putString("selectedDay",selectedDay?.toString());pickerState?.let {s.putString("dateSelection",it.encode());s.putString("pickerTarget",pickerTarget)};s.putBoolean("ratesAdvanced",ratesAdvanced);s.putInt("categoryTab",categoryTab);s.putString("categoryQuery",categoryQuery);s.putInt("analysisSpan",analysisSpan);s.putString("rateChoice",rateChoice);s.putString("rateCount",rateCount);s.putInt("page",page);s.putInt("previous",previous);s.putBoolean("demo",demo);s.putString("month",month.toString());s.putInt("chartMode",chartMode);s.putBoolean("grid",calendarGrid);s.putLong("detailId",detailId);draft?.let {s.putString("draft",it.json())};s.putBoolean("exportPrevious",exportPrevious);s.putString("baseline",draftBaseline);s.putString("parentBaseline",parentBaseline);s.putInt("parentPrevious",parentPrevious);s.putBoolean("pickingFilter",pickingFilter);parentDraft?.let {s.putString("parentDraft",it.json())}
    }
    override fun onResume(){super.onResume();if(renderedDay!=LocalDate.now()){renderedDay=LocalDate.now();render()}}
    override fun onDestroy(){dialog?.dismiss();store.close();super.onDestroy()}
    @Deprecated("Android 8 compatible back navigation") override fun onBackPressed() {
        if(page==30){onboarding.paused=true;go(0)}
        else if(page==22){rateBaseline?.let {draft=Draft.parse(it)};rateBaseline=null;go(20)}
        else if(page==21)go(if(pickingFilter)1 else 20)
        else if(page==20) {
            if(draftFingerprint()==draftBaseline)finishForm(false)
            else decision("Uscire senza salvare?","Le modifiche che hai inserito andranno perse.","Esci senza salvare","Continua a modificare",true){finishForm(false)}
        }
        else if(page==13)go(3) else if(page>=4)go(if(page in 7..11)6 else 0) else if(page!=0)go(0) else super.onBackPressed()
    }
    internal fun go(destination:Int){if(destination==9){categoryQuery="";categoryTab=2};hideKeyboard();dialog?.dismiss();page=destination;render();if(android.animation.ValueAnimator.areAnimatorsEnabled()){val v=findViewById<View>(android.R.id.content);v.alpha=.75f;v.animate().alpha(1f).setDuration(140).start()}}
    internal fun draftFingerprint():String?=draft?.let {d->d.type+":"+d.values.filter {it.key !in listOf("advanced","commandToken","quickIds") && it.value.isNotBlank() && it.value!="false"}.toSortedMap().toString()}
    internal fun finishForm(saved:Boolean) {
        if(previous==21 && draft?.type=="category") {
            val created=if(saved)draft?.id?.takeIf {it>0} ?: store.snapshot().categories.lastOrNull()?.id else null
            draft=parentDraft;previous=parentPrevious;draftBaseline=parentBaseline;parentDraft=null;parentBaseline=null
            val available=store.snapshot().categories
            if(draft?.values?.get("category")?.toLongOrNull()?.let {id->available.none {it.id==id}}==true)draft?.values?.set("category",(available.firstOrNull {it.name=="Altro"} ?: available.first()).id.toString())
            if(created!=null && !pickingFilter)draft?.values?.set("category",created.toString())
            go(21)
        } else {draft=null;draftBaseline=null;go(previous)}
    }
    internal fun hideKeyboard(){currentFocus?.let {getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(it.windowToken,0)}}
    internal fun fail(e:Exception) {sheet("Controlla i dati",listOf("Ho capito" to {}),e.message ?: "Operazione non completata: i dati precedenti sono conservati.")}
    internal fun change(action:()->Unit) {
        if(demo){Toast.makeText(this,"La demo non modifica i tuoi dati.",Toast.LENGTH_SHORT).show();return}
        try {action();render();Toast.makeText(this,"Aggiornato",Toast.LENGTH_SHORT).show()}catch(e:Exception){fail(e)}
    }
    internal fun sheet(title:String,actions:List<Pair<String,()->Unit>>,message:String="") {
        dialog?.dismiss()
        val d=Dialog(this);val body=ui.column().apply {setPadding(ui.dp(24),ui.dp(18),ui.dp(24),ui.dp(16));background=ui.shape(ui.colors.card,20)}
        body.addView(View(this).apply {background=ui.shape(ui.colors.line,2);layoutParams=LinearLayout.LayoutParams(ui.dp(34),ui.dp(4)).apply {gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=ui.dp(14)}})
        body.addView(ui.label(title,22f,ui.colors.text,true));if(message.isNotBlank())body.addView(ui.label(message,13f,ui.colors.muted))
        actions.forEach {(name,action)->body.addView(ui.listRow(name){d.dismiss();action()});body.addView(ui.divider())}
        if(actions.size!=1 || actions[0].first!="Ho capito")body.addView(ui.button("Annulla") {d.dismiss()});d.setContentView(ScrollView(this).apply {addView(body)})
        dialog=d;d.show();animateSheet(body);d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,-2);setGravity(Gravity.BOTTOM);addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);setDimAmount(.4f);setWindowAnimations(0);ui.flatten(decorView);decorView.setPadding(0,0,0,0)}
    }
    internal fun decision(title:String,message:String,accept:String,cancel:String="Annulla",destructive:Boolean=false,action:()->Unit) {
        dialog?.dismiss();hideKeyboard();val d=Dialog(this)
        val body=ui.column().apply {setPadding(ui.dp(22),ui.dp(22),ui.dp(22),ui.dp(22));background=ui.surface(ui.colors.card,24)}
        body.addView(ui.label("",12f).apply {setCompoundDrawablesWithIntrinsicBounds(null,UnsavedIcon(ui.dp(92)),null,null);gravity=Gravity.CENTER;setPadding(0,0,0,ui.dp(12))})
        body.addView(ui.label(title,18f,ui.colors.text,true).apply {gravity=Gravity.CENTER});body.addView(ui.space(10));body.addView(ui.label(message,14f,ui.colors.muted).apply {gravity=Gravity.CENTER});body.addView(ui.space(18))
        body.addView(ui.button(cancel,destructive){d.dismiss()}.apply {tag="cancel-action"})
        body.addView(ui.space(8));body.addView((if(destructive)ui.button(accept){d.dismiss();action()}.apply {background=ui.shape(ui.colors.line,24)} else ui.button(accept,true){d.dismiss();action()}).apply {tag="confirm-action"})
        val pageRoot=findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val clearBackdrop=DialogBackdrop.attach(pageRoot,ui.colors.dark);d.setOnDismissListener {clearBackdrop()}
        d.setContentView(ScrollView(this).apply {addView(body)});dialog=d;d.show()
        d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(minOf(resources.displayMetrics.widthPixels-ui.dp(36),ui.dp(380)),-2);setGravity(Gravity.CENTER);setDimAmount(.32f);setWindowAnimations(0);ui.flatten(decorView);decorView.setPadding(0,0,0,0)}
        animateSheet(body)
    }
    internal fun animateSheet(body:View){if(android.animation.ValueAnimator.areAnimatorsEnabled()){body.alpha=0f;body.translationY=ui.dp(12).toFloat();body.animate().alpha(1f).translationY(0f).setDuration(160).start()}}
    internal fun confirm(title:String,message:String,accept:String,destructive:Boolean=true,action:()->Unit)=decision(title,message,accept,destructive=destructive){change(action)}
    internal fun render() {
        l=if(demo)Demo.ledger(store.snapshot().settings.theme) else store.snapshot(expand=page!=5)
        ui=Design(this,Palette.of(this,l.settings.theme));val c=ui.colors
        window.statusBarColor=c.background;window.navigationBarColor=c.background
        window.decorView.systemUiVisibility=if(c.dark)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if(android.os.Build.VERSION.SDK_INT>=27)View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        val root=ui.column().apply {background=ui.pageBackground();isFocusableInTouchMode=true}
        root.setOnApplyWindowInsetsListener {v,insets->
            if(android.os.Build.VERSION.SDK_INT>=30){val i=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom)}
            else v.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        val titles=mapOf(0 to "Posso spenderlo?",1 to "Movimenti",2 to "Impegni",3 to "Anticipi da recuperare",4 to "Analisi",5 to "Simula una spesa",6 to "Impostazioni",7 to "Regole del saldo",8 to "Aspetto",9 to "Categorie",10 to "Backup",11 to "Avvisi",12 to "Come si calcola",13 to "Anticipo",20 to formTitle(),21 to "Scegli categoria",22 to "Numero di rate")
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
        when(page){30->onboardingScreen(body);0->home(body);1->movements(body);2->commitments(body);3->credits(body);4->analysis(body);5->simulator(body);6->settings(body);7->rules(body);8->appearance(body);9->categories(body);10->backups(body);11->reminders(body);12->calculation(body);13->creditDetail(body);20->form(body);21->categoryGrid(body,true);22->rates(body)}
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        if(page==20)root.addView(ui.button(saveLabel(),true){
            if(page!=20 || draft==null || saving)return@button
            if(demo && draft?.type!="creditFilter"){Toast.makeText(this,"Esci dalla demo per salvare.",Toast.LENGTH_SHORT).show();return@button}
            try {saving=true;val token=draft!!.values.getOrPut("commandToken"){java.util.UUID.randomUUID().toString()};store.once(token){saveDraft?.invoke()};finishForm(true);Toast.makeText(this,"Salvato",Toast.LENGTH_SHORT).show()}catch(e:Exception){fail(e)}finally{saving=false}
        }.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(12))}})
        if(page==22)root.addView(ui.button("Applica rate",true){applyRates()}.apply {layoutParams=LinearLayout.LayoutParams(-1,-2).apply {setMargins(ui.dp(20),ui.dp(8),ui.dp(20),ui.dp(12))}})
        if(page in listOf(0,1,2,4))root.addView(navigation())
        setContentView(root);root.requestApplyInsets()
    }
    internal fun navigation():View=NativeHome(this,ui,l).navigation(page,{go(it)}){createMenu(page==2)}
    internal fun saveLabel()=when(draft?.type){"creditFilter"->"Applica filtri";"series"->"Salva piano";"operation"->if(draft?.values?.get("kind")=="EXPENSE")"Salva spesa" else "Salva entrata";else->"Salva"}
    internal fun home(body:LinearLayout) {
        if(!l.settings.onboardingCompleted)body.addView(ui.button("Riprendi configurazione"){onboarding.paused=false;go(30)})
        NativeHome(this,ui,l).content(body,{go(it)},{manage(it)})
        if(demo)body.addView(ui.button("Esci dalla demo"){demo=false;go(0)})
    }
    internal data class HistoryItem(val date:LocalDate,val title:String,val subtitle:String,val signed:Long,val state:String,val action:()->Unit)
    internal fun export(previous:Boolean) {
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
