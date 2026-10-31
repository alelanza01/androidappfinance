package it.privato.disponibile.v2
import android.app.*
import android.os.Bundle
import android.view.*
import android.widget.*
import android.content.Intent
import android.graphics.Color
import android.text.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject
import it.privato.disponibile.v2.MainActivity.Draft
import it.privato.disponibile.v2.MainActivity.HistoryItem

/** Native screen composition and its UI events; financial writes remain in Store. */
internal fun MainActivity.formTitle()=when(draft?.type){"operation"->if(draft?.id==0L)if(draft?.values?.get("kind")!="INCOME")"Nuova spesa" else "Nuova entrata" else "Modifica movimento";"series"->if(draft?.id==0L)"Ricorrenza o rate" else "Modifica serie";"advance"->if(draft?.id==0L)"Nuovo anticipo" else "Modifica anticipo";"refund"->"Registra rimborso";"refundEdit"->"Modifica rimborso";"category"->"Categoria";"opening"->"Liquidità iniziale";"reserve"->"Riserva protetta";"reconcile"->"Liquidità attuale";"creditFilter"->"Filtri crediti";else->"Inserimento"}

internal fun MainActivity.openForm(type:String,id:Long=0,defaults:Map<String,String> = emptyMap()) {
        draftBaseline=null;previous=page;val v=mutableMapOf<String,String>();v.putAll(defaults)
        if(type=="operation" && id!=0L)l.operations.first {it.id==id}.let {o->v.putAll(mapOf("amount" to Money.input(o.cents),"description" to o.title,"kind" to o.kind.name,"status" to o.status.name,"date" to o.date.toString(),"category" to o.category.toString(),"uncertain" to o.uncertain.toString()))}
        if(type=="series" && id!=0L)l.series.first {it.id==id}.let {s->v.putAll(mapOf("amount" to Money.input(s.totalCents ?: s.cents),"amountMode" to s.amountMode.name,"description" to s.title,"kind" to s.kind.name,"date" to s.start.toString(),"category" to s.category.toString(),"uncertain" to s.uncertain.toString(),"frequency" to s.frequency.name,"count" to if(s.count==0)"" else s.count.toString()))}
        if(type=="advance" && id!=0L)l.advances.first {it.id==id}.let {a->v.putAll(mapOf("amount" to Money.input(a.cents),"description" to a.description,"debtor" to a.debtor,"date" to a.date.toString(),"project" to a.project,"cause" to a.cause,"due" to (a.due?.toString() ?: ""),"notes" to a.notes,"advanced" to "true"))}
        if(type=="refundEdit")l.refunds.first {it.id==id}.let {r->v.putAll(mapOf("amount" to Money.input(r.cents),"date" to r.date.toString(),"notes" to r.note,"advance" to r.advance.toString()))}
        if(type=="refund")v["advance"]=id.toString()
        if(type=="category" && id!=0L)l.categories.first {it.id==id}.let {c->v["description"]=c.name;v["color"]=c.color.toString();v["icon"]=c.icon;v["favorite"]=c.favorite.toString();v["pinned"]=(c.quickRank!=null).toString()}
        draft=Draft(type,id,v);go(20)
    }

internal fun MainActivity.moneyForm(type:String,value:Long)=openForm(type,defaults=mapOf("amount" to Money.input(value)))

internal fun MainActivity.watch(field:EditText,action:(String)->Unit){field.addTextChangedListener(object:TextWatcher {override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit;override fun afterTextChanged(s:Editable?)=Unit;override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){action(s.toString())}})}

internal fun MainActivity.pick(initial:LocalDate,action:(LocalDate)->Unit) {
        pickerTarget="horizon";pickerState=DateSelection(initial,YearMonth.from(initial),LocalDate.now(),LocalDate.now().plusYears(10));showPicker()
    }

internal fun MainActivity.pickField(key:String,initial:LocalDate) {
        val d=draft ?: return;val today=LocalDate.now()
        val actual=d.type in listOf("advance","refund","refundEdit") || d.type=="operation" && d.values["status"]!="PLANNED"
        val min=if(key=="due")d.values["date"]?.let {LocalDate.parse(it)} ?: today else if(d.type in listOf("refund","refundEdit"))l.advances.first {it.id==d.values["advance"]!!.toLong()}.date else LocalDate.of(1900,1,1)
        val max=if(actual && key!="due")today else today.plusYears(10)
        pickerTarget="draft:$key";val selected=initial.coerceIn(min,max);pickerState=DateSelection(selected,YearMonth.from(selected),min,max);showPicker()
    }

