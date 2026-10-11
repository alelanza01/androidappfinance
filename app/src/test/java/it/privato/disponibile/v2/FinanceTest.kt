package it.privato.disponibile.v2
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.YearMonth

class FinanceTest {
 private val today=LocalDate.now()
 private val c=listOf(Category(1,"Casa",0))
 private fun op(id:Long,amount:Long,kind:Kind,status:Status,date:LocalDate=today)=Operation(id,"Demo",amount,kind,status,date,1)
 @Test fun exactCentsAndLocaleInput() {assertEquals(12345L,Money.parse("123,45"));assertEquals(10L,Money.parse("0.10"));assertEquals(-1L,Money.parse("-0,01",true));assertEquals("1,25",Money.input(125))}
 @Test fun malformedAndExcessPrecisionRejected() {listOf("","1,001","1.000,00","-3","NaN","99999999").forEach {assertThrows(IllegalArgumentException::class.java){Money.parse(it)}}}
 @Test fun futureIncomeDoesNotBecomeCash() {
  val l=Ledger(Settings(100000,20000,today.plusDays(30)),c,listOf(op(1,30000,Kind.EXPENSE,Status.PLANNED),op(2,90000,Kind.INCOME,Status.PLANNED,today.plusDays(1))),emptyList())
  assertEquals(100000L,l.balance);assertEquals(50000L,l.spendable);assertEquals(90000L,l.expectedIncome);assertEquals(30000L,l.simulated(20000))
 }
 @Test fun paidExpensesAreNotCommittedAgainAndBoundaryIsIncluded() {
  val date=today.plusDays(30)
  val l=Ledger(Settings(100000,0,date),c,listOf(op(1,10000,Kind.EXPENSE,Status.ACTUAL),op(2,20000,Kind.EXPENSE,Status.PLANNED,date),op(3,5000,Kind.EXPENSE,Status.PLANNED,today.minusDays(5)),op(4,40000,Kind.EXPENSE,Status.PLANNED,date.plusDays(1))),emptyList())
  assertEquals(90000L,l.balance);assertEquals(25000L,l.commitments);assertEquals(65000L,l.spendable)
 }
 @Test fun negativeBalanceAndWideTotals() {
  val l=Ledger(Settings(-100,200),c,listOf(op(1,Money.MAX,Kind.EXPENSE,Status.PLANNED),op(2,Money.MAX,Kind.EXPENSE,Status.PLANNED),op(3,Money.MAX,Kind.EXPENSE,Status.PLANNED)),emptyList())
  assertEquals(2999999997L,l.commitments);assertEquals(-2999999997L-300,l.spendable)
 }
 @Test fun monthlyAnchorDoesNotDriftAtEndOfMonth() {
  val s=Series(title="Rata",cents=100,kind=Kind.EXPENSE,category=1,start=LocalDate.of(2024,1,31),frequency=Frequency.MONTHLY,count=3,uncertain=false)
  assertEquals(LocalDate.of(2024,2,29),s.date(1));assertEquals(LocalDate.of(2024,3,31),s.date(2))
 }
 @Test fun projectionsAreSeparatedAndOverdueIsAppliedToday() {
  val l=Ledger(Settings(10000,0,today.plusDays(10)),c,listOf(op(1,5000,Kind.EXPENSE,Status.PLANNED,today.minusDays(3)),op(2,2000,Kind.INCOME,Status.PLANNED,today.plusDays(5))),emptyList())
  assertEquals(5000L,l.projection(false).last().second);assertEquals(7000L,l.projection(true).last().second);assertTrue(l.projection(false).all {it.first>=today})
 }
 @Test fun monthlyReportsExcludePlannedAndReconciliation() {
  val l=Ledger(Settings(),c,listOf(op(1,1000,Kind.INCOME,Status.ACTUAL),op(2,2000,Kind.ADJUSTMENT,Status.ACTUAL),op(3,3000,Kind.INCOME,Status.PLANNED)),emptyList())
  assertEquals(1000L,l.month(YearMonth.now(),Kind.INCOME));assertEquals(3000L,l.balance)
 }
}
