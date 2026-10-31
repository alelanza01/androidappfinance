# V3.4 — Implementazione nativa, in corso

Base Android cfb9019, riferimento approvato a49b5f4. Branch dedicato, nessuna modifica a main.

## A — Fondamenta
Home, navigazione a quattro destinazioni e menu Registra composti da controller nativi separati. Palette allineata ai token approvati, anello proporzionale alla liquidità reale. Menu raggruppato con descrizioni, intestazione/chiusura fisse e corpo scorrevole. Versione predisposta 3.4.0 (340).

Verifica eseguita: test Native34Test, rendering Robolectric Native Graphics API 35, Home/menu nei due temi, navigazione e chiusura senza scritture. Screenshot reali delle View in milestone-A. Ispezionati prima di proseguire. Nessun emulatore/telefono utilizzato. Adattamento esplicito: Roboto di sistema in luogo di Arial del browser. Dati demo nativi diversi dal campione HTML, da uniformare nei confronti finali.

La release non è ancora completa: seguono operazioni, dominio avanzato e collaudo.

## B/C/D — Integrazione
Schermate isolate in InputScreens, LedgerScreens, CreditScreens, AnalysisScreens e SettingsScreens. Bozze e selezione date ripristinate tramite Bundle; onboarding interrotto salvato nelle preferenze private. Selettore date nativo con selezione confermata, range contestuali e alternativa accessibile. Categorie rapide congelate per bozza, preferite, frequenza e ordine fissato.

SQLite 5 e JSON 6: orizzonti fissi/mobili, completamento onboarding, quickRank, EACH/TOTAL e quote esplicite. Vecchi importi EACH invariati. Comandi con token transazionale; le modifiche ai piani preservano record effettuati e ID futuri. Modifica esplicita della singola rata aggiorna anche la relativa quota/totale.

Simulatore puro separato da SQLite, grafico a gradini con riserva e scenario entrate distinto. Impegni Elenco/Calendario leggono lo stesso archivio; dettaglio giorno e conferme esplicite. Anticipi scaduti inclusi negli aperti, totali globali distinti dalla selezione. Onboarding quattro passi applicato una sola volta.

Il collaudo finale è in esecuzione. Gli eventuali problemi e il risultato definitivo sono nel rapporto finale, non deducibili dai soli commit intermedi.
