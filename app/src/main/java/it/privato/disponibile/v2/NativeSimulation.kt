package it.privato.disponibile.v2
import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.*
import android.text.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.json.JSONObject

class SimulationDraft(var amount:String="",var end:LocalDate=LocalDate.now().plusDays(30),var income:Boolean=false) {
 fun encode()=JSONObject().put("amount",amount).put("end",end).put("income",income).toString()
 companion object {fun decode(text:String):SimulationDraft {val j=JSONObject(text);return SimulationDraft(j.getString("amount"),LocalDate.parse(j.getString("end")),j.getBoolean("income"))}}
}
class NativeSimulation(private val ui:Design,private val l:Ledger,private val draft:SimulationDraft) {
 fun content(body:LinearLayout,pickEnd:()->Unit) {
  body.addView(ui.label("Se spendo questa cifra oggi, come cambia la mia liquidità?",13f,ui.colors.muted));body.addView(ui.space(12))
  val input=ui.moneyEntry(body,"Importo in euro",draft.amount);val results=ui.column();body.addView(results)
  input.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){draft.amount=s.toString();draw(results,pickEnd)};override fun afterTextChanged(s:Editable?)=Unit});draw(results,pickEnd)
 }
 private fun draw(body:LinearLayout,pickEnd:()->Unit) {
  body.removeAllViews()
  val amount=runCatching {if(draft.amount.isBlank())0L else Money.parse(draft.amount)}.getOrElse {body.addView(ui.label("Importo non valido: usa massimo due decimali.",14f,ui.colors.coral));return}
  val sim=Simulator.calculate(l,amount,maxOf(draft.end,LocalDate.now()),includeIncome=draft.income)
  val hero=ui.column().apply {gravity=Gravity.CENTER;setPadding(ui.dp(16),ui.dp(20),ui.dp(16),ui.dp(20));background=android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,intArrayOf(ui.colors.hero,if(ui.colors.dark)ui.colors.card else Color.parseColor("#DCF5ED"))).apply {cornerRadius=ui.dp(26).toFloat()}}
  hero.addView(ui.label("MARGINE DOPO LA SPESA",12f,ui.colors.muted));hero.addView(ui.amount(DisplayMoney.amount(sim.margin),34f,if(sim.margin<0)ui.colors.coral else ui.colors.text).apply {gravity=Gravity.CENTER})
  hero.addView(ui.label(if(sim.margin<0)"Superi lo spendibile di ${DisplayMoney.amount(-sim.margin)}."else "Riserva e pagamenti protetti nell'orizzonte ufficiale.",13f,if(sim.margin<0)ui.colors.coral else ui.colors.sage).apply {gravity=Gravity.CENTER});body.addView(ui.space(16));body.addView(hero)
  body.addView(ui.space(12));ui.metric(body,"Margine attuale",DisplayMoney.amount(l.spendable));body.addView(ui.divider());ui.metric(body,"Impegni protetti",DisplayMoney.amount(l.commitments));body.addView(ui.divider());ui.metric(body,"Riserva protetta",DisplayMoney.amount(sim.reserve));body.addView(ui.divider())
  body.addView(ui.section("Come cambierebbe la tua liquidità"));body.addView(ui.label("Liquidità proiettata, non spendibile. Dopo la spesa: ${DisplayMoney.amount(sim.start)}.",12f,ui.colors.muted))
  val scope=ChronoUnit.DAYS.between(LocalDate.now(),draft.end).toInt();val periods=listOf(7,30,60,90)
  body.addView(ui.segments(periods.map {"$it g"},periods.indexOf(scope)){draft.end=LocalDate.now().plusDays(periods[it].toLong());draw(body,pickEnd)})
  body.addView(ui.button("Data finale · ${draft.end}"){pickEnd()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
  val toggle=CheckBox(ui.context).apply {text="Scenario con entrate attese";textSize=13f;setTextColor(ui.colors.text);isChecked=draft.income;minimumHeight=ui.dp(48);setOnCheckedChangeListener {_,checked->draft.income=checked;draw(body,pickEnd)}};body.addView(toggle)
  body.addView(ui.label("Liquidità prevista",13f,ui.colors.text,true));body.addView(SimulationChart(ui.context,ui.colors,sim),LinearLayout.LayoutParams(-1,ui.dp(210)))
  body.addView(ui.label("Blu: liquidità dopo le uscite · tratteggio: riserva",11f,ui.colors.muted))
  if(draft.income)body.addView(ui.label("Viola: scenario con incassi attesi, anche incerti. Non aumenta il margine ufficiale.",12f,ui.colors.muted))
  val detail=ui.label("Seleziona un punto per leggere data e scadenze.",12f,ui.colors.muted);body.addView(detail)
  sim.points.forEach {point->body.addView(ui.button("${point.date} · ${Money.format(point.cash)}"){detail.text="${point.date}: ${point.titles.joinToString(" · ")}\nLiquidità prevista ${Money.format(point.cash)}"}.apply {contentDescription="Punto ${point.date}, liquidità ${Money.format(point.cash)}";textSize=12f;gravity=Gravity.START})}
  body.addView(ui.label("Periodo indipendente dalla previsione ufficiale (${l.settings.effectiveHorizon()}). Crediti non riscossi esclusi. Nessun movimento viene salvato.",12f,ui.colors.muted))
 }
}
class SimulationChart(context:Context,private val colors:Palette,private val sim:Simulation):View(context) {
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private val path=Path()
 init {contentDescription="Grafico della liquidità prevista. ${sim.points.joinToString {"${it.date}: ${Money.format(it.cash)}"}}. Riserva ${Money.format(sim.reserve)}"}
 override fun onDraw(c:Canvas) {
  super.onDraw(c);val d=resources.displayMetrics.density;val left=14*d;val right=width-14*d;val top=20*d;val bottom=height-24*d
  val values=(sim.points+sim.scenario).map {it.cash}+sim.reserve+0L;val low=values.min();val high=maxOf(values.max(),low+100)
  fun y(n:Long)=bottom-(n-low).toFloat()/(high-low)*(bottom-top)
  val first=sim.points.first().date;val days=maxOf(1L,ChronoUnit.DAYS.between(first,sim.points.last().date))
  fun x(day:LocalDate)=left+ChronoUnit.DAYS.between(first,day).toFloat()/days*(right-left)
  paint.style=Paint.Style.STROKE;paint.strokeWidth=d;paint.color=colors.muted;paint.pathEffect=DashPathEffect(floatArrayOf(5*d,4*d),0f);c.drawLine(left,y(sim.reserve),right,y(sim.reserve),paint);paint.pathEffect=null
  paint.style=Paint.Style.FILL;paint.textSize=10*resources.displayMetrics.scaledDensity;c.drawText("Riserva ${DisplayMoney.amount(sim.reserve)}",left,maxOf(14*d,y(sim.reserve)-5*d),paint)
  fun line(points:List<SimulationPoint>,color:Int,fill:Boolean) {
   if(points.isEmpty())return
   path.reset();path.moveTo(x(points.first().date),y(points.first().cash));var prior=points.first().cash
   points.drop(1).forEach {point->path.lineTo(x(point.date),y(prior));path.lineTo(x(point.date),y(point.cash));prior=point.cash}
   if(fill){val area=Path(path);area.lineTo(x(points.last().date),bottom);area.lineTo(left,bottom);area.close();paint.style=Paint.Style.FILL;paint.shader=LinearGradient(0f,top,0f,bottom,intArrayOf(0x352688F5,0x032688F5),null,Shader.TileMode.CLAMP);c.drawPath(area,paint);paint.shader=null}
   paint.style=Paint.Style.STROKE;paint.strokeJoin=Paint.Join.ROUND;paint.strokeWidth=2.5f*d;paint.color=color;c.drawPath(path,paint);paint.style=Paint.Style.FILL;points.forEach {c.drawCircle(x(it.date),y(it.cash),3*d,paint)}
  }
  line(sim.points,colors.cool,true);line(sim.scenario,Color.parseColor(if(colors.dark)"#CCB0FF"else "#8453BA"),false)
  paint.style=Paint.Style.FILL;paint.color=colors.muted;paint.textSize=10*resources.displayMetrics.scaledDensity;c.drawText(first.toString(),left,height-4*d,paint);paint.textAlign=Paint.Align.RIGHT;c.drawText(sim.points.last().date.toString(),right,height-4*d,paint);paint.textAlign=Paint.Align.LEFT
 }
}
