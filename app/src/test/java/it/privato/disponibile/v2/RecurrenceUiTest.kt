package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.LocalDate
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.withStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class RecurrenceUiTest {
 @Before fun setup(){UiSupport.reset();withStore{it.settings(it.snapshot().settings.copy(opening=30000));it.saveSeries(Series(title="Affitto",cents=5000,kind=Kind.EXPENSE,category=it.snapshot().categories.first().id,start=LocalDate.now(),frequency=Frequency.MONTHLY,count=0,uncertain=false))}}
 @Test fun singleAndFutureDeletionChoicesStopOnlyRequestedOccurrences() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Impegni");click(a.window.decorView,"Affitto");choose("Elimina…");choose("Soltanto questa occorrenza");choose("Conferma");assertTrue(ledger().pending.none{it.index==0});assertTrue(ledger().pending.isNotEmpty())
  click(a.window.decorView,"Affitto");choose("Elimina intera serie");choose("Interrompi, mantieni lo storico");choose("Conferma");assertTrue(ledger().pending.isEmpty());c.pause().stop().destroy()
 }
 @Test fun stoppingFromAnOccurrenceKeepsPaidHistoryAndRemovesEveryLaterPrediction() {
  withStore{it.settle(it.snapshot().operations[1].id)};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Impegni");click(a.window.decorView,"Affitto");choose("Elimina…");choose("Questa e tutte le future");choose("Conferma")
  assertEquals(25000L,ledger().balance);assertEquals(1,ledger().actual.size);assertTrue(ledger().pending.isEmpty());click(a.window.decorView,"Movimenti");click(a.window.decorView,"Affitto");assertEquals(1,ledger().actual.size);c.pause().stop().destroy()
 }
 @Test fun paidSeriesNeedsSeparateHistoricalConfirmationWithExactBalanceEffect() {
  withStore{it.settle(it.snapshot().operations.first().id)};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Movimenti");click(a.window.decorView,"Affitto");choose("Elimina…");choose("Intera serie");choose("Elimina anche i movimenti effettuati")
  assertEquals(25000L,ledger().balance);assertEquals(1,ledger().actual.size)
  assertTrue(UiSupport.all(UiSupport.dialog().window!!.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString().contains("250,00") && it.text.toString().contains("300,00")})
  choose("Conferma");assertEquals(30000L,ledger().balance);assertTrue(ledger().operations.isEmpty());c.pause().stop().destroy()
 }
}
