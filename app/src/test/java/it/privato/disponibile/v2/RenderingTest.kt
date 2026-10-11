package it.privato.disponibile.v2
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderingTest {
 @Before fun reset(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db")}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 private fun click(v:View,text:String){all(v).filterIsInstance<Button>().first {it.text.toString()==text}.performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()}
 private fun screenshot(v:View,name:String) {
  v.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY));v.layout(0,0,1080,2400)
  val bitmap=Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap))
  val dir=File(requireNotNull(System.getProperty("posso.screenshots.dir")));dir.mkdirs();File(dir,name).outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle()
 }
 @Test fun renderBothThemesAndAllThreeCharts() {
  listOf("light","dark").forEach {theme->
   val store=Store(RuntimeEnvironment.getApplication());try {store.settings(store.snapshot().settings.copy(theme=theme))}finally {store.close()}
   val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
   click(a.window.decorView,"Altro");click(a.window.decorView,"Esplora la demo separata")
   screenshot(a.window.decorView,"demo-dashboard-$theme.png")
   click(a.window.decorView,"Grafici");screenshot(a.window.decorView,"demo-grafici-$theme.png")
   val scroll=all(a.window.decorView).filterIsInstance<ScrollView>().single();scroll.scrollTo(0,(all(a.window.decorView).filterIsInstance<Charts>()[1].parent as View).top);screenshot(a.window.decorView,"demo-proiezione-$theme.png")
   scroll.scrollTo(0,(all(a.window.decorView).filterIsInstance<Charts>()[2].parent as View).top);screenshot(a.window.decorView,"demo-mesi-$theme.png")
   click(a.window.decorView,"Scadenze");screenshot(a.window.decorView,"demo-calendario-$theme.png")
   controller.pause().stop().destroy()
  }
 }
 @Test fun emptyAndNegativeChartsRenderAndCanBeSelected() {
  val store=Store(RuntimeEnvironment.getApplication());try {store.settings(store.snapshot().settings.copy(opening=-10000,reserve=5000))}finally {store.close()}
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get();click(a.window.decorView,"Grafici")
  screenshot(a.window.decorView,"vuoto-saldo-negativo.png")
  all(a.window.decorView).filterIsInstance<Charts>().forEach {chart->chart.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,100f,100f,0))}
  controller.pause().stop().destroy()
 }
 @Test fun chartSelectionsExposeRealCategoryAndScenarioValues() {
  val context=RuntimeEnvironment.getApplication();val ledger=Demo.ledger("light");val palette=Palette(false);var detail=""
  val donut=Charts(context,palette,ledger,0,java.time.YearMonth.now(),6){detail=it};donut.layout(0,0,720,440)
  donut.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,360f,65f,0))
  assertTrue(detail.contains("Casa"));assertTrue(detail.contains("%"))
  val area=Charts(context,palette,ledger,1,java.time.YearMonth.now(),6){detail=it};area.layout(0,0,720,440)
  area.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,710f,120f,0))
  assertTrue(detail.contains("entrate previste"));assertTrue(detail.contains(Money.format(ledger.projection(true).last().second)))
  val bars=Charts(context,palette,ledger,2,java.time.YearMonth.now(),3){detail=it};bars.layout(0,0,720,440)
  bars.onTouchEvent(android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_UP,710f,120f,0))
  assertTrue(detail.contains("entrate"));assertTrue(detail.contains(Money.format(ledger.month(java.time.YearMonth.now(),Kind.EXPENSE))))
 }
}
