package it.privato.disponibile.v2
import android.graphics.*
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RefinementRenderingTest {
 @Before fun reset(){UiSupport.reset();RuntimeEnvironment.setFontScale(1f)}
 @Test fun nativeButtonStatesHaveFlatMatchedBoundsInBothThemes() {
  listOf(false,true).forEach {dark->
   val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val activity=controller.get();val ui=Design(activity,Palette(dark));val body=ui.column().apply {setBackgroundColor(ui.colors.background);setPadding(48,48,48,48)}
   listOf("Normale","Premuto","Selezionato","Disabilitato").forEachIndexed {i,state->
    body.addView(ui.label(state,12f,ui.colors.muted));val row=ui.row()
    listOf(ui.button("Salva",true){},ui.button("Secondario"){},ui.destructive("Elimina"){}).forEach {b->
     b.isPressed=i==1;b.isSelected=i==2;b.isEnabled=i!=3;b.refreshDrawableState();b.background.state=b.drawableState;b.background.jumpToCurrentState();if(i==1)b.drawableHotspotChanged(100f,50f)
     row.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply {marginEnd=9})
    };body.addView(row);body.addView(ui.space(16))
   }
   body.addView(ui.label("Selezione categoria",12f,ui.colors.muted));body.addView(ui.categoryTile(Category(1,"Casa",0,"home"),true){})
   activity.setContentView(body);Capture.layout(activity.window.decorView,1080,1600);Capture.layout(body,1080,1600);UiSupport.idle();val image=Bitmap.createBitmap(1080,1600,Bitmap.Config.ARGB_8888);body.draw(Canvas(image))
   UiSupport.all(body).filterIsInstance<Button>().forEach {b->
    b.refreshDrawableState()
    if(b.text.toString()=="Salva"){
     val content=(b.background as android.graphics.drawable.RippleDrawable).getDrawable(0);val fill=(if(content is android.graphics.drawable.StateListDrawable)content.current else content) as android.graphics.drawable.GradientDrawable
     assertEquals(if(!b.isEnabled)ui.colors.line else ui.colors.primary,fill.colors?.firstOrNull() ?: fill.color!!.getColorForState(b.drawableState,0))
    }
    assertNull(b.outlineProvider);assertNull(b.stateListAnimator);assertEquals(0f,b.translationZ,0f)
    val position=IntArray(2);val origin=IntArray(2);b.getLocationInWindow(position);body.getLocationInWindow(origin);position[0]-=origin[0];position[1]-=origin[1];assertEquals("No shadow outside rounded button",ui.colors.background,image.getPixel(position[0],position[1]))
   }
   val dir=File(System.getProperty("posso.screenshots.dir"));dir.mkdirs();File(dir,"componenti-stati-${if(dark)"dark" else "light"}.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle();controller.pause().stop().destroy()
  }
 }
 @Test fun pickerBlankExpenseInstallmentsAndContextualDecisionsAreRealViews() {
  listOf("light","dark").forEach {theme->
   UiSupport.reset();UiSupport.withStore{it.settings(it.snapshot().settings.copy(theme=theme));val c=it.snapshot().categories.first();it.category(c.id,c.name,c.color,c.icon,true)}
   val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
   click(a.window.decorView,"Registra");Capture.save(a,"popup-apertura-$theme.png",overlay=true);val decor=UiSupport.dialog().window!!.decorView;assertNull(decor.outlineProvider);assertEquals(0f,decor.elevation,0f)
   choose("Spesa");UiSupport.set(a.window.decorView,"Importo in euro","12,50");Capture.save(a,"spesa-descrizione-facoltativa-$theme.png")
   click(a.window.decorView,"Categoria");Capture.save(a,"categorie-griglia-$theme.png");click(a.window.decorView,"Preferite");Capture.save(a,"categorie-preferite-$theme.png");click(a.window.decorView,"Casa")
   a.onBackPressed();Capture.save(a,"conferma-uscita-$theme.png",overlay=true);choose("Continua a modificare");click(a.window.decorView,"Salva")
   click(a.window.decorView,"Movimenti");click(a.window.decorView,"Casa");choose("Elimina movimento");Capture.save(a,"conferma-eliminazione-$theme.png",overlay=true);choose("Annulla")
   click(a.window.decorView,"Home");click(a.window.decorView,"Registra");choose("Ricorrenza o rate");UiSupport.set(a.window.decorView,"Importo in euro","19,99");click(a.window.decorView,"Rate");UiSupport.set(a.window.decorView,"Numero rate","12")
   val scroll=UiSupport.all(a.window.decorView).filterIsInstance<ScrollView>().first();Capture.layout(a.window.decorView,1080,2400);scroll.fullScroll(View.FOCUS_DOWN);UiSupport.idle();Capture.save(a,"rate-controllo-totale-$theme.png")
   a.onBackPressed();a.onBackPressed();choose("Esci senza salvare");controller.pause().stop().destroy()
  }
 }
 @Test fun categoryGridIsAccessibleOnSmallScreensWithLargeFonts() {
  listOf(Triple(320,640,1f),Triple(360,800,1f),Triple(320,640,2f)).forEach {(w,h,font)->listOf("light","dark").forEach {theme->
   RuntimeEnvironment.setQualifiers("w${w}dp-h${h}dp-mdpi");RuntimeEnvironment.setFontScale(font);UiSupport.reset();UiSupport.withStore {it.settings(it.snapshot().settings.copy(theme=theme))}
   val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();click(a.window.decorView,"Registra");choose("Spesa");click(a.window.decorView,"Categoria")
   Capture.save(a,"responsive-categorie-$w-font${(font*100).toInt()}-$theme.png",w,h)
   val tiles=UiSupport.all(a.window.decorView).filterIsInstance<Button>().filter {it.tag=="category-choice"};assertEquals(27,tiles.size)
   tiles.forEach {assertTrue(it.width>=48);assertTrue(it.height>=48);assertEquals(0,it.layout.getEllipsisCount(0))}
   c.pause().stop().destroy()
  }};RuntimeEnvironment.setFontScale(1f)
 }
}
