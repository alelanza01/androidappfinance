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
  db.execSQL("ALTER TABLE categories RENAME TO categories_new")
  db.execSQL("CREATE TABLE categories(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,color INTEGER NOT NULL)")
  db.execSQL("INSERT INTO categories SELECT id,name,color FROM categories_new");db.execSQL("DROP TABLE categories_new")
 }
 fun field(v:View,label:String)=all(v).filterIsInstance<EditText>().first {it.contentDescription?.toString()==label}
 fun set(v:View,label:String,value:String){if(label=="Numero rate" && all(v).filterIsInstance<EditText>().none {it.contentDescription?.toString()==label})click(v,"Altre opzioni");field(v,label).setText(value);idle()}
 fun ledger():Ledger {val s=Store(RuntimeEnvironment.getApplication());return try{s.snapshot()}finally{s.close()}}
 fun reset(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db")}
 fun withStore(block:(Store)->Unit){val s=Store(RuntimeEnvironment.getApplication());try{block(s)}finally{s.close()}}
}
