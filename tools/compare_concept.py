"""QA comparisons: original reference pixels and actual Android captures; no generated UI.
Run after ConceptScreenshotsTest. Matplotlib/Pillow are developer tools, not app dependencies.
"""
from pathlib import Path
import os,argparse,textwrap
root=Path(__file__).resolve().parents[1]
os.environ.setdefault('MPLCONFIGDIR',str(root/'app/build/mpl-cache'))
os.environ.setdefault('XDG_CACHE_HOME',str(root/'app/build/xdg-cache'))
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages
from PIL import Image

parser=argparse.ArgumentParser();parser.add_argument('--version',default='3.3');args=parser.parse_args()
release=root/f'release/v{args.version}';refs=root/'release/v3.2/references';shots=root/'app/build/screenshots';out=release/'comparisons';out.mkdir(parents=True,exist_ok=True)
pairs=[('01_Home.png','concept-01-home-light.png','Home'),('02_Nuova_Spesa.png','concept-02-spesa-light.png','Nuova spesa'),('03_Categorie.png','concept-03-categorie-light.png','Categorie'),('04_Analisi.png','concept-04-analisi-light.png','Analisi'),('05_Rate.png','concept-05-rate-light.png','Rate'),('06_Conferma_Uscita.png','concept-06-conferma-light.png','Conferma di uscita')]
notes=[
 'Corretto: ring/gerarchie, superfici e pillole con icona, righe impegni e + sollevato. Residuo: azioni extra e ritmo verticale ancora diversi; la resa delle icone è più geometrica.',
 'Corretto: importo con valuta, medaglioni selezionabili, capsula categoria, descrizione leggera e Data/Rate insieme. Residuo: nomi/icone non sono tutti quelli del disegno; font grandi richiedono scorrimento.',
 'Corretto: ricerca compatta, preferite integrate, lista più leggera, filtri discreti e cuori sottili. Residuo: ordine e categorie dipendono dall’archivio, manca il dettaglio illustrato del concept.',
 'Corretto: tabs/legenda più leggere, prezzo centrale, saldo con riempimento, margini e date del tratto visibile. Residuo: linea a gradini, non fluida; i dati reali danno una composizione diversa dal mockup.',
 'Corretto: radio uniformi e riepilogo unico, continua/manuale nelle opzioni secondarie. Residuo: importo per ciascuna rata e totale del piano, non divisione del totale; pulsante Applica, poi Salva.',
 'Corretto: sfondo ammorbidito, illustrazione, angoli e spazi delle due CTA. Residuo: sfocatura approssimata da bitmap in memoria, non dal compositore hardware; icona semplificata.'
]
def figure(columns,title,note):
 fig,axes=plt.subplots(1,len(columns),figsize=(8 if len(columns)==2 else 12,10),facecolor='white')
 for ax,(path,label) in zip(axes,columns):
  with Image.open(path) as image:ax.imshow(image,interpolation='nearest' if label.startswith('Concept') else 'antialiased')
  ax.set_title(label,fontsize=12,pad=10);ax.set_axis_off();ax.set_anchor('N')
 fig.suptitle(title,fontsize=15,y=.985)
 footer=textwrap.fill(note,145 if len(columns)==3 else 108)+'\nReference originale a bassa risoluzione. Dati DEMO. Render Android nativi nel cloud, non fotografie del telefono.'
 fig.text(.5,.018,footer,ha='center',fontsize=7.5,color='#506D94')
 fig.tight_layout(rect=[0,.09,1,.95]);return fig
with PdfPages(out/f'Confronti-V{args.version}.pdf') as pdf, PdfPages(out/f'Evoluzione-V3.2-V{args.version}.pdf') as evolution:
 for i,((ref,shot,title),note) in enumerate(zip(pairs,notes),1):
  cols=[(refs/ref,'Concept originale'),(shots/shot,f'Android V{args.version}')]
  fig=figure(cols,f'{i}. {title} — Posso spenderlo? V{args.version}',note);fig.savefig(out/f'{i:02d}-confronto.png',dpi=160);pdf.savefig(fig);plt.close(fig)
  baseline=root/'release/v3.2/screenshots'/shot
  if baseline.exists():
   cols.insert(1,(baseline,'Android V3.2'))
   fig=figure(cols,f'{i}. {title} — confronto prima/dopo',note);fig.savefig(out/f'{i:02d}-evoluzione.png',dpi=140);evolution.savefig(fig);plt.close(fig)
(release/f'AUTOCRITICA-V{args.version}.txt').write_text('\n\n'.join(f'{i}. {pair[2]}\n{note}' for i,(pair,note) in enumerate(zip(pairs,notes),1))+'\n\nNon dichiariamo equivalenza pixel-per-pixel: la somiglianza va giudicata nei confronti, non dal numero di test passati. Le differenze sopra non vengono nascoste come soli limiti tecnici.\n')
print('Creati 6 affiancamenti, 6 confronti prima/dopo, due PDF e autocritica per schermata.')
