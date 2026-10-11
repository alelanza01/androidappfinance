package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class StoreTest {
 private lateinit var store:Store
 private val today=LocalDate.now()
 @Before fun init(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db");store=Store(RuntimeEnvironment.getApplication())}
 @After fun done(){store.close()}
 private fun add(kind:Kind=Kind.EXPENSE,status:Status=Status.PLANNED,cents:Long=1000):Long {
  store.save(Operation(title="Demo",cents=cents,kind=kind,status=status,date=today,category=store.snapshot().categories.first().id));return store.snapshot().operations.last().id
 }
 @Test fun persistenceAndCrudRecalculateExactly() {
  store.settings(store.snapshot().settings.copy(opening=10000));val id=add(status=Status.ACTUAL)
  val o=store.snapshot().operations.single();store.save(o.copy(cents=2000));store.close();store=Store(RuntimeEnvironment.getApplication())
  assertEquals(8000L,store.snapshot().balance);store.delete(id);assertEquals(10000L,store.snapshot().balance)
 }
 @Test fun settlingOnceMovesPredictionIntoActualWithoutDoubleCounting() {
  store.settings(store.snapshot().settings.copy(opening=10000));val id=add()
  assertEquals(9000L,store.snapshot().spendable);store.settle(id);assertEquals(9000L,store.snapshot().balance);assertEquals(9000L,store.snapshot().spendable)
  assertThrows(IllegalStateException::class.java){store.settle(id)};assertEquals(9000L,store.snapshot().balance)
 }
 @Test fun uncertainIncomeIsOnlyCashAfterExplicitReceipt() {
  val c=store.snapshot().categories.first().id;store.save(Operation(title="Incarico",cents=12000,kind=Kind.INCOME,status=Status.PLANNED,date=today,category=c,uncertain=true));val id=store.snapshot().operations.single().id
  assertEquals(0L,store.snapshot().balance);store.settle(id);assertEquals(12000L,store.snapshot().balance);assertFalse(store.snapshot().operations.single().uncertain)
 }
 @Test fun recurringMaterializationIsIdempotentIncludingEditsAndDeletes() {
  val c=store.snapshot().categories.first().id
  store.saveSeries(Series(title="Affitto",cents=50000,kind=Kind.EXPENSE,category=c,start=today,frequency=Frequency.MONTHLY,count=0,uncertain=false))
  val l=store.snapshot();val n=l.operations.size;val first=l.operations.first();store.save(first.copy(date=today.plusDays(2),cents=40000));store.delete(l.operations[1].id)
  repeat(3){assertEquals(n,store.snapshot().operations.size)}
  assertEquals(Status.DELETED,store.snapshot().operations.first {it.id==l.operations[1].id}.status)
  assertEquals(40000L,store.snapshot().operations.first {it.id==first.id}.cents)
 }
 @Test fun installmentsHaveAnEndAndCancellationPreservesActual() {
  store.saveSeries(Series(title="Rate",cents=1000,kind=Kind.EXPENSE,category=store.snapshot().categories.first().id,start=today,frequency=Frequency.MONTHLY,count=3,uncertain=false))
  val l=store.snapshot();assertEquals(3,l.operations.size);store.settle(l.operations.first().id);store.cancelSeries(l.series.single().id)
  assertEquals(1,store.snapshot().actual.size);assertEquals(0,store.snapshot().pending.size);assertEquals(-1000L,store.snapshot().balance)
 }
 @Test fun categoryRenameAndDeleteProtection() {
  val id=store.snapshot().categories.first().id;add();store.category(id,"Casa nuova",123)
  assertEquals("Casa nuova",store.snapshot().categoryName(id));assertThrows(IllegalArgumentException::class.java){store.deleteCategory(id)}
  store.category(null,"Test",321);val new=store.snapshot().categories.last().id;store.deleteCategory(new);assertTrue(store.snapshot().categories.none {it.id==new})
 }
 @Test fun reconcileDoesNotPretendToBeIncome() {
  store.reconcile(12345);assertEquals(12345L,store.snapshot().balance);assertEquals(Kind.ADJUSTMENT,store.snapshot().operations.single().kind)
  store.reconcile(12345);assertEquals(1,store.snapshot().operations.size)
 }
 @Test fun futureActualAndInvalidSettingsAreRejectedWithoutMutation() {
  val c=store.snapshot().categories.first().id
  assertThrows(IllegalArgumentException::class.java){store.save(Operation(title="Bad",cents=100,kind=Kind.EXPENSE,status=Status.ACTUAL,date=today.plusDays(1),category=c))}
  assertThrows(IllegalArgumentException::class.java){store.settings(store.snapshot().settings.copy(reserve=-1))};assertTrue(store.snapshot().operations.isEmpty())
 }
 @Test fun backupRoundTripAndDeletedIrregularIncome() {
  val c=store.snapshot().categories.first().id;store.save(Operation(title="Incarico",cents=1000,kind=Kind.INCOME,status=Status.PLANNED,date=today,category=c,uncertain=true));store.delete(store.snapshot().operations.single().id)
  val original=store.snapshot();val saved=Backup.encode(original);val parsed=Backup.decode(saved);store.replace(parsed)
  assertEquals(original,store.snapshot());assertEquals(Status.DELETED,parsed.operations.single().status)
 }
 @Test fun malformedAndDuplicateBackupDoNotChangeExistingData() {
  add();val before=store.snapshot();val json=org.json.JSONObject(Backup.encode(before));val a=json.getJSONArray("operations");a.put(a.getJSONObject(0))
  assertThrows(IllegalArgumentException::class.java){Backup.decode(json.toString())}
  assertThrows(Exception::class.java){Backup.decode("not json")};assertEquals(before,store.snapshot())
 }
 @Test fun failedReplacementRollsBackBeforeAnyDeletion() {
  add();val before=store.snapshot();val bad=before.copy(operations=before.operations.map {it.copy(category=999)})
  assertThrows(IllegalArgumentException::class.java){store.replace(bad)};assertEquals(before,store.snapshot())
 }
 @Test fun fractionalBackupAmountsAreRejected() {
  add();val root=org.json.JSONObject(Backup.encode(store.snapshot()));root.getJSONArray("operations").getJSONObject(0).put("cents",100.5)
  assertThrows(IllegalArgumentException::class.java){Backup.decode(root.toString())}
 }
 @Test fun oldRecurringBackupRemainsImportableWithoutDuplicatingExistingOccurrences() {
  val c=store.snapshot().categories.first().id
  val old=Series(1,"Storica",1000,Kind.EXPENSE,c,today.minusYears(3),Frequency.MONTHLY,0,false)
  val imported=store.snapshot().copy(series=listOf(old))
  store.replace(Backup.decode(Backup.encode(imported)));val count=store.snapshot().operations.size
  assertTrue(count>36);assertEquals(count,store.snapshot().operations.size)
 }
}
