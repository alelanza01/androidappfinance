package it.privato.disponibile.v2
import android.app.Activity
import android.content.Intent
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
class UiTest {
 @Before fun reset()=UiSupport.reset()
 private fun result(a:MainActivity,code:Int,status:Int,intent:Intent?) {val method=MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Intent::class.java);method.isAccessible=true;method.invoke(a,code,status,intent);UiSupport.idle()}
 @Test fun ordinaryExpenseCreateEditDeleteUpdatesHistoryCashAndSurvivesRestart() {
  withStore{it.settings(it.snapshot().settings.copy(opening=30000))}
  var controller=Robolectric.buildActivity(MainActivity::class.java).setup();var a=controller.get();click(a.window.decorView,"Registra");choose("Spesa")
  set(a.window.decorView,"Importo in euro","25,50");set(a.window.decorView,"Descrizione","Bolletta demo");click(a.window.decorView,"Salva")
  assertEquals(27450L,ledger().balance);click(a.window.decorView,"Movimenti");click(a.window.decorView,"Bolletta demo");choose("Modifica");set(a.window.decorView,"Importo in euro","30");click(a.window.decorView,"Salva");assertEquals(27000L,ledger().balance)
  click(a.window.decorView,"Bolletta demo");choose("Elimina movimento");choose("Conferma")
  assertTrue(ledger().operations.isEmpty());assertEquals(30000L,ledger().balance);assertFalse(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Bolletta demo"})
  controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity::class.java).setup();a=controller.get();click(a.window.decorView,"Movimenti");assertFalse(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Bolletta demo"});controller.pause().stop().destroy()
 }
 @Test fun simulatorLivesOutsideHomeAndNeverCreatesAnOperation() {
  withStore{it.settings(it.snapshot().settings.copy(opening=10000,reserve=2000))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
  assertTrue(all(a.window.decorView).filterIsInstance<android.widget.EditText>().isEmpty());click(a.window.decorView,"Simula spesa");set(a.window.decorView,"Importo in euro","25")
  assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString().contains("55,00")});assertTrue(ledger().operations.isEmpty());c.pause().stop().destroy()
 }
 @Test fun invalidAmountsStayEditableWithoutMutation() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","1,234");set(a.window.decorView,"Descrizione","Test");click(a.window.decorView,"Salva")
  assertTrue(ledger().operations.isEmpty());choose("Ho capito");set(a.window.decorView,"Importo in euro","10");click(a.window.decorView,"Salva");assertEquals(-1000L,ledger().balance);c.pause().stop().destroy()
 }
 @Test fun searchNavigationSettingsAndDemoAreIsolated() {
  withStore{val id=it.snapshot().categories.first().id;it.save(Operation(title="Affitto demo",cents=1000,kind=Kind.EXPENSE,status=Status.PLANNED,date=java.time.LocalDate.now(),category=id));it.save(Operation(title="Compenso demo",cents=2000,kind=Kind.INCOME,status=Status.ACTUAL,date=java.time.LocalDate.now(),category=id))}
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Movimenti");click(a.window.decorView,"Filtri");choose("Tutti");all(a.window.decorView).filterIsInstance<android.widget.EditText>().single().setText("Affitto");UiSupport.idle()
  assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Affitto demo"});assertFalse(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString()=="Compenso demo"})
  listOf("Impegni","Crediti","Home").forEach{click(a.window.decorView,it)};click(a.window.decorView,"Impostazioni");click(a.window.decorView,"Esplora la demo separata");assertTrue(all(a.window.decorView).filterIsInstance<android.widget.TextView>().any{it.text.toString().contains("DATI DIMOSTRATIVI")});assertEquals(2,ledger().operations.size);click(a.window.decorView,"Esci dalla demo");c.pause().stop().destroy()
 }
 @Test fun backupExportImportKeepASafetyCopy() {
  withStore{it.settings(it.snapshot().settings.copy(opening=12300))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val uri=android.net.Uri.parse("content://backup/export");val out=java.io.ByteArrayOutputStream();Shadows.shadowOf(a.contentResolver).registerOutputStream(uri,out)
  result(a,1,Activity.RESULT_OK,Intent().setData(uri));assertEquals(12300L,Backup.decode(out.toString("UTF-8")).settings.opening)
  val demo=Demo.ledger("light");val src=android.net.Uri.parse("content://backup/import");Shadows.shadowOf(a.contentResolver).registerInputStream(src,java.io.ByteArrayInputStream(Backup.encode(demo).toByteArray()))
  result(a,2,Activity.RESULT_OK,Intent().setData(src));choose("Conferma");assertEquals(demo.balance,ledger().balance);assertEquals(12300L,Backup.decode(java.io.File(a.filesDir,"pre-import.json").readText()).settings.opening);c.pause().stop().destroy()
 }
 @Test fun cancelledAndMalformedDocumentsLeaveDataUntouched() {
  withStore{it.settings(it.snapshot().settings.copy(opening=12300))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();result(a,2,Activity.RESULT_CANCELED,null)
  val src=android.net.Uri.parse("content://backup/broken");Shadows.shadowOf(a.contentResolver).registerInputStream(src,java.io.ByteArrayInputStream("broken".toByteArray()));result(a,2,Activity.RESULT_OK,Intent().setData(src));assertEquals(12300L,ledger().balance);c.pause().stop().destroy()
 }
 @Test fun uncertainIncomeIsOnlyCashAfterExplicitReceiptFromTheNewFlow() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Impegni");click(a.window.decorView,"Nuovo impegno");choose("Entrata prevista")
  set(a.window.decorView,"Importo in euro","50");set(a.window.decorView,"Descrizione","Incarico futuro");click(a.window.decorView,"Opzioni avanzate");click(a.window.decorView,"Entrata non garantita");choose("Sì, solo uno scenario");click(a.window.decorView,"Salva")
  assertEquals(0L,ledger().balance);assertEquals(0L,ledger().spendable);assertTrue(ledger().pending.single().uncertain);click(a.window.decorView,"Incarico futuro");choose("Conferma incasso oggi");choose("Conferma");assertEquals(5000L,ledger().balance);assertTrue(ledger().pending.isEmpty());c.pause().stop().destroy()
 }
 @Test fun finiteInstallmentsCanBeCreatedAndEditedWithoutRecreatingPaidHistory() {
  withStore{it.settings(it.snapshot().settings.copy(opening=10000,horizon=java.time.LocalDate.now().plusMonths(3)))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Ricorrenza o rate")
  set(a.window.decorView,"Importo in euro","10");set(a.window.decorView,"Descrizione","Rate test");set(a.window.decorView,"Numero rate (vuoto = continua)","3");click(a.window.decorView,"Salva");assertEquals(3,ledger().pending.size);assertEquals(7000L,ledger().spendable)
  click(a.window.decorView,"Impegni");click(a.window.decorView,"Rate test · 1/3");choose("Conferma pagamento oggi");choose("Conferma");assertEquals(9000L,ledger().balance)
  click(a.window.decorView,"Rate test");choose("Modifica serie");set(a.window.decorView,"Importo in euro","12");click(a.window.decorView,"Salva");assertEquals(1000L,ledger().actual.single().cents);assertTrue(ledger().pending.all{it.cents==1200L});assertEquals(6600L,ledger().spendable);c.pause().stop().destroy()
 }
 @Test fun staleSaveButtonCannotRegisterTheSameDraftTwice() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","10");set(a.window.decorView,"Descrizione","Una volta")
  val save=all(a.window.decorView).filterIsInstance<android.widget.Button>().first{it.text.toString()=="Salva"};save.performClick();save.performClick();UiSupport.idle();assertEquals(1,ledger().operations.size);assertEquals(-1000L,ledger().balance);c.pause().stop().destroy()
 }
 @Test fun draftSurvivesActivityRecreationWithoutWritingBeforeSave() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","12,50");set(a.window.decorView,"Descrizione","Bozza");c.recreate();a=c.get()
  assertEquals("12,50",UiSupport.field(a.window.decorView,"Importo in euro").text.toString());assertTrue(ledger().operations.isEmpty());click(a.window.decorView,"Salva");assertEquals(-1250L,ledger().balance);c.pause().stop().destroy()
 }
}