internal fun MainActivity.showPicker() {
        val state=pickerState ?: return;hideKeyboard();dialog?.dismiss()
        dialog=NativeDateSheet(this,ui,state).show({day->
            val target=pickerTarget;pickerState=null
            if(target.startsWith("draft:")){draft?.values?.set(target.substringAfter(":"),day.toString());render()}
            else if(target=="simulation"){simulationDraft.end=day;render()}
            else if(target=="horizon")change {store.settings(l.settings.copy(horizon=day,horizonMode=HorizonMode.FIXED))}
        },{pickerState=null})
    }

internal fun MainActivity.form(body:LinearLayout) {
        val d=draft ?: return
        fun value(key:String,default:String="")=d.values.getOrPut(key){default}
        fun field(key:String,title:String,money:Boolean=false,default:String=""):EditText {
            val e=ui.field(body,title,value(key,default),money);e.tag=key
            if(money && d.type in listOf("opening","reconcile"))e.inputType=e.inputType or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            if(key=="notes"){e.setSingleLine(false);e.maxLines=4;e.filters=arrayOf(android.text.InputFilter.LengthFilter(500))}
            watch(e){d.values[key]=it};return e
        }
        fun option(key:String,title:String,choices:List<Pair<String,String>>,default:String) {
            val selected=value(key,default);val label=choices.firstOrNull {it.first==selected}?.second ?: selected
            body.addView(ui.listRow(title,label){sheet(title,choices.map {(code,name)->name to {d.values[key]=code;render()}})})
        }
        fun date(key:String,title:String,optional:Boolean=false) {
            val text=value(key,if(optional)"" else LocalDate.now().toString())
            body.addView(ui.listRow(title,if(text.isBlank())"Facoltativa" else LocalDate.parse(text).format(dates)){pickField(key,if(text.isBlank())LocalDate.now() else LocalDate.parse(text))})
            if(optional && text.isNotBlank())body.addView(ui.button("Rimuovi data prevista"){d.values[key]="";render()})
        }
        fun category() {
            val default=l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()
            val cat=l.categories.first {it.id==value("category",default.id.toString()).toLong()}
            body.addView(ui.listRow("Categoria",cat.name){pickingFilter=false;categoryQuery="";categoryTab=2;go(21)})
        }
        fun advanced(action:()->Unit) {
            body.addView(ui.button(if(value("advanced","false")=="true")"Meno opzioni" else "Opzioni avanzate"){d.values["advanced"]=(value("advanced")=="false").toString();render()})
            if(value("advanced")=="true")action()
        }
        when(d.type) {
            "operation","series"->{
                val series=d.type=="series";val adjustment=value("kind","EXPENSE")=="ADJUSTMENT"
                val expense=value("kind","EXPENSE")=="EXPENSE"
                if(!expense && !adjustment)body.addView(ui.segments(listOf("Spesa","Entrata"),1){d.values["kind"]=if(it==0)"EXPENSE" else "INCOME";render()})
                val amount=ui.moneyEntry(body,"Importo in euro",value("amount")).apply {tag="amount"};watch(amount){d.values["amount"]=it}
                if(adjustment)amount.inputType=amount.inputType or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                if(series)body.addView(ui.label(if(value("amountMode","TOTAL")=="TOTAL" && value("rateMode")!="continuous")"Importo totale del piano"else "Importo di ciascuna rata",12f,ui.colors.muted).apply {gravity=Gravity.CENTER})
                if(expense){body.addView(ui.section("Categorie"));inlineCategories(body,d)}else category()
                field("description","Descrizione").apply {hint=if(expense)"Facoltativa" else "Compenso ricevuto";textSize=14f;minHeight=ui.dp(38);setPadding(0,ui.dp(3),0,ui.dp(3));background=null}
                if(expense){
                    val day=value("date",LocalDate.now().toString());val fields=ui.row().apply {orientation=if(resources.configuration.fontScale>=1.5f)LinearLayout.VERTICAL else LinearLayout.HORIZONTAL;background=ui.shape(ui.colors.hero,12)}
                    val dateView=ui.miniField(if(series)"Prima scadenza" else "Data",LocalDate.parse(day).format(dates)){pickField("date",LocalDate.parse(day))}
                    fields.addView(dateView,if(fields.orientation==LinearLayout.VERTICAL)LinearLayout.LayoutParams(-1,-2)else LinearLayout.LayoutParams(0,-2,1.2f))
                    if(!adjustment && (d.id==0L || series))fields.addView(ui.miniField("Rate",if(series){if(value("rateMode",if(value("count").isBlank())"continuous" else "finite")=="continuous")"Continua" else "${value("count")} rate"}else "Nessuna"){openRates()},if(fields.orientation==LinearLayout.VERTICAL)LinearLayout.LayoutParams(-1,-2)else LinearLayout.LayoutParams(0,-2,1f))
                    body.addView(ui.space(10));body.addView(fields)
                }else{date("date",if(series)"Prima scadenza" else "Data");if(!adjustment && (d.id==0L || series))body.addView(ui.listRow("Rate",if(series)"Piano rateale" else "Nessuna rata"){openRates()})}
                if(series)option("frequency","Ripetizione",Frequency.entries.map {it.name to frequencyName(it)},"MONTHLY")
                advanced {
                    if(!series && !adjustment)option("status","Quando incide sul saldo?",listOf("ACTUAL" to "Già effettuato","PLANNED" to "Previsto, ancora da effettuare"),"ACTUAL")
                    if(value("kind")=="INCOME" && (series || value("status","ACTUAL")=="PLANNED"))option("uncertain","Entrata non garantita",listOf("false" to "No","true" to "Sì, solo uno scenario"),"false")
                    if(!series && d.id==0L && !adjustment)body.addView(ui.button("Trasforma in ricorrenza o rate"){draft=Draft("series",values=d.values.toMutableMap().apply {put("status","PLANNED");put("rateMode","continuous")});render()})
                }
                value("status","ACTUAL");value("kind","EXPENSE");value("category",(l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()).id.toString());if(series){value("rateMode",if(value("count").isBlank())"continuous" else "finite");value("frequency","MONTHLY")}
                saveDraft={
                    val kind=Kind.valueOf(value("kind"));val cents=Money.parse(value("amount"),kind==Kind.ADJUSTMENT);val rawName=value("description").trim();val name=if(series && rawName.isBlank() && kind==Kind.EXPENSE)l.categoryName(value("category").toLong()) else if(rawName.isBlank() && kind==Kind.INCOME)"Entrata"else rawName;val day=LocalDate.parse(value("date"));val cat=value("category").toLong()
                    val uncertain=value("uncertain","false")=="true" && kind==Kind.INCOME && (series || value("status")=="PLANNED")
                    if(series){val count=if(value("rateMode")=="continuous")0 else value("count").toIntOrNull()?.also {require(it in 1..120){"Scegli da 1 a 120 rate."}} ?: error("Numero rate non valido.");val mode=if(count==0)AmountMode.EACH else AmountMode.valueOf(value("amountMode","TOTAL"));val quotes=if(mode==AmountMode.TOTAL)Installments.split(cents,count)else emptyList();val s=Series(d.id,name,if(mode==AmountMode.TOTAL)quotes.first()else cents,kind,cat,day,Frequency.valueOf(value("frequency")),count,uncertain,amountMode=mode,totalCents=if(mode==AmountMode.TOTAL)cents else null,installments=quotes);if(d.id==0L)store.saveSeries(s) else store.updateSeries(s)}
                    else {val old=l.operations.firstOrNull {it.id==d.id};store.save(Operation(d.id,name,cents,kind,if(adjustment)Status.ACTUAL else Status.valueOf(value("status")),day,cat,uncertain,old?.series,old?.index))}
                }
            }
            "advance"->{
                body.addView(ui.label("Il denaro è già uscito. Diventerà un credito da recuperare, distinto dalle spese personali.",13f,ui.colors.muted))
                field("amount","Importo anticipato in euro",true).textSize=30f;field("description","Descrizione");field("debtor","Da chi recuperare?");date("date","Data dell'anticipo")
                advanced {field("project","Immobile / progetto");field("cause","Categoria / causale");date("due","Restituzione prevista",true);field("notes","Note")}
                saveDraft={store.saveAdvance(Advance(d.id,Money.parse(value("amount")),LocalDate.parse(value("date")),value("description"),value("debtor"),value("project"),value("cause"),value("due").takeIf {it.isNotBlank()}?.let {LocalDate.parse(it)},value("notes")))}
            }
            "refund","refundEdit"->{
                val a=l.advances.first {it.id==value("advance").toLong()};body.addView(ui.label(a.description,20f,ui.colors.text,true));body.addView(ui.label("Da recuperare ${Money.format(l.remaining(a))} · ${a.debtor}",14f,ui.colors.muted))
                field("amount","Importo ricevuto in euro",true).textSize=30f;date("date","Data del rimborso");field("notes","Nota facoltativa")
                body.addView(ui.label("Registri soltanto denaro già ricevuto. Aumenterà la liquidità, senza diventare un guadagno.",13f,ui.colors.muted))
                saveDraft={store.saveRefund(Refund(if(d.type=="refundEdit")d.id else 0,a.id,Money.parse(value("amount")),LocalDate.parse(value("date")),value("notes")))}
            }
            "opening","reserve","reconcile"->{
                body.addView(ui.label(when(d.type){"opening"->"È la base prima dei movimenti: cambia anche lo storico. Per allineare solo il saldo di oggi usa Aggiorna liquidità.";"reserve"->"La somma protetta viene sottratta dallo spendibile.";else->"Verrà registrata una rettifica: nessun guadagno o spesa inventati."},14f,ui.colors.muted))
                field("amount","Euro",true).textSize=30f
                saveDraft={val n=Money.parse(value("amount"),d.type!="reserve");when(d.type){"opening"->store.settings(l.settings.copy(opening=n));"reserve"->store.settings(l.settings.copy(reserve=n));else->store.reconcile(n)}}
            }
            "category"->{
                field("description","Nome categoria")
                val colors=listOf("Salvia" to "#779B91","Oro" to "#DBBF8B","Blu" to "#7392B7","Lavanda" to "#AC91BB","Corallo" to "#C66B65","Sabbia" to "#B09060")
                option("color","Colore",colors.map {(name,hex)->Color.parseColor(hex).toString() to name},Color.parseColor("#779B91").toString())
                option("icon","Icona",CategoryCatalog.entries.map {it.icon to it.name},"other")
                option("favorite","Preferita",listOf("false" to "No","true" to "Sì"),"false")
                option("pinned","Fissata tra le rapide",listOf("false" to "No","true" to "Sì"),"false")
                if(d.id!=0L)body.addView(ui.destructive("Elimina categoria"){confirm("Eliminare la categoria?","È possibile soltanto se non è utilizzata da movimenti o ricorrenze attive.","Elimina categoria"){store.deleteCategory(d.id);finishForm(false)}})
                saveDraft={store.category(d.id.takeIf {it!=0L},value("description"),value("color").toInt(),value("icon"),value("favorite")=="true");val saved=store.snapshot(false).categories.first {it.name==value("description").trim()};store.pinCategory(saved.id,value("pinned")=="true")}
            }
            "creditFilter"->{
                field("subject","Soggetto");field("project","Immobile / progetto")
                option("state","Stato",listOf("Tutti","Aperti","Parziali","Recuperati","Scaduti").mapIndexed {i,name->i.toString() to name},"0")
                option("month","Periodo",listOf("false" to "Tutto il periodo","true" to "Mese selezionato"),"false")
                body.addView(ui.button("Azzera filtri"){d.values.clear();render()})
                saveDraft={creditSubject=value("subject");creditProject=value("project");creditStatus=value("state").toInt();creditMonth=value("month")=="true"}
            }
        }
        if(draftBaseline==null)draftBaseline=draftFingerprint()
    }

