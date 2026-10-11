package it.privato.disponibile.v2

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.*

class Charts(context:Context,private val palette:Palette,private val ledger:Ledger,private val mode:Int,private val month:YearMonth,private val months:Int,private val selection:(String)->Unit):View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val font=resources.displayMetrics.scaledDensity
    private var selected:Int?=null
    private val segments=ledger.categories.map {c->c to ledger.actual.filter {it.kind==Kind.EXPENSE && YearMonth.from(it.date)==month && it.category==c.id}.sumOf {it.cents}}.filter {it.second>0}
    private val barMonths=(months-1 downTo 0).map {month.minusMonths(it.toLong())}
    private fun text(canvas:Canvas,value:String,x:Float,y:Float,color:Int=palette.muted,size:Float=11f) {p.color=color;p.style=Paint.Style.FILL;p.textSize=size*font;canvas.drawText(value,x,y,p)}
    private fun line(canvas:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,color:Int=palette.line) {p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=color;canvas.drawLine(x1,y1,x2,y2,p)}
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        if(mode==0)donut(c) else if(mode==1)area(c) else bars(c)
    }
    private fun donut(c:Canvas) {
        val total=segments.sumOf {it.second};val radius=min(width,height)*.36f;val cx=width/2f;val cy=height/2f
        p.style=Paint.Style.STROKE;p.strokeWidth=radius*.3f;p.color=palette.line
        if(total==0L) {c.drawCircle(cx,cy,radius,p);text(c,"Nessuna uscita",cx-53*font,cy);return}
        val rect=RectF(cx-radius,cy-radius,cx+radius,cy+radius);var angle=-90f
        segments.forEachIndexed {i,(cat,n)->
            val sweep=n.toFloat()/total*360f;p.color=cat.color;p.strokeWidth=radius*(if(selected==i).37f else .28f)
            c.drawArc(rect,angle+.5f,max(0.5f,sweep-1f),false,p);angle+=sweep
        }
        val title="USCITE";p.textSize=11*font
        text(c,title,cx-p.measureText(title)/2,cy-12*font,palette.muted,11f)
        val sum=Money.format(total);var size=18f;p.textSize=size*font
        while(p.measureText(sum)>radius*1.35f && size>10f){size-=.5f;p.textSize=size*font}
        text(c,sum,cx-p.measureText(sum)/2,cy+16*font,palette.text,size)
    }
    private fun area(c:Canvas) {
        val history=ledger.history();val guaranteed=ledger.projection(false);val expected=ledger.projection(true)
        val all=history+guaranteed+expected;val start=all.minOf {it.first};val end=maxOf(all.maxOf {it.first},start.plusDays(1))
        val low=minOf(0L,all.minOf {it.second});val high=maxOf(100L,all.maxOf {it.second});val span=max(100L,high-low)
        val left=54*font;val right=width-12f;val top=24*font;val bottom=height-32*font
        fun x(d:LocalDate)=left+ChronoUnit.DAYS.between(start,d).toFloat()/ChronoUnit.DAYS.between(start,end)*(right-left)
        fun y(v:Long)=bottom-(v-low).toDouble().div(span).toFloat()*(bottom-top)
        listOf(low,low+span/2,high).forEach {v->line(c,left,y(v),right,y(v));text(c,compact(v),0f,y(v)+4*font)}
        fun plot(points:List<Pair<LocalDate,Long>>,color:Int,dashed:Boolean,fill:Boolean=false) {
            if(points.size<2)return
            val path=Path();points.forEachIndexed {i,point->if(i==0)path.moveTo(x(point.first),y(point.second)) else {
                // Step changes reflect transactions, rather than invented intermediate cash balances.
                path.lineTo(x(point.first),y(points[i-1].second));path.lineTo(x(point.first),y(point.second))
            }}
            if(fill) {val fillPath=Path(path);fillPath.lineTo(x(points.last().first),y(0));fillPath.lineTo(x(points.first().first),y(0));fillPath.close();p.color=color;p.alpha=30;p.style=Paint.Style.FILL;c.drawPath(fillPath,p);p.alpha=255}
            p.color=color;p.style=Paint.Style.STROKE;p.strokeWidth=2.5f*font;p.pathEffect=if(dashed)DashPathEffect(floatArrayOf(6*font,5*font),0f) else null;c.drawPath(path,p);p.pathEffect=null
        }
        plot(history,palette.chartSage,false,true);plot(guaranteed,palette.chartCoral,true,true);plot(expected,palette.gold,true)
        line(c,x(LocalDate.now()),top,x(LocalDate.now()),bottom,palette.muted)
        text(c,start.format(DateTimeFormatter.ofPattern("dd/MM")),left,height-8*font)
        text(c,end.format(DateTimeFormatter.ofPattern("dd/MM")),right-36*font,height-8*font)
        selected?.let {i->val point=all.sortedBy {it.first}.getOrNull(i);if(point!=null){p.color=palette.text;p.style=Paint.Style.FILL;c.drawCircle(x(point.first),y(point.second),4*font,p)}}
    }
    private fun bars(c:Canvas) {
        val max=maxOf(100L,barMonths.maxOf {maxOf(ledger.month(it,Kind.INCOME),ledger.month(it,Kind.EXPENSE))})
        val left=36*font;val top=20*font;val bottom=height-35*font;val group=(width-left-8f)/months
        listOf(0L,max/2,max).forEach {v->val y=bottom-v.toFloat()/max*(bottom-top);line(c,left,y,width.toFloat(),y);text(c,compact(v),0f,y+4*font)}
        barMonths.forEachIndexed {i,m->
            val x=left+i*group;val bar=group*.27f
            listOf(Kind.INCOME to palette.chartSage,Kind.EXPENSE to palette.chartCoral).forEachIndexed {j,(kind,color)->
                val value=ledger.month(m,kind);val y=bottom-value.toFloat()/max*(bottom-top)
                p.color=color;p.style=Paint.Style.FILL;p.alpha=if(selected==null || selected==i)255 else 100
                c.drawRoundRect(x+group*.14f+j*bar,y,x+group*.14f+(j+1)*bar-2,bottom,3f,3f,p);p.alpha=255
            }
            val label=m.format(DateTimeFormatter.ofPattern(if(months>6)"MM" else "MMM",Locale.ITALY));p.textSize=10*font
            val step=ceil((p.measureText(label)+6*font)/group).toInt().coerceAtLeast(1)
            if(i%step==0)text(c,label,x+group/2-p.measureText(label)/2,height-10*font,size=10f)
        }
    }
    private fun compact(v:Long):String=if(abs(v)>=100000)String.format(Locale.ITALY,"%.1fk",v/100000.0) else (v/100).toString()
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(event.action==MotionEvent.ACTION_UP) {
            performClick()
            when(mode) {
                0->{if(segments.isEmpty())return true
                    val dx=event.x-width/2f;val dy=event.y-height/2f;val radius=min(width,height)*.36f
                    if(sqrt(dx*dx+dy*dy) !in radius*.7f..radius*1.3f)return true
                    val angle=(Math.toDegrees(atan2(dy,dx).toDouble()).toFloat()+450f)%360f
                    val total=segments.sumOf {it.second};var sum=0f
                    val i=segments.indexOfFirst {sum+=it.second.toFloat()/total*360f;angle<=sum}.coerceAtLeast(0)
                    selected=i;val (cat,n)=segments[i];selection("${cat.name} · ${Money.format(n)} · ${String.format(Locale.ITALY,"%.1f",n*100.0/total)}%")}
                1->{val points=(ledger.history()+ledger.projection(false)+ledger.projection(true)).sortedBy {it.first};val start=points.first().first;val end=maxOf(points.last().first,start.plusDays(1));val ratio=((event.x-54*font)/(width-54*font-12f)).coerceIn(0f,1f)
                    val day=start.plusDays((ChronoUnit.DAYS.between(start,end)*ratio).toLong())
                    val actual=ledger.settings.opening+ledger.cashEvents.filter {it.date<=day}.sumOf {it.signed}
                    val base=ledger.balance+ledger.pending.filter {it.kind==Kind.EXPENSE && maxOf(it.date,LocalDate.now())<=day && it.date<=ledger.settings.horizon}.sumOf {it.signed}
                    val inc=ledger.pending.filter {it.kind==Kind.INCOME && maxOf(it.date,LocalDate.now())<=day && it.date<=ledger.settings.horizon}.sumOf {it.cents}
                    selection(if(day<LocalDate.now()) "${day.format(DateTimeFormatter.ofPattern("dd/MM"))} · storico ${Money.format(actual)}" else "${day.format(DateTimeFormatter.ofPattern("dd/MM"))} · uscite note ${Money.format(base)} · con entrate previste ${Money.format(base+inc)}")}
                2->{val i=((event.x-36*font)/((width-36*font-8)/months)).toInt().coerceIn(0,months-1);selected=i;val m=barMonths[i];selection("${m.format(DateTimeFormatter.ofPattern("MMM yyyy",Locale.ITALY))} · entrate ${Money.format(ledger.month(m,Kind.INCOME))} · uscite ${Money.format(ledger.month(m,Kind.EXPENSE))}")}
            }
            invalidate();return true
        }
        return true
    }
    override fun performClick():Boolean {super.performClick();return true}
}
