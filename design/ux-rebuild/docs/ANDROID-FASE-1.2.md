# Traduzione Android della Fase 1.2 — piano, non implementazione

Base installata: package `it.privato.disponibile.v2`, SQLite 4, backup JSON 5, piani storici con importo **per rata**. Nessuno di questi contratti è stato modificato durante questa fase HTML. La firma originale non è stata letta, trasferita o rigenerata.

## Architettura di presentazione

Confermata la raccomandazione: Activity contenitore, Fragment per schermata, ViewModel e SavedStateHandle per bozze e selezioni, ViewBinding/XML/ConstraintLayout per composizioni precise, RecyclerView per elenchi/calendario, custom View per ring/grafici, DialogFragment e BottomSheetDialogFragment con lo stesso design system. Non è necessaria una WebView o una migrazione automatica a Compose. Il JavaScript del prototipo non va copiato come nuovo motore Android.

Il dominio resta Kotlin puro, con `Clock` e `ZoneId` iniettati: `LocalDate.now(clock)` a inizio calcolo. Repository/Store eseguono comandi transazionali e restituiscono snapshot immutabili; rendering e simulazione sono funzioni senza scritture. Navigazione tipizzata, ritorni picker espliciti; stato di pagamento, numero rate, significato importo, prima data e frequenza sono proprietà distinte. Ogni submit ha un identificativo, stato in corso e esito per impedire doppi invii.

## Proposta SQLite 4 → 5

Migrazione da realizzare e testare soltanto dopo approvazione, nella transazione `onUpgrade`, senza distruzione o ricostruzione del database:

```sql
ALTER TABLE settings ADD COLUMN horizonMode TEXT NOT NULL DEFAULT 'fixed';
ALTER TABLE settings ADD COLUMN horizonDays INTEGER;
ALTER TABLE settings ADD COLUMN onboardingCompleted INTEGER NOT NULL DEFAULT 1;
ALTER TABLE categories ADD COLUMN quickRank INTEGER;
ALTER TABLE series ADD COLUMN amountMode TEXT NOT NULL DEFAULT 'each';
ALTER TABLE series ADD COLUMN totalCents INTEGER;
ALTER TABLE series ADD COLUMN allocationVersion INTEGER NOT NULL DEFAULT 1;
CREATE TABLE installments (
  series INTEGER NOT NULL,
  occurrence INTEGER NOT NULL,
  cents INTEGER NOT NULL CHECK(cents > 0),
  PRIMARY KEY(series, occurrence),
  FOREIGN KEY(series) REFERENCES series(id) ON DELETE CASCADE
);
```

- **Storico:** tutti i record esistenti rimangono `each`; `series.cents` resta l'importo originario per rata. `operations.id`, collegamenti serie/occorrenza, categorie, date, pagati e tombstone non cambiano. Per serie storiche non è necessario creare quote diverse.
- **Nuovo piano totale finito:** `amountMode='total'`, `totalCents=importo inserito`, `installments` contiene le quote contrattuali esatte per ogni indice. `series.cents` non viene reinterpretato retroattivamente: per un nuovo totale è una quota nominale positiva (q), mentre `totalCents=T` e tutte le n quote sono obbligatorie. La materializzazione di una serie totale usa esclusivamente `installments.cents` e fallisce senza scrivere se una quota manca; soltanto una serie each usa il fallback storico `series.cents`. Il modello JS demo conserva invece l’importo inserito in cents: non è un mapping da copiare nel database. Indice unico serie/occorrenza preservato.
- **Ripartizione:** totale T in centesimi, n rate, q=T div n, r=T mod n. Prime r quote q+1, altre q. Rifiutare T<n; totale e quote rigorosamente interi, senza floating point. Esempi: 10000/3 → 3334,3333,3333; 60000/3 → 20000×3; each 60000×3 →180000.
- **Modifica dopo pagamenti:** importi/date/ID effettuati restano immutati. Se si modifica un totale, sottrarre il già pagato dal nuovo totale e distribuire il resto sugli indici non effettuati, conservando i loro ID. Rifiutare totale inferiore al pagato, rate non positive o numero che esclude indici già pagati. Esclusioni e cutoff non vengono resuscitati. Distinguere sempre “totale contrattuale del piano” e “impegni ancora attivi”: interrompere una rata non riscrive uno storico reale.
- **Continua:** solo `each`, nessun totale finito e nessuna falsa somma complessiva. Materializzare fino all'orizzonte necessario come già previsto, rispettando limiti/esclusioni. Non chiamare la materializzazione dal disegno di un grafico.
- **Orizzonte:** vecchi `settings.horizon` restano date fisse, con `horizonMode='fixed'`. Nuovi `rolling` con giorni 30/60/90: fine=oggi locale+giorni. Il valore memorizzato per compatibilità non è la fonte di un rolling. Arretrati e scadenze di oggi aperte sempre incluse; mostrare un avviso quando una data fissa è passata, senza sostituire silenziosamente la scelta. Entrate attese e crediti non riscossi esclusi dallo spendibile.
- **Fissate:** `quickRank` nullo per categorie non fissate. Ordine gestito in un'unica transazione, senza cambiare ID. Poi preferite, frequenza d'uso effettiva e catalogo stabile; le bozze congelano un array di ID. Eventuali eliminazioni di categoria devono ripulire il rango senza riclassificare dati usati.
- **Onboarding:** migrazione pone completato per installazioni esistenti; mai azzerare il saldo o avviare un setup distruttivo. Per database appena creato, incompleto. Bozza guidata in archivio privato separato/atomicamente scritto; al commit una sola transazione. Se non esistono eventi, impostare opening; con storico già presente registrare esclusivamente la rettifica della differenza rispetto alla liquidità target. Non creare entrate duplicate. Gli impegni della bozza sono inseriti solo alla conferma finale.

