# Verifica della FASE 1.1 — 31 ottobre 2026

Consegna di progettazione da approvare; nessuna implementazione Android successiva autorizzata o eseguita.

## Ambito e isolamento

- Branch `design/ux-rebuild-prototype`, nato da V3.3 `cfb901983625eefc93aec8fc5c6604de05c6bb35` con checkout inizialmente pulito.
- Solo nuova directory `design/ux-rebuild/`. Main e branch V3.3 preservati; nessuna sostituzione degli APK consegnati.
- Finance.kt, Store.kt, Backup.kt, MainActivity e tutti i file Android invariati. SQLite 4, JSON 5, ID e firma non toccati. Nessuna compilazione, nuovo APK, lettura o rigenerazione della chiave.
- HTML/CSS/JS locali con SVG originali, nessun account, API, CDN, analytics, login o costo ricorrente. Tutti gli importi sono DEMO e la data è fissata al 31 ottobre 2026.

## Verifiche eseguite nel browser

Comando: `node design/ux-rebuild/qa/verify.cjs`. Esito macchina completo in `qa/results.json`.

**26 scenari automatici superati**, nessun errore JavaScript e nessuna richiesta a servizi esterni. Fra gli scenari: navigazione, spesa con solo importo, categorie/preferite/bozza, rate e frequenza, nessuna scrittura prima di Salva, rate >120 rifiutate, uscita con conferma, entrata e pagamento previsto distinti, calendario e conferma pagamento, filtri rimovibili, anticipo 300→250→270→300, rimborsi eccedenti o precedenti rifiutati, simulazione senza scritture, analisi personali, riserva modificabile, categorie personali/protezione usate, correzioni, stop/modifica piano con storico mantenuto, notifiche simulate, backup demo/importazione/errore, primo avvio, ritorno da gestione categorie senza perdita della bozza e focus del menu.

**97 catture**: sei schermate originali in chiaro e scuro, menu +, filtri, secondarie, form, errori, vuoti, negativo, importo lungo e tastiera dimostrativa. Matrice responsive 320×640 / 360×800 / 411×891, testo normale e 145%, senza overflow orizzontale nei cinque percorsi verificati; CTA form dentro il viewport. Il comportamento previsto è scorrimento, non riduzione di tutti gli elementi.

**6 confronti + 6 overlay al 50% + PDF** generati con `python design/ux-rebuild/qa/compare.py`, usando la sola area utile del reference. La revisione effettiva e le correzioni sono descritte in VERIFICA-VISIVA.md. I riferimenti restano nel repository in `release/v3.2/references/`.

Il file autonomo PROTOTIPO.html incorpora esattamente CSS, catalogo, SVG e logica locale. Una verifica separata apre quel file dal loopback e prova menu/spesa senza asset esterni. Il pacchetto ZIP è controllato per assenza di file Android, APK o materiale di firma.

## Cosa non dimostrano queste verifiche

Non sono test di usabilità con persone, né screenshot nativi Android. Non sono prova del gradimento dell'utente. Non verificano firma/installazione, IME Android, notifiche effettive, screen reader, compositore hardware o performance sul telefono. I test Android della V3.3 non sono rieseguiti: il codice di produzione non è stato modificato e non viene consegnata una release.

Il browser cloud gestito blocca `file://` con `ERR_BLOCKED_BY_ADMINISTRATOR`; per la QA è stato usato soltanto un server loopback temporaneo. Non è un requisito per l'utente: HTML autonomo senza moduli/fetch/font remoti, da aprire con doppio clic. Quel gesto su Windows deve ancora essere confermato dal proprietario. Nessun hosting pubblico o nuova configurazione di rete richiesta.

## Limiti intenzionali del prototipo

- Stato in RAM: ricaricare ripristina i dati DEMO; esportazione/importazione solo formato prototipo, non backup Android.
- Ricorrenza continua rappresentata da 12 scadenze demo; nessun motore di materializzazione permanente. L'interfaccia lo segnala.
- Notifiche e permesso sono una simulazione esplicita. Il browser non richiede permessi di notifica.
- Tastiera dimostrativa distinta da quella del browser; date picker del browser; testi non totalmente identici alla UI Android futura.
- Le restanti difformità grafiche non sono tutte obbligatorie: illustrazioni, font, profondità, proporzioni e densità restano da giudicare prima di approvare.
- Aspetto scuro coerente, senza originale scuro da replicare. Target calendario su 320 più piccolo del minimo ideale; lista alternativa disponibile.

## Consegna di progettazione

Prototipo completo, istruzioni Windows, screenshot, confronti, inventario/mappa dei percorsi, design system, audit UX, raccomandazione architetturale e rapporto differenze. Nessuna approvazione attribuita: attendere il riscontro esplicito sulle schermate e sui flussi prima della FASE 2.

## Correzione del collaudo precedente

La consegna iniziale aveva quattro catture Impegni errate: Elenco/Calendario identici. Il mancato reset di viewCalendar contaminava la sequenza di cattura. Il reset e le precondizioni delle catture sono stati corretti; lo stato attivo, l’assenza/presenza della griglia e i checksum diversi sono ora verificati. I quattro nuovi scenari coprono anche sei stati finanziari, coerenza anticipi e menu Registra su 320/360/411 con testo normale/grande. Dettagli e limiti in REVISIONE-1.1.md. Nessuna modifica ad Android o al modello finanziario JavaScript.
