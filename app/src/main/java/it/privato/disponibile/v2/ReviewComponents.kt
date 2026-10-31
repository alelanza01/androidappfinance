package it.privato.disponibile.v2
import android.graphics.Color
import android.graphics.drawable.*
import android.view.*
import android.widget.*

/** Components used only by the reviewed secondary screens; Home and date geometry stay unchanged. */
internal fun Design.panel():LinearLayout=column().apply {background=surface(colors.card,20);setPadding(dp(12),dp(2),dp(12),dp(2))}
internal fun Design.hero(title:String,value:String,caption:String)=column().apply {
 gravity=Gravity.CENTER;setPadding(dp(18),dp(24),dp(18),dp(24))
 background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(colors.hero,if(colors.dark)colors.card else Color.parseColor("#E6F5F7"))).apply {cornerRadius=dp(26).toFloat()}
 addView(label(title,13f,colors.muted).apply {gravity=Gravity.CENTER});addView(space(10));addView(amount(value,34f).apply {gravity=Gravity.CENTER});addView(space(10));addView(label(caption,12f,colors.muted).apply {gravity=Gravity.CENTER})
}
internal fun Design.badge(text:String,error:Boolean=false)=label(text,11f,if(error)colors.coral else colors.muted,true).apply {
 background=shape(if(error){if(colors.dark)0xFF4C2F43.toInt()else 0xFFFFEDF2.toInt()}else colors.hero,8);setPadding(dp(7),dp(4),dp(7),dp(4))
}
internal fun Design.symbol(name:String,size:Int=36):android.graphics.drawable.Drawable {
 val positive=name in listOf("bag","income")
 val color=if(colors.dark)Color.parseColor(if(positive)"#7DDDBC"else "#C6A5F5")else Color.parseColor(if(positive)"#24684F"else "#714C9E")
 return LayerDrawable(arrayOf(shape(if(colors.dark)colors.hero else Color.parseColor(if(positive)"#D7F2E6"else "#EEE6FC"),14),InsetDrawable(ReviewIcon(name,color,dp(size-16)),dp(8))))
}
internal fun Design.actionRow(title:String,subtitle:String,icon:String,emphasis:Boolean=false,action:()->Unit):LinearLayout = row().apply {
 minimumHeight=dp(64);setPadding(dp(10),dp(10),dp(10),dp(10))
 if(emphasis)background=shape(colors.hero,17).apply {setStroke(dp(1),this@actionRow.colors.line)}
 addView(View(context).apply {background=symbol(icon,44)},LinearLayout.LayoutParams(dp(44),dp(44)).apply {marginEnd=dp(12)})
 val copy=column();copy.addView(label(title,14f,colors.text,true));copy.addView(label(subtitle,12f,colors.muted));addView(copy,LinearLayout.LayoutParams(0,-2,1f));addView(label("›",24f,colors.muted),LinearLayout.LayoutParams(dp(22),-2))
 isFocusable=true;contentDescription=title;setOnClickListener {action()}
}
internal class ReviewIcon(private val name:String,private val tint:Int,private val size:Int):android.graphics.drawable.Drawable() {
 private val p=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=android.graphics.Paint.Style.STROKE;strokeWidth=1.7f;strokeJoin=android.graphics.Paint.Join.ROUND;strokeCap=android.graphics.Paint.Cap.ROUND}
 override fun getIntrinsicWidth()=size
 override fun getIntrinsicHeight()=size
 override fun draw(c:android.graphics.Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f)
  val path=android.graphics.Path()
  when(name){
   "calendar"->{val icon=NavIcon(2,tint,24);icon.setBounds(0,0,24,24);icon.draw(c)}
   "wallet"->{val icon=NavIcon(5,tint,24);icon.setBounds(0,0,24,24);icon.draw(c)}
   "shield"->{path.moveTo(12f,2f);path.lineTo(21f,6f);path.lineTo(20f,15f);path.lineTo(12f,22f);path.lineTo(4f,15f);path.lineTo(3f,6f);path.close();c.drawPath(path,p);c.drawLine(8f,11f,11f,14f,p);c.drawLine(11f,14f,16f,8f,p)}
   "moon"->{path.moveTo(17f,3f);path.cubicTo(1f,0f,0f,22f,17f,21f);path.cubicTo(8f,17f,9f,7f,17f,3f);c.drawPath(path,p)}
   "bell"->{path.moveTo(4f,18f);path.lineTo(7f,14f);path.lineTo(7f,8f);path.cubicTo(7f,1f,17f,1f,17f,8f);path.lineTo(17f,14f);path.lineTo(20f,18f);path.close();c.drawPath(path,p);c.drawArc(9f,18f,15f,23f,0f,180f,false,p)}
   "backup"->{c.drawRoundRect(3f,4f,21f,17f,4f,4f,p);c.drawLine(12f,9f,12f,23f,p);c.drawLine(8f,19f,12f,23f,p);c.drawLine(12f,23f,16f,19f,p)}
   "advance"->{c.drawCircle(16f,5f,3f,p);c.drawRect(2f,13f,6f,21f,p);path.moveTo(6f,16f);path.lineTo(12f,16f);path.lineTo(17f,12f);path.lineTo(20f,14f);path.lineTo(13f,21f);path.lineTo(6f,21f);c.drawPath(path,p)}
   "check"->{c.drawLine(5f,12f,10f,17f,p);c.drawLine(10f,17f,20f,6f,p)}
   else->{val icon=CategoryIcon(name,tint,24,null);icon.setBounds(0,0,24,24);icon.draw(c)}
  };c.restore()
 }
 override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:android.graphics.ColorFilter?){p.colorFilter=f}
 @Deprecated("Deprecated in Android")override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}

internal object AnalysisPalette {
 val order=listOf("home","basket","car","cup","travel","other")
 private val colors=listOf("#4295EF","#A3B9BD","#AA94E3","#F7AD7A","#EFBF77","#82C79A","#F28C9C").map {Color.parseColor(it)}
 fun rank(c:Category)=order.indexOf(c.icon).let {if(it<0)99 else it}
 fun color(index:Int)=colors[index%colors.size]
}

internal fun Design.reviewCta(title:String,action:()->Unit)=button(title,true,action).apply {
 setTextColor(Color.WHITE)
 background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x334398FF),GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,intArrayOf(Color.parseColor("#2B94F5"),Color.parseColor("#207EEA"))).apply {cornerRadius=dp(26).toFloat()},shape(Color.WHITE,26))
}
