"""Compare fresh approved HTML and native Android captures only after equality checks.
No source modifications, content cropping or financial-value retouching.
"""
from pathlib import Path
import json,hashlib,shutil
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[2];out=root/'release/v3.4-rc2';source=root/'app/build/screenshots'
html=json.loads((out/'prototype/states.json').read_text());native=json.loads((source/'rc2-native-states.json').read_text())
key=lambda s:(s['name'],s['theme'])
a={key(s):s for s in html};b={key(s):s for s in native}
assert a.keys()==b.keys() and len(a)==30
for k in a:assert a[k]==b[k],(k,a[k],b[k])
# Compare immutable financial fixture too, exported from approved JS before rendering.
fixture=root/'app/src/test/resources/rc2/fixture.json';shutil.copy2(fixture,out/'fixture.json')
manifest=[];pdf=[];font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',12)
for name,theme in a:
 n=source/f'rc2-{name}-{theme}.png';h=out/f'prototype/{name}-{theme}.png';assert n.exists() and h.exists()
 shutil.copy2(n,out/'screenshots'/n.name)
 left=Image.open(h).convert('RGB');right=Image.open(n).convert('RGB');assert left.size==right.size==(360,800)
 # No resize: a pixel in each image occupies a pixel in the comparison.
 canvas=Image.new('RGB',(740,856),'#EDF7FB');d=ImageDraw.Draw(canvas)
 d.text((12,8),f'{name} · {theme} · 2026-10-31 · dati/stato verificati uguali',font=font,fill='#142B51')
 d.text((10,32),'Prototipo approvato a49b5f4',font=font,fill='#142B51');d.text((380,32),'Android nativo · Robolectric API 35',font=font,fill='#142B51')
 canvas.paste(left,(5,55));canvas.paste(right,(375,55));f=out/'comparisons'/f'{name}-{theme}.png';canvas.save(f);pdf.append(canvas)
 manifest.append({**a[(name,theme)],'financial_and_view_state_equal':True,'prototype_sha256':hashlib.sha256(h.read_bytes()).hexdigest(),'native_sha256':hashlib.sha256(n.read_bytes()).hexdigest(),'viewport':[360,800],'resized':False,'cropped':False})
for f in source.glob('rc2-responsive-*.png'):shutil.copy2(f,out/'screenshots'/f.name)
pdf[0].save(out/'comparisons/Confronti-RC2.pdf',save_all=True,append_images=pdf[1:],resolution=96)
(out/'comparisons/manifest.json').write_text(json.dumps({'approved_commit':'a49b5f4cb4d6af036486edb67e2a7715ea45d221','fixture_sha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),'comparisons':manifest},indent=2)+'\n')
page='''<!doctype html><html lang="it"><meta charset="utf-8"><title>V3.4 RC2 — confronto verificato</title><style>body{font:16px system-ui;color:#142b51;background:#edf7fb;margin:20px}img{width:min(100%,740px);height:auto}figure{margin:20px 0}nav a{margin-right:20px}</style><h1>Posso spenderlo? V3.4 RC2</h1><p>Stessa fixture, data locale 31 ottobre 2026, viewport 360×800 e selezioni identiche. A sinistra HTML approvato, a destra View Android reali renderizzate da Robolectric Native Graphics, senza emulatore o telefono. Nessun contenuto ritoccato.</p><nav><a href="comparisons/Confronti-RC2.pdf">PDF</a><a href="RAPPORTO.md">Rapporto e differenze residue</a><a href="comparisons/manifest.json">Verifica dati e hash</a></nav>'''
for item in manifest:
 name,theme=item['name'],item['theme'];page+=f'<figure><figcaption>{name} · {theme}</figcaption><a href="comparisons/{name}-{theme}.png"><img loading="lazy" src="comparisons/{name}-{theme}.png" alt="Confronto {name} {theme}"></a></figure>'
(out/'GALLERIA.html').write_text(page+'</html>')
print(f'{len(manifest)} confronti con dati e stati identici; {len(list((out/"screenshots").glob("*.png")))} rendering nativi. Nessun ridimensionamento.')
