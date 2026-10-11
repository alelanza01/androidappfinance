# Posso spenderlo? 3.0

App privata Android 8+, Kotlin nativo e SQLite: offline, nessun account, server, analytics, permesso Internet o costo ricorrente.

**[APK V3.0](release/Posso-spenderlo-V3.0.apk)** · [SHA-256](release/SHA256-V3.0.txt) · [Sorgenti ZIP](release/Posso-spenderlo-V3.0-progetto.zip) · [Rendering Android](release/screenshots-v3.0/) · [Verifiche](release/VERIFICA-V3.0.txt).

Su GitHub apri il file APK e premi **Download raw file**. Tutti i sorgenti sono anche nelle cartelle del repository.

## Aggiornare la V2.1

1. Nella versione installata, esporta un backup e conservalo sul dispositivo o PC.
2. Scarica il nuovo APK e aprilo sul telefono. Autorizza questa fonte se Android lo richiede.
3. Premi **Aggiorna/Installa**. **Non disinstallare la V2.1**, perché perderesti il suo database.

Identità invariata `it.privato.disponibile.v2`, versione `3.0.0` / codice 300. Il certificato viene confrontato con gli APK V2 e V2.1 prima della consegna. La migrazione schema SQLite 2 → 3 conserva impostazioni, categorie, movimenti effettivi/previsti, anticipi e rimborsi. Rimuove soltanto residui già eliminati e aggiunge il limite persistente delle ricorrenze.

La V1 resta un’app separata. L’installazione fisica sul telefono del proprietario non viene dichiarata come eseguita dal cloud.

## Nuova esperienza

- **Home:** spendibile protagonista, un prossimo pagamento e totale crediti. Dettaglio del calcolo, simulatore e analisi si aprono su richiesta.
- **Movimenti:** storico per giorno, ricerca indipendente da quella dei crediti, filtri per tipo/stato e categoria. Anticipi e rimborsi hanno etichette distinte.
- **Impegni:** lista o calendario mensile, arretrati, entrate attese, restituzioni non garantite e programmazioni.
- **Crediti:** anticipi, residui, stati, filtri per soggetto/progetto/mese, dettaglio e storico dei rimborsi.
- **Analisi:** un grafico alla volta — spese, liquidità o mesi — con controlli contestuali e selezione dei dati.
- **Impostazioni:** accessibili dall’icona in Home; Regole del saldo, Aspetto, Avvisi, Categorie e Backup hanno pagine separate.

**Registra** o **+** apre un menu breve. Gli inserimenti complessi sono pagine con campi etichettati, tastiera numerica, opzioni avanzate espandibili e Salva sopra l’area della tastiera. Una spesa ordinaria richiede solo importo e descrizione: tipo, stato, data e categoria hanno valori iniziali modificabili. Le bozze sopravvivono alla ricreazione dell’attività; non vengono registrate prima del salvataggio.

Design condiviso: superfici neutre, blu notte, oro e salvia; liste con gerarchia tipografica, menu inferiori per decisioni brevi e feedback di pressione discreto. Le impostazioni di testo ingrandito restano attive sui contenuti; etichette dei controlli fissi rimangono compatte e leggibili.

## Cancellazioni e ricorrenze

Da un’occorrenza ricorrente: **Elimina…** offre singola occorrenza, questa e tutte le future, intera serie.

- Singola: non viene rigenerata; se già effettuata, la conferma illustra la rettifica del saldo.
- Da qui: salva un limite nella definizione; elimina solo le previsioni dalla relativa occorrenza. I pagamenti/incassi già effettuati restano.
- Intera serie: interrompe tutta la programmazione. La rimozione anche dello storico richiede una conferma distinta con saldo prima/dopo.

La modifica della serie aggiorna le previsioni non effettuate mantenendo identità e importi storici. Le interruzioni e le esclusioni di singole occorrenze sopravvivono a riavvio, orizzonte esteso e backup. La materializzazione avviene nella stessa transazione della lettura delle definizioni, evitando copie obsolete usate dai job locali.

Le eliminazioni ordinarie rimuovono la riga dal database e dai backup. Solo le esclusioni tecniche necessarie a non rigenerare singole occorrenze attive rimangono conservate: non sono movimenti economici visibili o conteggiati.

## Contabilità e archivio

Importi in centesimi interi. **Liquidità = base + incassi − pagamenti + rettifiche − anticipi + rimborsi ricevuti. Spendibile = liquidità − impegni entro la previsione − riserva.** Entrate attese e crediti aperti non aumentano lo spendibile. Anticipi/rimborsi sono inclusi nello storico del saldo, esclusi da guadagni e spese personali nei grafici anello/colonne.

Esempio: 300 € → anticipo 50 € → 250 €/credito 50 € → rimborso 20 € → 270 €/credito 30 € → rimborso 30 € → 300 €/credito 0 €. Rimborso superiore al residuo, date incompatibili e importi invalidi vengono rifiutati.

Backup JSON formato 4, compatibile in importazione con formati 2 e 3. Validazione prima della sostituzione transazionale e copia locale precedente all’importazione, esportabile dalla pagina Backup. JSON non cifrato: contiene dati personali. Limiti: 5 MB, 20.000 movimenti, 2.000 serie, 100 categorie, 10.000 anticipi, 20.000 rimborsi.

Avvisi giornalieri locali opzionali; Android può ritardarli. La demo è separata dall’archivio reale. Nessuna sincronizzazione o API esterna.

## Sviluppo e limiti delle verifiche

Apri il progetto in Android Studio con JDK 17+ e SDK 35. Nel cloud riutilizza il checkout esistente e `source tools/cloud-env.sh`.

```
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Rendering dai componenti Android reali nei runtime Robolectric, con fixture isolate; non fotografie di un telefono. I menu sono disegnati dai loro View Android sopra il rendering della pagina. L’area della tastiera è verificata tramite insets IME simulati: non è stato avviato o fotografato un vero servizio tastiera. Non sono dichiarati test fisici di installazione, animazioni, gesti o puntualità delle notifiche.

La firma legge `V2_SIGNING_PROPERTIES` fuori da Git. La chiave originale resta privata nell’ambiente: nessun keystore o password è incluso nel repository/ZIP sorgenti. Il precedente download privato della firma non è stato completato sul PC. **Non creare una chiave sostitutiva** per gli aggiornamenti della V2/V2.1.
