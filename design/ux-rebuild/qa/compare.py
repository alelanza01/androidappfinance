from pathlib import Path
import json,os
from PIL import Image,ImageOps,ImageDraw,ImageFont
os.environ['MPLCONFIGDIR']='/tmp/ps-prototype-mpl'
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages
root=Path(__file__).resolve().parents[1];repo=root.parents[1];out=root/'comparisons';out.mkdir(exist_ok=True)
files=['01_Home.png','02_Nuova_Spesa.png','03_Categorie.png','04_Analisi.png','05_Rate.png','06_Conferma_Uscita.png']
shots=['01-home-light.png','02-spesa-light.png','03-categorie-light.png','04-analisi-light.png','05-rate-light.png','06-conferma-light.png']
names=['Home','Nuova spesa','Categorie','Analisi','Numero di rate','Conferma di uscita']
notes=[
 'Riferimento: ring rifinito, metriche e due impegni con stato. UX: liquidità nominata, due accessi secondari. Residui: icone più semplici e altezze differenti; badge e azioni extra occupano spazio.',
 'Riferimento: area importo, griglia, capsula, data/rate e CTA. UX: solo importo obbligatorio. Residui: categorie reali differenti, icone meno illustrate, data nativa browser.',
 'Riferimento: ricerca, preferite, lista e cuori. UX: gestione esplicita fuori dalla scelta. Residui: ordine e nomi dell’archivio, dettaglio pittorico non riprodotto.',
 'Riferimento: tabs, donut, legenda e area blu. UX: filtro accessibile e scenari separati. Residui: gradini/data reale, donut e proporzioni ancora differenti dal raster.',
 'Riferimento: cinque radio, selezione e CTA. UX: scelta Totale/Ogni rata, quote esatte e avanzate. Residui: selettore di significato e riepilogo aggiunti; maggiore densità verticale.',
 'Riferimento: documento, blur, dialogo, due azioni. UX: focus/annulla specifici. Residui: illustrazione ridisegnata, titolo e spazi non identici; blur browser, non Android.'
]
crops={}
with PdfPages(out/'Confronti-UX-Rebuild.pdf') as pdf:
 for i,(ref,shot,name,note) in enumerate(zip(files,shots,names,notes),1):
  im=Image.open(repo/'release/v3.2/references'/ref).convert('RGB');w,h=im.size
  # Explicit proportional interior crop: 4% frame left/right, top status band 24 px Home / 18 px other screens, bottom frame 2.5%.
  box=(round(w*.04),24 if i==1 else 18,round(w*.96),round(h*.975));crops[ref]={'source':[w,h],'crop':box,'reason':'Esclusi cornice laterale, riga di stato illustrata e bordo inferiore; nessun ritaglio dei contenuti per favorire il risultato.'};original=im.crop(box)
  current=Image.open(root/'screenshots'/shot).convert('RGB')
  # Compare at a common width; preserve aspect ratio and top alignment, never stretch to equal height.
  a=original.resize((720,round(original.height*720/original.width)),Image.Resampling.NEAREST)
  b=current.resize((720,round(current.height*720/current.width)),Image.Resampling.LANCZOS)
  a.save(out/f'{i:02d}-concept-area-utile.png')
  fig,axes=plt.subplots(1,2,figsize=(9.4,12),gridspec_kw={'wspace':.08});fig.suptitle(f'{i}. {name} — proposta UX/UI · 1.2',fontsize=17,y=.98)
  for ax,image,title in zip(axes,[a,b],['Concept · area utile','Prototipo HTML · dati DEMO']):ax.imshow(image);ax.set_title(title,fontsize=11,pad=12);ax.axis('off');ax.set_anchor('N')
  import textwrap
  fig.text(.5,.045,'\n'.join(textwrap.wrap(note,115))+'\nStessa larghezza, altezze non deformate. Raster originale a bassa risoluzione. Non è una prova Android.',ha='center',fontsize=9,color='#506b86')
  fig.subplots_adjust(top=.92,bottom=.12,left=.03,right=.97);fig.savefig(out/f'{i:02d}-confronto.png',dpi=140);pdf.savefig(fig);plt.close(fig)
  # Overlay is geometric, not a similarity score: separate heights remain documented.
  canvas=Image.new('RGB',(720,max(a.height,b.height)),'white');canvas.paste(a,(0,0));other=Image.new('RGB',canvas.size,'white');other.paste(b,(0,0));Image.blend(canvas,other,.5).save(out/f'{i:02d}-overlay-50.png')
(out/'crops.json').write_text(json.dumps(crops,indent=2)+'\n')
print('Creati 6 confronti, 6 overlay al 50%, 6 ritagli espliciti e un PDF.')
# New surfaces are extensions of the same system, not replacements for the six references.
gallery=[('Selettore date','12-data-light.png'),('Rate · importo totale','12-rate-totale-light.png'),('Rate · ogni rata','12-rate-ogni-rata-light.png'),('Periodo protetto','12-orizzonte-light.png'),('Calendario operativo','12-calendario-giorno-light.png'),('Liquidità simulata','12-previsione-light.png'),('Dettaglio scadenza / riserva','12-previsione-dettaglio-light.png'),('Primo avvio · liquidità','12-onboarding-1-light.png'),('Primo avvio · riserva','12-onboarding-2-light.png'),('Primo avvio · impegni','12-onboarding-3-light.png'),('Primo avvio · risultato','12-onboarding-4-light.png'),('Categorie rapide','12-rapide-light.png')]
with PdfPages(out/'Galleria-FASE-1.2.pdf') as pdf:
 for start in range(0,len(gallery),2):
  fig,axes=plt.subplots(1,2,figsize=(9,11));fig.suptitle('Posso spenderlo? · Fase 1.2 · nuove interfacce',fontsize=15,y=.98)
  for ax,(title,name) in zip(axes,gallery[start:start+2]):ax.imshow(Image.open(root/'screenshots'/name));ax.set_title(title,fontsize=11);ax.axis('off');ax.set_anchor('N')
  fig.text(.5,.035,'Catture browser reali, dati DEMO al 31 ottobre 2026. Non un test Android né approvazione del design.',ha='center',fontsize=8,color='#506b86');fig.subplots_adjust(top=.93,bottom=.08,left=.04,right=.96,wspace=.15);pdf.savefig(fig);plt.close(fig)
html='''<!doctype html><html lang="it"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Galleria Fase 1.2</title><style>body{font:16px Arial;color:#142b51;background:#edf5f8;margin:28px}h1{font-size:25px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:24px}figure{margin:0;padding:16px;background:white;border-radius:20px}img{width:100%;max-width:360px;display:block;margin:auto}figcaption{font-weight:600;padding:0 0 12px}a{color:#1875e5}</style><h1>Posso spenderlo? · Galleria Fase 1.2</h1><p>Catture browser effettive, dati dimostrativi. <a href="PROTOTIPO.html">Apri il prototipo navigabile</a>. La galleria non sostituisce le interazioni.</p><main>'''
import html as html_module
for title,name in gallery:html+=f'<figure><figcaption>{html_module.escape(title)}</figcaption><a href="screenshots/{name}"><img src="screenshots/{name}" alt="{html_module.escape(title)}"></a></figure>'
(root/'GALLERIA.html').write_text(html+'</main></html>')
print('Galleria 1.2: 12 schermate, HTML e PDF.')
