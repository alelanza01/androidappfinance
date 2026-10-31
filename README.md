# Posso spenderlo? — V3.4.1 RC2

Revisione mirata della RC V3.4, sul branch `feature/v3.4-native-ux-rebuild`. Base `a8a8cdf`, prototipo approvato `a49b5f4`. Nessun merge su main e nessuna release definitiva.

**[APK RC2](release/v3.4-rc2/Posso-spenderlo-V3.4.1-RC2.apk)** · [Sorgenti ZIP](release/v3.4-rc2/Posso-spenderlo-V3.4.1-RC2-sorgenti.zip) · [Confronti PDF](release/v3.4-rc2/comparisons/Confronti-RC2.pdf) · [Screenshot Android](release/v3.4-rc2/screenshots/) · [Rapporto](release/v3.4-rc2/RAPPORTO.md) · [Collaudo sul telefono](release/v3.4-rc2/COLLAUDO-TELEFONO.md).

Su GitHub apri l'APK e premi **Download raw file**. Esporta prima un backup dall'app attuale; installa come aggiornamento **senza disinstallare**.

Identità invariata `it.privato.disponibile.v2`; versione `3.4.1-rc2`, codice **341**. Firma originale V2, confrontata anche con la RC 340. Nessuna chiave o password nei sorgenti. Gli artefatti della [RC precedente](release/v3.4/) restano disponibili.

Correzioni: selezione esplicita FIXED/ROLLING e data effettiva, linguaggio e riepilogo Anticipi, composizione di Registra/Impostazioni/Analisi/Onboarding/Movimenti. Il dominio economico e gli archivi esistenti sono conservati. SQLite 5 e JSON 6 invariati; backup precedenti ancora importabili.

App privata Android 8+, Kotlin/SQLite, offline, senza account, server, analytics, permesso Internet o costi ricorrenti. I rimborsi non sono guadagni e i crediti non riscossi non sono spendibili.

## Build e verifiche

Apri in Android Studio con JDK 17+ e SDK 35; wrapper Gradle incluso. Nel cloud: `source tools/cloud-env.sh`.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

La firma release legge `V2_SIGNING_PROPERTIES` da un file privato esterno al repository. Non creare una chiave sostitutiva. In sua assenza si può compilare `assembleDebug`, che non sostituisce la release installata.

163 test, inclusi i 156 precedenti. Risultati esatti in [VERIFICA.json](release/v3.4-rc2/VERIFICA.json). Rendering Android con Robolectric Native Graphics, senza emulatore/telefono. Per rigenerare i confronti, dal repository completo esegui `node tools/qa-rc2/capture-prototype.cjs`, i test e poi `python tools/qa-rc2/compare.py`: stessa fixture, data, viewport e selezioni, controllati prima di affiancare le immagini.
