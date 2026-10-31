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
  body.addView(ui.label("Prima di decidere, prova.",24f,ui.colors.text,true));body.addView(ui.label("Una spesa ipotetica. Il tuo archivio resta invariato.",13f,ui.colors.muted))
  val input=ui.moneyEntry(body,"Importo in euro",draft.amount);val results=ui.column()
  body.addView(ui.section("Periodo della simulazione"));val scope=ChronoUnit.DAYS.between(LocalDate.now(),draft.end).toInt()
  val periods=listOf(7,30,60,90);body.addView(ui.segments(periods.map {"$it g"},periods.indexOf(scope)){draft.end=LocalDate.now().plusDays(periods[it].toLong());body.removeAllViews();content(body,pickEnd)})
  body.addView(ui.button("Data finale · ${draft.end}"){pickEnd()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
  val toggle=CheckBox(ui.context).apply {text="Mostra scenario con entrate attese";textSize=13f;setTextColor(ui.colors.text);isChecked=draft.income;minimumHeight=ui.dp(48);setOnCheckedChangeListener {_,checked->draft.income=checked;draw(results)}};body.addView(toggle);body.addView(results)
  input.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){draft.amount=s.toString();draw(results)};override fun afterTextChanged(s:Editable?)=Unit});draw(results)
 }
 private fun draw(body:LinearLayout) {
  body.removeAllViews()
  val amount=runCatching {if(draft.amount.isBlank())0L else Money.parse(draft.amount)}.getOrElse {body.addView(ui.label("Importo non valido: usa massimo due decimali.",14f,ui.colors.coral));return}
  val sim=Simulator.calculate(l,amount,maxOf(draft.end,LocalDate.now()),includeIncome=draft.income)
  body.addView(ui.section("MARGINE PRUDENTE DOPO LA SPESA"));body.addView(ui.amount(Money.format(sim.margin),32f,if(sim.margin<0)ui.colors.coral else ui.colors.sage));body.addView(ui.label(if(sim.margin<0)"Superi lo spendibile di ${Money.format(-sim.margin)}."else "Impegni e riserva restano coperti nell'orizzonte ufficiale.",13f,ui.colors.muted))
  ui.pair(body,"Liquidità dopo la spesa",Money.format(sim.start),"Riserva protetta",Money.format(sim.reserve))
  body.addView(ui.section("Liquidità prevista"));body.addView(SimulationChart(ui.context,ui.colors,sim),LinearLayout.LayoutParams(-1,ui.dp(210)))
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
