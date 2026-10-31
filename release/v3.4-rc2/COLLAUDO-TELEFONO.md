# Aggiornare dalla RC 340 alla RC2 341

1. Nell'app attuale apri Impostazioni → Backup → Esporta backup. Conservalo prima di installare l'aggiornamento.
2. Scarica `Posso-spenderlo-V3.4.1-RC2.apk` da questa cartella GitHub con **Download raw file** (freccia in giù). Se usi il PC, trasferisci l'APK sul telefono via USB.
3. Apri l'APK sul telefono e scegli **Aggiorna**. Consenti a File/browser l'installazione da questa fonte, se Android lo richiede. **Non disinstallare** l'app attuale.
4. Verifica che saldo, impegni, riserva, anticipi e storico siano rimasti invariati.

## Prove mirate

- Impostazioni → Riserva e disponibilità: scegli 30 giorni. Deve comparire la modalità Mobile e la Home deve indicare la stessa data finale.
- Scegli Data personalizzata e conferma una data diversa: deve risultare Data fissa attiva, anche chiudendo e riaprendo l'app. I pulsanti 30/60/90 non devono restare selezionati.
- Torna a 60 o 90 giorni: verifica modalità Mobile e data finale. Dopo il cambio del giorno la data mobile avanza; la data fissa rimane tale. Gli arretrati non spariscono dallo spendibile.
- Anticipi: confronta totale generale e residuo dei risultati; cerca un nome senza corrispondenze. Il residuo filtrato deve diventare zero senza modificare il totale generale. Gli scaduti ancora aperti devono apparire sia in Aperti sia in Scaduti.
- Registra una spesa di prova e correggila/eliminala; verifica le rate Totale/Ogni rata e le bozze tornando indietro da categorie/date. Due tocchi su Salva non devono duplicare il movimento.
- Registra un rimborso parziale di prova: la liquidità aumenta, il credito residuo diminuisce, le entrate personali non aumentano.
- Prova più importi/periodi nel simulatore: tornando Home l'archivio deve essere identico.
- Controlla menu Registra, Impostazioni, Anticipi, Analisi e Movimenti nei due temi; ripeti con testo Android ingrandito. Chiusura dei menu, CTA e tastiera devono restare utilizzabili.
- Esporta un nuovo backup. La prova dell'importazione va fatta solo avendo già conservato il backup precedente.

La guida iniziale non viene imposta di nuovo agli archivi già configurati. Le quattro schermate aggiornate sono documentate nella galleria con dati di prova.

È una release candidata: nessuna pubblicazione definitiva e nessun merge su main. Il collaudo fisico non è stato eseguito dal cloud.