internal fun MainActivity.inlineCategories(body:LinearLayout,d:Draft) {
        val fallback=(l.categories.firstOrNull {it.name=="Altro"} ?: l.categories.first()).id.toString()
        val selected=d.values.getOrPut("category"){fallback}.toLong()
        val frozen=d.values.getOrPut("quickIds"){QuickCategories.ordered(l).take(12).joinToString(","){it.id.toString()}}
        val choices=frozen.split(",").mapNotNull {id->l.categories.firstOrNull {it.id==id.toLongOrNull()}}
        val columns=if(resources.configuration.fontScale>=1.5f)3 else 4
        choices.chunked(columns).forEach {chunk->val row=ui.row().apply {gravity=Gravity.TOP};chunk.forEach {cat->row.addView(ui.categoryTile(cat,cat.id==selected,true){d.values["category"]=cat.id.toString();render()},LinearLayout.LayoutParams(0,-2,1f))};repeat(columns-chunk.size){row.addView(View(this),LinearLayout.LayoutParams(0,1,1f))};body.addView(row)}
        body.addView(ui.button("Categoria"){pickingFilter=false;categoryQuery="";categoryTab=2;go(21)}.apply {text=l.categoryName(selected);contentDescription="Categoria";textSize=13f;gravity=Gravity.START;setPadding(ui.dp(12),ui.dp(6),ui.dp(12),ui.dp(6));l.categories.firstOrNull {it.id==selected}?.let {cat->setCompoundDrawablesWithIntrinsicBounds(CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(24),ui.categoryWash(cat)),null,null,null);compoundDrawablePadding=ui.dp(10)};minHeight=ui.dp(40);minimumHeight=ui.dp(40);background=ui.shape(ui.colors.hero,12)})
    }

