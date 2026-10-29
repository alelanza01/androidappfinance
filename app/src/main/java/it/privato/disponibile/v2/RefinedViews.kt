package it.privato.disponibile.v2

import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.view.View
import android.widget.EditText
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Presentation only; never parsed back or used in financial calculations. */
object DisplayMoney {
    fun amount(cents:Long):String {
        val number=NumberFormat.getNumberInstance(Locale.ITALY).apply {minimumFractionDigits=if(cents%100==0L)0 else 2;maximumFractionDigits=2}
        return (if(cents<0)"− " else "")+"€ "+number.format(BigDecimal.valueOf(cents,2).abs())
    }
}

/** Soft depth painted inside its allocated bounds: no native Z/outline or cut-off shadows. */
class SoftSurface(private val fill:Int,private val radius:Float,private val density:Float,private val dark:Boolean):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect=RectF()
    override fun draw(c:Canvas){
        for(i in 3 downTo 1){val inset=(3-i)*density;rect.set(bounds.left+inset,bounds.top+inset+2*density,bounds.right-inset,bounds.bottom-inset);p.color=Color.argb(if(dark)8 else 5,42,106,153);c.drawRoundRect(rect,radius,radius,p)}
        rect.set(bounds.left+2*density,bounds.top.toFloat(),bounds.right-2*density,bounds.bottom-4*density);p.color=fill;c.drawRoundRect(rect,radius,radius,p)
    }
    override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

class SearchIcon(private val tint:Int,private val px:Int):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=Paint.Style.STROKE;strokeWidth=1.6f;strokeCap=Paint.Cap.ROUND}
    override fun getIntrinsicWidth()=px;override fun getIntrinsicHeight()=px
    override fun draw(c:Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);c.drawCircle(10f,10f,6f,p);c.drawLine(14.5f,14.5f,21f,21f,p);c.restore()}
    override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

class RadioIcon(private val selected:Boolean,private val tint:Int,private val px:Int):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=tint;strokeWidth=1.3f}
    override fun getIntrinsicWidth()=px;override fun getIntrinsicHeight()=px
    override fun draw(c:Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);p.style=Paint.Style.STROKE;c.drawCircle(12f,12f,9f,p);if(selected){p.style=Paint.Style.FILL;c.drawCircle(12f,12f,5.5f,p)};c.restore()}
    override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

/** Separate currency and editor share a custom field; numeric input stays unchanged. */
class MoneyInput(context:Context,private val tint:Int):android.widget.FrameLayout(context) {
    val editor=EditText(context,null,0)
    private val currency=android.widget.TextView(context).apply {text="€";includeFontPadding=false;gravity=android.view.Gravity.CENTER_VERTICAL;setTextColor(tint);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO}
    private val d=resources.displayMetrics.density
    init {addView(editor,LayoutParams(-1,-1));addView(currency,LayoutParams(-2,-1));editor.addTextChangedListener(object:android.text.TextWatcher {override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int)=Unit;override fun afterTextChanged(s:android.text.Editable?){requestLayout()}})}
    override fun onMeasure(widthMeasureSpec:Int,heightMeasureSpec:Int){
        currency.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,editor.textSize);currency.typeface=editor.typeface;currency.setPadding(0,(12*d).toInt(),0,(12*d).toInt())
        val reserve=(editor.paint.measureText("€")+24*d).toInt();if(editor.paddingLeft!=reserve)editor.setPadding(reserve,(12*d).toInt(),(12*d).toInt(),(12*d).toInt())
        super.onMeasure(widthMeasureSpec,heightMeasureSpec)
    }
    override fun onLayout(changed:Boolean,left:Int,top:Int,right:Int,bottom:Int){
        super.onLayout(changed,left,top,right,bottom);val available=editor.width-editor.paddingLeft-editor.paddingRight
        val value=editor.text.toString().ifBlank {"0,00"};val textWidth=minOf(editor.paint.measureText(value),available.toFloat())
        val x=maxOf(12*d,editor.paddingLeft+(available-textWidth)/2-currency.measuredWidth-6*d).toInt();currency.layout(x,editor.top,x+currency.measuredWidth,editor.bottom)
    }
}

/** Local, ephemeral downsampled backdrop; never saved or uploaded, works without hardware blur. */
object DialogBackdrop {
    fun attach(root:View,dark:Boolean):()->Unit {
        if(root.width<=0 || root.height<=0)return {}
        val scale=14f
        val bitmap=Bitmap.createBitmap(maxOf(1,(root.width/scale).toInt()),maxOf(1,(root.height/scale).toInt()),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.scale(1/scale,1/scale);root.draw(canvas)
        val image=BitmapDrawable(root.resources,bitmap).apply {isFilterBitmap=true;setBounds(0,0,root.width,root.height)}
        val tint=ColorDrawable(if(dark)0x22255A8A else 0x24439AB4).apply {setBounds(0,0,root.width,root.height)}
        root.overlay.add(image);root.overlay.add(tint)
        return {root.overlay.remove(image);root.overlay.remove(tint)}
    }
}
