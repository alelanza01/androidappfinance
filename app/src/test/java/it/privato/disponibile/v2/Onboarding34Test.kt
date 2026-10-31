package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.ledger

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Onboarding34Test {
 @Test fun freshInstallResumesAndCompletionIsAtomicAndIdempotent() {
  val context=RuntimeEnvironment.getApplication();context.deleteDatabase("finance-v2.db");context.getSharedPreferences("onboarding-v34",0).edit().clear().commit()
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();set(a.window.decorView,"Liquidità iniziale in euro","1200");click(a.window.decorView,"Continua");set(a.window.decorView,"Riserva in euro","300");c.recreate();a=c.get();assertEquals("300",UiSupport.field(a.window.decorView,"Riserva in euro").text.toString());click(a.window.decorView,"Continua")
  set(a.window.decorView,"Descrizione","Affitto");set(a.window.decorView,"Importo previsto in euro","300");click(a.window.decorView,"Aggiungi impegno");click(a.window.decorView,"Riprendi più tardi");assertEquals(0L,ledger().balance);assertTrue(ledger().operations.isEmpty())
  click(a.window.decorView,"Riprendi configurazione");click(a.window.decorView,"Continua");Capture.save(a,"v34-D-onboarding-riepilogo.png",360,800);click(a.window.decorView,"Inizia a usare l'app")
  val l=ledger();assertEquals(120000L,l.balance);assertEquals(60000L,l.spendable);assertEquals(1,l.pending.size);assertTrue(l.actual.isEmpty());assertTrue(l.settings.onboardingCompleted);assertEquals(HorizonMode.ROLLING,l.settings.horizonMode)
  UiSupport.withStore {it.completeOnboarding(120000,30000,emptyList(),"second-completion")};assertEquals(l,ledger());c.recreate();assertFalse(UiSupport.all(c.get().window.decorView).filterIsInstance<android.widget.TextView>().any {it.text.toString()=="Da quanto parti?"});c.pause().stop().destroy()
 }
 @Test fun existingInstallationSkipsOnboardingAndPreservesBalance() {
  UiSupport.reset();UiSupport.withStore {it.settings(it.snapshot().settings.copy(opening=23456))};val before=ledger();val c=Robolectric.buildActivity(MainActivity::class.java).setup();assertTrue(UiSupport.all(c.get().window.decorView).any {it is SpendableRing});assertEquals(before,ledger());c.pause().stop().destroy()
 }
}
