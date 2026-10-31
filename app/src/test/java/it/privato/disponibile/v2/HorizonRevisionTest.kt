package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import android.widget.TextView
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class HorizonRevisionTest {
 private val day=LocalDate.of(2026,10,31)
 @Before fun setup(){AppClock.clock=Clock.fixed(day.atTime(12,0).toInstant(ZoneOffset.UTC),ZoneId.of("Europe/Rome"));UiSupport.reset()}
 @After fun restore(){AppClock.clock=Clock.systemDefaultZone()}
 @Test fun everyFixedRollingTransitionPersistsReopensAndRefreshesNextDay() {
  for(from in listOf(0,30,60,90))for(to in listOf(0,30,60,90)) {
   AppClock.clock=Clock.fixed(day.atTime(12,0).toInstant(ZoneOffset.UTC),ZoneId.of("Europe/Rome"));UiSupport.reset()
   UiSupport.withStore {s->val l=s.snapshot();s.settings(l.settings.copy(opening=10000,reserve=1000,horizon=day.minusDays(10),horizonMode=if(from==0)HorizonMode.FIXED else HorizonMode.ROLLING,horizonDays=if(from==0)30 else from))
    listOf(-10L to 100L,0L to 200L,20L to 300L,21L to 400L,30L to 500L,31L to 600L).forEach {(offset,n)->s.save(Operation(title="Scadenza",cents=n,kind=Kind.EXPENSE,status=Status.PLANNED,date=day.plusDays(offset),category=l.categories.first().id))}
   }
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();UiSupport.click(a.window.decorView,"Impostazioni");UiSupport.click(a.window.decorView,"Regole del saldo")
   if(to==0){UiSupport.click(a.window.decorView,"Data personalizzata");UiSupport.choose("Oggi");UiSupport.choose("Domani");UiSupport.choose("2026-11-20");UiSupport.choose("Conferma data")}else UiSupport.click(a.window.decorView,"$to giorni")
   val saved=UiSupport.ledger().settings;assertEquals(if(to==0)HorizonMode.FIXED else HorizonMode.ROLLING,saved.horizonMode)
   assertEquals(if(to==0)day.plusDays(20)else day.minusDays(10),saved.horizon)
   assertEquals(if(to==0)600L else if(to==30)1500L else 2100L,UiSupport.ledger().commitments)
   c.recreate();a=c.get();assertEquals(saved,UiSupport.ledger().settings);a.go(0)
   fun homeMatches(){val text=saved.effectiveHorizon().format(DateTimeFormatter.ofPattern("d MMM",Locale.ITALY));assertTrue(UiSupport.all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Previsione fino al $text"})}
   homeMatches();AppClock.clock=Clock.offset(AppClock.clock,Duration.ofDays(1));c.pause().resume();UiSupport.idle();homeMatches()
   assertEquals(saved,UiSupport.ledger().settings);assertEquals(if(to==0)600L else 2100L,UiSupport.ledger().commitments)
   assertEquals(saved,Backup.decode(Backup.encode(UiSupport.ledger())).settings);c.pause().stop().destroy()
  }
 }
 @Test fun historicalFixedBackupNeverMovesAndOverdueAlwaysProtected() {
  UiSupport.withStore {s->val l=s.snapshot();s.settings(l.settings.copy(opening=30000,horizon=day.minusDays(20),horizonMode=HorizonMode.FIXED));s.save(Operation(title="Arretrato",cents=7000,kind=Kind.EXPENSE,status=Status.PLANNED,date=day.minusDays(1),category=l.categories.first().id));s.save(Operation(title="Oggi",cents=3000,kind=Kind.EXPENSE,status=Status.PLANNED,date=day,category=l.categories.first().id));val before=s.snapshot()
   s.replace(Backup.decode(Backup.encode(before)));assertEquals(before,s.snapshot());assertEquals(10000L,s.snapshot().commitments);AppClock.clock=Clock.offset(AppClock.clock,Duration.ofDays(2));assertEquals(before.settings,s.snapshot().settings);assertEquals(10000L,s.snapshot().commitments)
  }
 }
}
