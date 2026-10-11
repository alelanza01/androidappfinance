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
    val background=Color.parseColor(if(dark) "#0C1425" else "#FAF9F6")
    val card=Color.parseColor(if(dark) "#162237" else "#FFFFFF")
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
    fun shape(color:Int,radius:Int=12)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    fun label(value:String,size:Float=15f,color:Int=colors.text,bold:Boolean=false)=TextView(context).apply {text=value;textSize=size;setTextColor(color);includeFontPadding=false;setPadding(0,dp(5),0,dp(5));if(bold)setTypeface(null,Typeface.BOLD)}
    fun card(color:Int=colors.card)=column().apply {
        val surface=color!=colors.card
        if(surface)background=shape(color,16)
        setPadding(if(surface)dp(20) else 0,dp(16),if(surface)dp(20) else 0,dp(18))
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8)}
    }
    fun divider()=View(context).apply {setBackgroundColor(colors.line);layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply {topMargin=dp(12);bottomMargin=dp(12)}}
    fun button(value:String,primary:Boolean=false,action:()->Unit)=Button(context).apply {
        text=value;isAllCaps=false;textSize=14f;includeFontPadding=false;gravity=android.view.Gravity.CENTER
        setTextColor(if(primary) (if(colors.dark)colors.navy else Color.WHITE) else colors.text)
        minWidth=0;minimumWidth=0;minHeight=dp(48);minimumHeight=dp(48)
        background=StateListDrawable().apply {addState(intArrayOf(android.R.attr.state_pressed),shape(colors.line,10));addState(intArrayOf(),shape(if(primary) (if(colors.dark)colors.gold else colors.navy) else Color.TRANSPARENT,10))}
        setPadding(dp(12),dp(10),dp(12),dp(10));layoutParams=LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(4);bottomMargin=dp(4)}
        setOnClickListener {action()}
    }
    fun input(hintValue:String,value:String="",money:Boolean=false)=EditText(context).apply {
        hint=hintValue;setText(value);setTextColor(colors.text);setHintTextColor(colors.muted);setSingleLine();textSize=16f;minHeight=dp(54)
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

/** Small vector icons without native button insets or overlapping selection pills. */
class NavIcon(private val index:Int,private val tint:Int,private val px:Int):android.graphics.drawable.Drawable() {
    private val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=android.graphics.Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=android.graphics.Paint.Cap.ROUND;strokeJoin=android.graphics.Paint.Join.ROUND}
    override fun getIntrinsicWidth()=px
    override fun getIntrinsicHeight()=px
    override fun draw(c:android.graphics.Canvas) {
        c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f)
        when(index) {
            0->{val path=android.graphics.Path();path.moveTo(3f,11f);path.lineTo(12f,3f);path.lineTo(21f,11f);c.drawPath(path,p);c.drawRect(6f,11f,18f,21f,p);c.drawRect(10f,15f,14f,21f,p)}
            1->{c.drawLine(4f,20f,4f,12f,p);c.drawLine(10f,20f,10f,5f,p);c.drawLine(16f,20f,16f,9f,p);c.drawLine(22f,20f,22f,3f,p)}
            2->{c.drawRoundRect(3f,5f,21f,21f,2f,2f,p);c.drawLine(3f,10f,21f,10f,p);c.drawLine(8f,3f,8f,7f,p);c.drawLine(16f,3f,16f,7f,p);c.drawLine(8f,15f,10f,15f,p);c.drawLine(14f,15f,16f,15f,p)}
            3->{c.drawCircle(12f,12f,9f,p);c.drawLine(12f,6f,12f,12f,p);c.drawLine(12f,12f,17f,15f,p)}
            else->{listOf(5f,12f,19f).forEach {x->c.drawCircle(x,12f,1.3f,p)}}
        };c.restore()
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
