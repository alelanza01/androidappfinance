package it.privato.disponibile.v2

import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.view.View
import android.widget.*
import android.graphics.*
import java.time.LocalDate
import java.time.YearMonth
import it.privato.disponibile.v2.UiSupport.all
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.ledger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class Refinement33Test {
 @Before fun reset()=UiSupport.reset()
 @Test fun advancedRateControlsAreSecondaryButManualTotalsAndRadioSelectionRemainCorrect() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","10,50");click(a.window.decorView,"Rate");click(a.window.decorView,"Ogni rata")
  assertFalse(all(a.window.decorView).filterIsInstance<EditText>().any {it.contentDescription=="Numero rate"});click(a.window.decorView,"Altre opzioni");set(a.window.decorView,"Numero rate","2")
  assertTrue(all(a.window.decorView).any {it.contentDescription=="2 rate" && it.isSelected});assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="2 rate · totale € 21"});assertTrue(ledger().operations.isEmpty())
  click(a.window.decorView,"Ricorrenza continua");click(a.window.decorView,"Applica rate");assertTrue(ledger().series.isEmpty());click(a.window.decorView,"Salva piano");assertEquals(0,ledger().series.single().count);assertEquals(1050L,ledger().series.single().cents);c.pause().stop().destroy()
 }
 @Test fun categoryFiltersRemainReachableWithoutDominantToolbarAndDraftSurvives() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","12,34");click(a.window.decorView,"Categoria")
  assertFalse(all(a.window.decorView).filterIsInstance<Button>().any {it.text.toString()=="Recenti"});click(a.window.decorView,"Filtra categorie");choose("Recenti");assertTrue(ledger().operations.isEmpty());click(a.window.decorView,"Filtra categorie");choose("Tutte");click(a.window.decorView,"Spesa")
  assertEquals("12,34",UiSupport.field(a.window.decorView,"Importo in euro").text.toString());assertTrue(ledger().operations.isEmpty());click(a.window.decorView,"Salva spesa");assertEquals(-1234L,ledger().balance);c.pause().stop().destroy()
 }
 @Test fun secondaryAnalysisViewsAndComparisonsAreAccessibleWithoutChangingData() {
  val before=ledger();val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Analisi")
  assertFalse(all(a.window.decorView).filterIsInstance<Button>().any {it.text.toString()=="Liquidità"});click(a.window.decorView,"Approfondimenti");choose("Confronto del periodo");assertTrue(all(UiSupport.dialog().window!!.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("Confronto non disponibile")});choose("Ho capito")
  click(a.window.decorView,"Approfondimenti");choose("Mesi");assertTrue(all(a.window.decorView).filterIsInstance<Button>().any {it.text.toString()=="12 mesi"});assertEquals(before,ledger());c.pause().stop().destroy()
 }
 @Test fun zoomedCashChartPreservesOpeningHistoryAdvanceRefundAndFinalBalance() {
  val today=LocalDate.now();val start=today.minusDays(20);val cat=Category(1,"Spesa",0)
  val l=Ledger(Settings(opening=30000),listOf(cat),listOf(Operation(1,"Vecchia spesa",400,Kind.EXPENSE,Status.ACTUAL,start.minusDays(2),1),Operation(2,"Spesa",1000,Kind.EXPENSE,Status.ACTUAL,today.minusDays(3),1),Operation(3,"Previsione",50000,Kind.EXPENSE,Status.PLANNED,today.plusDays(1),1)),emptyList(),listOf(Advance(1,5000,today.minusDays(5),"DEMO","DEMO")),listOf(Refund(1,1,2000,today.minusDays(1))))
  val chart=RecordedCashLine(RuntimeEnvironment.getApplication(),Palette(false),l,start,today)
  assertEquals(today.minusDays(6),chart.visibleStart);assertEquals(29600L,chart.balances.first().second);assertEquals(25600L,chart.balances.last().second);assertEquals(l.balance,chart.balances.last().second)
  assertEquals("€ 12,34",DisplayMoney.amount(1234));assertEquals("− € 1,01",DisplayMoney.amount(-101));assertEquals(1234L,Money.parse("12,34"))
 }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Backdrop33Test {
 @Test fun backdropIsEphemeralAndDismissalRestoresOriginalNativePixels() {
  val v=View(RuntimeEnvironment.getApplication()).apply {setBackgroundColor(Color.BLUE)};Capture.layout(v,280,500)
  fun pixel():Int {val b=Bitmap.createBitmap(280,500,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));val value=b.getPixel(140,250);b.recycle();return value}
  val before=pixel();val clear=DialogBackdrop.attach(v,false);assertNotEquals(before,pixel());clear();assertEquals(before,pixel())
 }
}
