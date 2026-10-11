package it.privato.disponibile.v2
import android.app.AlertDialog
import android.view.*
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class AdvanceUiTest {
 @Before fun setup(){val c=RuntimeEnvironment.getApplication();c.deleteDatabase("finance-v2.db");val s=Store(c);s.settings(s.snapshot().settings.copy(opening=30000));s.close()}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 private fun idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()}
 private fun click(v:View,text:String){all(v).filterIsInstance<Button>().first {it.text.toString()==text}.performClick();idle()}
 private fun dialog():AlertDialog {idle();return ShadowAlertDialog.getLatestAlertDialog()}
 private fun fields(d:AlertDialog)=all(d.window!!.decorView).filterIsInstance<EditText>()
 private fun snapshot():Ledger {val s=Store(RuntimeEnvironment.getApplication());return try{s.snapshot()}finally{s.close()}}
 @Test fun quickAdvancePartialAndCompleteRefundWorkThroughRealForms() {
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get();click(a.window.decorView,"Nuovo anticipo")
  var d=dialog();var f=fields(d);f[0].setText("50");f[1].setText("Pulizie Casa A");f[2].setText("Proprietario");d.getButton(-1).performClick();idle()
  assertEquals(25000L,snapshot().balance);click(a.window.decorView,"Anticipi da recuperare");click(a.window.decorView,"Apri anticipo")
  click(dialog().window!!.decorView,"Registra rimborso");d=dialog();fields(d)[0].setText("20");d.getButton(-1).performClick();idle()
  assertEquals(27000L,snapshot().balance);assertEquals(3000L,snapshot().outstanding)
  click(a.window.decorView,"Apri anticipo");click(dialog().window!!.decorView,"Registra rimborso");d=dialog();fields(d)[0].setText("30");d.getButton(-1).performClick();idle()
  assertEquals(30000L,snapshot().balance);assertEquals(0L,snapshot().outstanding)
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("Recuperato")})
  click(a.window.decorView,"Storico");assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("Anticipo · Pulizie")});assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("Rimborso · Pulizie")})
  controller.pause().stop().destroy()
 }
 @Test fun overpaymentErrorPreservesFormAndDataAndAllowsCorrection() {
  val s=Store(RuntimeEnvironment.getApplication());s.saveAdvance(Advance(cents=5000,date=java.time.LocalDate.now(),description="Test",debtor="Marco"));s.close()
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get();click(a.window.decorView,"Anticipi da recuperare");click(a.window.decorView,"Apri anticipo");click(dialog().window!!.decorView,"Registra rimborso")
  val form=dialog();fields(form)[0].setText("51");form.getButton(-1).performClick();idle();assertEquals("Operazione non completata",Shadows.shadowOf(dialog()).title.toString());assertTrue(snapshot().refunds.isEmpty())
  dialog().getButton(-1).performClick();fields(form)[0].setText("20");form.getButton(-1).performClick();idle();assertEquals(27000L,snapshot().balance)
  controller.pause().stop().destroy()
 }
 @Test fun subjectProjectStateAndPeriodFiltersAreIndependent() {
  val c=RuntimeEnvironment.getApplication();val s=Store(c);val today=java.time.LocalDate.now()
  s.saveAdvance(Advance(cents=5000,date=today,description="Pulizie",debtor="Marco",project="Casa A"));s.saveAdvance(Advance(cents=1000,date=today.minusMonths(1),description="Materiali",debtor="Anna",project="Casa B"));s.close()
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get();click(a.window.decorView,"Anticipi da recuperare")
  click(a.window.decorView,"Filtri anticipi");val inputs=all(a.window.decorView).filterIsInstance<EditText>();inputs[1].setText("Marco");idle();assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Pulizie"});assertFalse(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Materiali"})
  inputs[1].setText("");inputs[2].setText("Casa B");idle();assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Materiali"})
  inputs[2].setText("");val spinners=all(a.window.decorView).filterIsInstance<Spinner>();spinners[0].setSelection(3);idle();assertFalse(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Pulizie"})
  spinners[0].setSelection(0);spinners[1].setSelection(1);idle();assertFalse(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Materiali"})
  controller.pause().stop().destroy()
 }
}
