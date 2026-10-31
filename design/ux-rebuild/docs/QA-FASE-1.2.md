# QA e revisione visiva — Fase 1.2

Base `57b5d76475532514ed05d1f2bcd6defc44d90632`; branch `design/ux-rebuild-phase-1.2`. Solo HTML/CSS/JS e documentazione del prototipo. Nessun build/test Android eseguito, nessun APK prodotto, nessuna migrazione applicata.

## Verifiche automatiche effettive

- `node design/ux-rebuild/qa/verify.cjs`: **46 scenari browser superati**, inclusi i 26 della 1.1. Nessun errore JavaScript o richiesta esterna. Esito e nomi completi in `qa/results.json`.
- `node design/ux-rebuild/qa/edge12.cjs`: **2 ulteriori scenari mirati superati**: salvataggio reale di 120 rate (somma e ID), frequenza settimanale; margine zero, riserva superiore al cash e simulazione oltre liquidità senza scritture. Esito in `qa/edge-results.json`.
- Totale **48 scenari**. Il test di proprietà percorre tutti i numeri 1–120 per la somma esatta. Non si sommano asserzioni o screenshot al numero dei test.
- L’ultimo collaudo completo include anche apertura reale dei campi data di anticipo, restituzione, rimborso, pagamento, orizzonte e onboarding. Il controllo è tramite pulsanti visibili, non soltanto API interne del prototipo.
- File autonomo: verifica separata del vero PROTOTIPO.html, con digest e flussi, in `qa/standalone.json`. Nessun CSS/JS/font/API remoto. Il browser cloud gestito vieta `file://`; la QA usa solo loopback. Il doppio clic Windows resta da confermare sul PC del proprietario.

I26 scenari 1.1 sono conservati. Dove presupponevano una rata storica è selezionato esplicitamente “ogni rata”; dove usavano un input data del browser percorrono ora il calendario custom. L'intento di regressione rimane: non sono stati eliminati per nascondere il nuovo comportamento.

## Copertura dei sette interventi

| Area | Verifiche concrete |
|---|---|
| Rate | 600totale/3→200×3, 600each×3→1800, 100/3→3334/3333/3333 centesimi; n1–120, salvataggio120, quote zero rifiutate, continua soloeach, cambio modalità, ritorno Nessuna rata con stato/data/bozza, storico each, pagati/ID invariati dopo modifica totale, backup quote |
| Orizzonte | 30/60/90 mobili, fissa, cambio giorno, confine30nov/1dic, arretrato, cash/incassi distinti; UI periodo e scadenze escluse |
| Onboarding | completamento su dati esistenti, pausa e ripresa, ricarica browser, archivio demo vuoto, zero, nessun impegno, riserva alta, nessuna duplicazione saldo/invio |
| Categorie | fissate prima di preferite/frequenti, riordino, snapshot bozza, scelta conserva campi, persistenza dopo reload, categorie personali preesistenti |
| Calendario | stessi record elenco/griglia, selezione giorno senza scritture, dettaglio/aggiunta con data selezionata, pagamento/incasso con conferma, doppio pagamento bloccato, accesso al rimborso |
| Simulatore | 1200−600→600→300→220 con 520 di margine iniziale e −80 dopo; incasso incerto escluso; linea alternativa420 esplicita; riserva, stesso giorno aggregato, arretrati oggi, negativi/zero/centesimi, assenza scadenze, periodi lunghi, vista indipendente dal periodo protetto, nessuna scrittura |
| Date | annulla/conferma, mese/anno diretti, bisestile, dicembre/gennaio, tastiera, date future effettive disabilitate, minimo data rimborso, nessuna perdita del form; calcoli confrontati in Pacific/Kiritimati e America/Los_Angeles; selettore condiviso anche per orizzonte e Analisi |

## Catture e responsive

**235 immagini attuali**, fra schermate, menu, errori e stati. Le nuove interfacce hanno una matrice8 superfici ×3 viewport ×2 temi ×2 dimensioni testo =96 catture dedicate, più12 tavole principali. Home, Spesa, Analisi, Anticipi e Calendario hanno anche matrice responsive chiara/scura. Le catture Impegni Elenco/Calendario rimangono distinte e verificate da DOM e checksum.

Viewport 320×640, 360×800, 411×891 CSS px; testo normale e 145%. Nelle verifiche non c'è overflow orizzontale di pagina o sheet. Le CTA dei form e la chiusura dei menu sono raggiungibili; la selezione date ha header e azioni separati dal corpo scorrevole. Date e calendario offrono elenco alternativo per target maggiori. Tastiera dimostrativa, importi lunghi e negativi, filtri e vuoti della 1.1 conservati.

Sei confronti originali aggiornati con ritagli documentati in `comparisons/crops.json`, overlay 50% e PDF. Galleria delle 12 nuove tavole in HTML/PDF. Le immagini sono elementi browser reali, non screenshot del concept usati come UI.

## Revisione visiva e correzioni dopo le catture

Visionate le sei schermate del concept e le implementazioni, poi le nuove date, due modalità Rate, previsione/dettaglio, primo avvio, rapide e riepilogo giorno, includendo 320 con testo grande e scuro.

- Il primo calendario a font grande lasciava le ultime righe sotto le azioni sticky. Corretto: contenuto con scroll proprio, intestazione/chiusura e azioni fuori da quel corpo; data selezionata nell'header. Si scorre per i giorni restanti, non si riduce tutto.
- Aggiornata la selezione radio quando il numero personalizzato coincide con una scelta predefinita; il riepilogo non lascia una selezione incoerente.
- Il controllo della galleria ha rilevato una regola CSS ereditata che nascondeva i label con input convertito in hidden. Corretto: i label dei date picker custom rimangono visibili; nuovo scenario verifica apertura reale in tutti i contesti.
- Campo Data/Rate decorato con lo stesso componente condiviso; non dipende dall'aspetto di un date picker browser grezzo.
- Home conserva numero, metriche e due azioni; il periodo è una piccola indicazione, non una nuova card pesante.
- Le frecce delle categorie sono state ingrandite dopo la cattura con testo grande; il riordino non richiede drag.
- Gli scarti dal concept della pagina Rate sono intenzionali e visibili: selettore di significato e totale live richiedono spazio. Opzioni avanzate e scadenze restano espandibili.

## Autocritica e limiti

Nessun test con persone, telefono, TalkBack o tastiera Android. Nessuna certificazione di accessibilità. Date-grid su 320 resta compatta: l'elenco alternativo ha target maggiori; con testo grande servono scroll e focus corretti. Le animazioni, gli inset e il blur del browser non dimostrano la resa Android.

Le illustrazioni/icone e il font non sono copie pixel del reference. Home ha un'indicazione e azioni funzionali aggiuntive. La torta segue i valori demo; il saldo/previsione usa gradini veri, non una curva illustrativa. Il nuovo onboarding e le categorie rapide sono estensioni coerenti, non schermate presenti nel concept originale.

La demo salva nel browser, con fallback memoria se lo storage è vietato: evitare dati reali e verificare la persistenza sul proprio PC. Le continue sono rappresentate fino a 10 anni, non da un servizio perpetuo. Il JSON è del solo prototipo. Implementazione finanziaria Kotlin, schema, migrazioni e backup reali restano intatti: le modifiche proposte sono in ANDROID-FASE-1.2.md.

**Questi esiti rendono il prototipo valutabile, non approvato. La Fase 2 non è iniziata.**
