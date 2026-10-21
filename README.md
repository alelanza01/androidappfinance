# Posso spenderlo? 3.1

App privata Android 8+, Kotlin nativo e SQLite: offline, nessun account, server, analytics, permesso Internet o costo ricorrente.

**[APK V3.1](release/Posso-spenderlo-V3.1.apk)** · [SHA-256](release/SHA256-V3.1.txt) · [Sorgenti ZIP](release/Posso-spenderlo-V3.1-progetto.zip) · [Rendering Android](release/screenshots-v3.1/) · [Verifiche](release/VERIFICA-V3.1.txt).

Su GitHub apri il file APK e premi **Download raw file**. Tutti i sorgenti sono anche nelle cartelle del repository.

## Aggiornare la V3.0

1. Nella versione installata, esporta un backup e conservalo sul dispositivo o PC.
2. Scarica il nuovo APK e aprilo sul telefono. Autorizza questa fonte se Android lo richiede.
3. Premi **Aggiorna/Installa**. **Non disinstallare la V3.0**, perché perderesti il suo database.

Identità invariata `it.privato.disponibile.v2`, versione `3.1.0` / codice 310. Il certificato viene confrontato con gli APK V2 e V2.1 prima della consegna. La migrazione SQLite 3 → 4 aggiunge icone e preferiti alle categorie e completa il catalogo, conservando ID, nomi, colori e associazioni esistenti. Nessun movimento viene riclassificato. Sono coperte anche le migrazioni da V2/V2.1 e tutte le tre modalità di eliminazione delle ricorrenze.

La V1 resta un’app separata. L’installazione fisica sul telefono del proprietario non viene dichiarata come eseguita dal cloud.

## Nuova esperienza

- **Home:** spendibile protagonista su una superficie salvia, spiegato da liquidità/impegni/riserva, spesa veloce in un tocco, simulatore e prossimo pagamento. Dettaglio del calcolo, simulatore e analisi si aprono su richiesta.
- **Movimenti:** storico per giorno, ricerca indipendente da quella dei crediti, filtri per tipo/stato e categoria. Anticipi e rimborsi hanno etichette distinte.
- **Impegni:** lista o calendario mensile, arretrati, entrate attese, restituzioni non garantite e programmazioni.
- **Crediti:** anticipi, residui, stati, filtri per soggetto/progetto/mese, dettaglio e storico dei rimborsi.
- **Analisi:** un grafico alla volta — spese, liquidità o mesi — con controlli contestuali e selezione dei dati.
- **Impostazioni:** accessibili dall’icona in Home; Regole del saldo, Aspetto, Avvisi, Categorie e Backup hanno pagine separate.

**Registra** o **+** apre un menu breve. Gli inserimenti complessi sono pagine con campi etichettati, tastiera numerica, opzioni avanzate espandibili e Salva sopra l’area della tastiera. Una spesa ordinaria richiede solo l’importo; la descrizione è facoltativa e lo storico usa il nome della categoria se assente: tipo, stato, data e categoria hanno valori iniziali modificabili. Le bozze sopravvivono alla ricreazione dell’attività; non vengono registrate prima del salvataggio.

Design condiviso: neutri minerali, verde salvia e accenti freddi; tema scuro verde grafite; liste con gerarchia tipografica, menu inferiori per decisioni brevi e feedback di pressione discreto. Le impostazioni di testo ingrandito restano attive sui contenuti; etichette dei controlli fissi rimangono compatte e leggibili.

## Direzione di prodotto

Identità personale distinta da Elledue: salvia, neutri minerali, accenti freddi e scuro verde grafite. Gerarchia editoriale con un solo numero dominante in Home, riepiloghi compatti e timeline prudente negli impegni. Il simulatore è evidenziato in Home e mostra l’impatto sul margine, senza registrare spese. L’allegato concept fornito contiene effettivamente schermate Elledue, in contrasto con il suo LEGGIMI: non è stato copiato. Prevale il brief funzionale Posso spenderlo?.

## Novità 3.1

- Pulsanti piatti: stile nativo implicito, animatore Material, elevazione, tint e outline rimossi; ripple e maschera condividono gli angoli. Stati disabilitati espliciti.
- Categorie: 27 scelte comprese quelle precedenti, icone vettoriali uniformi, ricerca, griglia per gruppi, Recenti e Preferite. Tieni premuto per modificare; puoi creare una categoria senza perdere la bozza della spesa.
- Rate: continua oppure 1–120 rate, controllo ± e numero digitabile. Il totale è importo di ciascuna rata × numero; nessuna divisione o arrotondamento.
- Conferme: due azioni esplicite; uscita con modifiche, eliminazioni, pagamenti/incassi e importazione. Nessun «Conferma» generico o terzo «Chiudi».
- Analisi: categorie principali con quote, confronto con gli stessi giorni del mese precedente (oppure mesi completi), pagamenti ricorrenti effettuati e variazione reale della liquidità. Confronto assente quando mancano dati.
- Home con liquidità e riserva; riepiloghi mensili nei movimenti e impegni; stato e recuperi nei crediti. Transizioni 140–160 ms disattivate se Android disabilita le animazioni.

Dopo l’aggiornamento controlla sul telefono **Registra**, una pressione prolungata, l’apertura di un menu, una conferma di uscita, la categoria selezionata e il controllo rate ai limiti 1/120: in entrambi i temi gli angoli devono restare puliti senza ombre ritagliate. Gli screenshot nel repository sono rendering Android reali del cloud; non dimostrano da soli la scomparsa dell’artefatto specifico del tuo dispositivo. L’audit ha confermato la combinazione di sfondo personalizzato e animatore/outline nativo della V3, ora rimossa.

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

Backup JSON formato 5, compatibile in importazione con formati 2, 3 e 4; conserva anche icone e preferiti. Validazione prima della sostituzione transazionale e copia locale precedente all’importazione, esportabile dalla pagina Backup. JSON non cifrato: contiene dati personali. Limiti: 5 MB, 20.000 movimenti, 2.000 serie, 500 categorie, 10.000 anticipi, 20.000 rimborsi.

Avvisi giornalieri locali opzionali; Android può ritardarli. La demo è separata dall’archivio reale. Nessuna sincronizzazione o API esterna.

## Sviluppo e limiti delle verifiche

Apri il progetto in Android Studio con JDK 17+ e SDK 35. Nel cloud riutilizza il checkout esistente e `source tools/cloud-env.sh`.

```
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Rendering dai componenti Android reali nei runtime Robolectric, con fixture isolate; non fotografie di un telefono. I menu sono disegnati dai loro View Android sopra il rendering della pagina. L’area della tastiera è verificata tramite insets IME simulati: non è stato avviato o fotografato un vero servizio tastiera. Non sono dichiarati test fisici di installazione, animazioni, gesti o puntualità delle notifiche.

La firma legge `V2_SIGNING_PROPERTIES` fuori da Git. La chiave originale resta privata nell’ambiente: nessun keystore o password è incluso nel repository/ZIP sorgenti. Il precedente download privato della firma non è stato completato sul PC. **Non creare una chiave sostitutiva** per gli aggiornamenti della V2/V2.1.
