# Revisione V3.4 — RC2

Base: `a8a8cdfb2bab608d219bed0c3093bac1a13ed73d`, branch `feature/v3.4-native-ux-rebuild`. Riferimento grafico approvato: `a49b5f4`. Nessun nuovo progetto, redesign generale, tag definitivo o merge su main.

## Correzione dell'orizzonte

La riga in SettingsScreens passava a `pick()` un callback che aggiornava solo la data. In realtà quel callback era ignorato: la conferma in `showPicker()` impostava già FIXED. Il percorso funzionante non giustificava però il codice ambiguo, e la riga mostrava la data fissa memorizzata anche in modalità mobile.

Rimosso il callback inutilizzato: `pickHorizon()` ha un obiettivo esplicito; la conferma chiama il comando transazionale `Store.fixedHorizon()`. I pulsanti 30/60/90 chiamano `Store.rollingHorizon()`. La schermata dichiara la modalità attiva e visualizza la data effettiva; Home e calcolo usano la stessa `effectiveHorizon()`.

Nessuna conversione automatica delle impostazioni storiche. Passare a ROLLING conserva la vecchia data fissa memorizzata; solo una nuova scelta esplicita FIXED la modifica. Gli arretrati e i pagamenti di oggi restano protetti, anche con una data storica trascorsa. SQLite resta 5, JSON resta 6: nessuna nuova migrazione o riscrittura dei dati.

Introdotto un orologio locale sostituibile nei test. In produzione usa la data e il fuso del dispositivo, senza cambiare l'orologio di sistema. Permette di verificare realmente il ritorno in foreground dopo il cambio giorno e di riprodurre confronti con data fissa.

## Riallineamento mirato

- **Registra:** titoli di gruppo del prototipo, superfici evidenziate per spesa/pagamento, icone vettoriali, descrizioni e frecce. Intestazione/chiusura fisse e contenuto scorrevole. Conservato il collegamento secondario Entrata prevista, già presente nell'app.
- **Impostazioni:** sezioni Il tuo margine/I tuoi dati, due card raggruppate con icone e divisori. Percorsi e funzioni esistenti conservati.
- **Anticipi da recuperare:** eliminati Cerca crediti/Filtri crediti/Nessun credito. Hero generale non spendibile, aperti generali, rimborsi mensili generali, conteggi delle schede riferiti all'ambito filtrato, residuo dei soli risultati. Scaduti inclusi negli aperti; badge Aperto · scaduto e Rimborso parziale, icona e CTA fissa. I dati del progetto restano nei dettagli e nei filtri.
- **Analisi:** proporzioni del donut, ordine e colori coordinati con la legenda approvata, maggiore respiro delle righe e dell'andamento della liquidità. Solo importi reali, nessuna curva o dato inventato.
- **Onboarding:** quattro introduzioni con icona e indicatore del passo, area importo, impegni raccolti con editor secondario, hero centrale del margine finale e CTA fissa. Staging/ripresa/idempotenza conservati; il riepilogo tiene conto degli impegni già presenti. Non viene riproposto automaticamente agli utenti già configurati.
- **Movimenti:** ricerca prima dei filtri, filtri rimovibili, date separate e gerarchia delle righe con icone; gli stati finanziari espliciti sono conservati.

Home, selettore date e altre schermate non sono stati riprogettati. I loro accessi alla data sono stati ricondotti all'orologio verificabile; geometria e interazioni rimangono quelle della RC precedente.

## Confronti affidabili

`tools/qa-rc2/capture-prototype.cjs` legge i file approvati senza modificarli. Esporta la fixture HTML in `app/src/test/resources/rc2/fixture.json`; il test Android la carica direttamente. Data locale: **31 ottobre 2026**. Liquidità 1.200, riserva 300, impegni 380, spendibile 520; anticipato 50 e rimborsato 20; stessi movimenti, categorie, importi e date.

I confronti dei piani usano entrambi 600 euro, tre rate e la stessa modalità TOTAL o EACH; il simulatore usa 600 euro, 30 giorni e scenario entrate disattivato. Corrispondono anche filtri, tema, mese, passo della guida e valori inseriti. Le selezioni sono registrate nei manifest HTML e Android.