internal fun MainActivity.categoryGrid(body:LinearLayout,picker:Boolean) {
        val search=ui.input("Cerca categoria…",categoryQuery).apply {contentDescription="Cerca categorie";textSize=13f;minHeight=ui.dp(44);setPadding(ui.dp(12),ui.dp(7),ui.dp(12),ui.dp(7));background=ui.shape(ui.colors.hero,20).apply {setStroke(ui.dp(1),ui.colors.line)};setCompoundDrawablesWithIntrinsicBounds(SearchIcon(ui.colors.cool,ui.dp(16)),null,null,null);compoundDrawablePadding=ui.dp(8)};body.addView(search)
        val results=ui.column();body.addView(results)
        fun choose(cat:Category){if(!picker){openForm("category",cat.id)}else if(pickingFilter){categoryFilter=cat.id;go(1)}else {draft?.values?.set("category",cat.id.toString());go(20)}}
        fun edit(cat:Category){if(picker){parentDraft=draft;parentPrevious=previous;parentBaseline=draftBaseline};openForm("category",cat.id)}
        fun update(){
            results.removeAllViews()
            results.addView(ui.section("Preferite"))
            val favorites=l.categories.filter {it.favorite};val horizontal=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false};val row=ui.row()
            favorites.forEach {cat->row.addView(ui.categoryTile(cat,false,true){choose(cat)},LinearLayout.LayoutParams(ui.dp(if(resources.configuration.fontScale>=1.5f)100 else 72),-2))};horizontal.addView(row);results.addView(horizontal)
            if(favorites.isEmpty())results.addView(ui.label("Tocca un cuore per ritrovare qui le tue categorie.",12f,ui.colors.muted))
            val heading=ui.row();heading.addView(ui.section(if(categoryTab==2)"Tutte le categorie" else if(categoryTab==1)"Categorie preferite" else "Usate di recente"),LinearLayout.LayoutParams(0,-2,1f));heading.addView(ui.button("Filtri"){sheet("Mostra categorie",listOf("Recenti","Preferite","Tutte").mapIndexed {i,name->name to {categoryTab=i;render()}})}.apply {contentDescription="Filtra categorie";textSize=11f;setTextColor(ui.colors.cool)},LinearLayout.LayoutParams(ui.dp(52),ui.dp(48)));results.addView(heading)
            val recent=l.operations.filter {it.status!=Status.DELETED}.sortedByDescending {it.id}.map {it.category}.distinct().take(9)
            val choices=when(categoryTab){0->recent.mapNotNull {id->l.categories.firstOrNull {it.id==id}};1->favorites;else->l.categories}
            val found=choices.filter {categoryQuery.isBlank() || it.name.contains(categoryQuery,true) || (it.icon=="basket" && "alimentari".contains(categoryQuery,true)) || (it.icon=="vape" && "sigaretta elettronica".contains(categoryQuery,true))}
            if(found.isEmpty())results.addView(ui.label("Nessuna categoria in questa selezione",13f,ui.colors.muted))
            found.forEach {cat->val line=ui.row();val select=ui.button(cat.name){choose(cat)}.apply {tag="category-choice";gravity=Gravity.START;textSize=13f;typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL);setPadding(0,ui.dp(5),0,ui.dp(5));setCompoundDrawablesWithIntrinsicBounds(CategoryIcon(cat.icon,ui.categoryTint(cat),ui.dp(32),ui.categoryWash(cat)),null,null,null);compoundDrawablePadding=ui.dp(12);contentDescription=cat.name;setOnLongClickListener {edit(cat);true}}
                line.addView(select,LinearLayout.LayoutParams(0,-2,1f))
                line.addView(ui.button(""){if(demo){Toast.makeText(this,"La demo non modifica i tuoi dati.",Toast.LENGTH_SHORT).show()}else try {store.category(cat.id,cat.name,cat.color,cat.icon,!cat.favorite);l=store.snapshot();update()}catch(e:Exception){fail(e)}}.apply {setCompoundDrawablesWithIntrinsicBounds(HeartIcon(if(cat.favorite)ui.colors.primary else ui.colors.muted,cat.favorite,ui.dp(18)),null,null,null);contentDescription="${if(cat.favorite)"Rimuovi" else "Aggiungi"} ${cat.name} ${if(cat.favorite)"dai" else "ai"} preferiti";setPadding(ui.dp(12),0,ui.dp(12),0)},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)))
                results.addView(line)
            }
        };watch(search){categoryQuery=it;update()};update()
        if(picker && pickingFilter)body.addView(ui.button("Tutte le categorie"){categoryFilter=null;go(1)})
        body.addView(ui.button("Ordina rapide"){quickCategoryOrder()});body.addView(ui.label("Tieni premuto sul nome per modificare",11f,ui.colors.muted));body.addView(ui.button("Nuova categoria"){if(picker){parentDraft=draft;parentPrevious=previous;parentBaseline=draftBaseline};openForm("category")})
    }

