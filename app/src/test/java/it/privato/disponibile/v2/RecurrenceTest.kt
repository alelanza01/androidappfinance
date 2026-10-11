package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RecurrenceTest {
 private lateinit var store:Store
 private val today=LocalDate.now()
 @Before fun setup(){UiSupport.reset();store=Store(RuntimeEnvironment.getApplication());store.settings(store.snapshot().settings.copy(opening=30000))}
 @After fun close(){store.close()}
 private fun series():Ledger {store.saveSeries(Series(title="Affitto",cents=5000,kind=Kind.EXPENSE,category=store.snapshot().categories.first().id,start=today.minusMonths(2),frequency=Frequency.MONTHLY,count=0,uncertain=false));return store.snapshot()}
 private fun reopen(){store.close();store=Store(RuntimeEnvironment.getApplication())}
 @Test fun ordinaryDeletionRemovesDatabaseBackupReportsAndNeverReturnsAfterRestart() {
  val c=store.snapshot().categories.first().id;store.save(Operation(title="Errata",cents=5000,kind=Kind.EXPENSE,status=Status.ACTUAL,date=today,category=c));val id=store.snapshot().operations.single().id
  store.delete(id);reopen();val l=store.snapshot();assertTrue(l.operations.isEmpty());assertEquals(30000L,l.balance);assertEquals(30000L,l.spendable);assertEquals(0L,l.month(YearMonth.now(),Kind.EXPENSE));assertEquals(30000L,l.history().last().second);assertTrue(Backup.decode(Backup.encode(l)).operations.isEmpty())
 }
 @Test fun singleOccurrenceSuppressionSurvivesExpansionRestartAndBackup() {
  val l=series();val removed=l.operations[1];store.delete(removed.id);reopen();store.settings(store.snapshot().settings.copy(horizon=today.plusYears(3)))
  repeat(2){assertTrue(store.snapshot().pending.none {it.index==removed.index})}
  store.replace(Backup.decode(Backup.encode(store.snapshot())));assertTrue(store.snapshot().pending.none {it.index==removed.index});assertTrue(store.snapshot().pending.any {it.index==2})
 }
 @Test fun cutoffRemovesCurrentAndFutureAndStopsDefinitionAcrossBackupAndLongerHorizon() {
  val l=series();store.settle(l.operations[0].id);store.settle(l.operations[3].id);val before=store.snapshot().balance;store.removeSeries(l.series.single().id,2)
  var next=store.snapshot();assertTrue(next.pending.all {it.index!!<2});assertEquals(2,next.actual.size);assertEquals(before,next.balance);assertEquals(2,next.series.single().endIndex)
  reopen();store.settings(store.snapshot().settings.copy(horizon=today.plusYears(5)));next=store.snapshot();store.replace(Backup.decode(Backup.encode(next)));assertTrue(store.snapshot().pending.all {it.index!!<2});assertEquals(before,store.snapshot().balance)
 }
 @Test fun entireSeriesCancellationPreservesActualButCannotRegenerate() {
  val l=series();store.settle(l.operations.first().id);store.removeSeries(l.series.single().id);val after=store.snapshot();assertEquals(25000L,after.balance);assertEquals(1,after.actual.size);assertTrue(after.pending.isEmpty());assertFalse(after.series.single().active)
  reopen();store.settings(store.snapshot().settings.copy(horizon=today.plusYears(10)));store.replace(Backup.decode(Backup.encode(store.snapshot())));assertTrue(store.snapshot().pending.isEmpty());assertEquals(25000L,store.snapshot().balance)
 }
 @Test fun cancellingAnUnpaidSeriesRemovesDefinitionAndAllRowsFromFutureBackups() {
  val l=series();store.removeSeries(l.series.single().id);reopen();assertTrue(store.snapshot().series.isEmpty());assertTrue(store.snapshot().operations.isEmpty())
  val restored=Backup.decode(Backup.encode(store.snapshot()));store.replace(restored);assertTrue(store.snapshot().pending.isEmpty());assertEquals(30000L,store.snapshot().balance)
 }
 @Test fun explicitHistoricalRemovalReversesCashAndReportsAndPersists() {
  val l=series();store.settle(l.operations[0].id);store.settle(l.operations[1].id);store.removeSeries(l.series.single().id,removeActual=true);reopen();assertTrue(store.snapshot().operations.isEmpty());assertTrue(store.snapshot().series.isEmpty());assertEquals(30000L,store.snapshot().balance);assertEquals(0L,store.snapshot().month(YearMonth.now(),Kind.EXPENSE))
 }
 @Test fun updatingSeriesChangesFutureOnlyAndDoesNotResurrectDeletedOccurrence() {
  val l=series();store.settle(l.operations[0].id);store.delete(l.operations[1].id);store.updateSeries(l.series.single().copy(title="Affitto nuovo",cents=7000));var next=store.snapshot();assertEquals(5000L,next.actual.single().cents);assertTrue(next.pending.all {it.cents==7000L});assertTrue(next.pending.none {it.index==1})
  store.removeSeries(l.series.single().id,4);store.updateSeries(store.snapshot().series.single().copy(cents=8000));next=store.snapshot();assertTrue(next.pending.all {it.index!!<4});store.replace(Backup.decode(Backup.encode(next)));assertEquals(next,store.snapshot())
 }
 @Test fun shorteningInstallmentsRetainsAlreadyPaidHistoryAndImportAcceptsIt() {
  store.saveSeries(Series(title="Rate",cents=1000,kind=Kind.EXPENSE,category=store.snapshot().categories.first().id,start=today.minusMonths(3),frequency=Frequency.MONTHLY,count=4,uncertain=false));var l=store.snapshot();store.settle(l.operations.last().id);store.updateSeries(l.series.single().copy(count=2));l=store.snapshot();assertEquals(1,l.actual.size);assertEquals(2,l.pending.size);store.replace(Backup.decode(Backup.encode(l)));assertEquals(29000L,store.snapshot().balance)
 }
 @Test fun backgroundMaterializationCannotResurrectCancelledDefinitions() {
  val l=series();val start=java.util.concurrent.CountDownLatch(1)
  val pool=java.util.concurrent.Executors.newSingleThreadExecutor()
  try {
   val background=pool.submit {start.countDown();repeat(20){store.snapshot()}}
   assertTrue(start.await(5,java.util.concurrent.TimeUnit.SECONDS));store.removeSeries(l.series.single().id)
   background.get(10,java.util.concurrent.TimeUnit.SECONDS)
   assertTrue(store.snapshot().series.isEmpty());assertTrue(store.snapshot().operations.isEmpty())
  } finally {pool.shutdownNow()}
 }
 @Test fun invalidSeriesEditIsAtomic() {
  val l=series();assertThrows(IllegalArgumentException::class.java){store.updateSeries(l.series.single().copy(cents=-1))};assertEquals(l,store.snapshot())
 }
 @Test fun migrationFromV21PreservesPaidPendingCreditsRefundsSettingsAndIdempotency() {
  val l=series();store.settle(l.operations[0].id);store.saveAdvance(Advance(cents=5000,date=today,description="Pulizie",debtor="Marco"));store.saveRefund(Refund(advance=store.snapshot().advances.single().id,cents=2000,date=today));val before=store.snapshot();val db=store.writableDatabase
  db.execSQL("ALTER TABLE series RENAME TO series_new");db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,cents INTEGER NOT NULL,kind TEXT NOT NULL,category INTEGER NOT NULL,start TEXT NOT NULL,frequency TEXT NOT NULL,count INTEGER NOT NULL,uncertain INTEGER NOT NULL,active INTEGER NOT NULL)");db.execSQL("INSERT INTO series SELECT id,title,cents,kind,category,start,frequency,count,uncertain,active FROM series_new");db.execSQL("DROP TABLE series_new");db.version=2;reopen()
  assertEquals(3,store.readableDatabase.version);assertEquals(before,store.snapshot());reopen();assertEquals(before,store.snapshot())
 }
 @Test fun V2AndV21BackupsRemainCompatibleAndOldDeletedOrdinaryRowsArePurged() {
  val l=series();val root=org.json.JSONObject(Backup.encode(l));root.put("version",3);for(i in 0 until root.getJSONArray("series").length())root.getJSONArray("series").getJSONObject(i).remove("endIndex")
  val category=l.categories.first().id;root.getJSONArray("operations").put(org.json.JSONObject().put("id",9999).put("title","Eliminata").put("cents",1000).put("kind","EXPENSE").put("status","DELETED").put("date",today.toString()).put("category",category).put("uncertain",false).put("series",org.json.JSONObject.NULL).put("index",org.json.JSONObject.NULL))
  store.replace(Backup.decode(root.toString()));assertTrue(store.snapshot().operations.none {it.id==9999L});assertTrue(store.snapshot().pending.isNotEmpty())
  root.put("version",2).remove("advances");root.remove("refunds");store.replace(Backup.decode(root.toString()));assertTrue(store.snapshot().pending.isNotEmpty())
 }
}
