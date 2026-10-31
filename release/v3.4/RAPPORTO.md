# Posso spenderlo? — V3.4.0 RC

Data: 31 ottobre 2026. Release candidata all'uso sul telefono, non dichiarata già installata o approvata graficamente dal proprietario.

## Provenienza e modifiche

- Base Android verificata: `feature/v3.3-concept-match`, `cfb901983625eefc93aec8fc5c6604de05c6bb35`, senza commit remoti successivi.
- Prototipo approvato: `design/ux-rebuild-phase-1.2`, `a49b5f4cb4d6af036486edb67e2a7715ea45d221`. Recuperata solo la directory `design/ux-rebuild`, senza merge indiscriminato.
- Sviluppo su `feature/v3.4-native-ux-rebuild`. `main` non modificato.
- Milestone A: commit `45f69e9`, Home/navigazione/menu verificati con rendering nativi prima degli altri flussi. Integrazione B/C/D: `e88ced9`; rifiniture e collaudo successivi nella storia del branch.

Home, anello, quattro destinazioni, menu Registra a gruppi, form/categorie, rate, date, storico, calendario, anticipi, simulazione, onboarding, analisi e impostazioni sono View Android reali collegate al dominio Kotlin. Non viene eseguito HTML/JS nell'app.

## Architettura

Activity contenitore e gestione lifecycle; composizione separata in InputScreens, LedgerScreens, CreditScreens, AnalysisScreens, SettingsScreens e OperationActions. Controller NativeHome, NativeDateSheet, NativeSimulation, NativeOnboarding; componenti riutilizzabili Design/FinancialRows e custom View per grafici. Stato delle bozze e delle selezioni in Bundle; onboarding interrotto in preferenze private. Si conserva lo stack platform Android esistente: nessuna migrazione a Compose o nuove librerie UI. Non sono stati introdotti ViewModel Jetpack, Fragment o ViewBinding: i layout restano programmatici e le schermate separate evitano di espandere il precedente monolite. Le liste mantengono il limite di 300 risultati e non sono virtualizzate con RecyclerView; negli archivi grandi occorre restringere la ricerca.

I comandi di registrazione hanno un token conservato nella bozza e registrato nella stessa transazione delle scritture. Errore → rollback, token non consumato; doppia conferma → una sola registrazione. La conferma di un pagamento è condizionata allo stato ancora previsto. La navigazione e la semplice selezione del giorno non contabilizzano denaro.

## Integrità finanziaria

Liquidità = base + effettivi − anticipi + rimborsi. Spendibile = liquidità − riserva − uscite aperte nell'orizzonte, inclusi arretrati e oggi. Entrate future e crediti non riscossi esclusi. Rimborsi separati da entrate personali nei riepiloghi e nei grafici.

- 1.200 − 300 di riserva − 380 di impegni = 520 euro.
- Conferma del pagamento di 300: liquidità 900, impegni 80, spendibile ancora 520; nessuna seconda sottrazione.
- Anticipo 50, rimborso 20: residuo 30. Gli scaduti con residuo sono anche aperti.
- 600 totali/3 = 200×3; 600 ogni rata/3 = 1.800; 100/3 = 33,34+33,33+33,33.
- Modifica piano: pagamenti effettuati invariati, scadenze future aggiornate mantenendo gli ID; cutoff e soppressioni conservati. I vecchi piani EACH possono mantenere pagamenti storici anche oltre un conteggio ridotto. Nei piani TOTAL non si possono escludere indici già effettuati o ridurre il totale sotto i pagamenti e le quote minime residue.
- Simulazione 600 su liquidità 1.200: 600→300→220, riserva 300, margine −80. Incasso incerto 200 solo nella linea scenario separata. Motore Simulator puro, nessun accesso al database e nessun movimento creato.

## Migrazione e backup

SQLite 4→5: metadati orizzonte FIXED/ROLLING, giorni 30/60/90, onboardingCompleted, categorie quickRank, series amountMode/totalCents, tabella installments e tabella locale commands. Upgrade storico da versioni 1/2/3 conservato. Gli upgrade vengono eseguiti nella transazione di SQLiteOpenHelper: nessuna reinizializzazione distruttiva o reset dopo un errore.

Gli orizzonti precedenti restano FIXED con la data originale, anche se trascorsa. Il calcolo conserva comunque le uscite arretrate e di oggi. ROLLING usa la data locale e si aggiorna ogni giorno; al ritorno in foreground dopo cambio giorno viene aggiornato il rendering. Nuove installazioni: guida in quattro passi; installazioni migrate: guida già completata, dati conservati. La liquidità iniziale non è una falsa entrata.

JSON 6 esporta metadati, ordine categorie, modalità/totale/quote dei piani, cutoff, tombstone, anticipi e rimborsi. Importa JSON nativi 2–6; vecchi piani normalizzati come EACH, vecchi orizzonti FIXED. Per TOTAL sono obbligatorie quote positive, conteggio e somma coerenti; non si usa l'importo nominale come ripiego. Formati del prototipo non sono backup Android supportati.

