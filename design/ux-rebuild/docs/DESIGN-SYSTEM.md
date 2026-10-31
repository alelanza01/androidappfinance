# Specifica visiva misurabile

Proposta HTML da approvare; unità CSS px equivalenti alle dp del viewport di progetto, non alle dimensioni fisiche del monitor. `styles.css` è la specifica eseguibile. Riferimento primario: sei immagini originali, senza Elledue Control.

| Token | Chiaro | Scuro |
|---|---|---|
| Testo principale | #142B51 | #E8F2FF |
| Testo secondario | #526B87 | #A9BFD7 |
| Link/accessi | #1875E5 | #84BEFF |
| CTA gradiente | #2B94F5 → #207EEA; testo bianco | stesso blu, testo bianco |
| Superficie | #FFFFFF | #172C43 |
| Fondo | #F6FBFD | #102236 |
| Campo/supporto | #EDF7FB | #203950 |
| Divisore | #E1EDF3 | #2E485F |
| Ring | #A4E4E3 → #32BFAF | stessi toni, alone più tenue |
| Errore | #AE3D53 su #FFEDF2 | #FFACC0 su #4C2F43 |
| Positivo | #18785C | #7DDDBC |

Toni icone: menta #D7F2E6/#24684F, acqua #DFF5F4/#25777A, azzurro #E3F0FD/#286298, sabbia #FAEACB/#815424, pesca #FCE7D9/#9F5F2A, rosa #FCE3E8/#AB405D, lavanda #EEE6FC/#6E4A9B, grigio #EAF0F3/#536B7D. Il significato non dipende solo dal colore: icona, nome e stato restano presenti.

## Proporzioni ricavate dal riferimento

Le fonti sono piccole immagini raster, non un file di progetto: misure ottiche, non coordinate originali certificate. Il generatore dei confronti registra in `comparisons/crops.json` il ritaglio della sola area utile, rimuovendo cornice e riga di stato illustrata.

- Margine utile ≈5–6% della larghezza: 20 px su 360, 16 su 320.
- Ring Home ≈73–76% della larghezza utile; prototipo SVG 266/360, diametro esterno percepito 240 px, tratto 21/280 unità. Contenitore alto 284 px a 360; 248 su 320.
- Card metriche ≈metà larghezza, interspazio 10, altezza minima 70.
- Griglia spesa: 4 colonne ×3 righe, medaglioni 48, icona 27, label 11; testo grande passa a 3 colonne senza comprimere i testi.
- Donut: SVG 202×202 nel quadro Analisi da 360, raggio 91/240, tratto 38/240. Totale al centro, legenda allineata ai margini. Dati demo determinano gli archi.
- Dialogo: larghezza viewport−36, raggio 26, padding 26/22/20; illustrazione 92, documento interno 68×76; pulsanti pieni e spaziatura 11.
- La barra inferiore e gli header appartengono al viewport dell'app: la cornice del telefono desktop non prende parte ai calcoli.

## Tipografia

Font locale del sistema `Arial, Helvetica, sans-serif`, senza download. È una differenza reale dal reference raster; il font nativo definitivo sarà deciso con la traduzione Android, dopo l'approvazione.

| Uso | Dimensione normale | Peso |
|---|---|---|
| Ciao! | 28 | 700 |
| Titolo pagina | 17 | 700 |
| Numero spendibile/importo | 34 | 700–750 |
| Metrica Home | 18 | 700 |
| Sezione | 14–15 | 700 |
| Riga / form / CTA | 13 / 14 / 14 | 600 / 400 / 650 |
| Sottotesto | 11–12 | 400 |
| Label navigazione | 10 | 400, attiva 700 |

Numeri: centesimi interi nel modello demo, separatore decimale italiano, migliaia esplicite, due decimali se necessari. Non troncare negativi o importi lunghi: avvolgimento e scorrimento, non clipping. Tabularità/allineamento destro nelle colonne importo; titolo e valore non competono.

## Geometrie e componenti

- Scala principale: 4 / 8 / 12 / 16 / 20 / 24 / 32.
- Bordi: 1 px, luce/colore tenue; niente outline nero sul contenitore pagina. Focus tastiera blu 3 px sui controlli, mai rimosso dai controlli interattivi.
- Raggi: input 12, righe/card 17, importo 21, dialogo 26, CTA 26, sheet 27 agli angoli alti.
- Ombra card: `0 8px 22px #36688B0C, 0 2px 5px #35678608`; CTA include luce interna e lieve ombra blu. Niente ombre native Material.
- CTA: altezza minima 50; touch principale 44×44 o maggiore. Navigazione: 70 + separazione superiore; + 54, sollevato di 20, alone 4.
- Icone: griglia vettoriale 24, tratto 1.7 arrotondato, riempimento pastello per categorie; 19–21 nei controlli, 27 nelle categorie. Nessuna emoji/font icona dipendente dalla piattaforma.
- Calendario: sette colonne a larghezza disponibile, altezza cella 43–44; su 320 alcune celle sono più strette di 44. L'elenco resta alternativa con target ≥44. Questa eccezione non è dichiarata conforme al target ideale.
- Legenda Analisi informativa, non falsi pulsanti piccoli. Il filtro categoria è un controllo di 44 px nell'header e apre un menu con righe grandi.
- Componenti di navigazione secondaria: righe con icona, nome, scopo e freccia; mai una lunga colonna di CTA identiche.

## Stati e comportamento

- Premuto: lieve variazione luminosità/traslazione 1 px; niente cambio di dimensioni. Focus visibile; disabilitato opacità .45, non utilizzato al posto di una spiegazione di errore.
- Form: errori testuali, dati conservati; categoria e rate non salvano transazioni. Conferma uscita solo se modificato.
- Modalità: sfondo blur CSS 5 px + tinta #183F4C6B, contenuto sottostante `inert`, focus nel dialogo, Tab contenuto, Escape chiude, ritorno al controllo precedente quando esiste. Nessun menu sotto la barra.
- Salvataggio: aggiornamento demo + toast, nessuna scrittura nei dati Android. Screenshot di confronto nascondono solo il toast transitorio, non modificano i pixel dell'app.
- Temi: prima chiaro; scuro conserva gerarchia e supporti, non introduce oro o blu notte Elledue. Le illustrazioni pastello restano riconoscibili.
- Font grande: 145%, categorie a tre colonne, coppie di campi verticali, metriche e azioni Home impilate, scorrimento. Importi alti e titoli lunghi si spezzano.
- Viewport: 320×640, 360×800, 411×891; barra “PROTOTIPO” esterna di 28 px su mobile. Safe area tramite `env()`. Tastiera demo di 212 px esplicita; CTA resta sopra. Il browser può mostrare anche la propria tastiera, diversa dalla simulazione.
- Riduzione movimento: `prefers-reduced-motion`; nessuna animazione essenziale per capire il risultato.

Non è stato eseguito un audit completo con screen reader, utenti o telefono. La misura del contrasto e la dimensione finale in sp Android vanno verificate nella fase 2; non si dichiara una certificazione WCAG da queste catture.