internal fun MainActivity.quickCategoryOrder() {
        val ordered=QuickCategories.ordered(l).toMutableList();dialog?.dismiss()
        val d=Dialog(this);val container=ui.column().apply {setPadding(ui.dp(20),ui.dp(14),ui.dp(20),ui.dp(16));background=ui.shape(ui.colors.card,27)}
        container.addView(ui.label("Ordina e fissa le rapide",20f,ui.colors.text,true));container.addView(ui.label("Le prime 12 categorie saranno fissate. Le altre seguono preferite e frequenza d'uso.",12f,ui.colors.muted))
        val rows=ui.column();val scroll=ScrollView(this).apply {addView(rows)};container.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        fun draw(){rows.removeAllViews();ordered.forEachIndexed {i,cat->val row=ui.row();row.addView(ui.label(cat.name,14f),LinearLayout.LayoutParams(0,-2,1f));row.addView(ui.button("↑"){if(i>0){ordered.removeAt(i);ordered.add(i-1,cat);draw()}}.apply {contentDescription="Sposta ${cat.name} su";isEnabled=i>0},LinearLayout.LayoutParams(ui.dp(48),ui.dp(48)));rows.addView(row)}};draw()
        container.addView(ui.button("Salva ordine",true){if(!demo){store.quickOrder(ordered.take(12).map {it.id});d.dismiss();render()}});container.addView(ui.button("Ordine automatico"){if(!demo){store.quickOrder(emptyList());d.dismiss();render()}});container.addView(ui.button("Annulla"){d.dismiss()})
        d.setContentView(container);dialog=d;d.show();d.window?.apply {setBackgroundDrawableResource(android.R.color.transparent);setLayout(-1,(resources.displayMetrics.heightPixels*.82).toInt());setGravity(Gravity.BOTTOM)}
    }

