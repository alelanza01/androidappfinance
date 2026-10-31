# Posso spenderlo? — Prototipo UX/UI da approvare

**Solo FASE 1. Non è un APK né una riscrittura web dell'app.** Base Android V3.3 `cfb9019`, branch di lavoro `design/ux-rebuild-prototype`. Nessuna modifica alla V3.3 o a main.

## Aprirlo su Windows

1. Scarica `Posso-spenderlo-UX-prototipo.zip` da questa directory su GitHub: apri il file e premi **Download raw file** (freccia di download in alto a destra).
2. Nel PC: tasto destro sullo ZIP → **Estrai tutto…** → **Estrai**.
3. Apri la cartella estratta e fai doppio clic su **PROTOTIPO.html**. Si apre nel browser, senza installazioni, login o server.

In alternativa scarica soltanto `PROTOTIPO.html` con **Download raw file** e aprilo dal PC. Aprire il codice HTML nell'anteprima GitHub non esegue il prototipo. Non serve pubblicare il repository o attivare GitHub Pages.

Nel browser scegli **Percorsi da provare** oppure **Tutte le schermate**. Il prototipo si usa realmente: puoi salvare spese, entrate, anticipi e rimborsi, cambiare riserva, filtrare e simulare. **Ripristina demo** ripristina tutto. **Ricaricare la pagina elimina le modifiche demo**: restano solo nella memoria della scheda. Usa Esporta demo se vuoi conservare la tua prova; non importare dati reali.

Sul PC la cornice ha viewport 360×800. Restringendo la finestra il layout diventa mobile; il menu Guida resta sopra il telefono. Tema e testo grande si trovano anche in Impostazioni → Aspetto. La tastiera disegnata è esplicitamente dimostrativa e si attiva dal form/Guida, non è Android.

## Cosa provare prima di approvare

- Leggere €520 su €1.200 di liquidità; aprire l'anello per capire riserva e impegni.
- + → Registra spesa: basta un importo. Scegliere categoria e rate, tornare senza perdere la bozza, provare l'uscita senza salvare.
- Registrare un'entrata e programmare un pagamento; distinguere ciò che è effettivo da ciò che è previsto.
- Anticipi da recuperare → Pulizie appartamento → Registra rimborso. Il residuo diminuisce, il rimborso non diventa un guadagno.
- Simulare una spesa da €600: mostra il superamento del margine senza registrarla.
- Impostazioni → Riserva e disponibilità → Riserva: cambiarla e ritornare alla Home.
- Esplorare anche stati vuoti, negativi, importo lungo, calendario, filtri, backup e notifiche simulate dalla Guida.

## Consegne e limiti

- [Mappa completa e audit UX](docs/NAVIGAZIONE-E-AUDIT.md)
- [Design system misurabile](docs/DESIGN-SYSTEM.md)
- [Piano Android, non eseguito](docs/ANDROID-DOPO-APPROVAZIONE.md)
- [Valutazione visiva e differenze residue](docs/VERIFICA-VISIVA.md)
- [Rapporto delle verifiche](docs/VERIFICA-FASE-1.md)
- [Screenshot di pagine, menu e dimensioni](screenshots/)
- [Sei confronti e PDF](comparisons/)
- [Esiti riproducibili del browser](qa/results.json)

Il denaro è interamente dimostrativo, con data fissa **31 ottobre 2026**. Il modello JS serve solo alla coerenza delle interazioni; non sostituisce Finance.kt. Non gestisce la materializzazione illimitata Android: una ricorrenza continua è rappresentata dai primi 12 eventi demo, chiaramente indicati nell'interfaccia. JSON prototipo distinto e incompatibile con i backup Android reali. Notifiche, selettore date e tastiera non verificano i corrispondenti componenti Android. Nessun test con utenti reali o dispositivo fisico eseguito.

La rete non è necessaria al prototipo: CSS, JS e SVG sono locali, nessuna API, font CDN o analytics. La QA cloud usa un server temporaneo sul solo loopback perché il browser gestito blocca `file://`; non è necessario per l'utente. Il file autonomo contiene tutti gli asset e viene verificato anche come pagina unica; il doppio clic Windows resta da confermare sul PC dell'utente.

## Riproduzione per sviluppatori

`node qa/verify.cjs` richiede Playwright e Chromium locali; `CHROMIUM` può indicare l'eseguibile. Lo script avvia/chiude soltanto un server loopback temporaneo, esegue scenari e catture. Non tocca dati Android. `python qa/compare.py` genera affiancamenti e sovrapposizioni a partire dalle catture; richiede Pillow/matplotlib. `python qa/package.py` crea HTML autonomo e ZIP senza chiavi, APK, cache o dati reali.

**Serve approvazione esplicita prima della FASE 2 Android.** Non è stata attribuita alcuna approvazione automatica.
