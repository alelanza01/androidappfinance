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
 'Riferimento: cinque radio, selezione e CTA. UX: importo per rata, totale e avanzate. Residui: semantica per rata diversa dal mockup; opzioni avanzate aggiunte.',
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
  fig,axes=plt.subplots(1,2,figsize=(9.4,12),gridspec_kw={'wspace':.08});fig.suptitle(f'{i}. {name} — proposta UX/UI · 1.1',fontsize=17,y=.98)
  for ax,image,title in zip(axes,[a,b],['Concept · area utile','Prototipo HTML · dati DEMO']):ax.imshow(image);ax.set_title(title,fontsize=11,pad=12);ax.axis('off');ax.set_anchor('N')
  import textwrap
  fig.text(.5,.045,'\n'.join(textwrap.wrap(note,115))+'\nStessa larghezza, altezze non deformate. Raster originale a bassa risoluzione. Non è una prova Android.',ha='center',fontsize=9,color='#506b86')
  fig.subplots_adjust(top=.92,bottom=.12,left=.03,right=.97);fig.savefig(out/f'{i:02d}-confronto.png',dpi=140);pdf.savefig(fig);plt.close(fig)
  # Overlay is geometric, not a similarity score: separate heights remain documented.
  canvas=Image.new('RGB',(720,max(a.height,b.height)),'white');canvas.paste(a,(0,0));other=Image.new('RGB',canvas.size,'white');other.paste(b,(0,0));Image.blend(canvas,other,.5).save(out/f'{i:02d}-overlay-50.png')
(out/'crops.json').write_text(json.dumps(crops,indent=2)+'\n')
print('Creati 6 confronti, 6 overlay al 50%, 6 ritagli espliciti e un PDF.')
