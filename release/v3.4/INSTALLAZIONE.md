# Aggiornare Posso spenderlo? a 3.4.0

1. Nell'app attuale: Impostazioni → Backup → Esporta backup. Conserva il JSON sul telefono o PC.
2. Scarica `Posso-spenderlo-V3.4.0-RC.apk` da questa cartella GitHub usando **Download raw file** (icona freccia in giù). Se scarichi dal PC, trasferiscilo sul telefono via USB.
3. Sul telefono apri l'APK da Download/File. Se richiesto, consenti temporaneamente a File/browser di installare app da questa origine.
4. Tocca **Aggiorna**, poi Apri. **Non disinstallare la versione precedente**: l'aggiornamento mantiene l'archivio e ne migra lo schema.

Se Android propone solo Installa come app distinta, o rifiuta l'aggiornamento per firma incompatibile, fermati e conserva l'app attuale e il backup. Il pacchetto dell'aggiornamento deve essere `it.privato.disponibile.v2`.

## Breve prova sul telefono

- Confronta liquidità, spendibile, riserva, numero di movimenti e credito residuo con il backup precedente.
- Registra una piccola spesa; verifica modifica/eliminazione, data e categoria.
- Alterna Impegni → Elenco/Calendario, apri un giorno. La selezione non deve registrare pagamenti.
- Verifica un rimborso parziale e il suo storico: non deve aumentare le entrate personali.
- Apri Rate: distingui Importo totale/Ogni rata. La bozza non salva finché non premi Salva piano.
- Prova Simula spesa con più importi e periodi; tornando Home i valori registrati devono essere gli stessi.
- Controlla tema scuro, testo ingrandito, tastiera e tasto Indietro.
- Esporta un nuovo backup; importa solo una copia di prova se vuoi verificare anche il ripristino.

Nessuna pubblicazione Play Store, nessun account, nessun costo ricorrente. La chiave di firma non è inclusa in repository, APK sorgenti o backup dei dati.
