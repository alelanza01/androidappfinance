# Raccomandazione Android — non implementata

Il prototipo HTML è un mezzo di verifica, non una scelta di distribuire una WebView. Nessun nuovo APK, cambio di application ID, modifica a Finance/Store/Backup o accesso al materiale di firma è necessario in questa fase.

## Problema della struttura attuale

MainActivity riunisce pagine identificate da interi, campi di molte bozze, navigazione avanti/indietro, costruzione dei View, file picker, notifiche, azioni contabili e dialoghi. Design centralizza elementi visivi ma li costruisce con righe/colonne generiche; questo rende facile applicare gli stessi compromessi a tutte le schermate. Charts e ConceptViews possiedono già disegno e hit testing custom, ma l'allineamento tra layout e testo è gestito a mano. Il risultato non va semplicemente trasferito in una nuova Activity monolitica.

## Scelta raccomandata

Una Activity contenitore, schermate native separate in Fragment, ViewModel per destinazione e stato tipizzato salvabile. XML con ConstraintLayout per header, form e pagine a composizione precisa; RecyclerView per elenchi/calendario; custom View solo per ring e chart. ViewBinding per collegare i componenti, DialogFragment/BottomSheetDialogFragment con contenuti dedicati. Componenti e token condivisi, ma layout specifico per ogni schermata.

Questa scelta conserva Kotlin/View e il rendering nativo già presenti, permette di misurare baseline e proporzioni e riduce la riscrittura. Introduce dipendenze AndroidX locali/gratuite nella fase successiva, da bloccare a versioni compatibili minSdk 26/target attuale. Non richiede servizi o account.

Compose è un'alternativa possibile: stato unidirezionale e anteprime sono vantaggi, ma richiede una riscrittura più ampia, nuovi componenti, nuove regressioni accessibilità/IME e formazione su una struttura diversa. Non risolve da solo la fedeltà. Non lo raccomando come prerequisito né lo introduco automaticamente.

## Contratti da preservare

- Finance.kt è l'unica fonte del saldo/spendibile reale: nessuna copia del modello JavaScript in produzione.
- Store.kt mantiene identità, transazioni, tombstone per singole occorrenze e cutoff delle serie; nessun reset/migrazione distruttiva.
- Backup.kt conserva formato JSON 5 e importazione precedente; il formato DEMO del prototipo non verrà supportato nell'app reale.
- Le etichette possono cambiare, gli ID di categorie, movimenti e piani no. I colori personalizzati devono restare quelli dell'archivio.
- Il saldo di oggi cambia solo per movimenti effettivi, anticipi e rimborsi. Previsti e crediti mai spendibili.
- Importo della rata invariato, non diviso; applichi il numero nel form e salvi il piano dopo conferma.
- La futura sessione di aggiornamento verificherà ID `it.privato.disponibile.v2` e certificato della firma già distribuita, senza nuova chiave. Questa fase non legge né trasferisce credenziali.

## Stato e navigazione

Route esplicite: Home, Movimenti, Analisi, Impegni, Anticipi, Anticipo(id), Movimento(id), Form(tipo/id), CategoriaPicker(returnToken), RatePicker(returnToken), Impostazioni e sottopagine. La tab selezionata sopravvive alla ricreazione. Le bozze sono oggetti tipizzati con validazione; picker restituiscono un risultato e non sostituiscono la bozza genitore. `SavedStateHandle` conserva importo, categoria, date, numero/frequenza e testo in caso di rotazione o processo ricreato. Le scritture passano per un comando unico, disabilitando invii ripetuti mentre è in corso; nessuna scrittura in onRender/onResume.

Il ritorno usa lo stack effettivo, non intervalli numerici. Dialoghi distruttivi ricevono snapshot e impatto sul saldo, poi rilegge/valida il record al momento della conferma. I salvataggi e importazioni si fanno fuori dal thread UI e tornano con un esito esplicito; nessuna perdita della bozza per un errore.

## Passi soltanto dopo approvazione

1. Concordare le differenze visive residue e congelare sei tavole/stati approvati, componenti e nomi.
2. Introdurre struttura delle schermate e adapter del dominio conservando identità/db/backup. Nessuna migrazione se cambiano soltanto View e stato UI.
3. Implementare Home, +, spesa/categorie/rate; poi Movimenti/Impegni/Anticipi e secondarie, usando i token approvati.
4. Riutilizzare le 131 verifiche della base; aggiungere navigazione/bozze/modali/IME/accessibilità e snapshot nativi coerenti ai viewport, senza considerare i test prova di qualità visiva.
5. Collaudo sul telefono: aggiornamento senza disinstallazione, back di sistema, rotazione, font grandi, tastiera reale, night mode, permessi/notifiche, backup e firma. Solo allora release.

Rischi aperti: blur CSS non identico al compositore Android, font e date nativi, inset edge-to-edge, layout di importi lunghi, target calendario sotto 44 su 320, overscroll e contenuti oltre la prima schermata. Vanno risolti nel nativo con misure, non con un'immagine di sfondo.
