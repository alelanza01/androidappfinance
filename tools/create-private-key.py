"""Create a local release key once. Never commit this directory or its credentials."""
from pathlib import Path
import secrets,subprocess,os
root=Path('/workspace/private-v2-signing');root.mkdir(mode=0o700,exist_ok=True);root.chmod(0o700)
config=root/'signing.properties';key=root/'v2-release.p12'
if config.exists() and key.exists():
 print('Existing V2 signing key retained.');raise SystemExit(0)
if config.exists() or key.exists():raise RuntimeError('Incomplete signing configuration: preserved, not overwritten.')
if (Path(__file__).resolve().parents[1]/'release/Posso-spenderlo-V2.apk').exists():
 raise RuntimeError('V2 già distribuita: recupera la chiave da uno snapshot privato. Non generarne una nuova per aggiornare installazioni esistenti.')
password=secrets.token_urlsafe(32);environment=os.environ.copy();environment['V2_KEY_PASSWORD']=password
subprocess.run(['/workspace/toolchains/jdk-21.0.12.1/bin/keytool','-genkeypair','-keystore',str(key),'-storetype','PKCS12','-alias','posso-v2','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Posso Spenderlo Private V2','-storepass:env','V2_KEY_PASSWORD','-keypass:env','V2_KEY_PASSWORD'],env=environment,check=True,capture_output=True)
key.chmod(0o600)
config.write_text('storeFile='+str(key)+'\nkeyAlias=posso-v2\nstorePassword='+password+'\nkeyPassword='+password+'\n');config.chmod(0o600)
print('Private signing key created outside repository. No credentials displayed.')
