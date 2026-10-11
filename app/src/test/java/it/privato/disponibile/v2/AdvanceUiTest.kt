package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import it.privato.disponibile.v2.UiSupport.all
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.withStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class AdvanceUiTest {
 @Before fun setup(){UiSupport.reset();withStore{it.settings(it.snapshot().settings.copy(opening=30000))}}
 @Test fun partialAndFullRefundsUseDedicatedPagesAndHistoryDistinctFromIncome() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Anticipo")
  set(a.window.decorView,"Importo anticipato in euro","50");set(a.window.decorView,"Descrizione","Pulizie Casa A");set(a.window.decorView,"Da chi recuperare?","Proprietario");click(a.window.decorView,"Salva");assertEquals(25000L,ledger().balance)
  click(a.window.decorView,"Crediti");click(a.window.decorView,"Pulizie Casa A");click(a.window.decorView,"Registra rimborso");set(a.window.decorView,"Importo ricevuto in euro","20");click(a.window.decorView,"Salva");assertEquals(27000L,ledger().balance);assertEquals(3000L,ledger().outstanding)
  click(a.window.decorView,"Registra rimborso");set(a.window.decorView,"Importo ricevuto in euro","30");click(a.window.decorView,"Salva");assertEquals(30000L,ledger().balance);assertEquals(0L,ledger().outstanding)
  a.onBackPressed();click(a.window.decorView,"Movimenti");assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString().startsWith("Anticipo ·")});assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString().startsWith("Rimborso ·")});c.pause().stop().destroy()
 }
 @Test fun overpaymentLeavesFormRecoverableAndCannotChangeCash() {
  withStore{it.saveAdvance(Advance(cents=5000,date=java.time.LocalDate.now(),description="Test",debtor="Marco"))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Crediti");click(a.window.decorView,"Test");click(a.window.decorView,"Registra rimborso")
  set(a.window.decorView,"Importo ricevuto in euro","51");click(a.window.decorView,"Salva");assertTrue(ledger().refunds.isEmpty());choose("Ho capito");set(a.window.decorView,"Importo ricevuto in euro","20");click(a.window.decorView,"Salva");assertEquals(27000L,ledger().balance);c.pause().stop().destroy()
 }
 @Test fun creditFiltersWorkAndDoNotReuseMovementSearch() {
  val today=java.time.LocalDate.now();withStore{it.saveAdvance(Advance(cents=5000,date=today,description="Pulizie",debtor="Marco",project="Casa A"));it.saveAdvance(Advance(cents=1000,date=today.minusMonths(1),description="Materiali",debtor="Anna",project="Casa B"))}
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Movimenti");all(a.window.decorView).filterIsInstance<android.widget.EditText>().single().setText("Non esiste");UiSupport.idle();click(a.window.decorView,"Crediti")
  assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Pulizie"});click(a.window.decorView,"Filtri crediti");set(a.window.decorView,"Soggetto","Marco");click(a.window.decorView,"Applica filtri")
  assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Pulizie"});assertFalse(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Materiali"})
  click(a.window.decorView,"Filtri crediti");click(a.window.decorView,"Azzera filtri");set(a.window.decorView,"Immobile / progetto","Casa B");click(a.window.decorView,"Applica filtri");assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Materiali"});c.pause().stop().destroy()
 }
}
