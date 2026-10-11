package it.privato.disponibile.v2
import android.graphics.*
import android.view.*
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.YearMonth
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose

object Capture {
 fun layout(v:View,w:Int,h:Int){v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h)}
 fun save(a:MainActivity,name:String,w:Int=1080,h:Int=2400,overlay:Boolean=false) {
  UiSupport.idle();val v=a.window.decorView;layout(v,w,h);val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val canvas=Canvas(bmp);v.draw(canvas)
  if(overlay){val d=UiSupport.dialog();assertTrue(d.isShowing);val dv=d.window!!.decorView;dv.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec((h*.85).toInt(),View.MeasureSpec.AT_MOST));dv.layout(0,0,w,dv.measuredHeight);canvas.drawColor(Color.argb(100,0,0,0));canvas.save();canvas.translate(0f,(h-dv.height).toFloat());dv.draw(canvas);canvas.restore()}
  val dir=File(requireNotNull(System.getProperty("posso.screenshots.dir")));dir.mkdirs();File(dir,name).outputStream().use {assertTrue(bmp.compress(Bitmap.CompressFormat.PNG,100,it))};bmp.recycle()
 }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderingTest {
 @Before fun reset()=UiSupport.reset()
 @Test fun allPrincipalScreensFormsAndMenusInBothThemes() {
  listOf("light","dark").forEach {theme->
   UiSupport.withStore{it.settings(it.snapshot().settings.copy(theme=theme))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
   click(a.window.decorView,"Impostazioni");click(a.window.decorView,"Esplora la demo separata");Capture.save(a,"home-$theme.png")
   click(a.window.decorView,"Registra");Capture.save(a,"menu-registra-$theme.png",overlay=true);choose("Spesa");Capture.save(a,"inserimento-spesa-$theme.png");a.onBackPressed();choose("Conferma")
   click(a.window.decorView,"Registra");choose("Ricorrenza o rate");Capture.save(a,"inserimento-rate-$theme.png");a.onBackPressed();choose("Conferma")
   click(a.window.decorView,"Simula spesa");UiSupport.set(a.window.decorView,"Importo in euro","120");Capture.save(a,"simulatore-$theme.png");a.onBackPressed()
   click(a.window.decorView,"Dettaglio del calcolo");Capture.save(a,"calcolo-$theme.png");a.onBackPressed()
   click(a.window.decorView,"Analisi");Capture.save(a,"analisi-spese-$theme.png");click(a.window.decorView,"Liquidità");Capture.save(a,"analisi-liquidita-$theme.png");click(a.window.decorView,"Mesi");Capture.save(a,"analisi-mesi-$theme.png");a.onBackPressed()
   click(a.window.decorView,"Movimenti");Capture.save(a,"movimenti-$theme.png");click(a.window.decorView,"Filtri");Capture.save(a,"menu-filtri-$theme.png",overlay=true);choose("Tutti")
   click(a.window.decorView,"Impegni");Capture.save(a,"impegni-lista-$theme.png");click(a.window.decorView,"Calendario");Capture.save(a,"impegni-calendario-$theme.png")
   click(a.window.decorView,"Crediti");Capture.save(a,"crediti-$theme.png");click(a.window.decorView,"Pulizie Casa A");Capture.save(a,"anticipo-dettaglio-$theme.png");click(a.window.decorView,"Registra rimborso");Capture.save(a,"inserimento-rimborso-$theme.png");a.onBackPressed();choose("Conferma");a.onBackPressed()
   click(a.window.decorView,"Nuovo anticipo");Capture.save(a,"inserimento-anticipo-$theme.png");a.onBackPressed();choose("Conferma");click(a.window.decorView,"Filtri crediti");Capture.save(a,"filtri-crediti-$theme.png");a.onBackPressed();choose("Conferma")
   click(a.window.decorView,"Home");click(a.window.decorView,"Impostazioni");Capture.save(a,"impostazioni-$theme.png")
   listOf("Regole del saldo","Aspetto","Avvisi","Categorie","Backup").forEach {page->click(a.window.decorView,page);Capture.save(a,"impostazioni-${page.lowercase().replace(' ','-')}-$theme.png");a.onBackPressed()}
   c.pause().stop().destroy()
  }
 }
 @Test fun manyTransactionsAndLongDescriptionsRenderInBothThemesWithoutLoadingPersonalData() {
  RuntimeEnvironment.setQualifiers("w360dp-h800dp-xxhdpi")
  listOf("light","dark").forEach {theme->
   UiSupport.reset();UiSupport.withStore {store->
    val l=store.snapshot();store.settings(l.settings.copy(opening=500000,theme=theme))
    repeat(120){i->store.save(Operation(title="DEMO · Spesa con descrizione estesa ${i+1}",cents=1000L+i*10,kind=Kind.EXPENSE,status=Status.ACTUAL,date=java.time.LocalDate.now().minusDays((i%30).toLong()),category=l.categories[i%l.categories.size].id))}
   }
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Movimenti");Capture.save(a,"molti-movimenti-$theme.png")
   val rows=UiSupport.all(a.window.decorView).filterIsInstance<TextView>().count{it.text.toString().startsWith("DEMO · Spesa")};assertEquals(120,rows)
   click(a.window.decorView,"Home");click(a.window.decorView,"Analisi");Capture.save(a,"analisi-molti-dati-$theme.png");c.pause().stop().destroy()
  }
 }
 @Test fun emptyAndNegativeStatesBothThemes() {
  listOf("light","dark").forEach {theme->UiSupport.withStore{it.settings(it.snapshot().settings.copy(theme=theme,opening=-10000,reserve=5000))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();Capture.save(a,"saldo-negativo-$theme.png");click(a.window.decorView,"Analisi");Capture.save(a,"anello-vuoto-$theme.png");a.onBackPressed();listOf("Movimenti","Impegni","Crediti").forEach {page->click(a.window.decorView,page);Capture.save(a,"vuoto-${page.lowercase()}-$theme.png")};c.pause().stop().destroy()}
 }
 @Test fun emptyDonutTextInkIsCenteredInNativePixelsSmallScreensAndLargeFonts() {
  listOf(false,true).forEach {dark->listOf(1f,1.5f,2f).forEach {scale->
   RuntimeEnvironment.setQualifiers("w320dp-h640dp-mdpi");RuntimeEnvironment.setFontScale(scale)
   val palette=Palette(dark);val empty=Ledger(Settings(),listOf(Category(1,"Casa",0)),emptyList(),emptyList());val chart=Charts(RuntimeEnvironment.getApplication(),palette,empty,0,YearMonth.now(),6){}
   Capture.layout(chart,280,250);val bmp=Bitmap.createBitmap(280,250,Bitmap.Config.ARGB_8888);val canvas=Canvas(bmp);canvas.drawColor(palette.background);chart.draw(canvas)
   val pixels=buildList {for(y in 90..160)for(x in 65..215){val color=bmp.getPixel(x,y);if(color==palette.muted)add(x to y)}}
   assertTrue(pixels.isNotEmpty());val cx=(pixels.minOf{it.first}+pixels.maxOf{it.first})/2f;val cy=(pixels.minOf{it.second}+pixels.maxOf{it.second})/2f
   assertEquals("Horizontal text ink",140f,cx,2f);assertEquals("Vertical text ink",125f,cy,2f);bmp.recycle()
  }};RuntimeEnvironment.setFontScale(1f)
 }
 @Test fun chartSelectionsStillExposeCategoryScenarioAndMonthlyValues() {
  val l=Demo.ledger("light");val c=RuntimeEnvironment.getApplication();var detail="";val donut=Charts(c,Palette(false),l,0,YearMonth.now(),6){detail=it};donut.layout(0,0,720,440);donut.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_UP,360f,65f,0));assertTrue(detail.contains("Casa"));assertTrue(detail.contains("%"))
  val area=Charts(c,Palette(false),l,1,YearMonth.now(),6){detail=it};area.layout(0,0,720,440);area.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_UP,710f,120f,0));assertTrue(detail.contains(Money.format(l.projection(true).last().second)))
  val bars=Charts(c,Palette(false),l,2,YearMonth.now(),3){detail=it};bars.layout(0,0,720,440);bars.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_UP,710f,120f,0));assertTrue(detail.contains(Money.format(l.month(YearMonth.now(),Kind.EXPENSE))))
 }
}
