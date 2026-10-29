Quando si apriva la V3.1, composizioni e palette non corrispondevano al concept approvato. La V3.2 ricostruisce le sei schermate con anello turchese in Home, navigazione Home/Movimenti/Analisi/Impegni e + centrale, categorie colorate e preferiti a cuore, Analisi con anello/legenda/saldo, radio per le rate e conferme centrali.

La selezione delle rate prepara soltanto il piano: l’importo resta quello di ciascuna rata e nessuna operazione viene salvata prima del comando esplicito. Crediti, rimborsi, simulatore, calendario, regole, backup e avvisi restano accessibili.

Schema SQLite 4, formato backup 5 e application ID invariati; nessuna modifica al motore finanziario. APK di prova con versionCode 320 e firma confrontata con V3.1. Chiavi e password escluse.

Verifiche e limiti effettivi: `release/v3.2/VERIFICA.txt`. Sei confronti concept/Android in `release/v3.2/comparisons`, screenshot chiari/scuri e configurazioni piccole/accessibili in `release/v3.2/screenshots`.

Questa PR non autorizza il merge su main. Aggiornamento fisico, tastiera reale, ripple/compositore hardware e artefatto specifico del telefono restano da verificare sul dispositivo.

Validazione effettiva: 122 test superati, build release riuscita, lint 0 errori/29 avvisi; 188 screenshot e 6 confronti PNG/PDF.
