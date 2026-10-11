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
  var target=all(v).firstOrNull {it is Button && it.text.toString()==name} ?: all(v).firstOrNull {it.contentDescription?.toString()==name} ?: all(v).firstOrNull {it is TextView && it.text.toString()==name} ?: error("Missing UI action: $name")
  while(!target.isClickable && target.parent is View)target=target.parent as View
  assertTrue("Not clickable: $name",target.isClickable);target.performClick();idle()
 }
 fun dialog():Dialog {idle();return ShadowDialog.getLatestDialog()}
 fun choose(name:String)=click(dialog().window!!.decorView,name)
 fun field(v:View,label:String)=all(v).filterIsInstance<EditText>().first {it.contentDescription?.toString()==label}
 fun set(v:View,label:String,value:String){field(v,label).setText(value);idle()}
 fun ledger():Ledger {val s=Store(RuntimeEnvironment.getApplication());return try{s.snapshot()}finally{s.close()}}
 fun reset(){RuntimeEnvironment.getApplication().deleteDatabase("finance-v2.db")}
 fun withStore(block:(Store)->Unit){val s=Store(RuntimeEnvironment.getApplication());try{block(s)}finally{s.close()}}
}
