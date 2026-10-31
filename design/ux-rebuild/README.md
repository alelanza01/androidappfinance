# Posso spenderlo? — FASE 1.2 da approvare

Prototipo HTML offline, non una nuova APK. Base di progetto `57b5d76`, branch `design/ux-rebuild-phase-1.2`. Codice Android, main, database, backup reali e firma non modificati.

## Aprirlo su Windows

1. Apri `PROTOTIPO.html` su GitHub nel branch **design/ux-rebuild-phase-1.2**.
2. Premi **Download raw file**, poi fai doppio clic sul file scaricato.
3. Nel browser scegli **Percorsi da provare**. Nessuna installazione, login, server o configurazione di rete.

In alternativa scarica `Posso-spenderlo-UX-prototipo.zip`, tasto destro → **Estrai tutto…** → apri `PROTOTIPO.html`. Il pacchetto comprende sorgenti, documentazione, screenshot e confronti. L'anteprima codice di GitHub non esegue HTML: scaricalo prima.

## Cosa provare nella 1.2

- **Rate:** dalla Guida prova 600€ totali /3 =200×3, oppure 600€ ogni rata ×3 =1.800€. Anche 100€ /3 =33,34+33,33+33,33. Riepilogo sempre aggiornato, scadenze espandibili. Continua permette solo ogni rata. Conferma prepara; Salva registra.
- **Periodo protetto:** sotto l'anello, scegli prossimi 30/60/90 giorni o una data fissa. Il giorno locale avanza automaticamente; le scadenze escluse si possono consultare.
- **Primo avvio:** Guida → Prima installazione su dati demo vuoti (con conferma), oppure Impostazioni → Configurazione guidata per conservare i dati esistenti. Puoi interrompere e riprendere.
- **Rapide:** Categorie → Ordina rapide; Fissa/Sblocca e frecce su/giù. Fissate prima di preferite/frequenti; l'ordine della bozza non si muove durante la compilazione.
- **Calendario:** Impegni → Calendario → giorno. Mostra pagamenti, incassi e restituzioni con azioni esplicite. Toccare una data non registra denaro.
- **Simulatore:** Guida → Simula 600€; scorri al grafico, scegli una scadenza, cambia periodo o mostra gli incassi come scenario non garantito. Il margine ufficiale non viene alterato.
- **Date:** tocca il campo data; mese/anno diretti, scorciatoie, tastiera, Annulla/Conferma. Le date non ammesse sono disabilitate; la bozza rimane intatta.

Tutte le funzioni precedenti restano disponibili: movimenti/filtri/correzioni, anticipi e rimborsi, riserva, categorie personali, notifiche simulate, esportazione/importazione demo, temi e testo grande.

## Dati e giorno della demo

Usa soltanto dati dimostrativi. La demo e il primo avvio incompleto sono conservati localmente nel browser; **ricaricare non è più un reset**. Usa Ripristina demo per ricominciare. Se lo storage non è disponibile rimane la sola memoria della scheda: comportamento del file locale da confermare sul PC dell'utente. Nessun dato viene inviato a servizi esterni.

Nell'uso normale il giorno è quello locale del browser; i dati iniziali sono traslati rispetto alla data di apertura. Screenshot e test fissano il 31 ottobre 2026 per riprodurre l'esempio 1.200→600→300→220€. La Guida consente di provare il giorno successivo. Il formato JSON DEMO non è un backup Android e non va usato con dati personali.

## Materiali

- [Funzioni, audit UX e autocritica](docs/FASE-1.2.md)
- [Rapporto QA aggiornato](docs/QA-FASE-1.2.md)
- [Piano Android e migrazione proposta](docs/ANDROID-FASE-1.2.md)
- [Design system](docs/DESIGN-SYSTEM.md)
- [Navigazione completa e audit iniziale](docs/NAVIGAZIONE-E-AUDIT.md)
- [Sei confronti — PDF](comparisons/Confronti-UX-Rebuild.pdf)
- [Galleria nuove schermate — PDF](comparisons/Galleria-FASE-1.2.pdf)
- [Galleria locale HTML](GALLERIA.html)
- [Catture principali e responsive](screenshots/)
- [Esiti automatici](qa/results.json) e [verifica HTML autonomo](qa/standalone.json)

I rapporti 1.1 sono conservati come documenti storici, non descrivono tutte le funzioni attuali.

## Riprodurre le verifiche

`node qa/verify.cjs` con Playwright/Chromium locali (variabile `CHROMIUM` opzionale). Il server loopback temporaneo è solo uno strumento QA: il file autonomo non ne ha bisogno. `python qa/compare.py` genera confronti e galleria; `python qa/package.py` incorpora CSS/JS/SVG nell'HTML e crea ZIP/checksum. Nessun tool Android è eseguito.

**La Fase 2 Android resta sospesa fino all'approvazione esplicita.**
