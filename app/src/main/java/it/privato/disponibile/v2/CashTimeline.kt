package it.privato.disponibile.v2

import android.content.Context
import android.graphics.*
import android.view.View
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Step path uses the existing conservative projection; nothing is booked or inferred here. */
class CashTimeline(context:Context,private val colors:Palette,ledger:Ledger):View(context) {
    private val points=ledger.projection(false)
    private val reserve=ledger.settings.reserve
    private val until=ledger.settings.horizon
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init {contentDescription="Liquidità dopo le uscite note, da ${Money.format(points.first().second)} a ${Money.format(points.last().second)}. Riserva ${Money.format(reserve)}.";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES}
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val density=resources.displayMetrics.density;val pad=8*density;val top=12*density;val bottom=height-24*density
        val low=minOf(0L,points.minOf {it.second},reserve);val high=maxOf(1L,points.maxOf {it.second},reserve)
        val span=maxOf(1L,high-low).toDouble();val days=maxOf(1L,ChronoUnit.DAYS.between(LocalDate.now(),until)).toDouble()
        fun x(date:LocalDate)=pad+((width-2*pad)*ChronoUnit.DAYS.between(LocalDate.now(),date)/days).toFloat()
        fun y(amount:Long)=bottom-((bottom-top)*(amount-low)/span).toFloat()
        paint.color=colors.muted;paint.style=Paint.Style.STROKE;paint.strokeWidth=density;paint.pathEffect=DashPathEffect(floatArrayOf(4*density,5*density),0f)
        canvas.drawLine(pad,y(reserve),width-pad,y(reserve),paint);paint.pathEffect=null
        val path=Path();path.moveTo(x(points.first().first),y(points.first().second));points.zipWithNext().forEach {(before,after)->path.lineTo(x(after.first),y(before.second));path.lineTo(x(after.first),y(after.second))}
        paint.color=colors.cool;paint.strokeWidth=2.5f*density;paint.strokeJoin=Paint.Join.ROUND;canvas.drawPath(path,paint)
        paint.style=Paint.Style.FILL;canvas.drawCircle(x(points.last().first),y(points.last().second),3*density,paint)
        paint.color=colors.muted;paint.textSize=11*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.LEFT;canvas.drawText("Oggi",pad,height-5*density,paint);paint.textAlign=Paint.Align.RIGHT;canvas.drawText(until.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",java.util.Locale.ITALY)),width-pad,height-5*density,paint)
    }
}
