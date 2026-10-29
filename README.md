# Posso spenderlo? 3.3 — Concept Match

App privata Android 8+, Kotlin nativo e SQLite: offline, nessun account, server, analytics, permesso Internet o costo ricorrente.

**[APK di prova V3.3](release/v3.3/Posso-spenderlo-V3.3-test.apk)** · [SHA-256](release/v3.3/SHA256.txt) · [Sorgenti ZIP](release/v3.3/Posso-spenderlo-V3.3-progetto.zip) · [Rendering Android](release/v3.3/screenshots/) · [Confronti affiancati](release/v3.3/comparisons/Confronti-V3.3.pdf) · [Verifiche](release/v3.3/VERIFICA-V3.3.txt).

Branch `feature/v3.3-concept-match`, base V3.2 `b5e296a`. Nessun merge su `main`: la V3.3 è una proposta da provare e revisionare.

Su GitHub apri il file APK e premi **Download raw file**. Tutti i sorgenti sono anche nelle cartelle del repository.

## Provare l’aggiornamento sopra la versione installata

1. Nella versione installata, esporta un backup e conservalo sul dispositivo o PC.
2. Scarica il nuovo APK e aprilo sul telefono. Autorizza questa fonte se Android lo richiede.
3. Premi **Aggiorna/Installa**. **Non disinstallare la versione precedente**, perché perderesti il suo database.

Identità invariata `it.privato.disponibile.v2`, versione `3.3.0` / codice 330. Il certificato viene confrontato con gli APK V2 e V2.1 prima della consegna. Schema SQLite 4 e backup JSON 5 invariati: non è necessaria una nuova migrazione. Le migrazioni storiche da V2/V2.1/V3.0 e i cutoff delle ricorrenze restano coperti dai test. Nessun reset o riclassificazione automatica dei movimenti.

La V1 resta un’app separata. L’installazione fisica sul telefono del proprietario non viene dichiarata come eseguita dal cloud.

## Rifiniture V3.3

Anello e ombre più morbidi, quick actions con icona, importo con valuta separata dal testo editabile, categorie circolari e Data/Rate in una capsula. Filtri, approfondimenti e rate avanzate sono secondari. Il grafico mantiene il saldo reale e mostra le date del tratto visibile.

131 test superati; build release riuscita; lint: 0 errori e 30 avvisi. [Autocritica per le sei schermate](release/v3.3/AUTOCRITICA-V3.3.txt): restano differenze compositive e iconografiche, non viene dichiarata equivalenza al concept.

## Sei composizioni dal concept

| Riferimento | Implementazione Android |
| --- | --- |
| Home | Ciao!, anello turchese dello spendibile, pannelli mensili entrate/uscite, scadenze compatte, quattro destinazioni e + centrale. Simulatore, crediti, calcolo e spesa veloce restano accessibili. |
| Nuova spesa | Importo su azzurro tenue, griglia 4×3 di categorie con icone vettoriali colorate, descrizione facoltativa, data, accesso alle rate, Salva spesa blu sfumato. |
| Categorie | Ricerca, preferiti orizzontali, righe con icona e cuore sottile, filtri discreti, recenti e modifica tramite pressione prolungata. |
| Analisi | Mese/Trimestre/Anno, anello, legenda con percentuali/importi e andamento blu della liquidità. Previsioni, colonne e confronti rimangono in Approfondimenti. |
| Rate | Radio per nessuna/2/3/6/12 rate; continua e numero manuale 1–120 in Altre opzioni; Applica rate prepara il piano, il salvataggio è separato. |
| Conferma | Finestra centrale arrotondata, illustrazione vettoriale, sfondo attenuato, due azioni contestuali. |

La palette chiara usa bianco, azzurro, blu e turchese; lo scuro usa le stesse gerarchie con contrasti adeguati. Nessuna dipendenza o API esterna aggiunta. Impostazioni, crediti, calendario, simulatori e backup usano gli stessi componenti.

L’anello rappresenta `max(0, spendibile) / liquidità positiva`, limitato a 100%; con liquidità non positiva non mostra percentuali. Il numero centrale resta sempre lo spendibile reale, anche negativo. Un tocco apre il calcolo con impegni e riserva. Non viene inventato un budget-obiettivo.

Le finestre di Analisi sono mobili di 1/3/12 mesi, esplicitate in pagina. I confronti usano periodi equivalenti e lo stesso giorno finale per il periodo in corso; periodi futuri o senza spese non producono confronti. Il saldo include anticipi/rimborsi; le distribuzioni delle spese personali li escludono.

## Adattamenti dichiarati rispetto alle immagini

- Valori, date e proporzioni economiche dipendono dai dati registrati; le immagini di confronto usano fixture DEMO isolate, mai i dati dell’utente.
- Importo rate sempre **per ciascuna rata**, senza dividerlo. Totale = importo × numero; per interrompere una serie esistente rimane la gestione con conferma e storico preservato.
- I colori delle categorie predefinite sono una presentazione UI; nomi/ID/associazioni/colori salvati restano invariati. Categorie e colori personalizzati sono conservati.
- Linea del saldo a gradini, per non suggerire movimenti intermedi inesistenti. Grafici vuoti e negativi sono reali, senza dati fittizi.
- Le schermate scorrono su telefoni piccoli/font grandi; le icone sono vettori ricostruiti, non pixel estratti dal mockup. Cornice hardware e indicatori di sistema illustrati non sono disegnati dentro l’app.
- Fondo dei dialoghi ammorbidito con una copia ridotta temporanea della UI in memoria e oscuramento. Non è una sfocatura del compositore hardware; nessuna immagine viene salvata o trasmessa. Animazioni brevi e disattivabili tramite le impostazioni Android; ombre native indesiderate rimosse.
- I comandi aggiuntivi preservano le funzioni V3.1 non rappresentate nel concept. Recenti e inserimento manuale rimangono disponibili.

Le immagini originali restano in `release/v3.2/references`; i sei confronti e l’evoluzione V3.2/V3.3 sono in `release/v3.3/comparisons`. Per riprodurli, esegui i test e `python tools/compare_concept.py` (matplotlib/Pillow, solo strumenti di sviluppo).

Dopo l’installazione verifica sul telefono: pressione/rilascio del + e Salva, apertura/chiusura del popup, scelta categoria, tastiera e scorrimento a font grandi, in entrambi i temi. Gli screenshot cloud non provano la scomparsa di un artefatto specifico del dispositivo.

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
