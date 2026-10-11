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
    val chartGold=Color.parseColor(if(dark) "#DBBF8B" else "#8B6B32")
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
    fun label(value:String,size:Float=15f,color:Int=colors.text,bold:Boolean=false)=TextView(context).apply {
        text=value;textSize=size;setTextColor(color);includeFontPadding=false
        typeface=Typeface.create(if(bold) "sans-serif-medium" else "sans-serif",Typeface.NORMAL)
        setPadding(0,dp(3),0,dp(3))
    }
    fun divider()=View(context).apply {setBackgroundColor(colors.line);layoutParams=LinearLayout.LayoutParams(-1,dp(1))}
    fun space(size:Int=16)=View(context).apply {layoutParams=LinearLayout.LayoutParams(1,dp(size))}
    fun section(title:String)=label(title.uppercase(java.util.Locale.ITALY),11f,colors.muted,true).apply {letterSpacing=.08f;setPadding(0,dp(18),0,dp(8))}
    fun button(value:String,primary:Boolean=false,action:()->Unit)=Button(context).apply {
        text=value;isAllCaps=false;textSize=15f;includeFontPadding=false;gravity=android.view.Gravity.CENTER
        typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        setTextColor(if(primary) (if(colors.dark)colors.navy else Color.WHITE) else colors.text)
        minWidth=0;minimumWidth=0;minHeight=dp(48);minimumHeight=dp(48)
        val color=if(primary) (if(colors.dark)colors.gold else colors.navy) else Color.TRANSPARENT
        background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(colors.line),shape(color,12),shape(Color.WHITE,12))
        setPadding(dp(12),dp(10),dp(12),dp(10));layoutParams=LinearLayout.LayoutParams(-1,-2)
        setOnClickListener {action()}
    }
    fun input(hintValue:String,value:String="",money:Boolean=false)=EditText(context).apply {
        hint=hintValue;setText(value);setTextColor(colors.text);setHintTextColor(colors.muted);setSingleLine();textSize=16f
        minHeight=dp(52);setPadding(dp(12),dp(10),dp(12),dp(10))
        background=shape(colors.card,10).apply {setStroke(dp(1),this@Design.colors.line)}
        inputType=if(money) android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        filters=arrayOf(android.text.InputFilter.LengthFilter(if(money)12 else 80));setSelectAllOnFocus(true)
    }
    fun field(body:LinearLayout,title:String,value:String="",money:Boolean=false)=input(if(money) "0,00" else title,value,money).also {
        body.addView(label(title,12f,colors.muted,true).apply {setPadding(0,dp(14),0,dp(6))});body.addView(it)
        it.contentDescription=title
    }
    fun segments(labels:List<String>,selected:Int,action:(Int)->Unit)=row().apply {
        background=shape(colors.line,10);setPadding(dp(3),dp(3),dp(3),dp(3))
        labels.forEachIndexed {i,label->addView(button(label){action(i)}.apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,minOf(dp(14).toFloat(),12f*context.resources.displayMetrics.scaledDensity));setPadding(dp(4),dp(8),dp(4),dp(8));maxLines=2
            background=shape(if(i==selected)colors.card else Color.TRANSPARENT,8)
            setTextColor(if(i==selected)colors.text else colors.muted);isSelected=i==selected
        },LinearLayout.LayoutParams(0,-2,1f))}
    }
    fun amount(value:String,size:Float=34f,color:Int=colors.text)=label(value,size,color,true).apply {
        maxLines=1;setAutoSizeTextTypeUniformWithConfiguration(12,size.toInt(),1,android.util.TypedValue.COMPLEX_UNIT_SP)
    }
    fun listRow(title:String,subtitle:String="",amount:String="",color:Int=colors.text,action:()->Unit):LinearLayout {
        val paint=android.graphics.Paint().apply {textSize=16f*context.resources.displayMetrics.scaledDensity}
        val available=context.resources.displayMetrics.widthPixels-dp(48)
        val stacked=amount.isNotBlank() && (context.resources.configuration.fontScale>=1.5f || paint.measureText(amount)>available/2.7f)
        return row().apply {
            orientation=if(stacked)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(0,dp(13),0,dp(13));minimumHeight=dp(64)
            val text=column();text.addView(label(title,15f,colors.text,true))
            if(subtitle.isNotBlank())text.addView(label(subtitle,12f,colors.muted))
            addView(text,if(stacked)LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,if(amount.isBlank())1f else 1.7f))
            if(amount.isNotBlank())addView(this@Design.amount(amount,16f,color).apply {gravity=android.view.Gravity.END},if(stacked)LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,1f))
            else addView(label("›",22f,colors.muted).apply {setPadding(dp(12),0,0,0)})
            background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(colors.line),null,shape(Color.WHITE,0))
            isClickable=true;isFocusable=true;contentDescription=listOf(title,subtitle,amount).filter {it.isNotBlank()}.joinToString(", ")
            setOnClickListener {action()}
        }
    }
    fun metric(body:LinearLayout,title:String,value:String,color:Int=colors.text) {
        val stacked=context.resources.configuration.fontScale>=1.5f
        val line=row().apply {orientation=if(stacked)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL;gravity=android.view.Gravity.TOP;setPadding(0,dp(7),0,dp(7))}
        line.addView(label(title,14f,colors.muted),if(stacked)LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,1.4f))
        line.addView(amount(value,16f,color).apply {gravity=android.view.Gravity.END},if(stacked)LinearLayout.LayoutParams(-1,-2) else LinearLayout.LayoutParams(0,-2,1f));body.addView(line)
    }
    fun empty(body:LinearLayout,title:String,message:String) {
        body.addView(space(24));body.addView(label(title,20f,colors.text,true));body.addView(label(message,14f,colors.muted));body.addView(space(12))
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
            5->{c.drawRoundRect(3f,6f,21f,20f,3f,3f,p);c.drawLine(4f,6f,18f,3f,p);c.drawRect(15f,11f,22f,16f,p)}
            6->{val gear=android.graphics.Path();for(i in 0..31){val angle=Math.toRadians(i*11.25);val radius=if(i%4 in 0..1)10f else 8f;val x=12+radius*kotlin.math.cos(angle).toFloat();val y=12+radius*kotlin.math.sin(angle).toFloat();if(i==0)gear.moveTo(x,y)else gear.lineTo(x,y)};gear.close();c.drawPath(gear,p);c.drawCircle(12f,12f,3f,p)}
            else->{listOf(5f,12f,19f).forEach {x->c.drawCircle(x,12f,1.3f,p)}}
        };c.restore()
    }
    override fun setAlpha(alpha:Int){p.alpha=alpha}
    override fun setColorFilter(filter:android.graphics.ColorFilter?){p.colorFilter=filter}
    @Deprecated("Deprecated in Android") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