`tools/qa-rc2/compare.py` rifiuta differenze fra i manifest di dati/stato prima di produrre gli affiancamenti. Viewport **360×800** per entrambi; immagini non ridimensionate né ritagliate. Rimosso solo il contenitore desktop del telefono dal rendering HTML, senza cambiare componenti o contenuti. Hash delle immagini e della fixture nel manifest.

30 confronti: 15 schermate/stati nei due temi, inclusi tutti e quattro i passi della guida e i due significati dell'importo rateale. Rendering aggiuntivi 320×640 al 200% e 411×891 al 150%, con menu e contenuti scorrevoli. I PNG Android sono disegni di View Android tramite Robolectric Native Graphics API 35, non mockup o screenshot HTML.

## Test e build

Le 156 esecuzioni preesistenti sono conservate. Aggiornati soltanto i selettori di etichette rinominate e l'aspettativa della descrizione nel menu; nessuna asserzione economica rimossa o indebolita.

7 nuove esecuzioni portano il totale a **163**:

- Matrice completa fra FIXED e ROLLING 30/60/90, attraverso la UI, persistenza, ricreazione dell'Activity, ritorno dopo cambio giorno, Home e backup: API 26 e 35.
- Backup storico con data fissa trascorsa e protezione di arretrati/oggi: API 26 e 35.
- Rendering abbinati su fixture condivisa, senza scritture finanziarie; menu, CTA e schermate riviste a testo grande; anticipi scaduti/filtrati con totale generale invariato: API 35.

Restano eseguite le regressioni di migrazione SQLite 1–4, backup 2–6, rate TOTAL/EACH, centesimi e storico, doppio salvataggio/rollback, anticipi/rimborsi, simulatore senza scritture, navigazione, bozze/rotazione, temi, dialoghi, tastiera simulata e layout piccoli.

Comando: `source tools/cloud-env.sh`, poi `./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.

Esito finale, avvisi lint, identificativo dei sorgenti e controllo della firma: VERIFICA.json, BUILD.txt, LINT.txt e FIRMA.txt.

## Differenze residue e limiti

- Roboto e disegno vettoriale Kotlin differiscono da Arial/SVG per peso, baselines e antialiasing. Ombre, gradiente e blur del backdrop non sono identici al browser; non viene dichiarata identità pixel per pixel.
- Nel menu resta un accesso in più all'entrata prevista, per conservare la funzione precedente. I conteggi Anticipi sono immediatamente sotto le schede anziché dentro la stessa etichetta; a testo grande badge e righe si dispongono verticalmente.
- L'app conserva i pulsanti Passo precedente e Riprendi più tardi nella guida; Impostazioni mostra informazioni della RC e accesso alla demo al posto delle istruzioni proprie del prototipo web.
- Ricerca/categorie nello storico mantengono i percorsi nativi esistenti. Le categorie rapide possono avere un ordine suggerito diverso a parità di frequenza: la revisione non ne modifica l'algoritmo o le preferenze.
- Rimangono le differenze già note di Home/date/rate/simulatore, incluse nei nuovi confronti a dati uguali senza introdurre un altro redesign. Le liste non sono virtualizzate e mantengono il limite di 300 risultati.
- Nessun emulatore o telefono utilizzabile: non eseguiti installazione fisica, test strumentali o tastiera reale. Gli insets IME sono simulati nei test. Il collaudo finale sul dispositivo è descritto in COLLAUDO-TELEFONO.md.

## RC aggiornata

Application ID `it.privato.disponibile.v2` invariato. Versione **3.4.1-rc2**, **versionCode 341**, superiore alla RC 340. Firma esclusivamente con la chiave originale esterna al repository, confrontata con V2 e RC 340. Nessuna nuova chiave, password o archivio privato pubblicato. APK e sorgenti ZIP in questa cartella; SHA-256 in SHA256.txt. La RC 340 e i suoi artefatti restano disponibili nel percorso precedente.
