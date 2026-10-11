package it.privato.disponibile.v2

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.*
import android.view.View
import android.content.res.Configuration

data class Palette(val dark:Boolean) {
    val background=Color.parseColor(if(dark) "#0C1425" else "#F6F7F9")
    val card=Color.parseColor(if(dark) "#17243B" else "#FFFFFF")
    val text=Color.parseColor(if(dark) "#F1F3F7" else "#111B32")
    val muted=Color.parseColor(if(dark) "#B2BCCB" else "#596579")
    val line=Color.parseColor(if(dark) "#31415C" else "#E0E5EC")
    val navy=Color.parseColor("#111B32")
    val gold=Color.parseColor("#DBBF8B")
    val sage=Color.parseColor(if(dark) "#A1C8BB" else "#356858")
    val coral=Color.parseColor(if(dark) "#F3AAA3" else "#A63E39")
    val chartSage=Color.parseColor("#779B91")
    val chartCoral=Color.parseColor("#C66B65")
    companion object {
        fun of(c:Context,theme:String)=Palette(theme=="dark" || (theme=="system" && (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES))
    }
}
class Design(val context:Context,val colors:Palette) {
    fun dp(v:Int)=(v*context.resources.displayMetrics.density).toInt()
    fun column()=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL}
    fun row()=LinearLayout(context).apply {orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL}
    fun shape(color:Int,radius:Int=20)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    fun label(value:String,size:Float=15f,color:Int=colors.text,bold:Boolean=false)=TextView(context).apply {text=value;textSize=size;setTextColor(color);setPadding(0,dp(4),0,dp(4));if(bold)setTypeface(null,Typeface.BOLD)}
    fun card(color:Int=colors.card)=column().apply {background=shape(color);setPadding(dp(18),dp(16),dp(18),dp(16));layoutParams=LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(14)}}
    fun button(value:String,primary:Boolean=false,action:()->Unit)=Button(context).apply {
        text=value;isAllCaps=false;textSize=14f;setTextColor(if(primary) Color.WHITE else colors.text);minHeight=dp(48)
        background=StateListDrawable().apply {addState(intArrayOf(android.R.attr.state_pressed),shape(colors.line,14));addState(intArrayOf(),shape(if(primary) colors.navy else colors.card,14))}
        setPadding(dp(12),dp(6),dp(12),dp(6));layoutParams=LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(6);bottomMargin=dp(4)}
        setOnClickListener {action()}
    }
    fun input(hintValue:String,value:String="",money:Boolean=false)=EditText(context).apply {
        hint=hintValue;setText(value);setTextColor(colors.text);setHintTextColor(colors.muted);setSingleLine();minHeight=dp(54)
        inputType=if(money) android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        filters=arrayOf(android.text.InputFilter.LengthFilter(if(money) 12 else 80));setSelectAllOnFocus(true)
    }
    fun spinner(labels:List<String>,selected:Int=0)=Spinner(context).apply {
        adapter=object:ArrayAdapter<String>(context,android.R.layout.simple_spinner_dropdown_item,labels) {
            override fun getView(position:Int,convertView:View?,parent:android.view.ViewGroup):View=super.getView(position,convertView,parent).apply {(this as TextView).setTextColor(colors.text)}
        }
        setSelection(selected.coerceAtLeast(0));minimumHeight=dp(48)
    }
}
