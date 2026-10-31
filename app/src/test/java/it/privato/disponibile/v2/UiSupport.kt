package it.privato.disponibile.v2
import android.view.*
import android.widget.*
import android.app.Dialog
import org.robolectric.*
import org.robolectric.shadows.ShadowDialog
import org.junit.Assert.*
import java.time.Duration

object UiSupport {
 fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {all(v.getChildAt(it))}else emptyList()
 fun idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(250))}
 fun click(v:View,name:String) {
  val revised=mapOf("Regole del saldo" to "Riserva e disponibilità","Avvisi" to "Notifiche","Filtri crediti" to "Filtri anticipi","Nuovo anticipo" to "Registra anticipo","Inizia a usare l'app" to "Vai alla Home")
  if(name in revised && all(v).none {it is TextView && it.text.toString()==name}){click(v,revised.getValue(name));return}
  val register=mapOf("Spesa" to "Registra spesa","Entrata" to "Registra entrata","Spesa prevista" to "Programma pagamento","Ricorrenza o rate" to "Rate e ricorrenze","Anticipo" to "Registra anticipo")
  if(name in register && all(v).any {it is TextView && it.text.toString()=="Cosa vuoi registrare?"}){click(v,register.getValue(name));return}
  if(name=="Lista"){click(v,"Elenco");return}
  if(name=="Spesa veloce"){click(v,"Aggiungi");choose("Spesa");return}
  if(name=="Dettaglio del calcolo"){all(v).filterIsInstance<SpendableRing>().first().performClick();idle();return}
  if(name=="Crediti" && all(v).any {it is Button && it.text.toString()=="Anticipi da recuperare"}){click(v,"Anticipi da recuperare");return}
  if(name in listOf("Recenti","Preferite","Tutte") && all(v).none {it is Button && it.text.toString()==name} && all(v).any {it.contentDescription=="Filtra categorie"}){click(v,"Filtra categorie");choose(name);return}
  if(name in listOf("Spese","Liquidità","Mesi") && all(v).none {it is Button && it.text.toString()==name} && all(v).any {it is Button && it.text.toString()=="Approfondimenti"}){click(v,"Approfondimenti");choose(name);return}

  if(name=="Registra" && all(v).none {it is Button && it.text.toString()=="Registra"}){click(v,"Aggiungi");return}
  if(name=="Home" && all(v).none {it is Button && it.text.toString()=="Home"}){click(v,"Indietro");return}
  if(name=="Crediti" && all(v).none {it is Button && it.text.toString()=="Crediti"}){click(v,"Home");click(v,"Crediti");return}
  if(name in listOf("Movimenti","Analisi","Impegni") && all(v).none {it is Button && it.text.toString()==name}){click(v,"Home");click(v,name);return}
  if(name=="Salva" && all(v).none {it is Button && it.text.toString()=="Salva"}){val label=all(v).filterIsInstance<Button>().first {it.text.toString().startsWith("Salva ")}.text.toString();click(v,label);return}

  var target=all(v).firstOrNull {it is Button && it.text.toString()==name} ?: all(v).firstOrNull {it.contentDescription?.toString()==name} ?: all(v).firstOrNull {it is TextView && it.text.toString()==name} ?: error("Missing UI action: $name")
  while(!target.isClickable && target.parent is View)target=target.parent as View
  assertTrue("Not clickable: $name",target.isClickable);target.performClick();idle()
 }
 fun dialog():Dialog {idle();return ShadowDialog.getLatestDialog()}
 fun choose(name:String)=click(dialog().window!!.decorView,name)
 fun accept(){val d=dialog();if(!d.isShowing)return;val b=all(d.window!!.decorView).firstOrNull {it.tag=="confirm-action"} ?: error("Missing contextual decision");b.performClick();idle()}
 fun legacyCategories(db:android.database.sqlite.SQLiteDatabase){
  legacyV4(db)

  db.execSQL("ALTER TABLE categories RENAME TO categories_new")
  db.execSQL("CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,color INTEGER NOT NULL)")
  db.execSQL("INSERT INTO categories SELECT id,name,color FROM categories_new");db.execSQL("DROP TABLE categories_new")
 }
 /** Fixture downgrade recreates the actual old schema, rather than only changing user_version. */
 fun legacyV4(db:android.database.sqlite.SQLiteDatabase){
  fun columns(table:String)=db.rawQuery("PRAGMA table_info($table)",null).use {c->buildList {while(c.moveToNext())add(c.getString(1))}}
  if("horizonMode" in columns("settings")) {
   db.execSQL("ALTER TABLE settings RENAME TO settings_new")
   db.execSQL("CREATE TABLE settings(id INTEGER PRIMARY KEY CHECK(id=1),opening INTEGER NOT NULL,reserve INTEGER NOT NULL,horizon TEXT NOT NULL,theme TEXT NOT NULL,reminders INTEGER NOT NULL)")
   db.execSQL("INSERT INTO settings SELECT id,opening,reserve,horizon,theme,reminders FROM settings_new");db.execSQL("DROP TABLE settings_new")
  }
  if("amountMode" in columns("series")) {
   db.execSQL("ALTER TABLE series RENAME TO series_v5")
   db.execSQL("CREATE TABLE series(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,cents INTEGER NOT NULL,kind TEXT NOT NULL,category INTEGER NOT NULL,start TEXT NOT NULL,frequency TEXT NOT NULL,count INTEGER NOT NULL,uncertain INTEGER NOT NULL,active INTEGER NOT NULL,endIndex INTEGER)")
   db.execSQL("INSERT INTO series SELECT id,title,cents,kind,category,start,frequency,count,uncertain,active,endIndex FROM series_v5");db.execSQL("DROP TABLE series_v5")
  }
  db.execSQL("DROP TABLE IF EXISTS installments");db.execSQL("DROP TABLE IF EXISTS commands")
 }
 fun field(v:View,label:String)=all(v).filterIsInstance<EditText>().first {it.contentDescription?.toString()==if(label=="Cerca crediti")"Cerca anticipi"else label}
 fun set(v:View,label:String,value:String){if(label=="Descrizione" && all(v).filterIsInstance<EditText>().none {it.contentDescription?.toString()==label} && all(v).any {it is Button && it.text.toString()=="Aggiungi pagamento"})click(v,"Aggiungi pagamento");if(label=="Numero rate" && all(v).filterIsInstance<EditText>().none {it.contentDescription?.toString()==label})click(v,"Altre opzioni");field(v,label).setText(value);idle()}
 fun ledger():Ledger {val s=Store(RuntimeEnvironment.getApplication());return try{s.snapshot()}finally{s.close()}}
 fun reset(){val context=RuntimeEnvironment.getApplication();context.deleteDatabase("finance-v2.db");context.getSharedPreferences("onboarding-v34",0).edit().clear().commit();val s=Store(context);s.settings(s.snapshot(false).settings.copy(onboardingCompleted=true));s.close()}
 fun withStore(block:(Store)->Unit){val s=Store(RuntimeEnvironment.getApplication());try{block(s)}finally{s.close()}}
}
