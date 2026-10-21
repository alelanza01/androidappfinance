package it.privato.disponibile.v2
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26,35])
class ComponentAuditTest {
 @Test fun customButtonsNeverReacquireMaterialShadowOrOutlineInAnyState() {
  listOf(false,true).forEach {dark->val ui=Design(RuntimeEnvironment.getApplication(),Palette(dark));listOf(ui.button("Salva",true){},ui.button("Secondario"){},ui.destructive("Elimina spesa"){},ui.categoryTile(Category(1,"Casa",0,"home"),true){}).forEach {b->
   listOf("normal","pressed","selected","disabled").forEach {state->b.isPressed=state=="pressed";b.isSelected=state=="selected";b.isEnabled=state!="disabled";b.refreshDrawableState();UiSupport.idle()
    assertNull(b.stateListAnimator);assertNull(b.outlineProvider);assertNull(b.backgroundTintList);assertEquals(0f,b.elevation,0f);assertEquals(0f,b.translationZ,0f)
   }
  }}
 }
}
