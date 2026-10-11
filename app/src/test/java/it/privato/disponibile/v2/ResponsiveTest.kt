package it.privato.disponibile.v2
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.*
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResponsiveTest {
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 private fun idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()}
 private fun click(v:View,text:String){all(v).filterIsInstance<Button>().first {it.text.toString()==text}.performClick();idle()}
 private fun layout(v:View,w:Int,h:Int){v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h)}
 private fun screenshot(v:View,name:String,w:Int,h:Int) {layout(v,w,h);val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));val dir=File(requireNotNull(System.getProperty("posso.screenshots.dir")));dir.mkdirs();File(dir,name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 @Test fun smallNormalAndLargeScreensAtLargeFontHaveSeparateNavigationAndAlignedCalendar() {
  listOf(Triple(320,640,1.5f),Triple(360,800,1.5f),Triple(411,891,1.5f),Triple(320,640,2f)).forEach {(width,height,scale)->listOf("light","dark").forEach {theme->
   RuntimeEnvironment.setQualifiers("w${width}dp-h${height}dp-mdpi")
   RuntimeEnvironment.setFontScale(scale)
   val c=RuntimeEnvironment.getApplication();c.deleteDatabase("finance-v2.db");val store=Store(c);store.settings(store.snapshot().settings.copy(theme=theme));store.close()
   val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get();click(a.window.decorView,"Altro");click(a.window.decorView,"Esplora la demo separata")
   screenshot(a.window.decorView,"small-dashboard-${width}-font${(scale*100).toInt()}-$theme.png",width,height)
   listOf("Agenda","Grafici","Storico","Altro").forEach {page->
    click(a.window.decorView,page);layout(a.window.decorView,width,height)
    val nav=all(a.window.decorView).filterIsInstance<Button>().filter {it.text.toString() in listOf("Oggi","Grafici","Agenda","Storico","Altro")}
    assertEquals(5,nav.size);assertTrue(nav.all {it.width>0 && it.height>=48})
    nav.forEach {button->assertNotNull(button.layout);assertEquals("Navigation text is clipped: ${button.text}",0,button.layout.getEllipsisCount(0))}
    val positions=nav.map {v->val p=IntArray(2);v.getLocationInWindow(p);p[0] to (p[0]+v.width)}
    positions.zipWithNext().forEach {(first,next)->assertTrue("Navigation items overlap",first.second<=next.first)}
    if(page=="Agenda") {
     val days=all(a.window.decorView).filterIsInstance<Button>().filter {it.text.toString().toIntOrNull()!=null}
     assertEquals(java.time.YearMonth.now().lengthOfMonth(),days.size)
     assertTrue(days.all {it.height>=48 && it.width>=32})
     val weekday=all(a.window.decorView).filterIsInstance<TextView>().filter {it !is Button && it.text.toString() in listOf("L","M","G","V","S","D")};assertEquals(7,weekday.size)
     assertTrue(weekday.maxOf {it.width}-weekday.minOf {it.width}<=1)
     screenshot(a.window.decorView,"small-calendar-${width}-font${(scale*100).toInt()}-$theme.png",width,height)
    }
    if(page=="Grafici")screenshot(a.window.decorView,"small-charts-${width}-font${(scale*100).toInt()}-$theme.png",width,height)
   }
   click(a.window.decorView,"Oggi");click(a.window.decorView,"Anticipi da recuperare")
   screenshot(a.window.decorView,"small-advances-${width}-font${(scale*100).toInt()}-$theme.png",width,height)
   controller.pause().stop().destroy()
  }}
  RuntimeEnvironment.setFontScale(1f)
 }
 @Test fun systemInsetsKeepNavigationAboveGestureAreaAndKeyboard() {
  RuntimeEnvironment.setQualifiers("w360dp-h800dp-mdpi");RuntimeEnvironment.setFontScale(1f)
  val c=RuntimeEnvironment.getApplication();c.deleteDatabase("finance-v2.db")
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  val root=(a.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0)
  val insets=WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,24,0,34)).setInsets(WindowInsets.Type.ime(),android.graphics.Insets.of(0,0,0,280)).build()
  root.dispatchApplyWindowInsets(insets);layout(root,360,800)
  assertEquals(24,root.paddingTop);assertEquals(280,root.paddingBottom)
  val nav=all(root).filterIsInstance<Button>().first {it.text.toString()=="Oggi"};val p=IntArray(2);nav.getLocationInWindow(p)
  assertTrue(p[1]+nav.height<=800-280)
  controller.pause().stop().destroy()
 }
}
