package it.privato.disponibile.v2
import org.json.JSONObject
import java.time.*
object ReviewFixture {
 fun state():JSONObject=JSONObject(javaClass.getResourceAsStream("/rc2/fixture.json")!!.bufferedReader().use {it.readText()}).getJSONObject("state")
 fun install(theme:String):Ledger {
  UiSupport.reset();val j=state()
  fun rows(name:String)=j.getJSONArray(name).let {a->(0 until a.length()).map {a.getJSONObject(it)}}
  val categories=rows("categories").map {Category(it.getLong("id"),it.getString("name"),0xFF7392B7.toInt(),it.getString("icon"),it.optBoolean("favorite"))}
  val ops=rows("operations").map {Operation(it.getLong("id"),it.getString("title"),it.getLong("cents"),Kind.valueOf(it.getString("kind").uppercase()),Status.valueOf(it.getString("status").uppercase()),LocalDate.parse(it.getString("date")),it.getLong("category"),it.optBoolean("uncertain"))}
  val advances=rows("advances").map {Advance(it.getLong("id"),it.getLong("cents"),LocalDate.parse(it.getString("date")),it.getString("description"),it.getString("debtor"),it.getString("project"),it.getString("cause"),LocalDate.parse(it.getString("due")),it.getString("notes"))}
  val refunds=rows("refunds").map {Refund(it.getLong("id"),it.getLong("advance"),it.getLong("cents"),LocalDate.parse(it.getString("date")),it.getString("note"))}
  val ledger=Ledger(Settings(j.getLong("opening"),j.getLong("reserve"),LocalDate.parse(j.getString("horizon")),theme),categories,ops,emptyList(),advances,refunds)
  UiSupport.withStore {it.replace(ledger)};return ledger
 }
}
