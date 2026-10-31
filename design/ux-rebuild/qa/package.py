from pathlib import Path
import zipfile,hashlib,re
root=Path(__file__).resolve().parents[1]
s=(root/'index.html').read_text().replace('<link rel="stylesheet" href="styles.css">','<style>\n'+(root/'styles.css').read_text()+'\n</style>')
for name in ['catalog.js','icons.js','model.js','app.js']:
 s=s.replace('<script src="'+name+'"></script>','<script>\n'+(root/name).read_text().replace('</script','<\\/script')+'\n</script>')
assert not re.search(r'<(?:script|link)[^>]+(?:src|href)=',s)
(root/'PROTOTIPO.html').write_text(s)
archive=root/'Posso-spenderlo-UX-prototipo.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(root.rglob('*')):
  if not p.is_file() or p==archive or p.name=='SHA256.txt' or '__pycache__' in p.parts:continue
  assert p.suffix.lower() not in ['.p12','.jks','.keystore','.apk'] and p.name not in ['signing.properties','local.properties']
  z.write(p,Path('Posso-spenderlo-UX-prototipo')/p.relative_to(root))
(root/'SHA256.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in [root/'PROTOTIPO.html',archive]))
print('HTML autonomo:',len(s.encode()),'byte. ZIP:',archive.stat().st_size,'byte.')
