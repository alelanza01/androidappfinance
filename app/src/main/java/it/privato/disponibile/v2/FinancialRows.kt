package it.privato.disponibile.v2
import android.view.Gravity
import android.widget.LinearLayout
object FinancialState {
 fun label(o:Operation)=when {
  o.kind==Kind.ADJUSTMENT->"Rettifica del saldo"
  o.kind==Kind.INCOME && o.status==Status.ACTUAL->"Già ricevuto"
  o.kind==Kind.INCOME->"Ancora da ricevere"
  o.status==Status.ACTUAL->"Già pagato"
  else->"Ancora da pagare"
 }
}
object FinancialRows {
 fun row(ui:Design,title:String,subtitle:String,state:String,amount:String,color:Int,action:()->Unit):LinearLayout {
  val pending=state.startsWith("Ancora")
  return ui.column().apply {
   setPadding(ui.dp(10),ui.dp(8),ui.dp(10),ui.dp(8));background=ui.shape(if(pending)ui.colors.hero else ui.colors.card,17).apply {if(pending)setStroke(ui.dp(1),ui.colors.line,ui.dp(4).toFloat(),ui.dp(3).toFloat())}
   val top=ui.row();top.addView(ui.label(title,13f,ui.colors.text,true),LinearLayout.LayoutParams(0,-2,1.4f));top.addView(ui.amount(amount,15f,color).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));addView(top)
   addView(ui.label(subtitle,11f,ui.colors.muted));addView(ui.label(state,11f,if(pending)ui.colors.cool else ui.colors.sage,true).apply {background=ui.shape(ui.colors.hero,7);setPadding(ui.dp(7),ui.dp(4),ui.dp(7),ui.dp(4))},LinearLayout.LayoutParams(-2,-2))
   minimumHeight=ui.dp(68);isFocusable=true;contentDescription="$title, $state, $subtitle, $amount";setOnClickListener {action()}
  }
 }
}