Validazione prima della sostituzione, scritture atomiche, copia locale `pre-import.json` esportabile prima dell'importazione. Il backup è JSON in chiaro e deve essere conservato privatamente. Lo stato temporaneo della guida e i token dei comandi non sono esportati; il completamento della guida sì.

## Collaudo effettivo

Risultati numerici e riepilogo macchina: `VERIFICA.json`. Comando:

```sh
source tools/cloud-env.sh
./gradlew --no-daemon -Pkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Suite preesistenti mantenute; aspettative aggiornate per il nuovo schema e selezione esplicita EACH nei test del vecchio flusso. Nuovi test: calcoli obbligatori, simulatori senza scritture, quote/rounding/edit/ID, rollback e retry/doppio salvataggio, orizzonte mobile/fisso, categorie ordinate, backup, migrazione reale degli schemi 1/2/3/4, errore a metà upgrade con rollback, rotazione/bozze/date, onboarding interrotto e idempotente, navigazione, rendering e menu/data a testo ingrandito.

Test JVM/Robolectric su API 26, 28 e 35 secondo la suite; rendering Native Graphics su API 35, 320×640, 360×800, 411×891, temi chiaro/scuro e font fino a 200%. Simulazione degli insets IME nelle suite responsive. I PNG derivano da layout e disegno di View Android: non sono HTML travestito da app.

## Verifica visiva e differenze residue

`GALLERIA.html`, `screenshots/`, `comparisons/Confronti-V3.4.pdf` e manifest con hash delle immagini. Confronti affiancati con prototipo approvato e, per le prime sei schermate, concept originale con il ritaglio già documentato nella Fase 1.2. Nessun punteggio artificiale di somiglianza. Elenco/Calendario verificati distinti tramite hash oltre alla navigazione sugli stessi dati.

Controllati Home/menu nei due temi prima dell'integrazione; successivamente spesa, categorie, date, rate, analisi, impegni, anticipi e simulatore. Le fixture principali riproducono liquidità 1.200, riserva 300, impegni 380, entrate mensili 2.450 e spese 1.320. Nei confronti di form/rate/simulatore alcuni importi dimostrativi differiscono dagli screenshot HTML: il collaudo nativo include 100 euro/3 rate e una spesa simulata di 600 euro; queste differenze numeriche non sono presentate come difetti di rendering.

Adattamenti/differenze espliciti:

- Roboto di sistema al posto di Arial/Helvetica del browser; pesi, baselines, ombre e antialiasing non identici.
- Icone vettoriali Kotlin semplificate; alcune righe di storico e impegni privilegiano badge testuali rispetto ai medaglioni del concept. Badge Home compatti nella riga data, anziché identico componente CSS.
- Spaziature più compatte in alcune schermate; formato valuta italiano con simbolo finale in parte dei dettagli, simbolo iniziale nei blocchi principali. Non viene dichiarata identità pixel per pixel.
- Selettori mese/anno sono Spinner Android dentro il calendario personalizzato; a testo grande mese e anno sono su righe distinte e si usa l'elenco giorni accessibile. Celle della griglia su 320 dp sotto 44 dp di larghezza: l'alternativa a elenco mantiene target ampi.
- Le etichette della barra inferiore sono limitate a 12 dp per evitare troncamenti a font 200%; contenuto e moduli seguono l'ingrandimento. Le aree di tocco restano ampie.
- I grafici riflettono eventi reali a gradini; non si inventano punti per riprodurre una curva illustrata. Il simulatore conserva controlli di periodo e scenario visibili nel contenuto scorrevole.

Nessun emulatore Android o telefono collegato utilizzabile in questo ambiente; non eseguiti test strumentali su dispositivo, installazione fisica, fotocamera screenshot o tastiera reale. Il tentativo di interrogazione adb è limitato dal profilo filesystem della macchina. La prova finale su telefono è necessaria e guidata da INSTALLAZIONE.md. Lint può lasciare avvisi non bloccanti, riportati integralmente nei file di verifica.

## Firma e distribuzione

Application ID invariato `it.privato.disponibile.v2`, versionName 3.4.0, versionCode 340. Firma originale letta dalla directory privata esterna al repository. Certificato pubblico atteso SHA-256:

`3aae07d6255d8b04a893ef058275c40e4e0d5f7cfd1a6b15328647c7a0f65568`

Verificato da apksigner sull'APK aggiornato e confrontato con V2 e V3.3; dettagli pubblici in FIRMA.txt. Nessuna nuova chiave creata. Nessuna chiave, password o ZIP privato caricato su GitHub o incluso nello ZIP sorgenti. Il recupero della copia privata sul PC non viene dichiarato completato.

APK e sorgenti disponibili nella cartella `release/v3.4` del branch. SHA-256 in SHA256.txt. Esportare un backup, installare l'APK come aggiornamento e non disinstallare la versione attuale. Dopo la prova dell'utente non viene avviata automaticamente un'altra versione.
