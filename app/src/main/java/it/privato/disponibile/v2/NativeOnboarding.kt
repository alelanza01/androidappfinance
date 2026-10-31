package it.privato.disponibile.v2
import android.content.Context
import android.widget.*
import android.text.*
import java.time.LocalDate
import java.time.YearMonth
import org.json.*

/** Private on-device staging. No financial rows are written until the fourth step is confirmed. */
class OnboardingState(context:Context) {
 private val prefs=context.getSharedPreferences("onboarding-v34",Context.MODE_PRIVATE)
 var data=runCatching {JSONObject(prefs.getString("draft","{}")!!)}.getOrDefault(JSONObject())
 val token:String get()=data.optString("token").ifBlank {java.util.UUID.randomUUID().toString().also {put("token",it)}}
 var step:Int get()=data.optInt("step",0);set(n){data.put("step",n);persist()}
 var paused:Boolean get()=data.optBoolean("paused",false);set(n){data.put("paused",n);persist()}
 fun value(key:String)=data.optString(key,"")
 fun put(key:String,value:String){data.put(key,value);persist()}
 fun plans():List<JSONObject> {val a=data.optJSONArray("plans") ?: JSONArray();return (0 until a.length()).map {a.getJSONObject(it)}}
 fun addPlan(){val amount=Money.parse(value("planAmount"));require(amount>0){"Inserisci un importo positivo."};val date=LocalDate.parse(value("planDate").ifBlank {AppClock.today().toString()});val a=data.optJSONArray("plans") ?: JSONArray();a.put(JSONObject().put("amount",amount).put("title",value("planTitle").ifBlank {"Pagamento previsto"}).put("date",date));data.put("plans",a);put("planAmount","");put("planTitle","");persist()}
 fun removePlan(index:Int){val a=JSONArray();plans().forEachIndexed {i,p->if(i!=index)a.put(p)};data.put("plans",a);persist()}
 fun clear(){prefs.edit().clear().commit();data=JSONObject()}
 private fun persist(){prefs.edit().putString("draft",data.toString()).commit()}
}
class NativeOnboarding(private val ui:Design,private val state:OnboardingState,private val ledger:Ledger) {
 fun content(body:LinearLayout,refresh:()->Unit,finish:()->Unit,pause:()->Unit) {
  body.addView(ui.label("PASSAGGIO ${state.step+1} DI 4",11f,ui.colors.muted,true));body.addView(ui.space(18));body.addView(android.view.View(ui.context).apply {background=ui.symbol(listOf("wallet","shield","calendar","check")[state.step],56)},LinearLayout.LayoutParams(ui.dp(56),ui.dp(56)));body.addView(ui.space(14))
  val titles=listOf("Quanti soldi hai disponibili oggi?","Quanto vuoi tenere da parte?","Hai pagamenti in arrivo?","Il tuo primo margine")
  body.addView(ui.label(titles[state.step],22f,ui.colors.text,true));body.addView(ui.space(14))
  fun field(key:String,label:String,money:Boolean=false){val e=if(money)ui.moneyEntry(body,label,state.value(key))else ui.field(body,label,state.value(key));e.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){state.put(key,s.toString())}})}
  when(state.step) {
   0->{body.addView(ui.label("La liquidità disponibile sui tuoi conti e in contanti. È il punto di partenza, non un'entrata.",14f,ui.colors.muted));field("opening","Liquidità iniziale in euro",true)}
   1->{body.addView(ui.label("La somma che preferisci non spendere. Puoi cambiarla in seguito.",14f,ui.colors.muted));field("reserve","Riserva in euro",true)}
   2->{body.addView(ui.label("Aggiungi più pagamenti, oppure continua senza nuovi impegni.",13f,ui.colors.muted))
    state.plans().forEachIndexed {i,p->body.addView(ui.listRow(p.getString("title"),p.getString("date"),Money.format(p.getLong("amount"))){state.removePlan(i);refresh()})}
    if(state.value("editing")!="true")body.addView(ui.button("Aggiungi pagamento"){state.put("editing","true");refresh()}.apply {background=ui.shape(ui.colors.hero,24)})
    else {
      field("planTitle","Descrizione");field("planAmount","Importo previsto in euro",true)
      body.addView(ui.button("Data · ${state.value("planDate").ifBlank {AppClock.today().toString()}}"){val day=LocalDate.parse(state.value("planDate").ifBlank {AppClock.today().toString()});NativeDateSheet(ui.context as android.app.Activity,ui,DateSelection(day,YearMonth.from(day),LocalDate.of(1900,1,1),AppClock.today().plusYears(10))).show({state.put("planDate",it.toString());refresh()})})
      body.addView(ui.button("Aggiungi impegno"){try {state.addPlan();state.put("editing","false");refresh()}catch(e:Exception){Toast.makeText(ui.context,e.message,Toast.LENGTH_LONG).show()}})
      body.addView(ui.button("Annulla pagamento"){state.put("editing","false");refresh()})
    }
    body.addView(ui.label("${ledger.pending.size} scadenze già presenti verranno conservate.",12f,ui.colors.muted))
   }

   3->{val cash=Money.parse(state.value("opening").ifBlank {"0"},true);val reserve=Money.parse(state.value("reserve").ifBlank {"0"});val horizon=AppClock.today().plusDays(30);val due=ledger.pending.filter {it.kind==Kind.EXPENSE && it.date<=horizon}.sumOf {it.cents}+state.plans().filter {LocalDate.parse(it.getString("date"))<=horizon}.sumOf {it.getLong("amount")}
    body.addView(ui.hero("Spendibile iniziale",DisplayMoney.amount(cash-reserve-due),"Scadenze protette fino al ${horizon.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",java.util.Locale.ITALY))}"));body.addView(ui.space(22))
    ui.metric(body,"Liquidità",DisplayMoney.amount(cash));body.addView(ui.divider());ui.metric(body,"Riserva",DisplayMoney.amount(reserve));body.addView(ui.divider());ui.metric(body,"Pagamenti protetti",DisplayMoney.amount(due));body.addView(ui.divider());body.addView(ui.space(16));body.addView(ui.label("Il riepilogo conserva i movimenti esistenti. Lo storico parte dai dati registrati, non da guadagni inventati.",13f,ui.colors.muted))
   }
  }
  body.addView(ui.space(20));body.addView(ui.reviewCta(if(state.step==3)"Vai alla Home"else "Continua"){try {when(state.step){0->Money.parse(state.value("opening").ifBlank {"0"},true);1->Money.parse(state.value("reserve").ifBlank {"0"})};if(state.step==3)finish()else{state.step++;refresh()}}catch(e:Exception){Toast.makeText(ui.context,e.message,Toast.LENGTH_LONG).show()}}.apply {tag="onboarding-next"})
  if(state.step>0)body.addView(ui.button("Passo precedente"){state.step--;refresh()})
  body.addView(ui.button("Riprendi più tardi"){state.paused=true;pause()})
 }
}
