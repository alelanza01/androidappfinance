package it.privato.disponibile.v2
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import it.privato.disponibile.v2.UiSupport.idle
import it.privato.disponibile.v2.UiSupport.reset
import it.privato.disponibile.v2.UiSupport.withStore
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.all
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.dialog
import it.privato.disponibile.v2.UiSupport.choose

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Native34Test {
 @Test fun milestoneAHomeAndRegister() {
  listOf("light","dark").forEach {theme->
   VisualFixture34.install(theme);val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val before=ledger()
   Capture.save(a,"v34-A-home-$theme.png",360,800)
   assertEquals(4,all(a.window.decorView).filterIsInstance<Button>().count {it.text.toString() in listOf("Home","Movimenti","Analisi","Impegni")})
   click(a.window.decorView,"Aggiungi");Capture.save(a,"v34-A-registra-$theme.png",360,800,true)
   assertTrue(all(dialog().window!!.decorView).any {it is TextView && it.text.toString()=="Denaro che hai pagato"})
   choose("Chiudi Registra");assertEquals(before,ledger());c.pause().stop().destroy()
  }
 }
 @Test fun dailyDraftAndNativeDateSurviveRecreationAndCommitOnce() {
  reset();val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get()
  click(a.window.decorView,"Spesa veloce");UiSupport.set(a.window.decorView,"Importo in euro","23,45")
  click(a.window.decorView,"Categoria");chooseIfShowingNothing();click(a.window.decorView,"Spesa");click(a.window.decorView,"Data")
  val tomorrow=java.time.LocalDate.now().plusDays(1).toString()
  assertTrue(all(dialog().window!!.decorView).filterIsInstance<Button>().filter {it.contentDescription==tomorrow}.all {!it.isEnabled})
  c.recreate();a=c.get();assertTrue(dialog().isShowing);choose("Ieri");choose("Conferma data")
  assertEquals("23,45",UiSupport.field(a.window.decorView,"Importo in euro").text.toString());assertTrue(ledger().operations.isEmpty())
  val save=all(a.window.decorView).filterIsInstance<Button>().first {it.text=="Salva spesa"};save.performClick();save.performClick();idle()
  assertEquals(1,ledger().operations.size);assertEquals(-2345L,ledger().balance);c.pause().stop().destroy()
 }
 private fun chooseIfShowingNothing()=Unit
 @Test fun nativeDailyScreensBothThemes() {
  listOf("light","dark").forEach {theme->VisualFixture34.install(theme);val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
   click(a.window.decorView,"Spesa veloce");UiSupport.set(a.window.decorView,"Importo in euro","100");Capture.save(a,"v34-B-spesa-$theme.png",360,800)
   click(a.window.decorView,"Categoria");Capture.save(a,"v34-B-categorie-$theme.png",360,800);a.onBackPressed()
   click(a.window.decorView,"Data");Capture.save(a,"v34-B-date-$theme.png",360,800,true);choose("Annulla")
   click(a.window.decorView,"Rate");click(a.window.decorView,"3 rate");Capture.save(a,"v34-B-rate-$theme.png",360,800);click(a.window.decorView,"Applica rate");click(a.window.decorView,"Salva piano")
   assertEquals(listOf(3334L,3333L,3333L),ledger().series.single().installments)
   click(a.window.decorView,"Movimenti");click(a.window.decorView,"Filtri");choose("Tutti");Capture.save(a,"v34-B-movimenti-$theme.png",360,800)
   c.pause().stop().destroy()
  }
 }

 @Test fun advancedScreensAndDistinctCommitmentModes() {
  listOf("light","dark").forEach {theme->VisualFixture34.install(theme);val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val before=ledger()
   click(a.window.decorView,"Impegni");a.month=java.time.YearMonth.from(before.pending.first().date);a.render();Capture.save(a,"v34-C-impegni-elenco-$theme.png",360,800)
   click(a.window.decorView,"Calendario");Capture.save(a,"v34-C-impegni-calendario-$theme.png",360,800)
   assertTrue(a.calendarGrid);assertEquals(before,ledger());val date=before.pending.first().date
   a.month=java.time.YearMonth.from(date);a.render();a.dayDetails(date);Capture.save(a,"v34-C-giorno-$theme.png",360,800,true);choose("Annulla");assertEquals(before,ledger())
   click(a.window.decorView,"Home");click(a.window.decorView,"Anticipi da recuperare");Capture.save(a,"v34-C-anticipi-$theme.png",360,800);a.creditStatus=0;a.render();assertFalse(all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()=="Aperti"}.isSelected);a.creditStatus=1;a.render();click(a.window.decorView,"Pulizie Casa A");Capture.save(a,"v34-C-anticipo-dettaglio-$theme.png",360,800)
   click(a.window.decorView,"Registra rimborso");Capture.save(a,"v34-C-rimborso-$theme.png",360,800);a.onBackPressed();a.onBackPressed();a.onBackPressed()
   click(a.window.decorView,"Home");click(a.window.decorView,"Simula spesa");UiSupport.set(a.window.decorView,"Importo in euro","600");Capture.save(a,"v34-C-simulatore-$theme.png",360,800);assertEquals(before,ledger());c.recreate();assertEquals("600",UiSupport.field(c.get().window.decorView,"Importo in euro").text.toString());c.pause().stop().destroy()
   val c2=Robolectric.buildActivity(MainActivity::class.java).setup();val a2=c2.get();click(a2.window.decorView,"Analisi");Capture.save(a2,"v34-D-analisi-$theme.png",360,800);click(a2.window.decorView,"Home");click(a2.window.decorView,"Impostazioni");Capture.save(a2,"v34-D-impostazioni-$theme.png",360,800);click(a2.window.decorView,"Backup");Capture.save(a2,"v34-D-backup-$theme.png",360,800);assertEquals(before,ledger());c2.pause().stop().destroy()
  }
 }
 @Test fun groupedMenuAndNativeDateKeepCloseAndConfirmReachableAtLargeText() {
  listOf(Triple(320,640,2f),Triple(360,800,1f),Triple(411,891,1.5f)).forEach {(w,h,font)->listOf("light","dark").forEach {theme->
   RuntimeEnvironment.setQualifiers("w${w}dp-h${h}dp-mdpi");RuntimeEnvironment.setFontScale(font);VisualFixture34.install(theme);val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Aggiungi")
   val suffix="$w-${(font*100).toInt()}-$theme";Capture.save(a,"v34-E-registra-$suffix.png",w,h,true)
   val close=all(dialog().window!!.decorView).first {it.contentDescription=="Chiudi Registra"};assertTrue(close.height>=44);assertTrue(all(dialog().window!!.decorView).any {it is ScrollView});choose("Spesa");UiSupport.set(a.window.decorView,"Importo in euro","12,30");click(a.window.decorView,"Data");Capture.save(a,"v34-E-date-$suffix.png",w,h,true)
   val year=all(dialog().window!!.decorView).filterIsInstance<Spinner>().first {it.contentDescription=="Anno"}.selectedView as TextView
   assertTrue("Anno leggibile a testo ingrandito",year.paint.measureText(year.text.toString())<=year.width-year.paddingLeft-year.paddingRight)
   val confirm=all(dialog().window!!.decorView).filterIsInstance<Button>().first {it.text.toString()=="Conferma data"};assertTrue(confirm.height>=44);choose("Annulla");assertEquals("12,30",UiSupport.field(a.window.decorView,"Importo in euro").text.toString());c.pause().stop().destroy()
  }};RuntimeEnvironment.setFontScale(1f)
 }

}
