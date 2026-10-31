# Mappa completa e audit UX — proposta da approvare

Base esaminata: V3.3 `cfb901983625eefc93aec8fc5c6604de05c6bb35`. Non è un'approvazione del design attuale. L'audit considera le immagini originali, i confronti V3.2/V3.3, AUTOCRITICA e VERIFICA V3.3, MainActivity, Design, ConceptViews, RefinedViews, Charts, CategoryCatalog, Finance, Store, Backup e le suite finanziarie/UI esistenti. Nessuno di questi file è modificato.

Lettura visiva: il concept concentra l'identità in superfici lattiginose, peso del numero, icone colorate con riempimento, contenitori bassi, un ring dominante e una barra inferiore sottile. Nelle versioni precedenti la presenza di molti accessi dello stesso peso, icone solo geometriche, form tecnici e funzioni nascoste produce una struttura diversa anche con colori simili. La precedente autocritica descrive i sintomi, ma non risolve il significato delle sezioni.

## Nuova mappa

```
Home ── anello → Come nasce il margine → Riserva / Allinea liquidità
  ├── Simula spesa (nessuna registrazione)
  ├── Anticipi da recuperare → Dettaglio → Registra rimborso
  └── Impostazioni → Riserva e disponibilità / Aspetto / Notifiche
                     Categorie / Backup / Come iniziare
Movimenti → ricerca / filtri rimovibili → Dettaglio → Modifica / Elimina
Analisi → Mese, Trimestre, Anno → filtro categoria → Movimenti
        └── Confronti, entrate e previsioni → confronto / riepilogo mensile
Impegni → Elenco / Calendario → Dettaglio → Conferma pagamento o incasso
        └── Piani attivi → Modifica / Interrompi / Correzione storico
+ Registra
  ├── Già pagato o ricevuto: Spesa / Entrata
  ├── Da pagare o incassare: Programma pagamento / Rate e ricorrenze
  └── Denaro da recuperare: Registra anticipo
Form spesa ↔ Categorie / Rate → Conferma → form preservato → Salva
```

Quattro destinazioni inferiori stabili. Il + è un'azione centrale, non una quinta destinazione. Anticipi e Simulatore hanno due accessi nominati in Home, non quattro card duplicate. Il dettaglio del calcolo si apre toccando l'anello e ha una destinazione esplicita.

## Inventario funzione per funzione

Le sigle Home/Movimenti/Impegni/Analisi/Impostazioni sono destinazioni; `+` apre il menu. Ogni riga esplicita azioni e destinazione proposta. Le funzioni con effetti finanziari richiedono sempre Salva o una conferma.

