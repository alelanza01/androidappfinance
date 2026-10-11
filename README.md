# Posso spenderlo? 2.1

App Android privata, offline e gratuita per capire quanto denaro puoi spendere dopo impegni e riserva. Kotlin nativo, SQLite, Android 8.0+, nessun server, account, analytics o permesso Internet.

**[Scarica l’aggiornamento V2.1](release/Posso-spenderlo-V2.1.apk)** — su GitHub apri il file e premi **Download raw file**. [SHA-256](release/SHA256-V2.1.txt). I sorgenti completi sono in questo repository.

## Aggiornamento della V2 installata

Versione `2.1.0` (210), stesso application ID `it.privato.disponibile.v2` e **stessa firma della V2 originale**. La migrazione SQLite da schema 1 a 2 aggiunge anticipi e rimborsi senza cancellare impostazioni, movimenti, categorie o ricorrenze. La V1 resta un’app separata.

1. Nella V2 attuale: **Altro → Esporta backup** e conserva il JSON.
2. Scarica `Posso-spenderlo-V2.1.apk` dal collegamento sopra e trasferiscilo al telefono.
3. Aprilo da File, autorizza questa fonte se richiesto e premi **Aggiorna/Installa**. **Non disinstallare la V2**: la disinstallazione cancellerebbe l’archivio.

Se Android rifiuta l’aggiornamento, conserva app e dati e verifica il messaggio: non risolvere disinstallando. La firma della V2.1 è stata confrontata con l’APK V2 conservato in `release/Posso-spenderlo-V2.apk`; l’installazione sul tuo telefono resta da eseguire.

## Design e anticipi

Sezioni aperte con separatori discreti, importi allineati, palette blu notte/oro, barra con icone e indicatore attivo senza pulsanti sovrapposti. Calendario con sette colonne uniformi e indicatori separati dai numeri. Temi chiaro/scuro/sistema.

**Nuovo anticipo** è disponibile da Oggi; **Anticipi da recuperare** si apre da Oggi o Altro. Richiede importo, descrizione e soggetto; data predefinita oggi. Progetto, causale, restituzione prevista e note sono facoltativi e raccolti in Dettagli facoltativi. I filtri per soggetto, progetto, stato e mese si aprono con **Filtri anticipi**.

Dalla scheda: registra rimborsi parziali o completi, modifica anticipo/rimborsi e correggi registrazioni errate tramite eliminazione. Un rimborso non può superare il residuo. Un anticipo già rimborsato non può essere ridotto sotto il totale restituito; le date rispettano la cronologia. Eliminare un anticipo elimina anche i suoi rimborsi e ne annulla l’effetto netto sul saldo, previa conferma.

Esempio verificato: **300 € → anticipo 50 € → liquidità 250 €/credito 50 € → rimborso 20 € → liquidità 270 €/credito 30 € → rimborso 30 € → liquidità 300 €/credito 0 €**.

Le restituzioni attese sono segnate in oro nel calendario e restano non garantite: non entrano automaticamente nel saldo o nelle proiezioni. Anticipi e rimborsi compaiono nello Storico con etichette esplicite e nel grafico della liquidità, esclusi da guadagni e spese personali (anello e colonne).

## Cosa comprende

- **Oggi:** liquidità, uscite programmate fino alla data scelta, riserva, spendibile spiegabile, prossima scadenza, criticità e simulatore immediato senza salvataggio.
- **Grafici:** anello delle spese effettuate per categoria/mese, area con storico e due scenari futuri, colonne entrate/uscite a 3/6/12 mesi. Tocca i grafici o le categorie per i dettagli. Mese selezionabile.
- **Agenda:** calendario mensile, arretrati, pagamenti e incassi futuri, ricorrenze settimanali/mensili/annuali e rate mensili finite (1–120, importo per rata). Conferma manuale del pagamento/incasso. Le ricorrenze continue sono preparate almeno per 12 mesi e fino alla previsione.
- **Storico:** ricerca per descrizione/categoria, filtri per stato, entrata/uscita e categoria. Modifica ed eliminazione dei singoli movimenti, con ricalcolo del saldo.
- **Altro:** liquidità iniziale, riserva, data di previsione, categorie con colore, tema chiaro/scuro/sistema, backup JSON e demo isolata. Avvisi locali giornalieri opzionali.

