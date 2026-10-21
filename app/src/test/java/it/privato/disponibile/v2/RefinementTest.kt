package it.privato.disponibile.v2

import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import it.privato.disponibile.v2.UiSupport.reset
import it.privato.disponibile.v2.UiSupport.withStore
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.set
import it.privato.disponibile.v2.UiSupport.ledger
import it.privato.disponibile.v2.UiSupport.all
import it.privato.disponibile.v2.UiSupport.field
import it.privato.disponibile.v2.UiSupport.idle
import it.privato.disponibile.v2.UiSupport.dialog
import it.privato.disponibile.v2.UiSupport.legacyCategories
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class RefinementTest {
 @Before fun setup(){reset()}
 @Test fun expenseNeedsOnlyAmountAndFallbackSurvivesRenameBackupAndRestart() {
  withStore {it.settings(it.snapshot().settings.copy(opening=30000))}
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
  click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","50");click(a.window.decorView,"Salva")
  var l=ledger();assertEquals(25000L,l.balance);assertEquals(5000L,l.month(YearMonth.now(),Kind.EXPENSE));assertEquals("",l.actual.single().title);assertEquals("Altro",l.operationTitle(l.actual.single()))
  click(a.window.decorView,"Movimenti");assertTrue(all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()=="Altro"})
  val backup=Backup.decode(Backup.encode(l));withStore {it.replace(backup);it.category(l.actual.single().category,"Spese quotidiane",123)}
  l=ledger();assertEquals(25000L,l.balance);assertEquals("Spese quotidiane",l.operationTitle(l.actual.single()));c.pause().stop().destroy()
 }
 @Test fun customCategoryCanBeCreatedWithinExpenseWithoutLosingDraftAndFavorited() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get()
  click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","12,34");click(a.window.decorView,"Categoria");click(a.window.decorView,"Nuova categoria")
  set(a.window.decorView,"Nome categoria","Laboratorio");click(a.window.decorView,"Preferita");choose("Sì");click(a.window.decorView,"Icona");choose("Lavoro");c.recreate();a=c.get();click(a.window.decorView,"Salva")
  val cat=ledger().categories.first {it.name=="Laboratorio"};assertTrue(cat.favorite);assertEquals("work",cat.icon)
  click(a.window.decorView,"Preferite");click(a.window.decorView,"Laboratorio");assertEquals("12,34",field(a.window.decorView,"Importo in euro").text.toString());click(a.window.decorView,"Salva")
  val l=ledger();assertEquals(cat.id,l.actual.single().category);assertEquals(-1234L,l.balance);assertEquals(l,Backup.decode(Backup.encode(l)));c.pause().stop().destroy()
 }
 @Test fun pickerSearchRecentAndExistingCategoryEditingKeepAssociations() {
  withStore {val l=it.snapshot();it.save(Operation(title="",cents=1000,kind=Kind.EXPENSE,status=Status.ACTUAL,date=LocalDate.now(),category=l.categories.first().id))}
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
  click(a.window.decorView,"Registra");choose("Spesa");click(a.window.decorView,"Categoria");click(a.window.decorView,"Recenti");assertTrue(all(a.window.decorView).filterIsInstance<Button>().any {it.text.toString()=="Casa"})
  click(a.window.decorView,"Tutte");set(a.window.decorView,"Cerca categorie","svapo");assertEquals(1,all(a.window.decorView).filterIsInstance<Button>().count {it.compoundDrawables[1] is CategoryIcon});click(a.window.decorView,"Svapo");click(a.window.decorView,"Categoria")
  set(a.window.decorView,"Cerca categorie","casa");val casa=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()=="Casa"};casa.performLongClick();idle();set(a.window.decorView,"Nome categoria","Casa mia");click(a.window.decorView,"Salva");a.onBackPressed()
  val l=ledger();assertEquals("Casa mia",l.categoryName(l.actual.single().category));assertEquals(-1000L,l.balance);c.pause().stop().destroy()
 }
 @Test fun exitConfirmationHasOnlyTwoExplicitActionsAndNeverWritesDraft() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get();click(a.window.decorView,"Registra");choose("Spesa")
  a.onBackPressed();assertTrue(all(a.window.decorView).filterIsInstance<Button>().any {it.text.toString()=="Registra"})
  click(a.window.decorView,"Registra");choose("Spesa");set(a.window.decorView,"Importo in euro","20");c.recreate();a=c.get();a.onBackPressed()
  val buttons=all(dialog().window!!.decorView).filterIsInstance<Button>().map {it.text.toString()};assertEquals(listOf("Continua a modificare","Esci senza salvare"),buttons)
  choose("Continua a modificare");assertEquals("20",field(a.window.decorView,"Importo in euro").text.toString());a.onBackPressed();choose("Esci senza salvare");assertTrue(ledger().operations.isEmpty());c.pause().stop().destroy()
 }
 @Test fun numberOfInstallmentsHasBoundariesManualInputAndExactPerInstallmentAmounts() {
  val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Ricorrenza o rate");set(a.window.decorView,"Importo in euro","9999999,99");click(a.window.decorView,"Numero di rate")
  set(a.window.decorView,"Numero rate","1");assertFalse(all(a.window.decorView).first {it.contentDescription=="Riduci rate"}.isEnabled)
  click(a.window.decorView,"Aumenta rate");assertEquals("2",field(a.window.decorView,"Numero rate").text.toString());set(a.window.decorView,"Numero rate","120");assertFalse(all(a.window.decorView).first {it.contentDescription=="Aumenta rate"}.isEnabled)
  set(a.window.decorView,"Numero rate","121");click(a.window.decorView,"Salva");assertTrue(ledger().series.isEmpty());choose("Ho capito");set(a.window.decorView,"Numero rate","3");click(a.window.decorView,"Salva")
  val l=ledger();assertEquals(3,l.pending.size);assertTrue(l.pending.all {it.cents==Money.MAX});assertEquals(Money.MAX*3,l.pending.sumOf {it.cents});assertEquals(l,Backup.decode(Backup.encode(l)));c.pause().stop().destroy()
 }
 @Test fun migrationFromV3PreservesCustomCategoriesFinancialRecordsAndDeletionCutoffs() {
  withStore {s->val l=s.snapshot();s.settings(l.settings.copy(opening=30000));s.category(null,"Casa originale",456);val cat=s.snapshot().categories.last().id
   s.save(Operation(title="Vecchio movimento",cents=1000,kind=Kind.EXPENSE,status=Status.ACTUAL,date=LocalDate.now(),category=cat))
   s.saveSeries(Series(title="Vecchia serie",cents=500,kind=Kind.EXPENSE,category=cat,start=LocalDate.now(),frequency=Frequency.MONTHLY,count=5,uncertain=false));s.removeSeries(s.snapshot().series.single().id,2)
   s.saveAdvance(Advance(cents=5000,date=LocalDate.now(),description="Anticipo",debtor="DEMO"));s.saveRefund(Refund(advance=s.snapshot().advances.single().id,cents=2000,date=LocalDate.now()))
   val before=s.snapshot();val db=s.writableDatabase;db.delete("categories","id>6 AND id!=?",arrayOf(cat.toString()));legacyCategories(db);db.version=3
   s.close();val migrated=Store(RuntimeEnvironment.getApplication());val after=migrated.snapshot();assertEquals(before.balance,after.balance);assertEquals(before.spendable,after.spendable);assertEquals(before.operations,after.operations);assertEquals(before.series,after.series);assertEquals(before.advances,after.advances);assertEquals(before.refunds,after.refunds);assertEquals("Casa originale",after.categoryName(cat));assertEquals(4,migrated.readableDatabase.version);assertTrue(after.categories.size>=28);migrated.close()
  }
 }
 @Test fun monthlyInsightsCompareEqualElapsedPeriodsAndExcludeCredits() {
  val m=YearMonth.of(2026,10);val cat=Category(1,"Spesa",0)
  val ops=listOf(Operation(1,"",1000,Kind.EXPENSE,Status.ACTUAL,m.atDay(4),1),Operation(2,"Vecchia",2000,Kind.EXPENSE,Status.ACTUAL,m.minusMonths(1).atDay(4),1),Operation(3,"Più avanti",9000,Kind.EXPENSE,Status.ACTUAL,m.minusMonths(1).atDay(25),1))
  val l=Ledger(Settings(opening=30000),listOf(cat),ops,emptyList(),listOf(Advance(1,5000,m.atDay(3),"Anticipo","DEMO")),listOf(Refund(1,1,2000,m.atDay(5))))
  val i=Insights(l,m,m.atDay(12));assertEquals(1000L,i.total);assertEquals(2000L,i.previousTotal);assertEquals(-50.0,i.percent!!,0.001);assertEquals(-4000L,i.monthCash);assertEquals(0L,i.recurring)
  assertFalse(Insights(l.copy(operations=ops.take(1)),m,m.atDay(12)).comparable)
 }
}
