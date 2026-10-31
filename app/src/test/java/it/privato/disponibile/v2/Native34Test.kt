package it.privato.disponibile.v2
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
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
   reset();withStore {it.replace(Demo.ledger(theme))};val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val before=ledger()
   Capture.save(a,"v34-A-home-$theme.png",360,800)
   assertEquals(4,all(a.window.decorView).filterIsInstance<Button>().count {it.text.toString() in listOf("Home","Movimenti","Analisi","Impegni")})
   click(a.window.decorView,"Aggiungi");Capture.save(a,"v34-A-registra-$theme.png",360,800,true)
   assertTrue(all(dialog().window!!.decorView).any {it is TextView && it.text.toString()=="Denaro già pagato"})
   choose("Chiudi Registra");assertEquals(before,ledger());c.pause().stop().destroy()
  }
 }
}
