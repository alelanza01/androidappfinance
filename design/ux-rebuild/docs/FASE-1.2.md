# Fase 1.2 — funzioni e decisioni di prodotto

Base verificata: `57b5d76475532514ed05d1f2bcd6defc44d90632`. Branch dedicato `design/ux-rebuild-phase-1.2`. Nessuna modifica al codice Android, agli APK, alla firma o al database. Il prototipo resta da approvare.

## Sette interventi utilizzabili

1. **Rate in due modalità.** Nuovi piani finiti: Importo totale predefinito. Selettore esplicito totale/ogni rata, riepilogo live e dettaglio espandibile con frequenza, prima scadenza e quote effettive. Continua forza ogni rata. Tornare a Nessuna rata ripristina lo stato singolo precedente, non trasforma silenziosamente una spesa pagata in prevista. La data non viene spostata arbitrariamente: se è futura, per registrare un pagamento effettuato va corretta.
2. **Periodo protetto.** Home indica la data e apre 30/60/90 giorni mobili o data fissa. Scadenze oltre il periodo esplicitamente consultabili. Arretrati aperti considerati; incassi e crediti non diventano spendibili. Il giorno locale è aggiornato periodicamente e al ritorno alla scheda. La Guida offre una prova esplicita del giorno successivo.
3. **Primo avvio.** Quattro passi reali: liquidità, riserva (anche zero), pagamenti (anche nessuno, più inserimenti e ripetizione), risultato. Continua più tardi e ripresa; i dati già presenti non vengono cancellati. Su archivio popolato viene aggiunta solo l'eventuale differenza di liquidità, non una nuova entrata. La Guida consente anche una prima installazione demo vuota con conferma.
4. **Categorie rapide.** Fissate in ordine manuale, poi preferite, più frequenti nelle spese effettuate e resto del catalogo. Nessun duplicato o nuovo ID. Fissa/Sblocca e frecce su/giù accessibili; ripristino dell'ordine di base delle fissate/preferite. L'uso effettivo continua a suggerire le frequenti. Ogni bozza conserva una fotografia dell'ordine; gli aggiornamenti valgono dalla prossima bozza.
5. **Calendario operativo.** Elenco e griglia rimangono viste dello stesso archivio. P/I/R/! oltre ai colori, Oggi, giorno selezionato e riepiloghi. Toccare un giorno apre pagamenti/incassi/restituzioni attese con dettaglio, modifica, conferma effettiva o rimborso e nuova scadenza nella data selezionata. Nessuna contabilizzazione toccando una data. Incasso ricevuto e rimborso ricevuto restano distinti.
6. **Previsione della liquidità.** Simulatore live con grafico a gradini, soglia riserva, punti selezionabili anche con pulsanti di dimensioni utili e dettaglio delle operazioni aggregate per giorno. Primo punto: liquidità reale meno spesa ipotetica, non spendibile meno impegni. Vista 7/30/60 giorni o personalizzata indipendente dall'orizzonte protetto. Scenario incassi facoltativo, viola tratteggiato e non garantito. Nessun Salva, nessuna transazione prodotta dalla simulazione.
7. **Date condivise.** Calendario disegnato con lo stesso linguaggio grafico: lunedì, mese/anno diretti, frecce, oggi/selezione, scorciatoie coerenti, Annulla e Conferma. Date future disabilitate per operazioni effettive; rimborsi non anteriori all'anticipo. Navigazione a frecce, Home/End, PageUp/Down, Ctrl+PageUp/Down; elenco alternativo per target più grandi. Usato anche per orizzonte, configurazione, simulatore e periodo Analisi. Date-only senza conversione del giorno locale attraverso UTC.

## Regole economiche verificabili

- 600 € totale /3 = 200,200,200; 600 € ogni rata ×3 =1.800.
- 100 € totale /3 =33,34 +33,33 +33,33. Divisione intera q e resto r: i primi r indici ricevono 1 centesimo aggiuntivo. Rifiuto di quote zero. Gli stessi valori sono salvati nelle scadenze, mostrati nel calendario e utilizzati dalla previsione.
- Modifica di un totale con rate pagate: gli eventi pagati mantengono ID, importo e data; si distribuisce solo il residuo tra gli indici ancora non effettuati. Se il totale non è compatibile viene rifiutato. I piani storici senza nuova modalità restano “ogni rata”.
- Esempio richiesto: liquidità1.200, riserva300, impegni300+80, spendibile520. Simulando600: margine−80, liquidità600→300→220; incasso incerto200 escluso dal prudente. Solo la linea alternativa mostra420 dopo il suo arrivo ipotetico.
- Anticipi non rimborsati non sono cash; un rimborso ricevuto modifica il saldo una sola volta e non entra nelle spese personali/guadagni.

