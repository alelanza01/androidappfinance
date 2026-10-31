package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.LocalDate
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class Financial34Test {
 private lateinit var store:Store
 private val today=LocalDate.now()
 @Before fun start(){UiSupport.reset();store=Store(RuntimeEnvironment.getApplication())}
 @After fun stop(){store.close()}
 private fun planned(n:Long,days:Long=0,kind:Kind=Kind.EXPENSE)=Operation(title="Test",cents=n,kind=kind,status=Status.PLANNED,date=today.plusDays(days),category=store.snapshot(false).categories.first().id,uncertain=kind==Kind.INCOME)
 @Test fun mandatorySpendableSettleAndSimulationNeverDoubleCount() {
  store.settings(store.snapshot().settings.copy(opening=120000,reserve=30000));store.save(planned(30000,2));store.save(planned(8000,5));store.save(planned(20000,3,Kind.INCOME))
  val before=store.snapshot(false);assertEquals(52000L,before.spendable)
  val sim=Simulator.calculate(before,60000,today.plusDays(30),includeIncome=true)
  assertEquals(-8000L,sim.margin);assertEquals(listOf(60000L,30000L,22000L,22000L),sim.points.map {it.cash});assertEquals(42000L,sim.scenario.last().cash)
  repeat(5){Simulator.calculate(before,it*10000L,today.plusDays(7))};assertEquals(before,store.snapshot(false))
  store.settle(before.pending.first().id);assertEquals(90000L,store.snapshot().balance);assertEquals(52000L,store.snapshot().spendable)
 }
 @Test fun totalAndEachCentRoundingWithPaidHistoryAndFutureIds() {
  assertEquals(listOf(3334L,3333L,3333L),Installments.split(10000,3));assertEquals(listOf(20000L,20000L,20000L),Installments.split(60000,3))
  val base=Series(title="Piano",cents=20000,kind=Kind.EXPENSE,category=planned(1).category,start=today,frequency=Frequency.MONTHLY,count=3,uncertain=false,amountMode=AmountMode.TOTAL,totalCents=60000,installments=Installments.split(60000,3));store.saveSeries(base)
  val first=store.snapshot();store.settle(first.pending.first().id);val paid=store.snapshot().actual.single()
  store.updateSeries(first.series.single().copy(totalCents=70000,installments=Installments.split(70000,3)))
  val next=store.snapshot();assertEquals(paid,next.actual.single());assertEquals(first.operations.map {it.id},next.operations.map {it.id});assertEquals(listOf(20000L,25000L,25000L),next.series.single().installments)
  val before=next;assertThrows(IllegalArgumentException::class.java){store.updateSeries(next.series.single().copy(totalCents=10000,installments=Installments.split(10000,3)))};assertEquals(before,store.snapshot())
  assertEquals(next,Backup.decode(Backup.encode(next)))
  store.saveSeries(base.copy(amountMode=AmountMode.EACH,totalCents=null,installments=emptyList(),cents=60000));assertEquals(180000L,store.snapshot().pending.filter {it.series!=first.series.single().id}.sumOf {it.cents})
 }
 @Test fun commandIdempotencyAndFailureRollback() {
  repeat(2){store.once("double"){store.save(planned(1200))}};assertEquals(1,store.snapshot().pending.size)
  assertThrows(IllegalStateException::class.java){store.once("retry"){store.save(planned(2000));error("interrotto")}};assertEquals(1,store.snapshot().pending.size)
  store.once("retry"){store.save(planned(2000))};assertEquals(2,store.snapshot().pending.size)
 }
 @Test fun rollingClockAndFixedOverdueRules() {
  val s=Settings(opening=10000,reserve=1000,horizon=today.minusDays(10),horizonMode=HorizonMode.FIXED)
  val l=store.snapshot(false).copy(settings=s,operations=listOf(planned(1000,-20),planned(2000,0),planned(3000,1)))
  assertEquals(3000L,l.commitments);assertEquals(6000L,l.spendable)
  val rolling=s.copy(horizonMode=HorizonMode.ROLLING,horizonDays=60)
  assertEquals(today.plusDays(60),rolling.effectiveHorizon(today));assertEquals(today.plusDays(61),rolling.effectiveHorizon(today.plusDays(1)))
  store.settings(s);assertEquals(s,store.snapshot(false).settings)
 }
 @Test fun quickOrderFavoriteFrequencyAndBackup() {
  val cats=store.snapshot().categories;store.save(planned(100).copy(status=Status.ACTUAL,category=cats[2].id));store.category(cats[1].id,cats[1].name,cats[1].color,cats[1].icon,true);store.quickOrder(listOf(cats[4].id))
  assertEquals(listOf(cats[4].id,cats[1].id,cats[2].id),QuickCategories.ordered(store.snapshot()).take(3).map {it.id});assertEquals(store.snapshot(),Backup.decode(Backup.encode(store.snapshot())))
 }
 @Test fun oldJsonRetainsEachAndFixedAndBadQuotasCannotReplace() {
  store.saveSeries(Series(title="Storica",cents=60000,kind=Kind.EXPENSE,category=planned(1).category,start=today,frequency=Frequency.MONTHLY,count=3,uncertain=false))
  val before=store.snapshot();val root=JSONObject(Backup.encode(before)).put("version",5);val plan=root.getJSONArray("series").getJSONObject(0);plan.remove("amountMode");plan.remove("totalCents");plan.remove("installments")
  val legacy=Backup.decode(root.toString());assertEquals(AmountMode.EACH,legacy.series.single().amountMode);assertEquals(HorizonMode.FIXED,legacy.settings.horizonMode);assertEquals(180000L,legacy.pending.sumOf {it.cents})
  plan.put("amountMode","TOTAL").put("totalCents",60000).put("installments",org.json.JSONArray(listOf(20000,20000)))
  root.put("version",6);assertThrows(IllegalArgumentException::class.java){store.replace(Backup.decode(root.toString()))};assertEquals(before,store.snapshot())
 }
}