internal fun MainActivity.openRates(){
        val d=draft ?: return
        rateBaseline=d.json()
        rateChoice=if(d.type!="series")"none" else if(d.values["rateMode"]=="continuous")"continuous" else "finite"
        rateCount=d.values["count"]?.takeIf {it.isNotBlank()} ?: "12";ratesAdvanced=rateChoice=="finite" && rateCount !in listOf("2","3","6","12");go(22)
    }

internal fun MainActivity.rates(body:LinearLayout) {
        val d=draft ?: return
        val amount=d.values["amount"]?.let {runCatching {Money.parse(it)}.getOrNull()}
        val amountMode=if(rateChoice=="continuous")"EACH"else d.values.getOrPut("amountMode"){"TOTAL"}
        body.addView(ui.segments(listOf("Importo totale","Ogni rata"),if(amountMode=="TOTAL")0 else 1){d.values["amountMode"]=if(it==0)"TOTAL"else "EACH";if(it==0 && rateChoice=="continuous")rateChoice="finite";render()})
        body.addView(ui.amount(amount?.let {DisplayMoney.amount(it)} ?: "—",32f).apply {gravity=Gravity.CENTER;background=ui.shape(ui.colors.hero,20);setPadding(ui.dp(10),ui.dp(14),ui.dp(10),ui.dp(14))})
        body.addView(ui.label(if(amountMode=="TOTAL")"Importo totale da suddividere"else "Importo di ciascuna rata",12f,ui.colors.muted).apply {gravity=Gravity.CENTER});body.addView(ui.space(12))
        val radioUpdates=mutableListOf<()->Unit>()
        fun option(name:String,subtitle:String,choice:String,n:Int?=null,enabled:Boolean=true){val selected=rateChoice==choice && (n==null || rateCount==n.toString())
            val row=ui.row().apply {setPadding(ui.dp(14),ui.dp(9),ui.dp(12),ui.dp(9));background=ui.shape(if(selected)ui.colors.hero else ui.colors.card,16).apply {if(selected)setStroke(ui.dp(1),ui.colors.primary)};isEnabled=enabled;alpha=if(enabled)1f else .45f}
            row.addView(View(this).apply {background=RadioIcon(selected,if(selected)ui.colors.primary else ui.colors.muted,ui.dp(22))},LinearLayout.LayoutParams(ui.dp(22),ui.dp(22)).apply {marginEnd=ui.dp(12)})
            val text=ui.column();text.addView(ui.label(name,14f,ui.colors.text,true));text.addView(ui.label(subtitle,12f,ui.colors.muted));row.addView(text,LinearLayout.LayoutParams(0,-2,1f));row.contentDescription=name;row.isSelected=selected;row.isFocusable=true;row.minimumHeight=ui.dp(56)
            radioUpdates.add {val checked=rateChoice==choice && (n==null || rateCount==n.toString());row.isSelected=checked;row.background=ui.shape(if(checked)ui.colors.hero else ui.colors.card,16).apply {if(checked)setStroke(ui.dp(1),ui.colors.primary)};row.getChildAt(0).background=RadioIcon(checked,if(checked)ui.colors.primary else ui.colors.muted,ui.dp(22))}
            if(enabled)row.setOnClickListener {rateChoice=choice;if(n!=null)rateCount=n.toString();render()};body.addView(row);body.addView(ui.space(7))
        }
        option("Nessuna rata","Spesa singola","none",enabled=d.id==0L)
        listOf(2,3,6,12).forEach {n->option("$n rate",if(amount==null)"Importo per rata da inserire" else if(amountMode=="TOTAL")runCatching {Installments.split(amount,n).let {quotes->(if(quotes.first()==quotes.last())DisplayMoney.amount(quotes.first())else "${DisplayMoney.amount(quotes.last())}–${DisplayMoney.amount(quotes.first())}")+" per rata · totale ${DisplayMoney.amount(amount)}"}}.getOrDefault("Totale insufficiente")else "${DisplayMoney.amount(amount)} per rata","finite",n)}
        val countValue=rateCount.toIntOrNull()
        val summary=ui.label(if(rateChoice=="finite" && amount!=null && countValue in 1..120)if(amountMode=="TOTAL")"$rateCount rate · totale ${DisplayMoney.amount(amount)}"else "$rateCount rate · totale ${DisplayMoney.amount(amount*countValue!!)}" else if(rateChoice=="continuous")"Piano: ricorrenza continua" else "Nessuna suddivisione",12f,ui.colors.muted);body.addView(summary)
        body.addView(ui.button(if(ratesAdvanced)"Meno opzioni" else "Altre opzioni"){ratesAdvanced=!ratesAdvanced;render()}.apply {textSize=12f;setTextColor(ui.colors.cool)})
        if(ratesAdvanced){
        option("Ricorrenza continua","Senza numero finale, alla frequenza scelta","continuous")
        body.addView(ui.section("Numero personalizzato · da 1 a 120"))
        val row=ui.row();val count=ui.input("12",rateCount).apply {contentDescription="Numero rate";inputType=android.text.InputType.TYPE_CLASS_NUMBER;filters=arrayOf(InputFilter.LengthFilter(3));gravity=Gravity.CENTER}
        val minus=ui.button("−"){val n=count.text.toString().toIntOrNull() ?: 1;count.setText(maxOf(1,n-1).toString())}.apply {contentDescription="Riduci rate"}
        val plus=ui.button("+"){val n=count.text.toString().toIntOrNull() ?: 1;count.setText(minOf(120,n+1).toString())}.apply {contentDescription="Aumenta rate"}
        row.addView(minus,LinearLayout.LayoutParams(ui.dp(48),ui.dp(52)));row.addView(count,LinearLayout.LayoutParams(0,ui.dp(52),1f));row.addView(plus,LinearLayout.LayoutParams(ui.dp(48),ui.dp(52)));body.addView(row)
        fun update(){val n=count.text.toString().toIntOrNull();minus.isEnabled=n!=null && n>1;plus.isEnabled=n!=null && n<120;count.error=if(n==null || n !in 1..120)"Da 1 a 120" else null}
        watch(count){rateCount=it;rateChoice="finite";update();radioUpdates.forEach {refresh->refresh()};val n=it.toIntOrNull();summary.text=if(n!=null && n in 1..120 && amount!=null)if(amountMode=="TOTAL")"$n rate · totale ${DisplayMoney.amount(amount)}"else "$n rate · totale ${DisplayMoney.amount(amount*n)}" else "Scegli da 1 a 120 rate"};update()
        }
        if(ratesAdvanced) {
            body.addView(ui.listRow("Prima scadenza",d.values.getOrPut("date"){LocalDate.now().toString()}){pickField("date",LocalDate.parse(d.values.getValue("date")))})
            val frequency=Frequency.valueOf(d.values.getOrPut("frequency"){"MONTHLY"})
            body.addView(ui.listRow("Ripetizione",frequencyName(frequency)){sheet("Ripetizione",Frequency.entries.map {f->frequencyName(f) to {d.values["frequency"]=f.name;render()}})})
            if(rateChoice=="finite" && amount!=null && countValue in 1..120) {
                val quotes=runCatching {if(amountMode=="TOTAL")Installments.split(amount,countValue!!)else List(countValue!!){amount}}.getOrDefault(emptyList())
                body.addView(ui.section("Dettaglio rate"));quotes.forEachIndexed {i,n->val first=LocalDate.parse(d.values.getValue("date"));val date=when(frequency){Frequency.WEEKLY->first.plusWeeks(i.toLong());Frequency.YEARLY->first.plusYears(i.toLong());else->first.plusMonths(i.toLong())};ui.metric(body,"${i+1}. ${date.format(dates)}",Money.format(n))}
            }
        }
        body.addView(ui.label("Applica prepara il piano. Nessun pagamento viene registrato.",11f,ui.colors.muted))
    }

internal fun MainActivity.applyRates(){try {
        val d=draft ?: return
        if(rateChoice=="none"){require(d.id==0L);draft=Draft("operation",values=d.values.toMutableMap().apply {put("count","");put("rateMode","none")})}
        else {val n=if(rateChoice=="continuous")0 else rateCount.toIntOrNull()?.also {require(it in 1..120){"Scegli da 1 a 120 rate."}} ?: error("Numero rate non valido.")
            draft=Draft("series",d.id,d.values.toMutableMap().apply {put("count",if(n==0)"" else n.toString());put("rateMode",if(n==0)"continuous" else "finite");put("frequency",get("frequency") ?: "MONTHLY");if(n==0)put("amountMode","EACH")})}
        rateBaseline=null;go(20)
    }catch(e:Exception){fail(e)}}

internal fun MainActivity.categories(body:LinearLayout)=categoryGrid(body,false)