| Nome V3.3 e scopo | Ingresso corrente e relazioni/azioni | Ambiguità / semplificazione | Percorso proposto nel prototipo |
|---|---|---|---|
| Home: spendibile e metriche | Avvio; ring, quattro quick actions, tab e + | Crediti/Dettaglio poco espliciti; eccesso di accessi equivalenti | Home; anello apre calcolo; due azioni nominate |
| Dettaglio del calcolo | Ring/Dettaglio; regole e riconciliazione | Denominatore non spiegato; tecnicismi | Home → anello → Come nasce il tuo margine |
| Registra | +; Spesa, Entrata, Anticipo, Ricorrenza | Mescola passato/futuro; quattro righe senza gerarchia | + → tre gruppi con icone e spiegazioni |
| Spesa veloce / Nuova spesa | Quick action o +; categoria/data/rate | Duplicazione di ingresso e form standard | + → Registra spesa; solo importo necessario, Altro predefinita |
| Nuova entrata | +; categoria/data/stato/incerta | Attesa ed effettiva facili da confondere | + → Registra entrata; ricevuta per default; attesa nelle Altre opzioni |
| Spesa prevista | Impegni → Nuovo / menu | Termine astratto, stato sotto avanzate | + → Programma pagamento: scadenza in evidenza |
| Entrata prevista e incerta | Form/avanzate | Non è liquidità | Entrata → Altre opzioni → ancora da ricevere + non garantita; distinta in Impegni |
| Selezione categoria | Form; elenco, favoriti, recenti | Gestione e scelta si sovrappongono | Form → capsula → ricerca/preferite/elenco; Gestisci secondario |
| Preferiti e recenti | Cuore / Filtri categorie | Filtri nascondono la lista senza motivo evidente | Cuori sempre visibili; Recenti sotto Gestisci |
| Rate | Form → Rate; 1/2/3/6/12/manuale/continua | “Nessuna” non chiarisce importo per rata | Stesso punto; importo di ciascuna rata + unico totale |
| Frequenza e prima scadenza | Form serie | Troppo testo, continuità nello stesso livello | Rate avanzate; frequenza/inizio nel piano, non nella spesa ordinaria |
| Annullamento form | Indietro, conferma | Salvataggio e applicazione rate confusi | Conferma prepara; Salva registra; dialogo esplicito per bozza modificata |
| Movimenti | Tab; cerca, filtra, categoria; tap apre menu | Previsti/effettivi e anticipi non immediati | Tipologia e stato in ogni riga; dettaglio dedicato |
| Filtri movimenti | Menu + selettore categoria | Filtro attivo poco evidente | Sheet Tipo/Stato/Categoria + chip rimovibili e conteggio |
| Gestione movimento | Tap riga → menu, modifica/elimina | Manca contesto prima di azioni distruttive | Dettaglio importo/stato/data; azioni specifiche |
| Conferma pagamento/incasso | Menu movimento previsto | Rischio doppio conteggio interpretato dall'utente | Dettaglio previsto → conferma; data oggi, effetto spiegato |
| Impegni lista | Tab; scadenze, arretrati, entrate, piani | Previsioni e storico sembrano equivalenti | Da pagare / Incassi attesi / Restituzioni attese; effettuati in Movimenti |
| Calendario | Impegni → Calendario | Celle/bordi/controlli troppo presenti | Griglia leggera, pallini, giorno selezionato e lista sottostante |
| Programmazioni | Fondo Impegni; menu serie | Nascosto tra tutte le scadenze | Impegni → Piani attivi; + → Rate e ricorrenze |
| Modifica serie | Menu serie o occorrenza | Effetto sui pagati poco chiaro | Modifica piano: aggiorna solo futuro, storico mantenuto |
| Elimina singola occorrenza | Menu movimento ricorrente | Rischio eliminazione intera serie | Dettaglio → Gestisci ricorrenza → Solo questa scadenza |
| Interrompi da qui | Menu movimento ricorrente | “Elimina” può implicare storico perso | Questa e le prossime; conferma storico preservato |
| Interrompi serie / rimuovi anche reali | Menu serie; due conferme | Azioni contabili molto diverse | Interrompi piano → Mantieni storico oppure Elimina anche storico con saldo prima/dopo |
| Crediti: denaro anticipato | Home Crediti / + Anticipo | Sembra un prodotto bancario | Anticipi da recuperare + frase introduttiva e stato vuoto didattico |
| Filtri crediti | Ricerca + form soggetto/progetto/stato/mese | Gergo e form troppo simile a registrazione | Tab Aperti/Recuperati/Scaduti; ricerca; filtri persona/progetto/periodo |
| Nuovo anticipo | + / Crediti | Confondibile con spesa personale | Registra anticipo: motivo/persona/data; liquidità ridotta, credito distinto |
| Dettaglio anticipo | Tap credito | Residuo e denaro spendibile confusi | Residuo dominante, anticipato/restituito, scadenza, progress e rimborsi |
| Rimborso / modifica / elimina | Dettaglio anticipo → azioni | Entrata personale? | Solo nel dettaglio anticipo; ricevuto, residuo e data validati; correzione mostra effetto |
| Simulatore | Home quick action | Poco individuabile e simile a form | Home Simula spesa; margine prima/dopo e coperture protette, nessun Salva |
| Analisi distribuzione | Tab; periodo; donut e legenda | Più tabella tecnica che quadro visivo | Composizione unica; categorie identificate coerentemente |
| Saldo / previsioni | Approfondimenti → Liquidità | Scenari sembrano spendibili | Saldo reale a gradini; scenari numerici separati, descritti come previsioni |
| Confronto / ricorrenti / mesi | Approfondimenti | Diversi menu dello stesso peso | Confronti, entrate e previsioni: riepilogo + scelte motivate |
| Impostazioni | Ingranaggio | Etichette tecniche e navigazione poco strutturata | Il tuo margine / I tuoi dati; righe con icona e descrizione |
| Liquidità iniziale | Regole del saldo | “Saldo-base” difficile da capire | Importo di partenza; esplicita effetto sullo storico |
| Riconciliazione | Calcolo o Regole | Termine contabile | Allinea la liquidità di oggi; rettifica, non guadagno |
| Riserva / previsione | Regole del saldo | “Orizzonte” tecnico | Riserva protetta / Pagamenti da considerare fino al |
| Aspetto | Impostazioni | Colore come unica differenza | Anteprime Chiaro/Scuro/Sistema + testo ingrandito |
| Avvisi | Impostazioni; permessi sistema | Promessa implicita di puntualità | Notifiche: riepilogo locale, ritardi espliciti; permesso simulato |
| Categorie personali | Impostazioni o pressione lunga | Gesto nascosto | Impostazioni → Categorie → Nuova/Modifica; non interferisce con scelta |
| Backup e copia precedente | Impostazioni → Backup, picker Android | Operazione distruttiva non contestualizzata | Esporta / Importa / copia prima importazione; conferma quantità e sostituzione |
| Demo separata | Impostazioni; dati non modificabili | Demo inutilizzabile per provare salvataggi | Tutto il prototipo è una demo modificabile in RAM, Reset esplicito |
| Primo avvio | Implicito, valori vuoti | Non si sa da dove iniziare | Come iniziare: liquidità → riserva → impegni → Home |

