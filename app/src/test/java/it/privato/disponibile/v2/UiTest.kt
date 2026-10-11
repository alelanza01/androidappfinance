package it.privato.disponibile.v2

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class UiTest {
 @Before fun reset(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db")}
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 private fun idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()}
 private fun click(root:View,text:String){all(root).filterIsInstance<Button>().first {it.text.toString()==text}.performClick();idle()}
 private fun dialog():AlertDialog {idle();return ShadowAlertDialog.getLatestAlertDialog()}
 private fun documentResult(a:MainActivity,request:Int,result:Int,intent:android.content.Intent?) {
  val method=MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,android.content.Intent::class.java)
  method.isAccessible=true;method.invoke(a,request,result,intent)
 }
 private fun withStore(block:(Store)->Unit){idle();val store=Store(RuntimeEnvironment.getApplication());try {block(store)}finally {store.close()}}
 @Test fun createEditDeleteAndNavigation() {
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  click(a.window.decorView,"+ Nuova operazione");var d=dialog();val fields=all(d.window!!.decorView).filterIsInstance<EditText>()
  fields[0].setText("Bolletta demo");fields[1].setText("25,50");d.getButton(-1).performClick();idle()
  withStore {assertEquals(2550L,it.snapshot().operations.single().cents);assertEquals(0L,it.snapshot().balance)}
  click(a.window.decorView,"Storico");click(a.window.decorView,"Gestisci");dialog().listView.performItemClick(null,1,1);d=dialog()
  all(d.window!!.decorView).filterIsInstance<EditText>()[1].setText("30,00");d.getButton(-1).performClick();idle()
  withStore {assertEquals(3000L,it.snapshot().operations.single().cents)}
  click(a.window.decorView,"Gestisci");dialog().listView.performItemClick(null,2,2);dialog().getButton(-1).performClick();idle()
  withStore {assertEquals(0,it.snapshot().pending.size)}
  listOf("Agenda","Grafici","Altro","Oggi").forEach {click(a.window.decorView,it)}
  controller.pause().stop().destroy()
 }
 @Test fun simulatorIsImmediateAndNeverCreatesAnOperation() {
  withStore {it.settings(it.snapshot().settings.copy(opening=10000,reserve=2000))}
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  all(a.window.decorView).filterIsInstance<EditText>().single().setText("25,00");idle()
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("55,00")})
  withStore {assertTrue(it.snapshot().operations.isEmpty());assertEquals(10000L,it.snapshot().balance)}
  controller.pause().stop().destroy()
 }
 @Test fun searchFiltersAndThemeAndDemoIsolation() {
  withStore {val c=it.snapshot().categories.first().id;it.save(Operation(title="Affitto demo",cents=500,kind=Kind.EXPENSE,status=Status.PLANNED,date=LocalDate.now(),category=c));it.save(Operation(title="Compenso demo",cents=1000,kind=Kind.INCOME,status=Status.ACTUAL,date=LocalDate.now(),category=c))}
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  click(a.window.decorView,"Storico");all(a.window.decorView).filterIsInstance<EditText>().single().setText("Affitto");idle()
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Affitto demo"});assertFalse(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Compenso demo"})
  click(a.window.decorView,"Altro");click(a.window.decorView,"Esplora la demo separata")
  assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString().contains("DATI DIMOSTRATIVI")})
  withStore {assertEquals(2,it.snapshot().operations.size)}
  click(a.window.decorView,"Esci dalla demo");controller.pause().stop().destroy()
 }
 @Test fun invalidAmountCannotBeSaved() {
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  click(a.window.decorView,"+ Nuova operazione");val d=dialog();val fields=all(d.window!!.decorView).filterIsInstance<EditText>();fields[0].setText("Test");fields[1].setText("1,234");d.getButton(-1).performClick();idle()
  withStore {assertTrue(it.snapshot().operations.isEmpty())}
  assertEquals("Operazione non completata",Shadows.shadowOf(dialog()).title.toString())
  controller.pause().stop().destroy()
 }
 @Test fun backupDocumentExportAndImportKeepASafetyCopy() {
  withStore {it.settings(it.snapshot().settings.copy(opening=12300))}
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  val exportUri=android.net.Uri.parse("content://backup/export")
  val output=java.io.ByteArrayOutputStream();Shadows.shadowOf(a.contentResolver).registerOutputStream(exportUri,output)
  documentResult(a,1,android.app.Activity.RESULT_OK,android.content.Intent().setData(exportUri));idle()
  assertEquals(12300L,Backup.decode(output.toString("UTF-8")).settings.opening)
  val imported=Demo.ledger("light");val importUri=android.net.Uri.parse("content://backup/import")
  Shadows.shadowOf(a.contentResolver).registerInputStream(importUri,java.io.ByteArrayInputStream(Backup.encode(imported).toByteArray()))
  documentResult(a,2,android.app.Activity.RESULT_OK,android.content.Intent().setData(importUri));dialog().getButton(-1).performClick();idle()
  withStore {assertEquals(imported.balance,it.snapshot().balance)}
  val previous=java.io.File(a.filesDir,"pre-import.json");assertTrue(previous.exists());assertEquals(12300L,Backup.decode(previous.readText()).settings.opening)
  controller.pause().stop().destroy()
 }
 @Test fun cancelledOrMalformedDocumentLeavesDataUntouched() {
  withStore {it.settings(it.snapshot().settings.copy(opening=12300))}
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
  documentResult(a,2,android.app.Activity.RESULT_CANCELED,null)
  val uri=android.net.Uri.parse("content://backup/broken");Shadows.shadowOf(a.contentResolver).registerInputStream(uri,java.io.ByteArrayInputStream("broken".toByteArray()))
  documentResult(a,2,android.app.Activity.RESULT_OK,android.content.Intent().setData(uri));idle()
  withStore {assertEquals(12300L,it.snapshot().balance)};controller.pause().stop().destroy()
 }
}
