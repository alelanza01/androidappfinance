# Revisione visiva — 31 ottobre 2026

Sono state guardate realmente le sei immagini originali, i confronti V3.2/V3.3 e quelli generati del prototipo. L'HTML è composto da elementi, SVG e controlli reali, non screenshot dentro una cornice. I numeri demo sono coerenti con i saldi: €1.200 liquidità, €380 impegni e €300 riserva → €520 spendibili; entrate personali €2.450, spese personali €1.320. Le somme non imitano una classificazione contabile incoerente nel raster.

I confronti usano ritagli dichiarati in `comparisons/crops.json`: cornice illustrata e riga di stato esclusi, stessa larghezza, nessuna deformazione per pareggiare altezze diverse. Le sovrapposizioni al 50% servono a vedere gli scostamenti, non sono un punteggio di somiglianza. Non viene dichiarata equivalenza pixel-per-pixel né accettazione raggiunta.

## Correzioni fatte dopo le catture

1. Tolto il bordo nero del focus programmatico sul contenitore pagina; mantenuto il focus blu per i controlli.
2. Analisi: ridotti donut, margini verticali e altezza dell'area chart dopo aver visto il grafico tagliato. Legenda informativa compatta; filtro categoria separato con target grande, anziché righe sottili cliccabili.
3. Barra di ricerca resa un blocco per centrare realmente la lente; non una decorazione spostata sulla baseline del testo.
4. Ripristinata la formattazione italiana delle migliaia anche per quattro cifre.
5. Catture in stato rilasciato: nessun toast transitorio sopra le azioni e nessun focus ereditato da un test precedente. Focus e tastiera sono verificati separatamente.
6. Rivisto il ritaglio superiore del reference per non tagliare il titolo delle pagine non Home.

## Per le sei schermate

| Schermata | Fedeltà al reference | Miglioramento UX distinto dalla fedeltà | Difformità visive residue |
|---|---|---|---|
| Home | Ring dominante, due metriche, luce menta, due impegni e + centrale compatto | Denominatore “di liquidità”; Simula e Anticipi nominati | Il ring è meno tridimensionale e il tratto ha un gradiente diverso. Ritmo verticale più lungo, due azioni assenti nel reference, icone meno illustrate. Non è una replica esatta. |
| Nuova spesa | Importo ampio, tre righe di categorie, capsula, descrizione leggera, data/rate e CTA | Solo importo obbligatorio; picker conserva bozza; errori recuperabili | La griglia usa le categorie reali, quindi disposizione/nomi non coincidono. Il simbolo Bar e ristoranti è una tazza; il reference è più pittorico. Altre opzioni aggiunte e formato data del browser. |
| Categorie | Ricerca tenue, preferite, icone pastello, cuori sottili | Gestisci esplicito separato dalla scelta; categorie personali preservate | Font e icone non identici; elenco e preferite sono quelli della demo, non l'ordine disegnato. Spazi ancora più regolari del raster. |
| Analisi | Tabs separate, donut, legenda allineata e chart blu con riempimento | Incassi/previsioni distinti dallo spendibile; filtro accessibile | Il reference associa colori/quote della torta non perfettamente alla sua legenda: qui ogni arco segue i dati e la legenda. Curva a gradini discendente e non curva liscia crescente; le proporzioni di donut/area blu restano differenti. |
| Rate | Cinque opzioni, radio e check, selezione tenue, contenitore importo e CTA | Importo per rata + totale, avanzate secondarie, applicazione senza scritture | Testi per-rata diversi dal totale diviso nel reference. Il contenuto aggiuntivo allunga la pagina, card e spazi non identici. |
| Conferma | Blur effettivo CSS, documento con badge, due pulsanti, layering e angoli | Focus contenuto e azioni specifiche Continua/Esci | Documento più semplice e meno stratificato; altezza dialogo e distanze diverse, blur dipende dal browser e non prova Android. |

Non tutti gli scostamenti sono tecnici o obbligatori: pesi tipografici, profondità e illustrazioni rimangono scelte progettuali da giudicare. La proposta è sottoposta all'utente, non dichiarata approvata sulla base dei test.

## Estensioni coerenti, non presenti nel concept

- Registra: sheet a tre gruppi, spesa come scelta prioritaria, icone e descrizioni di scopo. Non è una lista di quattro parole o una nuova dashboard.
- Anticipi: frase introduttiva, totale non spendibile, aperti/recuperati/scaduti; dettaglio con progressione e storico. Rimborso solo nel contesto dell'anticipo.
- Movimenti: tipo e stato insieme al nome; filtri evidenti e rimovibili, dettaglio prima delle correzioni.
- Impegni: scadenze e incassi separati; calendario con pallini e dettagli del giorno; piani in sezione propria, storico effettuato in Movimenti.
- Simulatore: risultato live e coperture protette, nessun pulsante che registra una spesa per errore.
- Impostazioni/backup/notifiche: struttura con icona e descrizione, senza introdurre uno stile alternativo.

## Limiti verificati e aperti

Il viewport desktop principale è 360×800; sono presenti catture reali Chromium a 320×640, 360×800 e 411×891, normali e testo al 145%. A 320 e testo grande lo scroll è necessario: non si comprime tutto. La CTA dei form resta visibile; i campi inferiori si raggiungono scorrendo. La Home più piccola richiede uno scroll per le azioni secondarie. Le celle calendario strette restano un punto da verificare nel nativo.

Il tema scuro conserva struttura e gerarchia; non proviene da un reference originale scuro. La tastiera è una simulazione esplicita, il date picker è del browser. Non sono state verificate tastiera Android, screen reader, notifiche reali, prestazioni native o rendering sul telefono. Nessun test con utenti reali è stato dichiarato.

## Domande per l'approvazione

1. L'identità grafica è ora abbastanza vicina al tuo concept, incluse le differenze dichiarate sopra?
2. Il menu + e “Anticipi da recuperare” si capiscono senza spiegazioni aggiuntive?
3. Il percorso di una spesa e il ritorno da categorie/rate sono naturali?
4. Le pagine secondarie sembrano appartenere alla stessa app?
5. Quali differenze sono ancora bloccanti prima di implementare Android?

La FASE 2 resta sospesa fino a una risposta esplicita dell'utente.

## Revisione mirata 1.1

Gradiente dell’anello a tre stop, tratto 22 unità, metriche più compatte e uscita in rosa; righe impegni con stato esplicito, badge compatti accanto alla data in Home; azioni secondarie conservate. Campi importo e Data/Rate più uniformi, cuori armonizzati, Analisi con donut 208 px e area 72 px (sempre a gradini reali), spiegazioni Rate abbreviate e avanzate secondarie. Popup con ombra e spazi leggermente rivisti. Non è un nuovo redesign.

I confronti aggiornati mantengono le differenze già dichiarate: illustrazioni/icone e font non identici al concept, Home con due azioni aggiuntive e stati previsti, differenze di composizione della torta e della linea dovute ai dati, semantica per-rata obbligatoria, blur browser e non Android. La vista Calendario richiede scorrimento per leggere tutte le scadenze; i target delle celle più strette restano un limite aperto su 320.
