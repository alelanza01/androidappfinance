package it.privato.disponibile.v2

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.*
import android.widget.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Native view controllers keep screen composition out of the Activity and never write data. */
class NativeHome(private val host:Activity,private val ui:Design,private val ledger:Ledger) {
    fun content(body:LinearLayout,go:(Int)->Unit,manage:(Operation)->Unit) {
        val ring=SpendableRing(host,ui.colors,ledger).apply {setOnClickListener {go(12)};isFocusable=true}
        body.addView(ring,LinearLayout.LayoutParams(-1,ui.dp(if(host.resources.configuration.screenWidthDp<=320)236 else 252)))
        body.addView(ui.button("Previsione fino al ${ledger.settings.effectiveHorizon().format(DateTimeFormatter.ofPattern("d MMM",Locale.ITALY))}"){go(7)}.apply {textSize=11f;setTextColor(ui.colors.cool);minHeight=ui.dp(44);setPadding(0,0,0,0)})
        val metrics=ui.row()
        listOf("Entrate" to Kind.INCOME,"Uscite" to Kind.EXPENSE).forEachIndexed {i,(label,kind)->
            val cat=Category(0,label,Color.parseColor(if(i==0)"#74CFA3" else "#F49BA8"),if(i==0)"income" else "bag")
            val card=ui.row().apply {background=ui.surface();setPadding(ui.dp(10),ui.dp(10),ui.dp(10),ui.dp(10));minimumHeight=ui.dp(68)}
            card.addView(View(host).apply {background=CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(32),ui.categoryWash(cat))},LinearLayout.LayoutParams(ui.dp(32),ui.dp(32)).apply {marginEnd=ui.dp(8)})
            val values=ui.column();values.addView(ui.label(label,11f,ui.colors.muted));values.addView(ui.amount(DisplayMoney.amount(ledger.month(AppClock.month(),kind)),19f))
            card.addView(values,LinearLayout.LayoutParams(0,-2,1f));card.isFocusable=true;card.setOnClickListener {go(1)}
            metrics.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply {if(i==0)marginEnd=ui.dp(10)})
        };body.addView(metrics)
        val heading=ui.row();heading.addView(ui.section("Prossimi impegni"),LinearLayout.LayoutParams(0,-2,1f));heading.addView(ui.button("Tutti"){go(2)}.apply {textSize=11f;setTextColor(ui.colors.cool)},LinearLayout.LayoutParams(ui.dp(50),ui.dp(48)));body.addView(heading)
        val items=ledger.pending.filter {it.kind==Kind.EXPENSE}.take(2)
        if(items.isEmpty())body.addView(ui.label("Nessun pagamento previsto",13f,ui.colors.muted))
        items.forEach {o->
            val cat=ledger.categories.first {it.id==o.category}
            val row=ui.row().apply {minimumHeight=ui.dp(62);setPadding(ui.dp(10),ui.dp(8),ui.dp(10),ui.dp(8));background=ui.shape(ui.colors.hero,17).apply {setStroke(ui.dp(1),ui.colors.line,ui.dp(4).toFloat(),ui.dp(3).toFloat())}}
            row.addView(View(host).apply {background=CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(32),ui.categoryWash(cat))},LinearLayout.LayoutParams(ui.dp(32),ui.dp(32)).apply {marginEnd=ui.dp(10)})
            val labels=ui.column();labels.addView(ui.label(ledger.operationTitle(o),13f,ui.colors.text,true));labels.addView(ui.label("${o.date.format(DateTimeFormatter.ofPattern("d MMM",Locale.ITALY))} · Ancora da pagare",11f,ui.colors.muted))
            row.addView(labels,LinearLayout.LayoutParams(0,-2,1.5f));row.addView(ui.amount(DisplayMoney.amount(o.cents),15f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));row.isFocusable=true;row.contentDescription="${ledger.operationTitle(o)}, ancora da pagare";row.setOnClickListener {manage(o)};body.addView(row);body.addView(ui.space(5))
        }
        body.addView(ui.space(8));val actions=ui.row()
        listOf(Triple("Simula spesa",7,5),Triple("Anticipi da recuperare",5,3)).forEachIndexed {i,(text,icon,destination)->
            actions.addView(ui.button(text){go(destination)}.apply {textSize=12f;setPadding(ui.dp(8),ui.dp(8),ui.dp(8),ui.dp(8));background=ui.shape(ui.colors.hero,22);setCompoundDrawablesWithIntrinsicBounds(NavIcon(icon,ui.colors.cool,ui.dp(18)),null,null,null);compoundDrawablePadding=ui.dp(6)},LinearLayout.LayoutParams(0,-2,1f).apply {if(i==0)marginEnd=ui.dp(8)})
        };body.addView(actions)
    }
    fun navigation(page:Int,go:(Int)->Unit,add:()->Unit):View {
        val frame=FrameLayout(host).apply {layoutParams=LinearLayout.LayoutParams(-1,ui.dp(90))}
        val nav=ui.row().apply {setPadding(ui.dp(5),ui.dp(14),ui.dp(5),ui.dp(2));setBackgroundColor(ui.colors.card)}
        listOf(0 to "Home",1 to "Movimenti",4 to "Analisi",2 to "Impegni").forEachIndexed {i,(dest,label)->
            if(i==2)nav.addView(View(host),LinearLayout.LayoutParams(ui.dp(60),1))
            nav.addView(ui.button(label){go(dest)}.apply {setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(ui.dp(12).toFloat(),10f*host.resources.displayMetrics.scaledDensity));maxLines=1;setPadding(0,ui.dp(2),0,ui.dp(2));setTextColor(if(page==dest)ui.colors.cool else ui.colors.muted);isSelected=page==dest;setCompoundDrawablesWithIntrinsicBounds(null,NavIcon(when(dest){0->0;1->3;4->1;else->2},if(page==dest)ui.colors.cool else ui.colors.muted,ui.dp(21)),null,null);compoundDrawablePadding=ui.dp(3);contentDescription=label},LinearLayout.LayoutParams(0,ui.dp(54),1f))
        };frame.addView(nav,FrameLayout.LayoutParams(-1,ui.dp(70),Gravity.BOTTOM))
        frame.addView(View(host).apply {background=ui.shape(ui.colors.background,99)},FrameLayout.LayoutParams(ui.dp(62),ui.dp(62),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        frame.addView(ui.button("+",true){add()}.apply {contentDescription="Aggiungi";textSize=30f;setPadding(0,0,0,0)},FrameLayout.LayoutParams(ui.dp(54),ui.dp(54),Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {topMargin=ui.dp(4)})
        return frame
    }
}

class RegisterSheet(private val host:Activity,private val ui:Design) {
    fun show(planned:Boolean,open:(String,Map<String,String>)->Unit):Dialog {
        val d=Dialog(host);val body=ui.column().apply {setPadding(ui.dp(20),ui.dp(10),ui.dp(20),ui.dp(12));background=ui.shape(ui.colors.card,27)}
        body.addView(View(host).apply {background=ui.shape(ui.colors.line,2)},LinearLayout.LayoutParams(ui.dp(34),ui.dp(4)).apply {gravity=Gravity.CENTER_HORIZONTAL;bottomMargin=ui.dp(12)})
        val header=ui.row();header.addView(ui.label("Cosa vuoi registrare?",19f,ui.colors.text,true),LinearLayout.LayoutParams(0,-2,1f));header.addView(ui.button("×"){d.dismiss()}.apply {contentDescription="Chiudi Registra";textSize=26f},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));body.addView(header)
        val content=ui.column();body.addView(ScrollView(host).apply {addView(content)},LinearLayout.LayoutParams(-1,0,1f))
        fun entry(title:String,description:String,icon:String,type:String,kind:String="EXPENSE",status:String="ACTUAL",featured:Boolean=false) {
            content.addView(ui.actionRow(title,description,icon,featured){d.dismiss();open(type,mapOf("kind" to kind,"status" to status))});content.addView(ui.space(4))
        }
        content.addView(ui.section("GIÀ PAGATO O RICEVUTO"));entry("Registra spesa","Denaro che hai pagato","bag","operation",featured=true);entry("Registra entrata","Denaro che hai ricevuto","income","operation","INCOME")
        content.addView(ui.section("DA PAGARE O INCASSARE"));entry("Programma pagamento","Scegli importo e scadenza","calendar","operation",status="PLANNED",featured=true);entry("Rate e ricorrenze","Pagamenti o incassi ripetuti","repeat","series")
        content.addView(ui.button("Entrata prevista"){d.dismiss();open("operation",mapOf("kind" to "INCOME","status" to "PLANNED"))}.apply {textSize=12f;setTextColor(ui.colors.cool);gravity=Gravity.START})
        content.addView(ui.section("DENARO DA RECUPERARE"));entry("Registra anticipo","Hai pagato per conto di altri","advance","advance")
        d.setContentView(body);d.show();d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,minOf((host.resources.displayMetrics.heightPixels*.82).toInt(),ui.dp(720)));setGravity(Gravity.BOTTOM);setDimAmount(.32f);ui.flatten(decorView)}
        return d
    }
}
