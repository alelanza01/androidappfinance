# Revisione mirata FASE 1.1 — 31 ottobre 2026

Base: `8c77ffbd6e048078aa3c605d9b353d9fd34115ca`; stesso branch `design/ux-rebuild-prototype`. Nessuna nuova direzione artistica, nessun APK o modifica ai sorgenti Android. Il prototipo rimane da approvare.

## Incongruenza Impegni: causa e correzione

Confermato il difetto prima di intervenire: `10-impegni-light.png` e `11-calendario-light.png` avevano entrambi SHA-256 `b0cb326fa604cb03a96eff94f3bc148b98b49bf8b4434445857b90e3e4d72b68`; le due versioni scure avevano entrambe `9cf6029f63be9e75ab026c9081704035c70749f0e03d9c701a1e86e7386f9529`.

Un test apriva Calendario e lasciava `viewCalendar=true`. I successivi `reset()` ricreavano i dati, ma non ripristinavano quella modalità UI: anche la cattura chiamata “impegni” era dunque un calendario. La sequenza delle immagini non verificava le proprie precondizioni. Non era un difetto del database, né una duplicazione degli impegni.

Ora reset ripristina esplicitamente Elenco, giorno/mese, dettagli e stato secondario dei form. Tema e testo grande restano preferenze del revisore. Le catture impostano e verificano Elenco/Calendario prima dello scatto; `aria-pressed` espone la scelta attiva. Si controllano assenza/presenza della griglia, record identici prima della selezione del giorno e stesso dettaglio del pagamento raggiungibile tramite entrambi i percorsi. Il cambio modalità non scrive dati. Checksum diversi verificati anche automaticamente dopo la cattura.

## Interventi richiesti

- **Stati finanziari:** sei etichette univoche: Già ricevuto, Ancora da ricevere, Già pagato, Ancora da pagare, Anticipo versato, Rimborso ricevuto. Le righe previste hanno anche superficie azzurra e bordo tratteggiato; l'importo positivo previsto non è verde come un incasso ricevuto. Badge coerenti in Home, Movimenti, Impegni, dettaglio e storico rimborsi; rettifiche nominate separatamente. Lo stato non dipende dal segno o dal colore.
- **Home:** gradiente ring a tre stop, tratto 22, ombra più contenuta; metriche compatte e uscita in rosa; stati previsti accanto alla data, allineamento importi e due azioni conservate. Dopo la cattura è stata corretta la maggiore altezza delle righe con badge, che inizialmente tagliava le quick actions a 360×800. Su 320 o testo grande resta lo scroll previsto.
- **Spesa/Categorie:** stessa famiglia di icone, superfici importo più morbide, Data/Rate con separazione tenue, cuori con tratto uniforme; scelta e gestione continuano ad avere gerarchie separate. Nessuna rinomina o perdita delle categorie.
- **Analisi:** donut 208 e andamento 72 px, legenda compatta e riempimento conservato. Nessun dato inventato o interpolazione di movimenti inesistenti; numeri e classificazione invariati.
- **Rate/Popup:** spiegazione abbreviata, ripetizioni avanzate secondarie, per-rata e totale distinti; ombra, illustrazione e spazi dialogo ritoccati senza cambiare le azioni.
- **Registra:** gruppi e descrizioni invariati, testi seguono l'ingrandimento; contenuto scorrevole e header con chiusura sticky. Provato a 320×640, 360×800, 411×891 con testo normale/grande: ultima azione raggiungibile e chiusura utilizzabile anche dopo lo scroll. L'annullamento conserva importo e descrizione della bozza e non muta dati. Le otto nuove immagini menu 320 mostrano inizio/fondo, entrambi i temi e dimensioni del testo. Una riga che passa sotto l'header durante lo scroll non è eliminata: ritorna interamente scorrendo.
- **Anticipi:** Scaduti è esplicitamente un sottoinsieme di Aperti, non una terza categoria disgiunta. I conteggi delle schede seguono ricerca/persona/progetto/periodo; il totale generale e “Aperti · tutti” restano globali e nominati. Il residuo dei soli risultati è separato. Zero risultati filtrati non viene presentato come assenza assoluta di anticipi. Dettaglio mostra Aperto · scaduto e Rimborso parziale contemporaneamente; recuperato solo a residuo zero, con storico mantenuto. I crediti non riscossi restano esclusi dallo spendibile.

## Verifiche effettivamente eseguite

`node design/ux-rebuild/qa/verify.cjs`: **26 scenari superati** (22 precedenti + 4 mirati), 0 errori JavaScript, 0 richieste esterne. Esiti completi in `qa/results.json`.

I quattro scenari aggiuntivi verificano: reset/alternanza e navigazione equivalente degli impegni senza mutazioni; sei stati e conteggio record senza duplicati; anticipi aperti/chiusi/scaduti, filtri e completamento del rimborso; menu su tre viewport e due dimensioni testo, scroll/chiusura e conservazione della bozza. I test economici demo precedenti, incluso 300→250→270→300, continuano a passare.

**97 catture attuali**: sono state aggiornate quelle influenzate dai componenti condivisi e dal reset; otto nuove catture sono specifiche del menu su 320. Nessuna produzione di nuovi screenshot Android. Rigenerati i sei confronti, overlay e PDF con le stesse immagini originali e gli stessi ritagli. Visionate le sei catture principali, le due modalità Impegni, i nuovi movimenti, gli anticipi e il menu a testo grande; confronto con le immagini originali tramite gli affiancamenti aggiornati.

Il file HTML autonomo e lo ZIP vengono rigenerati dai medesimi sorgenti. La verifica separata dell'HTML è registrata in `qa/standalone.json`, con digest della pagina testata. Nessun file di firma o APK nel pacchetto.

## Differenze residue

Restano icone/illustrazioni meno pittoriche del concept, font del browser diverso, due azioni Home e badge non illustrati nel riferimento. La torta segue le quote reali della demo e il saldo è a gradini, non la curva illustrativa. Il selettore rate mantiene l'importo per rata, non divide il totale. Ombre e blur del browser non provano la resa Android. Il calendario e le pagine con testo grande possono richiedere scroll; alcune celle a 320 sono più strette del target ideale di 44 px. Tastiera e notifiche restano simulate; stato demo in memoria, ricorrenze continue rappresentate dalle prime 12 scadenze.

Non sono stati svolti test con persone o su telefono Android. Il numero di test non dimostra approvazione visiva. Nessuna certificazione di accessibilità o somiglianza perfetta viene dichiarata.

**FASE 2 sospesa: serve autorizzazione esplicita dell'utente.**

## Checksum delle quattro catture corrette

- `10-impegni-light.png`: `3014e8e1c518bf47f1e897d4d48071e6f881ff4abc07800c734b1c8ece716e41`
- `11-calendario-light.png`: `16712de94bf5b60c5cc1e62734505cfea242825fcd57ea5ce05fc71c78fedef2`
- `10-impegni-dark.png`: `73ce6f9c403bfc7b798c5805e2705ddc5db933954fe9c4bf633c8a8a41f330c4`
- `11-calendario-dark.png`: `fc540840eacc169e33d3934b9e593cd89a19f3edc1606052f427f9f8cce12026`
