"""Reproducible visual review, no similarity score or claim of pixel identity.
Run after :app:testDebugUnitTest. Only native View captures and public design references are read.
"""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'release/v3.4'
SHOTS=OUT/'screenshots';SHOTS.mkdir(exist_ok=True)
for f in (ROOT/'app/build/screenshots').glob('v34-*.png'):shutil.copy2(f,SHOTS/f.name)
COMP=OUT/'comparisons';COMP.mkdir(exist_ok=True)
rows=[
 ('01','Home','01-home-light.png','v34-A-home-light.png'),
 ('02','Nuova spesa','02-spesa-light.png','v34-B-spesa-light.png'),
 ('03','Categorie','03-categorie-light.png','v34-B-categorie-light.png'),
 ('04','Analisi','04-analisi-light.png','v34-D-analisi-light.png'),
 ('05','Rate','05-rate-light.png','v34-B-rate-light.png'),
 ('06','Conferma uscita','06-conferma-light.png','concept-06-conferma-light.png'),
 ('07','Menu Registra','07-registra-light.png','v34-A-registra-light.png'),
 ('08','Impegni Elenco','10-impegni-light.png','v34-C-impegni-elenco-light.png'),
 ('09','Impegni Calendario','11-calendario-light.png','v34-C-impegni-calendario-light.png'),
 ('10','Selettore date','12-data-light.png','v34-B-date-light.png'),
 ('11','Simulatore','16-simulatore-light.png','v34-C-simulatore-light.png'),
]
manifest=[];pdf=[]
for code,name,proto,native in rows:
 a=ROOT/'design/ux-rebuild/screenshots'/proto
 b=SHOTS/native
 if not b.exists():
  source=ROOT/'app/build/screenshots'/native
  if source.exists():shutil.copy2(source,b)
 if not a.exists() or not b.exists():raise FileNotFoundError(f'{a} / {b}')
 # Original six raster sources already have documented frame crops from phase 1.2.
 ref=ROOT/f'design/ux-rebuild/comparisons/{code}-concept-area-utile.png'
 images=[];labels=[]
 if int(code)<=6 and ref.exists():images.append(Image.open(ref).convert('RGB'));labels.append('Concept originale · ritaglio documentato')
 images += [Image.open(a).convert('RGB'),Image.open(b).convert('RGB')];labels += ['Prototipo approvato Fase 1.2','Android nativo · Robolectric Native Graphics']
 width=480;scaled=[im.resize((width,round(im.height*width/im.width)),Image.Resampling.LANCZOS)for im in images]
 canvas=Image.new('RGB',(len(scaled)*width,max(im.height for im in scaled)+100),'#EAF1F6');draw=ImageDraw.Draw(canvas)
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',13)
 draw.text((14,8),f'{code}. {name} — stessa larghezza, proporzioni conservate',font=font,fill='#142B51')
 for i,(im,label)in enumerate(zip(scaled,labels)):draw.text((i*width+10,35),label,font=font,fill='#142B51');canvas.paste(im,(i*width,65))
 canvas.save(COMP/f'{code}-confronto.png');pdf.append(canvas)
 manifest.append({'screen':name,'prototype':str(a.relative_to(ROOT)),'native':str(b.relative_to(ROOT)),'native_sha256':hashlib.sha256(b.read_bytes()).hexdigest(),'resize':'width 480; aspect ratio unchanged; no native content cropped'})
pdf[0].save(COMP/'Confronti-V3.4.pdf',save_all=True,append_images=pdf[1:],resolution=110)
(COMP/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
list_png=SHOTS/'v34-C-impegni-elenco-light.png';calendar_png=SHOTS/'v34-C-impegni-calendario-light.png'
assert hashlib.sha256(list_png.read_bytes()).digest()!=hashlib.sha256(calendar_png.read_bytes()).digest()
html='''<!doctype html><html lang="it"><meta charset="utf-8"><title>Posso spenderlo? V3.4 — rendering nativi</title><style>body{font:16px system-ui;margin:24px;background:#edf7fb;color:#142b51}section{display:flex;flex-wrap:wrap;gap:20px}figure{margin:0;width:360px}img{width:100%;height:auto}figcaption{padding:12px}a{color:#1875e5}</style><h1>V3.4 — rendering Android nativi</h1><p>View Android reali, Robolectric Native Graphics API 35. Non fotografie del telefono né screenshot di emulatore. I dati sono fixture dimostrative.</p><p><a href="comparisons/Confronti-V3.4.pdf">Confronti con prototipo e sei concept</a> · <a href="RAPPORTO.md">Rapporto tecnico e limiti</a></p><section>'''
for f in sorted(SHOTS.glob('*.png')):html+=f'<figure><a href="screenshots/{f.name}"><img loading="lazy" src="screenshots/{f.name}" alt="{f.stem}"></a><figcaption>{f.stem}</figcaption></figure>'
(OUT/'GALLERIA.html').write_text(html+'</section></html>')
print(f'{len(list(SHOTS.glob("*.png")))} rendering; {len(manifest)} confronti; Elenco e Calendario distinti.')
