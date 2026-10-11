package it.privato.disponibile.v2

import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class AdvanceTest {
 private lateinit var store:Store
 private val today=LocalDate.now()
 @Before fun setup(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db");store=Store(RuntimeEnvironment.getApplication());store.settings(store.snapshot().settings.copy(opening=30000))}
 @After fun close(){store.close()}
 private fun advance(cents:Long=5000):Advance {store.saveAdvance(Advance(cents=cents,date=today.minusDays(5),description="Pulizie Casa A",debtor="Proprietario",due=today.minusDays(1)));return store.snapshot().advances.last()}
 @Test fun requiredExamplePartialAndFullRefundWithoutDoubleCounting() {
  val a=advance();var l=store.snapshot();assertEquals(25000L,l.balance);assertEquals(5000L,l.outstanding);assertEquals("Scaduto",l.advanceStatus(a))
  store.saveRefund(Refund(advance=a.id,cents=2000,date=today));l=store.snapshot();assertEquals(27000L,l.balance);assertEquals(3000L,l.outstanding)
  store.saveRefund(Refund(advance=a.id,cents=3000,date=today));l=store.snapshot();assertEquals(30000L,l.balance);assertEquals(0L,l.outstanding);assertEquals("Recuperato",l.advanceStatus(a))
  assertTrue(l.operations.isEmpty());assertEquals(0L,l.month(YearMonth.now(),Kind.INCOME));assertEquals(0L,l.month(YearMonth.now(),Kind.EXPENSE))
  assertEquals(l.balance,l.history().last().second);assertEquals(l.balance,l.projection(true).last().second)
 }
 @Test fun spendableNeverAddsOutstandingOrExpectedRefunds() {
  val a=advance();val c=store.snapshot().categories.first().id
  store.settings(store.snapshot().settings.copy(reserve=3000));store.save(Operation(title="Bolletta",cents=4000,kind=Kind.EXPENSE,status=Status.PLANNED,date=today.plusDays(1),category=c))
  assertEquals(18000L,store.snapshot().spendable);assertEquals(21000L,store.snapshot().projection(true).last().second)
  store.saveRefund(Refund(advance=a.id,cents=2000,date=today));assertEquals(20000L,store.snapshot().spendable)
 }
 @Test fun overRefundZeroNegativeFutureAndPreAdvanceDatesAreRejectedAtomically() {
  val a=advance();store.saveRefund(Refund(advance=a.id,cents=2000,date=today));val before=store.snapshot()
  listOf(3001L,0L,-1L).forEach {amount->assertThrows(IllegalArgumentException::class.java){store.saveRefund(Refund(advance=a.id,cents=amount,date=today))}}
  listOf(today.plusDays(1),a.date.minusDays(1)).forEach {date->assertThrows(IllegalArgumentException::class.java){store.saveRefund(Refund(advance=a.id,cents=1,date=date))}}
  assertEquals(before,store.snapshot())
 }
 @Test fun correctionsAndDeletionsRecalculateCashAndCredit() {
  val a=advance();store.saveRefund(Refund(advance=a.id,cents=2000,date=today));val r=store.snapshot().refunds.single()
  store.saveRefund(r.copy(cents=2500));assertEquals(27500L,store.snapshot().balance);assertEquals(2500L,store.snapshot().outstanding)
  store.saveAdvance(a.copy(cents=6000));assertEquals(26500L,store.snapshot().balance);assertEquals(3500L,store.snapshot().outstanding)
  assertThrows(IllegalArgumentException::class.java){store.saveAdvance(a.copy(cents=2000))}
  store.deleteRefund(r.id);assertEquals(24000L,store.snapshot().balance);assertEquals(6000L,store.snapshot().outstanding)
  store.saveRefund(Refund(advance=a.id,cents=3000,date=today));store.deleteAdvance(a.id);assertEquals(30000L,store.snapshot().balance);assertTrue(store.snapshot().refunds.isEmpty());assertTrue(store.snapshot().advances.isEmpty())
 }
 @Test fun refundEditsCannotCrossAdvanceOrOverpayOtherRefunds() {
  val a=advance();val b=advance(9000);store.saveRefund(Refund(advance=a.id,cents=3000,date=today));store.saveRefund(Refund(advance=a.id,cents=1000,date=today));val r=store.snapshot().refunds.first()
  val before=store.snapshot();assertThrows(IllegalArgumentException::class.java){store.saveRefund(r.copy(cents=4001))};assertThrows(IllegalArgumentException::class.java){store.saveRefund(r.copy(advance=b.id))};assertEquals(before,store.snapshot())
 }
 @Test fun advanceValidationAndDateChangesPreserveRefundChronology() {
  val a=advance();store.saveRefund(Refund(advance=a.id,cents=1000,date=today.minusDays(2)));val before=store.snapshot()
  listOf(a.copy(description=""),a.copy(debtor=""),a.copy(cents=0),a.copy(date=today.plusDays(1)),a.copy(date=today),a.copy(due=a.date.minusDays(1))).forEach {bad->assertThrows(IllegalArgumentException::class.java){store.saveAdvance(bad)}}
  assertEquals(before,store.snapshot())
 }
 @Test fun persistenceAndBackupRoundTripIncludeEveryDetail() {
  val a=advance();store.saveAdvance(a.copy(project="Casa A",cause="Pulizie",notes="Pagato in contanti"));store.saveRefund(Refund(advance=a.id,cents=2000,date=today,note="Primo acconto"))
  val before=store.snapshot();store.close();store=Store(RuntimeEnvironment.getApplication());assertEquals(before,store.snapshot())
  store.replace(Backup.decode(Backup.encode(before)));assertEquals(before,store.snapshot())
 }
 @Test fun oldV2BackupLoadsAndInvalidNewBackupsDoNotReplaceData() {
  advance();val before=store.snapshot();val root=org.json.JSONObject(Backup.encode(before));root.put("version",2).remove("advances");root.remove("refunds")
  val old=Backup.decode(root.toString());assertTrue(old.advances.isEmpty());assertEquals(30000L,old.balance)
  val bad=before.copy(refunds=listOf(Refund(1,before.advances.single().id,6000,today)))
  assertThrows(IllegalArgumentException::class.java){store.replace(bad)};assertEquals(before,store.snapshot())
  val orphan=before.copy(refunds=listOf(Refund(1,999,1000,today)))
  assertThrows(IllegalStateException::class.java){store.replace(orphan)};assertEquals(before,store.snapshot())
 }
 @Test fun migrationFromRealV2SchemaRetainsSettingsOperationsCategoriesAndSeries() {
  val c=store.snapshot().categories.first().id;store.save(Operation(title="Spesa precedente",cents=700,kind=Kind.EXPENSE,status=Status.ACTUAL,date=today,category=c))
  store.saveSeries(Series(title="Rate precedenti",cents=100,kind=Kind.EXPENSE,category=c,start=today,frequency=Frequency.MONTHLY,count=2,uncertain=false))
  val before=store.snapshot();val db=store.writableDatabase;db.execSQL("DROP TABLE refunds");db.execSQL("DROP TABLE advances");db.version=1;store.close()
  store=Store(RuntimeEnvironment.getApplication());assertEquals(2,store.readableDatabase.version);assertEquals(before,store.snapshot());val a=advance();store.saveRefund(Refund(advance=a.id,cents=2000,date=today));assertEquals(before.balance-3000,store.snapshot().balance)
 }
 @Test fun duplicateAndFractionalAdvanceBackupIsRejected() {
  advance();val root=org.json.JSONObject(Backup.encode(store.snapshot()));val a=root.getJSONArray("advances");a.put(a.getJSONObject(0));assertThrows(IllegalArgumentException::class.java){Backup.decode(root.toString())}
  val fractional=org.json.JSONObject(Backup.encode(store.snapshot()));fractional.getJSONArray("advances").getJSONObject(0).put("cents",50.5);assertThrows(IllegalArgumentException::class.java){Backup.decode(fractional.toString())}
 }
 @Test fun historyFollowsActualAdvanceAndRefundDates() {
  val a=advance();store.saveRefund(Refund(advance=a.id,cents=2000,date=today.minusDays(2)))
  val history=store.snapshot().history();assertEquals(30000L,history.first().second);assertEquals(25000L,history.first {it.first==a.date}.second);assertEquals(27000L,history.last().second)
 }
}
