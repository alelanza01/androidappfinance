package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class Migration34Test {
 private val context get()=RuntimeEnvironment.getApplication()
 private fun fixture(version:Int):Ledger {
  UiSupport.reset();val s=Store(context);s.settings(s.snapshot().settings.copy(opening=30000,horizon=LocalDate.now().plusDays(17)))
  s.category(null,"Personale storica",123,"work",true);val cat=s.snapshot().categories.last().id
  s.saveSeries(Series(title="Rate storiche",cents=1000,kind=Kind.EXPENSE,category=cat,start=LocalDate.now(),frequency=Frequency.MONTHLY,count=5,uncertain=false))
  val first=s.snapshot();s.settle(first.pending[0].id);s.delete(first.pending[1].id)
  if(version>=3)s.removeSeries(first.series.single().id,4)
  if(version>=2){s.saveAdvance(Advance(cents=5000,date=LocalDate.now(),description="Acquisti",debtor="Rossi"));s.saveRefund(Refund(advance=s.snapshot().advances.single().id,cents=2000,date=LocalDate.now()))}
  val before=s.snapshot(false);val db=s.writableDatabase
  if(version<4)UiSupport.legacyCategories(db)else {
   UiSupport.legacyV4(db)
   db.execSQL("ALTER TABLE categories RENAME TO categories_v5")
   db.execSQL("CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,color INTEGER NOT NULL,icon TEXT NOT NULL DEFAULT 'other',favorite INTEGER NOT NULL DEFAULT 0)")
   db.execSQL("INSERT INTO categories SELECT id,name,color,icon,favorite FROM categories_v5");db.execSQL("DROP TABLE categories_v5")
  }
  if(version<3) {
   db.execSQL("ALTER TABLE series RENAME TO series_v3")
   db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,cents INTEGER NOT NULL,kind TEXT NOT NULL,category INTEGER NOT NULL,start TEXT NOT NULL,frequency TEXT NOT NULL,count INTEGER NOT NULL,uncertain INTEGER NOT NULL,active INTEGER NOT NULL)")
   db.execSQL("INSERT INTO series SELECT id,title,cents,kind,category,start,frequency,count,uncertain,active FROM series_v3");db.execSQL("DROP TABLE series_v3")
  }
  if(version<2){db.execSQL("DROP TABLE refunds");db.execSQL("DROP TABLE advances")}
  db.version=version;s.close();return before
 }
 @Test fun allFourRealSchemasUpgradeWithoutLossOrReinterpretation() {
  (1..4).forEach {v->val before=fixture(v);val s=Store(context);val after=s.snapshot(false)
   assertEquals(5,s.readableDatabase.version);assertEquals(before.operations,after.operations);assertEquals(before.series,after.series);assertEquals(before.advances,after.advances);assertEquals(before.refunds,after.refunds)
   assertEquals(before.balance,after.balance);assertEquals(before.spendable,after.spendable);assertEquals(before.settings.horizon,after.settings.horizon);assertEquals(HorizonMode.FIXED,after.settings.horizonMode);assertTrue(after.settings.onboardingCompleted)
   assertEquals(before.categories.map {it.id},after.categories.map {it.id});assertEquals(after,Backup.decode(Backup.encode(after)));s.close()
  }
 }
 @Test fun migrationFailureRollsBackSchemaAndNeverResetsData() {
  val before=fixture(4);val file=context.getDatabasePath("finance-v2.db")
  SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE).use {it.execSQL("CREATE TABLE commands(token TEXT PRIMARY KEY)")}
  val bad=Store(context);assertThrows(Exception::class.java){bad.readableDatabase};bad.close()
  SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE).use {db->
   assertEquals(4,db.version);val columns=db.rawQuery("PRAGMA table_info(settings)",null).use {c->buildList {while(c.moveToNext())add(c.getString(1))}};assertFalse("horizonMode" in columns);db.execSQL("DROP TABLE commands")
  }
  val restored=Store(context);try {assertEquals(before.operations,restored.snapshot(false).operations);assertEquals(before.balance,restored.snapshot(false).balance)}finally{restored.close()}
 }
}
