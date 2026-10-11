package it.privato.disponibile.v2
import android.view.*
import android.widget.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import it.privato.disponibile.v2.UiSupport.click
import it.privato.disponibile.v2.UiSupport.choose
import it.privato.disponibile.v2.UiSupport.all

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResponsiveTest {
 @Test fun smallStandardLargeAndLargeFontScreensRemainNavigable() {
  listOf(Triple(320,640,1f),Triple(360,800,1f),Triple(411,891,1.5f),Triple(320,640,2f)).forEach {(w,h,scale)->listOf("light","dark").forEach {theme->
   RuntimeEnvironment.setQualifiers("w${w}dp-h${h}dp-mdpi");RuntimeEnvironment.setFontScale(scale);UiSupport.reset();UiSupport.withStore{it.settings(it.snapshot().settings.copy(theme=theme))}
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Impostazioni");click(a.window.decorView,"Esplora la demo separata")
   val label="${w}-font${(scale*100).toInt()}-$theme"
   listOf("Home","Movimenti","Impegni","Crediti").forEach {page->click(a.window.decorView,page);Capture.save(a,"responsive-${page.lowercase()}-$label.png",w,h)
    val monetary=all(a.window.decorView).filterIsInstance<TextView>().filter{it !is EditText && it.text.toString().endsWith(" €") && it.maxLines==1}
    monetary.forEach {if(it.layout!=null)assertEquals("Clipped monetary value",0,it.layout.getEllipsisCount(0))}
    val nav=all(a.window.decorView).filterIsInstance<Button>().filter{it.text.toString() in listOf("Home","Movimenti","Impegni","Crediti")};assertEquals(4,nav.size)
    nav.forEach {assertEquals(0,it.layout.getEllipsisCount(0));assertTrue(it.paint.measureText(it.text.toString())<=it.width-it.paddingLeft-it.paddingRight);assertTrue(it.width>=48);assertTrue(it.height>=48)}
    val positions=nav.map{v->val p=IntArray(2);v.getLocationInWindow(p);p[0] to p[0]+v.width};positions.zipWithNext().forEach{(one,next)->assertTrue(one.second<=next.first)}
    if(page=="Impegni"){click(a.window.decorView,"Calendario");Capture.save(a,"responsive-calendario-$label.png",w,h);val days=all(a.window.decorView).filterIsInstance<Button>().filter{it.text.toString().toIntOrNull()!=null};assertEquals(java.time.YearMonth.now().lengthOfMonth(),days.size);assertTrue(days.all{it.width>=32 && it.height>=44})}
   }
   click(a.window.decorView,"Home");click(a.window.decorView,"Analisi");Capture.save(a,"responsive-anello-$label.png",w,h);a.onBackPressed();click(a.window.decorView,"Registra");choose("Spesa");Capture.save(a,"responsive-form-$label.png",w,h)
   val root=(a.findViewById<ViewGroup>(android.R.id.content)).getChildAt(0);val insets=WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,24,0,34)).setInsets(WindowInsets.Type.ime(),android.graphics.Insets.of(0,0,0,220)).build();root.dispatchApplyWindowInsets(insets)
   Capture.save(a,"responsive-form-ime-insets-$label.png",w,h)
   assertEquals(220,root.paddingBottom);val save=all(root).filterIsInstance<Button>().first{it.text.toString()=="Salva"};val p=IntArray(2);save.getLocationInWindow(p);assertTrue("Save overlaps keyboard area",p[1]+save.height<=h-220)
   c.pause().stop().destroy()
  }};RuntimeEnvironment.setFontScale(1f)
 }
}
