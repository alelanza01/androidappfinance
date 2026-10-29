package it.privato.disponibile.v2

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.min

/** A share of positive recorded cash, never a made-up budget target. */
class SpendableRing(context:Context,private val palette:Palette,private val ledger:Ledger):View(context) {
    val fraction:Double?=if(ledger.balance>0)(ledger.spendable.toDouble()/ledger.balance).coerceIn(0.0,1.0) else null
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val density=resources.displayMetrics.density
    init {contentDescription="Puoi spendere ${Money.format(ledger.spendable)}. ${if(fraction==null)"Percentuale non applicabile: liquidità non positiva." else "Quota della liquidità positiva dopo impegni e riserva."}";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES}
    override fun onDraw(c:Canvas) {
        super.onDraw(c);val radius=min(width,height)*.425f;val x=width/2f;val y=height/2f
        p.style=Paint.Style.FILL
        p.shader=RadialGradient(x,y+radius*.25f,radius*1.2f,intArrayOf(if(palette.dark)0x353CB8B7 else 0x344BC7C5,Color.TRANSPARENT),null,Shader.TileMode.CLAMP);c.drawCircle(x,y,radius*1.2f,p);p.shader=null
        p.color=palette.card;c.drawCircle(x,y,radius,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=radius*.15f;p.color=palette.line;p.strokeCap=Paint.Cap.ROUND;c.drawCircle(x,y,radius,p)
        if(fraction!=null && fraction>0){p.shader=SweepGradient(x,y,intArrayOf(Color.parseColor("#A2E5E6"),Color.parseColor("#38C6BD"),Color.parseColor("#A2E5E6")),null);c.save();c.rotate(-90f,x,y);c.drawArc(x-radius,y-radius,x+radius,y+radius,0f,(fraction*360).toFloat(),false,p);c.restore();p.shader=null}
        fun text(value:String,position:Float,size:Float,color:Int,bold:Boolean=false){p.style=Paint.Style.FILL;p.color=color;p.typeface=Typeface.create(if(bold)"sans-serif-medium" else "sans-serif",Typeface.NORMAL);p.textSize=size*resources.displayMetrics.scaledDensity
            while(p.measureText(value)>radius*1.65f && p.textSize>10*resources.displayMetrics.scaledDensity)p.textSize-=density
            if(p.measureText(value)>radius*1.65f)p.textSize*=radius*1.65f/p.measureText(value)
            val bounds=Rect();p.getTextBounds(value,0,value.length,bounds);c.drawText(value,x-(bounds.left+bounds.right)/2f,position-(bounds.top+bounds.bottom)/2f,p)}
        text("Puoi spendere",y-radius*.39f,13f,palette.text,true);text(DisplayMoney.amount(ledger.spendable),y,32f,if(ledger.spendable<0)palette.coral else palette.text,true)
        text(if(fraction==null)"Liquidità ≤ 0" else "di ${DisplayMoney.amount(ledger.balance)}",y+radius*.4f,12f,palette.muted)
    }
}

class HeartIcon(private val tint:Int,private val filled:Boolean,private val size:Int):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=tint;style=if(filled)Paint.Style.FILL else Paint.Style.STROKE;strokeWidth=1.35f;strokeJoin=Paint.Join.ROUND}
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=size
    override fun draw(c:Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);val path=Path();path.moveTo(12f,21f);path.cubicTo(8f,17f,2f,13f,2f,8f);path.cubicTo(2f,2f,9f,1f,12f,6f);path.cubicTo(15f,1f,22f,2f,22f,8f);path.cubicTo(22f,13f,16f,17f,12f,21f);c.drawPath(path,p);c.restore()}
    override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}

class PeriodInsights(val ledger:Ledger,val anchor:YearMonth,val months:Int,val today:LocalDate=LocalDate.now()) {
    val start=anchor.minusMonths(months.toLong()-1).atDay(1)
    val end=anchor.atEndOfMonth()
    val cutoff=minOf(today,end)
    private val priorStart=start.minusMonths(months.toLong())
    private val priorAnchor=anchor.minusMonths(months.toLong())
    private val priorEnd=if(anchor==YearMonth.from(today))priorAnchor.atDay(minOf(today.dayOfMonth,priorAnchor.lengthOfMonth())) else priorAnchor.atEndOfMonth()
    val expenses=ledger.actual.filter {it.kind==Kind.EXPENSE && it.date>=start && it.date<=cutoff}
    val prior=ledger.actual.filter {it.kind==Kind.EXPENSE && it.date>=priorStart && it.date<=priorEnd}
    val total=expenses.sumOf {it.cents}
    val priorTotal=prior.sumOf {it.cents}
    val comparable=expenses.isNotEmpty() && prior.isNotEmpty() && anchor<=YearMonth.from(today)
    val ranking=expenses.groupBy {it.category}.map {(id,ops)->ledger.categories.first {it.id==id} to ops.sumOf {it.cents}}.sortedByDescending {it.second}
    val recurring=expenses.filter {it.series!=null}.sumOf {it.cents}
}

