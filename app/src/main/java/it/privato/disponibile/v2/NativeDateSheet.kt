package it.privato.disponibile.v2

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

/** Selection is staged inside this sheet. Only Confirm calls its owner; cancellation never mutates a draft. */
data class DateSelection(var selected:LocalDate,var month:YearMonth,val min:LocalDate,val max:LocalDate,var accessible:Boolean=false) {
    fun encode()=JSONObject().put("selected",selected).put("month",month).put("min",min).put("max",max).put("accessible",accessible).toString()
    companion object {fun decode(text:String):DateSelection {val j=JSONObject(text);return DateSelection(LocalDate.parse(j.getString("selected")),YearMonth.parse(j.getString("month")),LocalDate.parse(j.getString("min")),LocalDate.parse(j.getString("max")),j.getBoolean("accessible"))}}
}
class NativeDateSheet(private val host:Activity,private val ui:Design,val state:DateSelection) {
    fun show(confirm:(LocalDate)->Unit,cancel:()->Unit={}):Dialog {
        val d=Dialog(host);val body=ui.column().apply {setPadding(ui.dp(18),ui.dp(14),ui.dp(18),ui.dp(12));background=ui.shape(ui.colors.card,27)}
        val title=ui.label("Scegli una data",20f,ui.colors.text,true);body.addView(title)
        val scroll=ScrollView(host);val content=ui.column();scroll.addView(content);body.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val footer=ui.row();footer.addView(ui.button("Annulla"){d.cancel()},LinearLayout.LayoutParams(0,-2,1f));footer.addView(ui.button("Conferma data",true){val selected=state.selected;d.dismiss();confirm(selected)},LinearLayout.LayoutParams(0,-2,1f));body.addView(footer)
        fun draw() {
            content.removeAllViews();content.addView(ui.label(state.selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",Locale.ITALY)),14f,ui.colors.text,true))
            val header=ui.row()
            fun shift(delta:Long){val next=state.month.plusMonths(delta);if(next>=YearMonth.from(state.min) && next<=YearMonth.from(state.max)){state.month=next;draw()}}
            header.addView(ui.button("‹"){shift(-1)}.apply {contentDescription="Mese precedente"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)))
            val months=Spinner(host).apply {contentDescription="Mese";adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,(1..12).map {YearMonth.of(2024,it).format(DateTimeFormatter.ofPattern("MMMM",Locale.ITALY))});setSelection(state.month.monthValue-1)}
            val years=Spinner(host).apply {contentDescription="Anno";adapter=ArrayAdapter(host,android.R.layout.simple_spinner_dropdown_item,(state.min.year..state.max.year).map {it.toString()});setSelection(state.month.year-state.min.year)}
            val large=host.resources.configuration.fontScale>=1.5f
            header.addView(months,LinearLayout.LayoutParams(0,ui.dp(52),if(large)1f else 1.4f));if(!large)header.addView(years,LinearLayout.LayoutParams(0,ui.dp(52),1f))
            header.addView(ui.button("›"){shift(1)}.apply {contentDescription="Mese successivo"},LinearLayout.LayoutParams(ui.dp(44),ui.dp(48)));content.addView(header)
            if(large)content.addView(ui.row().apply {addView(ui.label("Anno",14f,ui.colors.muted),LinearLayout.LayoutParams(0,-2,.6f));addView(years,LinearLayout.LayoutParams(0,ui.dp(56),1.4f))})
            fun listener(change:(Int)->Unit)=object:AdapterView.OnItemSelectedListener {override fun onNothingSelected(p:AdapterView<*>?)=Unit;override fun onItemSelected(p:AdapterView<*>?,v:View?,position:Int,id:Long){change(position)}}
            months.onItemSelectedListener=listener {position->if(position+1!=state.month.monthValue){state.month=YearMonth.of(state.month.year,position+1);draw()}}
            years.onItemSelectedListener=listener {position->if(state.min.year+position!=state.month.year){state.month=YearMonth.of(state.min.year+position,state.month.monthValue);draw()}}
            val today=AppClock.today();val shortcuts=ui.row()
            listOf("Oggi" to today,if(state.max<=today)"Ieri" to today.minusDays(1)else "Domani" to today.plusDays(1),"+30 giorni" to today.plusDays(30)).filter {it.second in state.min..state.max}.forEach {(label,day)->shortcuts.addView(ui.button(label){state.selected=day;state.month=YearMonth.from(day);draw()}.apply {textSize=11f;setPadding(0,0,0,0)},LinearLayout.LayoutParams(0,-2,1f))};content.addView(shortcuts)
            if(state.accessible || host.resources.configuration.fontScale>=1.5f) {
                (1..state.month.lengthOfMonth()).forEach {n->val day=state.month.atDay(n);if(day in state.min..state.max)content.addView(ui.button(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM",Locale.ITALY)),day==state.selected){state.selected=day;draw()}.apply {contentDescription=day.toString();isSelected=day==state.selected})}
            }else {
                val weekdays=ui.row();listOf("L","M","M","G","V","S","D").forEach {weekdays.addView(ui.label(it,11f,ui.colors.muted).apply {gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,ui.dp(28),1f))};content.addView(weekdays)
                val offset=state.month.atDay(1).dayOfWeek.value-1
                (0 until (offset+state.month.lengthOfMonth()+6)/7).forEach {week->val row=ui.row();(0..6).forEach {column->val n=week*7+column-offset+1
                    if(n !in 1..state.month.lengthOfMonth())row.addView(View(host),LinearLayout.LayoutParams(0,ui.dp(44),1f))
                    else {val day=state.month.atDay(n);row.addView(ui.button(n.toString(),day==state.selected){state.selected=day;draw()}.apply {contentDescription=day.toString();isSelected=day==state.selected;isEnabled=day in state.min..state.max;textSize=14f;setPadding(0,0,0,0);if(day==today && day!=state.selected)background=ui.shape(ui.colors.hero,22).apply {setStroke(ui.dp(1),ui.colors.cool)}},LinearLayout.LayoutParams(0,ui.dp(44),1f))}
                };content.addView(row)}
            }
            content.addView(ui.button(if(state.accessible)"Vista calendario"else "Elenco accessibile"){state.accessible=!state.accessible;draw()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
        }
        draw();d.setOnCancelListener {cancel()};d.setContentView(body);d.show();d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,minOf((host.resources.displayMetrics.heightPixels*.84).toInt(),ui.dp(650)));setGravity(Gravity.BOTTOM);setDimAmount(.32f)};return d
    }
}
