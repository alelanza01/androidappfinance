package it.privato.disponibile.v2

import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import android.widget.TextView
import java.time.LocalDate
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.withStore
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.all

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class ProductExperienceTest {
 @Before fun reset()=UiSupport.reset()
 @Test fun quickExpenseStartsDirectlyWithDefaultsAndSavesOnlyOnce() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","15,99");click(a.window.decorView,"Salva")
  val l=ledger();assertEquals(1,l.actual.size);assertEquals(-1599L,l.balance);assertEquals("Altro",l.operationTitle(l.actual.single()));assertEquals(-1599L,l.spendable);c.pause().stop().destroy()
 }
 @Test fun simulatorReflectsRefundsButNeverSpendsUnrecoveredCreditsOrExpectedIncome() {
  withStore {s->val category=s.snapshot().categories.first().id
   s.settings(s.snapshot().settings.copy(opening=30000,reserve=5000,horizon=LocalDate.now().plusMonths(2)))
   s.saveAdvance(Advance(cents=5000,date=LocalDate.now(),description="DEMO · Acquisto condiviso",debtor="DEMO"))
   s.saveRefund(Refund(advance=s.snapshot().advances.single().id,cents=2000,date=LocalDate.now()))
   s.saveSeries(Series(title="DEMO · Rate",cents=2000,kind=Kind.EXPENSE,category=category,start=LocalDate.now(),frequency=Frequency.MONTHLY,count=2,uncertain=false))
   s.save(Operation(title="DEMO · Entrata attesa",cents=100000,kind=Kind.INCOME,status=Status.PLANNED,date=LocalDate.now().plusDays(3),category=category))
  }
  val before=ledger();assertEquals(27000L,before.balance);assertEquals(18000L,before.spendable);assertEquals(3000L,before.outstanding)
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Simula spesa");set(a.window.decorView,"Importo in euro","80")
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()==Money.format(10000)});assertEquals(before,ledger())
  set(a.window.decorView,"Importo in euro","181");assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("Superi lo spendibile")});assertEquals(before,ledger());c.pause().stop().destroy()
 }
 @Test fun commitmentTimelineUsesConservativeProjectionAndLeavesLedgerUntouched() {
  withStore {s->val cat=s.snapshot().categories.first().id;s.settings(s.snapshot().settings.copy(opening=30000,reserve=10000))
   s.save(Operation(title="DEMO · Pagamento",cents=5000,kind=Kind.EXPENSE,status=Status.PLANNED,date=LocalDate.now().plusDays(3),category=cat))
   s.save(Operation(title="DEMO · Entrata",cents=50000,kind=Kind.INCOME,status=Status.PLANNED,date=LocalDate.now().plusDays(3),category=cat))
  }
  val before=ledger();val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Impegni")
  click(a.window.decorView,"Andamento previsto");val timeline=all(a.window.decorView).filterIsInstance<CashTimeline>().single();assertTrue(timeline.contentDescription.toString().contains(Money.format(25000)));assertEquals(25000L,before.projection(false).last().second);assertEquals(75000L,before.projection(true).last().second);assertEquals(before,ledger());c.pause().stop().destroy()
 }
}
