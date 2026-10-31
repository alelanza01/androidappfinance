package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*
import android.widget.*
import java.time.*
import java.io.File
import org.json.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RevisionVisualTest {
 @Before fun clock(){AppClock.clock=Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"),ZoneId.of("Europe/Rome"))}
 @After fun resetClock(){AppClock.clock=Clock.systemDefaultZone();RuntimeEnvironment.setFontScale(1f)}
 @Test fun pairedNativeViewsUseExactApprovedFixtureAndSelections() {
  val states=JSONArray()
  for(theme in listOf("light","dark")) {
   val fixture=ReviewFixture.install(theme);assertEquals(120000L,fixture.balance);assertEquals(52000L,fixture.spendable)
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
   fun capture(name:String,view:JSONObject=JSONObject(),overlay:Boolean=false){Capture.save(a,"rc2-$name-$theme.png",360,800,overlay);states.put(JSONObject().put("name",name).put("theme",theme).put("today",AppClock.today()).put("cash",a.l.balance).put("reserve",a.l.settings.reserve).put("commitments",a.l.commitments).put("view",view))}
   for((name,page)in listOf("home" to 0,"settings" to 6,"advances" to 3,"analysis" to 4,"movements" to 1,"commitments" to 2)) {
    a.moveFilter=0;a.month=if(page==2)YearMonth.of(2026,11)else YearMonth.of(2026,10);a.calendarGrid=false;a.go(page)
    capture(name,JSONObject().put("page",mapOf(0 to "home",6 to "settings",3 to "advances",4 to "analysis",1 to "movements",2 to "commitments")[page]).put("filter",when(page){1->"all/all";3->"open";else->JSONObject.NULL}))
   }
   a.go(0);a.createMenu();capture("register",JSONObject().put("underlying","home"),true);UiSupport.choose("Chiudi Registra")
   a.openForm("operation",defaults=mapOf("kind" to "EXPENSE","status" to "ACTUAL","amount" to "600","category" to "27","date" to "2026-10-31"));capture("expense",JSONObject().put("amount",60000).put("category",27).put("count",1))
   a.openRates();a.rateChoice="finite";a.rateCount="3";a.draft!!.values["amountMode"]="TOTAL";a.render();capture("rates-total",JSONObject().put("amount",60000).put("count",3).put("mode","total"))
   a.draft!!.values["amountMode"]="EACH";a.render();capture("rates-each",JSONObject().put("amount",60000).put("count",3).put("mode","each"))
   a.simulationDraft=SimulationDraft("600",LocalDate.of(2026,11,30),false);a.go(5);capture("simulator",JSONObject().put("simulation",60000).put("period",30).put("scenario",false))
   for(step in 0..3){a.onboarding.clear();a.onboarding.step=step;a.onboarding.put("opening","1200");a.onboarding.put("reserve","300");a.go(30);capture("onboarding-${step+1}",JSONObject().put("step",step).put("amount",120000).put("reserve",30000).put("items",JSONArray()))}
   assertEquals(fixture,UiSupport.ledger());c.pause().stop().destroy()
  }
  File(System.getProperty("posso.screenshots.dir"),"rc2-native-states.json").writeText(states.toString(2))
 }
 @Test fun reviewedScreensLargeTextHaveScrollableContentAndReachableActions() {
  for(theme in listOf("light","dark"))for((width,height,font)in listOf(Triple(320,640,2f),Triple(411,891,1.5f))) {
   RuntimeEnvironment.setQualifiers("w${width}dp-h${height}dp-mdpi");RuntimeEnvironment.setFontScale(font);val before=ReviewFixture.install(theme);val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
   for(page in listOf(1,3,4,6,30)){a.go(page);Capture.save(a,"rc2-responsive-$page-$width-$theme.png",width,height);assertTrue(UiSupport.all(a.window.decorView).any {it is ScrollView});if(page==3){val scroll=UiSupport.all(a.window.decorView).filterIsInstance<ScrollView>().first();scroll.fullScroll(android.view.View.FOCUS_DOWN);UiSupport.idle();Capture.save(a,"rc2-responsive-bottom-$page-$width-$theme.png",width,height);val badge=UiSupport.all(a.window.decorView).filterIsInstance<TextView>().first {it.text.toString()=="Rimborso parziale"};assertEquals(0,badge.layout.getEllipsisCount(0))};if(page==3 || page==30){val b=UiSupport.all(a.window.decorView).first {it.tag==if(page==3)"advance-cta"else "onboarding-next"};val pos=IntArray(2);b.getLocationInWindow(pos);assertTrue(pos[1]>=0 && pos[1]+b.height<=height)}}
   a.go(0);a.createMenu();Capture.save(a,"rc2-responsive-register-$width-$theme.png",width,height,true);UiSupport.choose("Chiudi Registra");assertEquals(before,UiSupport.ledger());c.pause().stop().destroy()
  }
 }
 @Test fun scopedAdvancesNeverChangeGlobalTotalOrMislabelOverdueAsClosed() {
  ReviewFixture.install("light");val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();a.go(3);val before=UiSupport.ledger()
  assertTrue(UiSupport.all(a.window.decorView).filterIsInstance<TextView>().none {it.text.toString() in listOf("Cerca crediti","Filtri crediti","Nessun credito")})
  a.creditStatus=4;a.render();assertTrue(UiSupport.all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Aperto · scaduto"});a.creditQuery="nessun risultato";a.render()
  assertTrue(UiSupport.all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Residuo dei risultati: € 0"});assertTrue(UiSupport.all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="€ 30"});assertEquals(before,UiSupport.ledger());c.pause().stop().destroy()
 }
}
