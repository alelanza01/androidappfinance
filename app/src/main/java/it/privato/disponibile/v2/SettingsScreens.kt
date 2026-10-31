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
internal fun MainActivity.settings(body:LinearLayout) {
        body.addView(ui.section("Preferenze"))
        body.addView(ui.listRow("Regole del saldo","Liquidità iniziale, riserva e previsione"){go(7)});body.addView(ui.divider())
        body.addView(ui.listRow("Aspetto","Tema chiaro, scuro o di sistema"){go(8)});body.addView(ui.divider())
        body.addView(ui.listRow("Avvisi","Promemoria locali facoltativi"){go(11)})
        body.addView(ui.section("Archivio"));body.addView(ui.listRow("Categorie","Organizza le spese e le entrate"){go(9)});body.addView(ui.divider());body.addView(ui.listRow("Backup","Esporta e recupera i tuoi dati"){go(10)})
        body.addView(ui.section("App privata"));body.addView(ui.label("3.4 · offline · nessun account o costo ricorrente",13f,ui.colors.muted));body.addView(ui.button(if(demo)"Esci dalla demo" else "Esplora la demo separata"){demo=!demo;go(0)})
    }

internal fun MainActivity.rules(body:LinearLayout) {
        body.addView(ui.listRow("Liquidità iniziale","Prima dei movimenti registrati",Money.format(l.settings.opening)){moneyForm("opening",l.settings.opening)});body.addView(ui.divider())
        body.addView(ui.listRow("Riserva protetta","Somma da non spendere",Money.format(l.settings.reserve)){moneyForm("reserve",l.settings.reserve)});body.addView(ui.divider())
        body.addView(ui.section("Orizzonte finanziario"))
        body.addView(ui.segments(listOf("30 giorni","60 giorni","90 giorni"),if(l.settings.horizonMode==HorizonMode.ROLLING)listOf(30,60,90).indexOf(l.settings.horizonDays)else -1){i->change {store.settings(l.settings.copy(horizonMode=HorizonMode.ROLLING,horizonDays=listOf(30,60,90)[i]))}})
        body.addView(ui.label(if(l.settings.horizonMode==HorizonMode.ROLLING)"Mobile: si aggiorna ogni giorno. Oggi termina il ${l.settings.effectiveHorizon().format(dates)}."else "Data fissa: conservata dal tuo archivio.",12f,ui.colors.muted))
        body.addView(ui.listRow("Previsione fino al",l.settings.horizon.format(dates)){pick(l.settings.horizon){date->change {store.settings(l.settings.copy(horizon=date))}}})
        body.addView(ui.section("Correggi il saldo di oggi"));body.addView(ui.label("La base iniziale modifica anche lo storico. Per allineare solo la liquidità attuale, registra una rettifica.",14f,ui.colors.muted));body.addView(ui.button("Aggiorna liquidità"){moneyForm("reconcile",l.balance)})
    }

internal fun MainActivity.appearance(body:LinearLayout) {
        body.addView(ui.label("Un tema per ogni momento",22f,ui.colors.text,true));body.addView(ui.space(16))
        body.addView(ui.segments(listOf("Sistema","Chiaro","Scuro"),listOf("system","light","dark").indexOf(l.settings.theme)){i->change {store.settings(l.settings.copy(theme=listOf("system","light","dark")[i]))};if(!demo)recreate()})
        body.addView(ui.space(16));body.addView(ui.label("La dimensione del testo segue le preferenze di accessibilità del telefono.",14f,ui.colors.muted))
    }

internal fun MainActivity.backups(body:LinearLayout) {
        body.addView(ui.label("I dati restano tuoi",22f,ui.colors.text,true));body.addView(ui.label("Il backup JSON contiene dati personali e non è cifrato. Scegli una cartella locale sicura. L'importazione verifica il file e conserva prima una copia dell'archivio attuale.",14f,ui.colors.muted));body.addView(ui.space())
        body.addView(ui.button("Esporta backup",true){export(false)});body.addView(ui.button("Importa backup"){if(!demo)startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),2)})
        if(java.io.File(filesDir,"pre-import.json").exists())body.addView(ui.button("Esporta copia precedente all'importazione"){export(true)})
        body.addView(ui.section("Compatibilità"));body.addView(ui.label("Include movimenti, regole, categorie, ricorrenze, anticipi e rimborsi. Accetta i backup V2 e V2.1; le interruzioni delle serie sono conservate nei nuovi backup.",14f,ui.colors.muted))
    }

internal fun MainActivity.reminders(body:LinearLayout) {
        body.addView(ui.label(if(l.settings.reminders)"Avvisi attivi" else "Avvisi disattivati",22f,ui.colors.text,true));body.addView(ui.label("Un riepilogo locale delle spese entro domani e degli arretrati, controllato circa una volta al giorno. Android può ritardarlo per risparmiare batteria: non è un allarme puntuale.",14f,ui.colors.muted));body.addView(ui.space())
        body.addView(ui.button(if(l.settings.reminders)"Disattiva avvisi" else "Attiva avvisi",true){
            if(demo)return@button
            if(!l.settings.reminders && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),3)
            else change {ReminderService.schedule(this,!l.settings.reminders);store.settings(l.settings.copy(reminders=!l.settings.reminders))}
        })
        if(l.settings.reminders && !getSystemService(NotificationManager::class.java).areNotificationsEnabled())body.addView(ui.button("Apri autorizzazioni Android"){startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,packageName))})
    }

internal fun MainActivity.calculation(body:LinearLayout) {
        body.addView(ui.label("Solo denaro realmente disponibile",22f,ui.colors.text,true));ui.metric(body,"Liquidità",Money.format(l.balance));ui.metric(body,"− Impegni entro la previsione",Money.format(l.commitments),ui.colors.coral);ui.metric(body,"− Riserva protetta",Money.format(l.settings.reserve));body.addView(ui.divider());ui.metric(body,"Puoi spendere",Money.format(l.spendable),ui.colors.sage)
        body.addView(ui.section("La regola"));body.addView(ui.label("Il saldo include soltanto incassi, pagamenti, anticipi e rimborsi già effettuati. I crediti aperti e le entrate previste non sono denaro spendibile.",15f,ui.colors.muted))
        body.addView(ui.listRow("Aggiorna liquidità","Riconcilia il saldo reale senza inventare guadagni"){moneyForm("reconcile",l.balance)})
        body.addView(ui.listRow("Riserva e previsione","Modifica le regole del calcolo"){go(7)})
    }

internal fun MainActivity.onboardingScreen(body:LinearLayout) {
        NativeOnboarding(ui,onboarding,l).content(body,{render()},{
            try {
                val cash=Money.parse(onboarding.value("opening").ifBlank {"0"},true);val reserve=Money.parse(onboarding.value("reserve").ifBlank {"0"})
                val planned=onboarding.plans().map {p->Operation(title=p.getString("title"),cents=p.getLong("amount"),kind=Kind.EXPENSE,status=Status.PLANNED,date=LocalDate.parse(p.getString("date")),category=l.categories.first {it.name=="Altro"}.id)}
                store.completeOnboarding(cash,reserve,planned,onboarding.token);onboarding.clear();go(0)
            }catch(e:Exception){fail(e)}
        },{go(0)})
    }