## Verifica dei flussi critici

Conteggi indicativi di attivazioni, esclusa digitazione; le verifiche sono progettuali/automatiche, non test con persone.

| Scopo | Percorso e passaggi tipici | Errore/ritorno/risultato |
|---|---|---|
| Capire lo spendibile | 0 per leggere; 1 per spiegazione | Denominatore nominato “liquidità”; negativo senza ring positivo |
| Spesa ordinaria | +, Spesa, importo, Salva (3 azioni + campo) | Errore vicino al form; bozza conservata |
| Categoria | Capsula, scelta (2) | Ricerca/cuore non cancellano importo o descrizione |
| Rate | Rate, scelta, Conferma (3) | Nessuna scrittura prima di Salva piano; totale n×importo |
| Uscita | Indietro, Continua/Esci (2) | Bozza non persa finché non confermi |
| Entrata / pagamento futuro | +, tipo, campi, Salva | Effettivo vs previsto esplicito; effetti distinti |
| Movimenti | Tab, filtro, Applica; chip × per rimuovere | Stato e tipo leggibili; zero risultati con recupero |
| Anticipo | Home Anticipi, Registra anticipo, campi, Salva | Motivo e soggetto obbligatori; credito non spendibile |
| Rimborso | Anticipo, Registra rimborso, importo, Salva | Rifiuta eccedenza/data precedente; storico cumulato |
| Simulazione | Home Simula, importo | Risultato live; nessuna transazione o conversione automatica |
| Riserva | Ingranaggio, Riserva/disponibilità, Riserva, importo, Salva | Home aggiornata; dati dimostrativi soltanto |

## Scelte da approvare prima di Android

1. “Anticipi da recuperare” e collocazione in Home, non una quinta tab.
2. Gruppi e ordine del menu +; una spesa richiede solo l'importo.
3. Categorie: mantenere le identità esistenti, senza rinominarle per copiare un disegno.
4. Rate: numero per rata, totale esplicito, pianificazione distinta dall'avvenuto pagamento.
5. Estensioni secondarie e flussi di correzione; saldi previsti mai chiamati spendibili.
6. Aspetto delle sei schermate e delle estensioni: l'accettazione resta all'utente.

## Aggiornamento 1.2

Home → indicazione sotto ring → 30/60/90/fissa; simulatore → grafico → punto/dettaglio/scenario/periodo. Form → data → calendario condiviso, oppure Rate → significato importo → numero → Conferma → Salva. Categorie → Ordina rapide → Fissa e sposta. Impegni → giorno → gruppi e azioni → conferma reale. Impostazioni → Configurazione guidata →4 passi, ripresa; Guida → prima installazione demo vuota (con conferma). Vedere FASE-1.2.md per audit e limiti attuali.
