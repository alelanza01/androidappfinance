# Posso spenderlo? 2.0

App Android privata, offline e gratuita per capire quanto denaro puoi spendere dopo impegni e riserva. Kotlin nativo, SQLite, Android 8.0+, nessun server, account, analytics o permesso Internet.

**[Scarica l'APK V2](release/Posso-spenderlo-V2.apk)** — su GitHub apri il file e premi **Download raw file**. [SHA-256](release/SHA256-V2.txt). I sorgenti completi sono in questo repository.

## Installazione e rapporto con la V1

Questa V2 è una ricostruzione autorizzata: application ID `it.privato.disponibile.v2`, versione `2.0.0` (200), nuova firma privata. **Si installa accanto alla V1**, senza disinstallarla e senza alterarne i dati. Parte vuota e non importa il database V1. La precedente chiave non è disponibile: non è un aggiornamento in-place della V1.

1. Scarica l'APK sul PC e trasferiscilo al telefono, oppure scaricalo dal telefono.
2. Aprilo da File, consenti l'installazione da questa fonte se richiesto, poi premi Installa.
3. Apri **Posso spenderlo? 2**. Inserisci la liquidità iniziale da Altro oppure aggiorna quella attuale da Oggi, poi registra i tuoi movimenti.

Per aggiornamenti futuri della V2 serviranno lo stesso ID e la stessa chiave. Prima di aggiornare, usa **Altro → Esporta backup**. Conserva il file in una cartella locale sicura; disinstallare la V2 cancella il suo archivio.

## Cosa comprende

- **Oggi:** liquidità, uscite programmate fino alla data scelta, riserva, spendibile spiegabile, prossima scadenza, criticità e simulatore immediato senza salvataggio.
- **Grafici:** anello delle spese effettuate per categoria/mese, area con storico e due scenari futuri, colonne entrate/uscite a 3/6/12 mesi. Tocca i grafici o le categorie per i dettagli. Mese selezionabile.
- **Scadenze:** calendario mensile, arretrati, pagamenti e incassi futuri, ricorrenze settimanali/mensili/annuali e rate mensili finite (1–120, importo per rata). Conferma manuale del pagamento/incasso. Le ricorrenze continue sono preparate almeno per 12 mesi e fino alla previsione.
- **Storico:** ricerca per descrizione/categoria, filtri per stato, entrata/uscita e categoria. Modifica ed eliminazione dei singoli movimenti, con ricalcolo del saldo.
- **Altro:** liquidità iniziale, riserva, data di previsione, categorie con colore, tema chiaro/scuro/sistema, backup JSON e demo isolata. Avvisi locali giornalieri opzionali.

## Regole finanziarie

Tutti gli importi sono centesimi interi, senza calcoli finanziari in virgola mobile. Input massimo €9.999.999,99, due decimali, senza separatori delle migliaia.

**Liquidità = base iniziale + entrate effettive − uscite effettive + rettifiche.**

**Spendibile = liquidità − tutte le uscite previste entro la data scelta (inclusi arretrati) − riserva.**

Le entrate future, anche probabili, non aumentano lo spendibile. La proiezione corallo considera solo le uscite note; quella oro aggiunge le entrate previste, comprese le non garantite. Sono scenari distinti dallo storico verde. I grafici mensili contano solo incassi e pagamenti effettivi; le rettifiche sono escluse.

Confermare una scadenza la rende effettiva **oggi** una sola volta. Puoi correggere la data nello storico. Eliminare un movimento effettivo rimuove il suo effetto sul saldo. Modificare la liquidità iniziale cambia anche la base dello storico: per riconciliare il saldo di oggi usa Aggiorna liquidità.

Ogni occorrenza ricorrente ha un'identità univoca. Modifiche/eliminazioni non la rigenerano. Per cambiare una serie intera, interrompila dal calendario e creane una nuova; i movimenti già effettuati restano. Gli ancoraggi mensili mantengono il giorno originale quando possibile (31 gennaio → fine febbraio → 31 marzo).

## Backup e promemoria

Il backup è JSON **non cifrato**, con dati personali. Scegli una destinazione locale; i provider cloud eventualmente presenti nel selettore Android non fanno parte dell'app. L'importazione valida formato, importi, date e riferimenti prima di sostituire i dati in una transazione. Conserva prima una copia privata dell'archivio precedente, esportabile da Altro. Limiti di importazione: 5 MB, 20.000 operazioni, 100 categorie e 2.000 serie; non viene eseguito codice dal file.

Gli avvisi richiedono consenso su Android 13+. JobScheduler controlla circa ogni 24 ore le uscite arretrate o entro domani; Android può ritardare l'esecuzione per batteria e altre restrizioni. Non sono allarmi puntuali. Il calendario resta il riferimento.

## Verifiche e immagini

I test coprono contabilità, importi, date, doppia conferma, ricorrenze, rate, riconciliazione, CRUD, categorie, persistenza, backup, ricerca, navigazione, simulatore, avvisi e grafici. UI verificata nei runtime Android 8 e 15 di Robolectric; rendering grafico nativo e immagini su Android 15. Nessun test su telefono fisico viene dichiarato.

[Immagini della UI](release/screenshots/) generate dai componenti Android reali durante i test, con dati dimostrativi e intestazione DEMO. Non sono mockup né fotografie di un dispositivo fisico. [Risultati finali](release/VERIFICA-V2.txt).

## Sviluppo e firma

Apri questa cartella in Android Studio e usa JDK 17+ e SDK 35. Per la build di sviluppo:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Nel cloud: `python3 tools/cloud-setup.py`, poi `source tools/cloud-env.sh`. Usa il checkout esistente; non serve un worktree. Lo script installa toolchain ufficiali con checksum verificati e cache dedicate.

La firma release legge `V2_SIGNING_PROPERTIES` da un file privato fuori dal repository. Qui la chiave e le proprietà sono in `/workspace/private-v2-signing/`, con permessi restrittivi; **non sono pubblicate su GitHub**. Conserva lo snapshot dell'ambiente e una copia privata di questa directory tramite un canale sicuro disponibile: non caricare chiavi, password o database in Git. `tools/create-private-key.py` crea una chiave solo per la prima installazione e non sostituisce chiavi esistenti.

```
source tools/cloud-env.sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

APK release: `app/build/outputs/apk/release/app-release.apk`. Senza la chiave originale non si può firmare un aggiornamento della V2 installata; una nuova chiave non è un recupero della precedente.
