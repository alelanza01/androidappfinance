# Posso spenderlo? 3.4.0 — Release candidata nativa

App privata Android 8+, Kotlin e SQLite. Offline: nessun account, server, analytics, permesso Internet o costo ricorrente.

**[Scarica APK V3.4.0 RC](release/v3.4/Posso-spenderlo-V3.4.0-RC.apk)** · [Sorgenti ZIP](release/v3.4/Posso-spenderlo-V3.4.0-sorgenti.zip) · [Screenshot nativi](release/v3.4/screenshots/) · [Confronti PDF](release/v3.4/comparisons/Confronti-V3.4.pdf) · [Rapporto](release/v3.4/RAPPORTO.md) · [Aggiornamento e prova sul telefono](release/v3.4/INSTALLAZIONE.md).

Su GitHub apri il file APK e premi **Download raw file** (freccia in giù). Esporta prima un backup dall'app attuale e installa come aggiornamento, **senza disinstallare**.

Branch `feature/v3.4-native-ux-rebuild`, base Android `cfb9019`. Prototipo approvato `a49b5f4`, recuperato selettivamente in `design/ux-rebuild/`; nessun merge su `main`.

Identità `it.privato.disponibile.v2`, versione 3.4.0 / codice 340. SQLite 4→5 e backup JSON 5→6; importazione formati 2–6. La firma usa la chiave originale V2, confrontata con gli APK precedenti. Nessuna chiave o password è inclusa nei sorgenti.

## Funzioni

- Spendibile da liquidità effettiva, impegni e riserva; orizzonte fisso o mobile 30/60/90 giorni.
- Spese, entrate effettive/previste, categorie preferite/fissate e rapide ordinate; bozze conservate, calendario nativo e salvataggi transazionali.
- Rate con importo totale oppure per rata, ripartizione esatta dei centesimi, ricorrenze e conservazione dello storico.
- Impegni Elenco/Calendario, dettaglio del giorno e conferme esplicite.
- Anticipi da recuperare, rimborsi parziali/totali e storico; i crediti non riscossi non sono spendibili e i rimborsi non sono guadagni.
- Simulatore con liquidità prevista, riserva, periodo indipendente e scenario separato degli incassi attesi; nessuna scrittura nell'archivio.
- Primo avvio in quattro passi, analisi su dati reali, temi chiaro/scuro, avvisi locali facoltativi, esportazione/importazione con copia precedente.

## Sviluppo

Apri la cartella del progetto in Android Studio con JDK 17+ e SDK 35. Il wrapper Gradle è incluso. La prima sincronizzazione scarica gratuitamente le dipendenze.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Per una release aggiornabile serve il materiale di firma originale, conservato privatamente: configura `V2_SIGNING_PROPERTIES` verso il relativo file locale, esterno al repository, quindi `./gradlew :app:assembleRelease`. In sua assenza non rigenerare una chiave sostitutiva. Il debug usa l'identità di firma di sviluppo e non sostituisce la release installata.

Nel cloud: `source tools/cloud-env.sh`. I rendering si generano con i test Robolectric Native Graphics; `python tools/compare_native34.py` prepara galleria e confronti. Non sono screenshot di un emulatore né prove sul telefono del proprietario.

Dati personali solo sul dispositivo. Il JSON di backup è in chiaro: conservarlo privatamente. Il recupero della chiave sul PC resta un'attività separata; questa release non pubblica il materiale privato.