Abilitare le foreign key in `onConfigure`, prima di iniziare la transazione, per la nuova relazione quote/serie. Non aggiungere retroattivamente vincoli distruttivi ai record storici; validarli e segnalare eventuali incongruenze senza cancellarli.

Se qualsiasi passaggio fallisce: rollback integrale, version 4 e dati preservati, errore recuperabile. Nessun `fallbackToDestructiveMigration`. Conservare una copia locale prima dell'upgrade e offrire esportazione; provare spazio insufficiente, SQL fallita, riavvio e concorrenza con avvisi. Non promettere che la vecchia APK possa leggere uno schema più recente: un downgrade richiede gestione esplicita e un backup compatibile, non una cancellazione automatica.

## Backup JSON 6 proposto

Non implementato in Android. La versione 6 dovrà contenere:

- regole orizzonte con `mode`, `days` opzionale e data fissa originale;
- stato onboarding completato (non necessariamente la bozza), `quickRank` e gli stessi ID categorie;
- serie con `amountMode`, `totalCents`, regola versione 1 e quote per indice;
- operations/anticipi/rimborsi invariati, inclusi esclusioni/cutoff e storico reale.

Decoder 2–5: mode each, orizzonte fixed, nessuna redistribuzione. Decoder 6: controlli stretti di tipo/intervallo, indici unici, somme esatte, quote positive, coerenza di scadenze non effettuate, assenza di record orfani e limiti quantitativi. Importazione validata completamente prima della sostituzione transazionale e copia precedente come già presente. Versioni future sconosciute rifiutate; mai importare un totale come rata. Il JSON DEMO HTML resta distinto e non deve essere importato nella futura app.

## Calendari, previsione e accessibilità

Componente nativo date-only condiviso con lunedì iniziale, vincoli contestuali, anno diretto, input/azioni di tastiera e azioni Annulla/Conferma. Non limitarsi al widget browser trasferito in WebView. Su 320, griglia compatta più alternativa elenco con target ≥48dp; verificare TalkBack, focus, font 200%, touch exploration e scroll fino alla CTA. Gestire WindowInsets, IME reale, edge-to-edge, dark mode e riduzione movimento.

Calendario Impegni e lista usano lo stesso repository e filtro; selezionare una data non scrive. Conferma pagamento/incasso passa per un update condizionato `status='PLANNED'`, data effettiva e transazione. Rimborso rimane un comando distinto da entrata personale.

Previsione pura: cash effettivo − spesa simulata come primo punto; eventi futuri pendenti raggruppati per data, arretrati applicati oggi; sottrarre solo uscite. Eventuale seconda serie aggiunge incassi previsti e resta non garantita. Crediti esclusi; soglia riserva distinta. Mai partire dallo spendibile sottraendo ancora gli impegni. Periodo grafico indipendente dalle regole ufficiali; gestire molte scadenze e decimazioni visuali senza perdere il dettaglio esatto degli eventi.

## Verifiche prima di una release futura

1. Migrazione di vere fixture SQLite 1/2/3/4 →5 e vecchi backup 2/3/4/5 →6: saldi, ID, quote storiche, rimborsi, cutoff e rigenerazione invariati. Failure injection e rollback.
2. Test proprietà ripartizione su 1–120 rate, limiti, centesimi, pagati parziali e modifiche, concorrenza e doppi submit.
3. Clock con mezzanotte locale, DST, fusi opposti, orizzonti mobili/fissi, date oltre/confine, arretrati e simulazione senza scritture.
4. Snapshot nativi delle tavole approvate + test strumentali navigazione/bozze/rotazione/process death/IME/TalkBack; poi collaudo fisico sul telefono del proprietario.
5. Prima dell'APK: stesso application ID, codice versione superiore e verifica del certificato originale; nessuna nuova chiave. Build/test/lint, aggiornamento senza disinstallare e backup locale preventivo.

**Questo documento non autorizza né esegue la Fase 2.**
