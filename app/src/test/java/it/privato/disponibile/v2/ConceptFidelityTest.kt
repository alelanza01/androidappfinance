package it.privato.disponibile.v2

import android.view.*
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.withStore
import it.privato.disponibile.v2.UiSupport.all
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class ConceptFidelityTest {
 @Before fun reset()=UiSupport.reset()
 @Test fun homeRingUsesPositiveCashAndNeverClaimsAPercentForNonpositiveCash() {
  val context=RuntimeEnvironment.getApplication();val cat=Category(1,"Altro",0);val l=Ledger(Settings(opening=30000,reserve=5000),listOf(cat),listOf(Operation(1,"Prevista",5000,Kind.EXPENSE,Status.PLANNED,LocalDate.now(),1)),emptyList())
  assertEquals(2.0/3.0,SpendableRing(context,Palette(false),l).fraction!!,.00001)
  assertEquals(0.0,SpendableRing(context,Palette(false),l.copy(settings=l.settings.copy(reserve=40000))).fraction!!,0.0)
  assertNull(SpendableRing(context,Palette(false),l.copy(settings=l.settings.copy(opening=0))).fraction)
  assertNull(SpendableRing(context,Palette(false),l.copy(settings=l.settings.copy(opening=-5000))).fraction)
 }
 @Test fun heartsPersistWithoutLosingExpenseDraftOrChangingHistoricalCategories() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();val before=ledger();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","23,45");click(a.window.decorView,"Categoria")
  click(a.window.decorView,"Aggiungi Spesa ai preferiti");assertTrue(ledger().categories.first {it.name=="Spesa"}.favorite);c.recreate();a=c.get();click(a.window.decorView,"Spesa")
  assertEquals("23,45",UiSupport.field(a.window.decorView,"Importo in euro").text.toString());assertEquals(before.operations,ledger().operations);click(a.window.decorView,"Salva spesa")
  val l=ledger();assertEquals(-2345L,l.balance);assertEquals("Spesa",l.categoryName(l.actual.single().category));assertEquals(l,Backup.decode(Backup.encode(l)));c.pause().stop().destroy()
 }
 @Test fun installmentSelectionAndBackNeverBookUntilExplicitSave() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","19,99");click(a.window.decorView,"Rate");click(a.window.decorView,"Ogni rata");click(a.window.decorView,"3 rate");a.onBackPressed();assertTrue(ledger().operations.isEmpty())
  click(a.window.decorView,"Rate");click(a.window.decorView,"Ogni rata");click(a.window.decorView,"3 rate");c.recreate();a=c.get();click(a.window.decorView,"Applica rate");assertTrue(ledger().operations.isEmpty());click(a.window.decorView,"Salva piano")
  val l=ledger();assertEquals(3,l.pending.size);assertTrue(l.pending.all {it.cents==1999L});assertEquals(5997L,l.pending.sumOf {it.cents});assertEquals(0L,l.balance);c.pause().stop().destroy()
 }
 @Test fun monthQuarterYearOnlyAggregateActualPersonalExpensesAndKeepPeriodComparisonsHonest() {
  val m=YearMonth.now();val today=LocalDate.now();withStore {s->val cat=s.snapshot().categories.first().id
   listOf(today to 1000L,m.minusMonths(2).atDay(1) to 2000L,m.minusMonths(6).atDay(1) to 4000L).forEach {(day,n)->s.save(Operation(title="DEMO",cents=n,kind=Kind.EXPENSE,status=Status.ACTUAL,date=day,category=cat))}
   s.save(Operation(title="DEMO futura",cents=8000,kind=Kind.EXPENSE,status=Status.PLANNED,date=today.plusDays(3),category=cat));s.saveAdvance(Advance(cents=5000,date=today,description="DEMO anticipo",debtor="DEMO"));s.saveRefund(Refund(advance=s.snapshot().advances.single().id,cents=2000,date=today))
  }
  val before=ledger();assertEquals(1000L,PeriodInsights(before,m,1).total);assertEquals(3000L,PeriodInsights(before,m,3).total);assertEquals(7000L,PeriodInsights(before,m,12).total);assertFalse(PeriodInsights(before,m.plusMonths(1),3).comparable)
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Analisi");click(a.window.decorView,"Trimestre");assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()==DisplayMoney.amount(3000)});click(a.window.decorView,"Anno");assertEquals(before,ledger());c.pause().stop().destroy()
 }
 @Test fun newNavigationExposesFourDestinationsAndCreationWhileCreditsRemainReachable() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();listOf("Home","Movimenti","Analisi","Impegni").forEach {page->click(a.window.decorView,page);val nav=all(a.window.decorView).filterIsInstance<Button>().filter {it.text.toString() in listOf("Home","Movimenti","Analisi","Impegni")};assertEquals(4,nav.size);assertEquals(page,nav.single {it.isSelected}.text.toString());assertTrue(all(a.window.decorView).any {it.contentDescription=="Aggiungi"})}
  click(a.window.decorView,"Home");click(a.window.decorView,"Crediti");click(a.window.decorView,"Nuovo anticipo");assertTrue(ledger().advances.isEmpty());c.pause().stop().destroy()
 }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ConceptScreenshotsTest {
 private fun capture(a:MainActivity,name:String,overlay:Boolean=false){
  val root=a.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0);val d=a.resources.displayMetrics.density
  root.dispatchApplyWindowInsets(WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,(24*d).toInt(),0,(24*d).toInt())).build())
  Capture.save(a,name,overlay=overlay)
 }

 @Test fun ratesAndCenteredDialogStayUsableWithLargeFontsAndSimulatedKeyboard() {
  listOf(Triple(320,640,2f),Triple(360,800,1f),Triple(411,891,1.5f)).forEach {(w,h,font)->listOf("light","dark").forEach {theme->
   RuntimeEnvironment.setQualifiers("w${w}dp-h${h}dp-mdpi");RuntimeEnvironment.setFontScale(font);UiSupport.reset();withStore {it.settings(it.snapshot().settings.copy(theme=theme))}
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","9999999,99");click(a.window.decorView,"Rate");click(a.window.decorView,"12 rate")
   val suffix="$w-font${(font*100).toInt()}-$theme";Capture.save(a,"responsive-rate-concept-$suffix.png",w,h)
   val root=a.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
   root.dispatchApplyWindowInsets(WindowInsets.Builder().setInsets(WindowInsets.Type.ime(),android.graphics.Insets.of(0,0,0,220)).build())
   Capture.save(a,"responsive-rate-ime-$suffix.png",w,h);val apply=all(root).filterIsInstance<Button>().first {it.text.toString()=="Applica rate"};val point=IntArray(2);apply.getLocationInWindow(point);assertTrue(point[1]+apply.height<=h-220)
   a.onBackPressed();a.onBackPressed();Capture.save(a,"responsive-dialogo-concept-$suffix.png",w,h,overlay=true)
   assertEquals(Gravity.CENTER,UiSupport.dialog().window!!.attributes.gravity);choose("Esci senza salvare");assertTrue(ledger().operations.isEmpty());c.pause().stop().destroy()
  }};RuntimeEnvironment.setFontScale(1f)
 }
 @Test fun nonpositiveCashAndLongNegativeMarginsAreRenderedWithoutFictitiousProgress() {
  RuntimeEnvironment.setQualifiers("w320dp-h640dp-mdpi");RuntimeEnvironment.setFontScale(2f)
  listOf("light","dark").forEach {theme->UiSupport.reset();withStore {it.settings(it.snapshot().settings.copy(opening=-Money.MAX,reserve=Money.MAX,theme=theme))}
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val ring=all(a.window.decorView).filterIsInstance<SpendableRing>().single();assertNull(ring.fraction);assertTrue(ring.contentDescription.toString().contains(Money.format(-Money.MAX*2)))
   Capture.save(a,"concept-margine-negativo-lungo-$theme.png",320,640);c.pause().stop().destroy()
  };RuntimeEnvironment.setFontScale(1f)
 }
 @Test fun sixRealScreensInBothThemesWithIsolatedDemoFixtures() {
  listOf("light","dark").forEach {theme->UiSupport.reset();withStore {s->val l=s.snapshot();s.settings(l.settings.copy(opening=175000,reserve=30000,theme=theme));val keys=listOf("basket","home","car","travel","health","bag")
   keys.forEachIndexed {i,key->val cat=l.categories.first {it.icon==key};s.save(Operation(title="DEMO · ${cat.name}",cents=2100L+i*1400,kind=Kind.EXPENSE,status=Status.ACTUAL,date=LocalDate.now().minusDays((i+1).toLong()),category=cat.id));if(i<4)s.category(cat.id,cat.name,cat.color,cat.icon,true)}
   s.save(Operation(title="DEMO · Compenso",cents=65000,kind=Kind.INCOME,status=Status.ACTUAL,date=LocalDate.now().minusDays(8),category=l.categories.first {it.icon=="income"}.id));s.save(Operation(title="DEMO · Affitto",cents=52000,kind=Kind.EXPENSE,status=Status.PLANNED,date=LocalDate.now().plusDays(3),category=l.categories.first {it.icon=="home"}.id));s.save(Operation(title="DEMO · Connessione",cents=2900,kind=Kind.EXPENSE,status=Status.PLANNED,date=LocalDate.now().plusDays(7),category=l.categories.first {it.icon=="home"}.id))
  }
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();capture(a,"concept-01-home-$theme.png")
  click(a.window.decorView,"Spesa veloce");set(a.window.decorView,"Importo in euro","37,80");capture(a,"concept-02-spesa-$theme.png");click(a.window.decorView,"Categoria");capture(a,"concept-03-categorie-$theme.png");a.onBackPressed()
  click(a.window.decorView,"Rate");click(a.window.decorView,"3 rate");capture(a,"concept-05-rate-$theme.png");a.onBackPressed();Capture.layout(a.window.decorView,1080,2400);a.onBackPressed();capture(a,"concept-06-conferma-$theme.png",overlay=true);choose("Esci senza salvare")
  click(a.window.decorView,"Analisi");capture(a,"concept-04-analisi-$theme.png");c.pause().stop().destroy()
  }
 }
}