/** Recorded balances only, including advances/refunds. Steps avoid invented intra-day balances. */
class RecordedCashLine(context:Context,private val palette:Palette,ledger:Ledger,from:LocalDate,through:LocalDate):View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val end=minOf(through,LocalDate.now())
    private val events=ledger.cashEvents.filter {it.date>=from && it.date<=end}.groupBy {it.date}.toSortedMap()
    val visibleStart:LocalDate=maxOf(from,events.keys.firstOrNull()?.minusDays(1) ?: from)
    val balances=buildList {var balance=ledger.settings.opening+ledger.cashEvents.filter {it.date<visibleStart}.sumOf {it.signed};add(visibleStart to balance);events.forEach {(date,items)->balance+=items.sumOf {it.signed};add(date to balance)};add(end to balance)}
    init {contentDescription=if(events.isEmpty())"Nessun movimento nel periodo" else "Liquidità registrata dal $visibleStart al $end, anticipi e rimborsi inclusi. Saldo finale ${Money.format(balances.last().second)}";importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES}
    override fun onDraw(c:Canvas){super.onDraw(c);val d=resources.displayMetrics.density
        if(events.isEmpty()){p.color=palette.muted;p.textSize=12*resources.displayMetrics.scaledDensity;val text="Nessun movimento nel periodo";while(p.measureText(text)>width-16*d && p.textSize>9*d)p.textSize-=d;c.drawText(text,8*d,height/2f,p);return}
        val low=balances.minOf {it.second};val high=balances.maxOf {it.second};val span=maxOf(100L,high-low).toDouble();val floor=low-span*.12;val range=span*1.28;val days=maxOf(1L,ChronoUnit.DAYS.between(visibleStart,end)).toDouble()
        val left=8*d;val right=width-8*d;val top=8*d;val bottom=height-26*d
        fun x(date:LocalDate)=left+((right-left)*ChronoUnit.DAYS.between(visibleStart,date)/days).toFloat()
        fun y(amount:Long)=bottom-((bottom-top)*(amount-floor)/range).toFloat()
        p.color=palette.line;p.style=Paint.Style.STROKE;p.strokeWidth=.6f*d;listOf(.15f,.55f,.95f).forEach {f->c.drawLine(left,top+(bottom-top)*f,right,top+(bottom-top)*f,p)}
        val path=Path();path.moveTo(x(balances.first().first),y(balances.first().second));balances.zipWithNext().forEach {(a,b)->path.lineTo(x(b.first),y(a.second));path.lineTo(x(b.first),y(b.second))}
        val fill=Path(path);fill.lineTo(x(end),bottom);fill.lineTo(x(visibleStart),bottom);fill.close();p.style=Paint.Style.FILL;p.shader=LinearGradient(0f,top,0f,bottom,0x553C98FA,0x033C98FA,Shader.TileMode.CLAMP);c.drawPath(fill,p);p.shader=null
        p.style=Paint.Style.STROKE;p.color=palette.primary;p.strokeWidth=1.7f*d;p.strokeJoin=Paint.Join.ROUND;p.strokeCap=Paint.Cap.ROUND;c.drawPath(path,p);p.style=Paint.Style.FILL;c.drawCircle(x(end),y(balances.last().second),3.5f*d,p);p.color=palette.card;c.drawCircle(x(end),y(balances.last().second),1.5f*d,p)
        p.color=palette.muted;p.textSize=10*resources.displayMetrics.scaledDensity;p.textAlign=Paint.Align.LEFT;c.drawText(visibleStart.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",java.util.Locale.ITALY)),left,height-4*d,p);p.textAlign=Paint.Align.RIGHT;c.drawText(end.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",java.util.Locale.ITALY)),right,height-4*d,p);p.textAlign=Paint.Align.LEFT
    }
}

class UnsavedIcon(private val size:Int):Drawable() {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun getIntrinsicWidth()=size
    override fun getIntrinsicHeight()=size
    override fun draw(c:Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/64f,bounds.height()/64f)
        p.color=Color.parseColor("#D7EBFF");c.drawRoundRect(10f,4f,44f,52f,5f,5f,p)
        p.color=Color.parseColor("#88BFFF");val fold=Path();fold.moveTo(34f,4f);fold.lineTo(44f,14f);fold.lineTo(34f,14f);fold.close();c.drawPath(fold,p)
        p.color=Color.parseColor("#2788FA");p.strokeWidth=2.4f;p.strokeCap=Paint.Cap.ROUND;listOf(23f,30f,37f).forEach {y->c.drawLine(17f,y,36f,y,p)}
        p.color=Color.parseColor("#9170FA");c.drawCircle(47f,46f,12f,p);p.color=Color.WHITE;c.drawLine(43f,42f,51f,50f,p);c.drawLine(51f,42f,43f,50f,p);c.restore()}
    override fun setAlpha(a:Int){p.alpha=a};override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Deprecated in Android") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