## Regole finanziarie

Tutti gli importi sono centesimi interi, senza calcoli finanziari in virgola mobile. Input massimo €9.999.999,99, due decimali, senza separatori delle migliaia.

**Liquidità = base iniziale + entrate effettive − uscite effettive + rettifiche − anticipi + rimborsi ricevuti.**

**Spendibile = liquidità − tutte le uscite previste entro la data scelta (inclusi arretrati) − riserva.**

Le entrate future, anche probabili, non aumentano lo spendibile. La proiezione corallo considera solo le uscite note; quella oro aggiunge le entrate previste, comprese le non garantite. Sono scenari distinti dallo storico verde. I grafici mensili contano solo incassi e pagamenti effettivi; le rettifiche sono escluse.

Confermare una scadenza la rende effettiva **oggi** una sola volta. Puoi correggere la data nello storico. Eliminare un movimento effettivo rimuove il suo effetto sul saldo. Modificare la liquidità iniziale cambia anche la base dello storico: per riconciliare il saldo di oggi usa Aggiorna liquidità.

Ogni occorrenza ricorrente ha un'identità univoca. Modifiche/eliminazioni non la rigenerano. Per cambiare una serie intera, interrompila dal calendario e creane una nuova; i movimenti già effettuati restano. Gli ancoraggi mensili mantengono il giorno originale quando possibile (31 gennaio → fine febbraio → 31 marzo).

## Backup e promemoria

Il backup di formato 3 include anticipi e rimborsi; l’importazione accetta anche i backup originali V2 (formato 2, senza anticipi). Il backup è JSON **non cifrato**, con dati personali. Scegli una destinazione locale; i provider cloud eventualmente presenti nel selettore Android non fanno parte dell'app. L'importazione valida formato, importi, date e riferimenti prima di sostituire i dati in una transazione. Conserva prima una copia privata dell'archivio precedente, esportabile da Altro. Limiti di importazione: 5 MB, 20.000 operazioni, 100 categorie, 2.000 serie, 10.000 anticipi e 20.000 rimborsi; non viene eseguito codice dal file.

Gli avvisi richiedono consenso su Android 13+. JobScheduler controlla circa ogni 24 ore le uscite arretrate o entro domani; Android può ritardare l'esecuzione per batteria e altre restrizioni. Non sono allarmi puntuali. Il calendario resta il riferimento.

## Verifiche e immagini

I test coprono contabilità, importi, date, doppia conferma, ricorrenze, rate, riconciliazione, CRUD, categorie, persistenza, backup, ricerca, navigazione, simulatore, avvisi e grafici. UI verificata nei runtime Android 8 e 15 di Robolectric; rendering grafico nativo e immagini su Android 15. Nessun test su telefono fisico viene dichiarato.

[Immagini della UI V2.1](release/screenshots-v2.1/) generate dai componenti Android reali durante i test, con dati dimostrativi e intestazione DEMO. Non sono mockup né fotografie di un dispositivo fisico. [Risultati finali V2.1](release/VERIFICA-V2.1.txt).

## Sviluppo e firma

Apri questa cartella in Android Studio e usa JDK 17+ e SDK 35. Per la build di sviluppo:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Nel cloud: `python3 tools/cloud-setup.py`, poi `source tools/cloud-env.sh`. Usa il checkout esistente; non serve un worktree. Lo script installa toolchain ufficiali con checksum verificati e cache dedicate.

La firma release legge `V2_SIGNING_PROPERTIES` da un file privato fuori dal repository. Qui la chiave e le proprietà sono in `/workspace/private-v2-signing/`, con permessi restrittivi; **non sono pubblicate su GitHub**. La copia privata della firma è ancora soltanto nell’ambiente: il download dell’interfaccia è fallito e non è stata recuperata sul PC. Uno snapshot non sostituisce una copia esterna verificata. Conserva una copia privata della directory tramite un canale sicuro disponibile: non caricare chiavi, password o database in Git. `tools/create-private-key.py` crea una chiave solo per la prima installazione e non sostituisce chiavi esistenti.

```
source tools/cloud-env.sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

APK release: `app/build/outputs/apk/release/app-release.apk`. Senza la chiave originale non si può firmare un aggiornamento della V2 installata; una nuova chiave non è un recupero della precedente.