## Organizzazione e limiti della demo

Il prototipo conserva solo dati DEMO nel localStorage del browser (fallback in memoria quando lo storage è indisponibile). Ricaricare non è più il comando di reset: usare Ripristina demo. L'onboarding incompleto e le preferenze rapide sono recuperabili nello stesso browser/file. Non usare dati reali. Il comportamento di localStorage per un file scaricato dipende dal browser; la persistenza via HTTP locale è stata testata, il PC Windows dell'utente no.

Per riproducibilità, la QA fissa il 31 ottobre 2026. Nell'uso normale il giorno è quello locale del browser e la demo iniziale viene traslata coerentemente rispetto al giorno di apertura. Le date/metriche possono differire dalle catture. La Guida permette di avanzare esplicitamente il giorno DEMO; ricaricare torna all'orologio reale mantenendo le registrazioni. Non è un ambiente finanziario di produzione.

Le ricorrenze continue sono materializzate nella demo fino a dieci anni, non infinite; la rappresentazione non è una promessa di un servizio perpetuo. L'Android futuro conserverà materializzazione incrementale e limiti controllati. I backup sono JSON DEMO, indipendenti dai backup reali Android, con quote e modalità validate.

Le nuove parti sono isolate in `dates.js` (date), `finance12.js` (modello demo) e `features12.js` (interazioni) mantenendo i componenti della 1.1. È un'organizzazione di prototipazione; l'implementazione Android avrà schermate e stato tipizzati, senza portare questi adapter JavaScript in produzione.

## Audit UX dei dieci percorsi

| Obiettivo | Percorso visibile | Recupero/errori |
|---|---|---|
| Spendibile | Home, numero dominante | Negativo distinto, denominatore cash |
| Scadenze protette | Indicazione sotto ring → periodo | Mobile/fisso nominati, pagamenti esclusi consultabili |
| Spesa | + → Registra spesa → importo → Salva | Descrizione facoltativa, bozza conservata |
| Categoria | Griglia o capsula → scelta | Preferite/fissate secondarie; nessuna perdita del form |
| Data | Campo → calendario → Conferma | Annulla conserva, limiti contestuali espliciti |
| Rate | Rate → Totale/Ogni rata → scelta → Conferma | Totale live, dettaglio espandibile, Salva separato |
| Scadenza | Impegni → giorno/dettaglio → conferma | Previsto distinto da effettuato, doppi invii bloccati |
| Simulazione | Home → Simula → cifra → scadenza | Linea prudente e scenario distinti, nessun Salva |
| Anticipo | Home → Anticipi → dettaglio → rimborso | Residuo/storico, eccedenza e cronologia rifiutate |
| Primo avvio | Guida o Impostazioni → configurazione | Ripresa, zero valido, conferma finale unica |

## Autocritica

La direzione1.1 è conservata, ma Rate richiede più spazio per la scelta di significato dell'importo; a320 o font grande scorre. Calendario e grafico hanno un'alternativa a righe/pulsanti per non imporre piccoli target. Non tutti i contenuti avanzati stanno nella prima vista e questo è intenzionale; CTA e chiusure devono rimanere raggiungibili.

Restano differenze di font, icone/illustrazioni, profondità e proporzioni rispetto al concept. Il grafico prudente scende a gradini perché rappresenta pagamenti reali; la torta segue le quote del dataset. Il nuovo calendario è HTML custom, non una prova del dialogo Android. Non sono stati eseguiti test con persone o su Android reale; tastiera, screen reader, back di sistema, inset, notifiche e firma richiedono verifica nativa successiva. Il test automatico non attribuisce approvazione grafica.

Piano tecnico dettagliato: [ANDROID-FASE-1.2.md](ANDROID-FASE-1.2.md). Fase 2 sospesa fino all'approvazione esplicita.
