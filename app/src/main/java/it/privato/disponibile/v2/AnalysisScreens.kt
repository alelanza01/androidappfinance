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
import org.json.JSONObject
import it.privato.disponibile.v2.MainActivity.Draft
import it.privato.disponibile.v2.MainActivity.HistoryItem

/** Native screen composition and its UI events; financial writes remain in Store. */
internal fun MainActivity.analysis(body:LinearLayout) {
        body.addView(ui.compactSegments(listOf("Mese","Trimestre","Anno"),listOf(1,3,12).indexOf(analysisSpan)){analysisSpan=listOf(1,3,12)[it];render()});monthControls(body,analysisSpan)
        val data=PeriodInsights(l,month,analysisSpan)
        if(analysisSpan>1)body.addView(ui.label("${data.start.format(dates)} – ${(if(data.start>AppClock.today())data.end else minOf(data.end,AppClock.today())).format(dates)} · periodo mobile",12f,ui.colors.muted))
        val detail=ui.label("Tocca una categoria per esplorarla",12f,ui.colors.muted)
        if(chartMode==0){
            body.addView(Charts(this,ui.colors,l,0,month,period,analysisSpan){detail.text=it;detail.visibility=View.VISIBLE}.apply {contentDescription="Distribuzione delle spese personali"},LinearLayout.LayoutParams(-1,ui.dp(202)))
            detail.textSize=10f;detail.visibility=View.GONE;body.addView(detail)
            if(data.ranking.isEmpty())body.addView(ui.label("Le categorie compariranno dopo la prima spesa.",13f,ui.colors.muted))
            data.ranking.sortedWith(compareBy({AnalysisPalette.rank(it.first)},{it.first.id})).forEachIndexed {index,(cat,n)->
                val row=ui.row();row.addView(View(this).apply {background=ui.shape(AnalysisPalette.color(index),20)},LinearLayout.LayoutParams(ui.dp(7),ui.dp(7)).apply {marginEnd=ui.dp(9)})
                row.addView(ui.label(cat.name,12f),LinearLayout.LayoutParams(0,-2,1.4f));row.addView(ui.label("${String.format(Locale.ITALY,"%.0f",n*100.0/data.total)}%",12f,ui.colors.muted).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,.5f));row.addView(ui.amount(DisplayMoney.amount(n),13f).apply {gravity=Gravity.END},LinearLayout.LayoutParams(0,-2,1f));row.setPadding(0,ui.dp(3),0,ui.dp(3));for(i in 1 until row.childCount)row.getChildAt(i).setPadding(0,0,0,0);row.setOnClickListener {detail.visibility=View.VISIBLE;detail.text="${cat.name} · ${Money.format(n)}"};row.isFocusable=true;body.addView(row)
            }
            body.addView(ui.label("Andamento liquidità",13f,ui.colors.text,true).apply {setPadding(0,ui.dp(8),0,ui.dp(4))});body.addView(RecordedCashLine(this,ui.colors,l,data.start,data.end),LinearLayout.LayoutParams(-1,ui.dp(112)))
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

internal fun MainActivity.monthControls(body:LinearLayout,step:Int=1) {
        val row=ui.row();row.addView(ui.button("‹"){month=month.minusMonths(step.toLong());selectedDay=null;render()}.apply {contentDescription="Mese precedente"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)))
        row.addView(ui.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ITALY)),15f,ui.colors.text,true).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f))
        row.addView(ui.button("›"){month=month.plusMonths(step.toLong());selectedDay=null;render()}.apply {contentDescription="Mese successivo"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)));body.addView(row)
    }

internal fun MainActivity.simulator(body:LinearLayout) {
        NativeSimulation(ui,l,simulationDraft).content(body){pickerTarget="simulation";pickerState=DateSelection(simulationDraft.end,YearMonth.from(simulationDraft.end),AppClock.today(),AppClock.today().plusYears(10));showPicker()}
    }
