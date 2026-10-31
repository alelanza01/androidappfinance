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
 fun addPlan(){val amount=Money.parse(value("planAmount"));require(amount>0){"Inserisci un importo positivo."};val date=LocalDate.parse(value("planDate").ifBlank {LocalDate.now().toString()});val a=data.optJSONArray("plans") ?: JSONArray();a.put(JSONObject().put("amount",amount).put("title",value("planTitle").ifBlank {"Pagamento previsto"}).put("date",date));data.put("plans",a);put("planAmount","");put("planTitle","");persist()}
 fun removePlan(index:Int){val a=JSONArray();plans().forEachIndexed {i,p->if(i!=index)a.put(p)};data.put("plans",a);persist()}
 fun clear(){prefs.edit().clear().commit();data=JSONObject()}
 private fun persist(){prefs.edit().putString("draft",data.toString()).commit()}
}
class NativeOnboarding(private val ui:Design,private val state:OnboardingState,private val ledger:Ledger) {
 fun content(body:LinearLayout,refresh:()->Unit,finish:()->Unit,pause:()->Unit) {
  body.addView(ui.label("${state.step+1} di 4",12f,ui.colors.cool,true))
  val titles=listOf("Da quanto parti?","Una riserva per stare tranquillo","Cosa devi ancora pagare?","Il tuo margine, adesso")
  body.addView(ui.label(titles[state.step],25f,ui.colors.text,true));body.addView(ui.space(14))
  fun field(key:String,label:String,money:Boolean=false){val e=ui.field(body,label,state.value(key),money);e.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){state.put(key,s.toString())}})}
  when(state.step) {
   0->{body.addView(ui.label("La liquidità disponibile sui tuoi conti e in contanti. È il punto di partenza, non un'entrata.",14f,ui.colors.muted));field("opening","Liquidità iniziale in euro",true)}
   1->{body.addView(ui.label("La somma che preferisci non spendere. Puoi cambiarla in seguito.",14f,ui.colors.muted));field("reserve","Riserva in euro",true)}
   2->{body.addView(ui.label("Aggiungi solo pagamenti non ancora effettuati. Puoi saltare questo passo e inserirli dopo.",14f,ui.colors.muted));field("planTitle","Descrizione");field("planAmount","Importo previsto in euro",true)
    body.addView(ui.button("Data · ${state.value("planDate").ifBlank {LocalDate.now().toString()}}"){val day=LocalDate.parse(state.value("planDate").ifBlank {LocalDate.now().toString()});NativeDateSheet(ui.context as android.app.Activity,ui,DateSelection(day,YearMonth.from(day),LocalDate.of(1900,1,1),LocalDate.now().plusYears(10))).show({state.put("planDate",it.toString());refresh()})})
    body.addView(ui.button("Aggiungi impegno"){try {state.addPlan();refresh()}catch(e:Exception){Toast.makeText(ui.context,e.message,Toast.LENGTH_LONG).show()}})
    state.plans().forEachIndexed {i,p->body.addView(ui.listRow(p.getString("title"),p.getString("date"),Money.format(p.getLong("amount"))){state.removePlan(i);refresh()})}
   }
   3->{val cash=Money.parse(state.value("opening").ifBlank {"0"},true);val reserve=Money.parse(state.value("reserve").ifBlank {"0"});val horizon=LocalDate.now().plusDays(30);val due=state.plans().filter {LocalDate.parse(it.getString("date"))<=horizon}.sumOf {it.getLong("amount")}
    ui.metric(body,"Liquidità iniziale",Money.format(cash));ui.metric(body,"Riserva",Money.format(reserve));ui.metric(body,"Impegni nei prossimi 30 giorni",Money.format(due));body.addView(ui.section("Puoi spendere"));body.addView(ui.amount(Money.format(cash-reserve-due)));body.addView(ui.label("Le entrate previste e i crediti non riscossi restano esclusi. Tutto rimane sul dispositivo.",14f,ui.colors.muted))
   }
  }
  body.addView(ui.space(20));body.addView(ui.button(if(state.step==3)"Inizia a usare l'app"else "Continua",true){try {when(state.step){0->Money.parse(state.value("opening").ifBlank {"0"},true);1->Money.parse(state.value("reserve").ifBlank {"0"})};if(state.step==3)finish()else{state.step++;refresh()}}catch(e:Exception){Toast.makeText(ui.context,e.message,Toast.LENGTH_LONG).show()}})
  if(state.step>0)body.addView(ui.button("Passo precedente"){state.step--;refresh()})
  body.addView(ui.button("Riprendi più tardi"){state.paused=true;pause()})
 }
}
