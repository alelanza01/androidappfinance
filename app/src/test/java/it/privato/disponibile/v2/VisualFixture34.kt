package it.privato.disponibile.v2
import java.time.LocalDate
object VisualFixture34 {
 fun install(theme:String) {UiSupport.reset();UiSupport.withStore {s->
  val cats=s.snapshot().categories;val today=LocalDate.now();s.settings(s.snapshot().settings.copy(opening=10000,reserve=30000,theme=theme))
  val keys=listOf("home","basket","car","travel","health","bag");val amounts=listOf(60000L,23000L,18000L,17000L,8000L,6000L)
  keys.forEachIndexed {i,key->val cat=cats.first {it.icon==key};s.save(Operation(title=cat.name,cents=amounts[i],kind=Kind.EXPENSE,status=Status.ACTUAL,date=today.minusDays((i+1).toLong()),category=cat.id));if(i<4)s.category(cat.id,cat.name,cat.color,cat.icon,true)}
  s.save(Operation(title="Stipendio",cents=245000,kind=Kind.INCOME,status=Status.ACTUAL,date=today.minusDays(12),category=cats.first {it.icon=="income"}.id))
  s.save(Operation(title="Affitto",cents=30000,kind=Kind.EXPENSE,status=Status.PLANNED,date=today.plusDays(5),category=cats.first {it.icon=="home"}.id))
  s.save(Operation(title="Bolletta luce",cents=8000,kind=Kind.EXPENSE,status=Status.PLANNED,date=today.plusDays(10),category=cats.first {it.icon=="bolt"}.id))
  s.save(Operation(title="Incarico atteso",cents=20000,kind=Kind.INCOME,status=Status.PLANNED,date=today.plusDays(15),category=cats.first {it.icon=="income"}.id,uncertain=true))
  s.saveAdvance(Advance(cents=5000,date=today.minusDays(4),description="Pulizie Casa A",debtor="Proprietario",due=today.plusDays(7)));s.saveRefund(Refund(advance=s.snapshot().advances.single().id,cents=2000,date=today.minusDays(2),note="Primo rimborso"))
  // Pin approved initial quick grid; subsequent usage cannot reorder an open draft.
  s.quickOrder(listOf("basket","home","car","travel","wine","smoke","vape","health","bag","holiday","work","other").map {key->cats.first {it.icon==key}.id})
 }}
}
