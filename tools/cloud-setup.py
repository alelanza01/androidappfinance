from pathlib import Path
import hashlib,zipfile,tarfile,urllib.request,tempfile,os,urllib.parse
root=Path('/workspace/toolchains');root.mkdir(exist_ok=True)
def install(url,checksum,algorithm,destination,tar=False):
 if destination.exists():return
 archive=root/url.rsplit('/',1)[-1]
 if not archive.exists() or hashlib.new(algorithm,archive.read_bytes()).hexdigest()!=checksum:urllib.request.urlretrieve(url,archive)
 assert hashlib.new(algorithm,archive.read_bytes()).hexdigest()==checksum,'Checksum mismatch'
 with tempfile.TemporaryDirectory(dir=root) as temp:
  if tar:
   with tarfile.open(archive) as z:z.extractall(temp,filter='data')
  else:
   with zipfile.ZipFile(archive) as z:
    z.extractall(temp)
    for i in z.infolist():
     mode=(i.external_attr>>16)&0o777
     if mode:(Path(temp)/i.filename).chmod(mode)
  destination.parent.mkdir(parents=True,exist_ok=True);next(Path(temp).iterdir()).rename(destination)
install('https://download.oracle.com/java/21/archive/jdk-21.0.12.1_linux-x64_bin.tar.gz','12f870b21301b42292558a3f872ce543affa2b86cb6458591c78388c41ddb111','sha256',root/'jdk-21.0.12.1',True)
install('https://downloads.gradle.org/distributions/gradle-8.11.1-bin.zip','f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6','sha256',root/'gradle-8.11.1')
sdk=root/'android-sdk'
install('https://dl.google.com/android/repository/platform-35_r02.zip','0bb560a90a7a2cbd0dd8348224d518b638fe7949','sha1',sdk/'platforms/android-35')
install('https://dl.google.com/android/repository/build-tools_r35_linux.zip','2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc','sha1',sdk/'build-tools/35.0.0')
install('https://dl.google.com/android/repository/platform-tools_r37.0.1-linux.zip','477254aa5f903c15cf51001717bdf347fb6b53e0','sha1',sdk/'platform-tools')
(root/'android-user').mkdir(exist_ok=True);cache=root/'gradle-home';cache.mkdir(exist_ok=True)
proxy=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''));lines=[]
if proxy.hostname:
 for protocol in ('http','https'):lines.extend([f'systemProp.{protocol}.proxyHost={proxy.hostname}',f'systemProp.{protocol}.proxyPort={proxy.port or 80}'])
if Path('/etc/ssl/certs/java/cacerts').exists():lines.append('systemProp.javax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
(cache/'gradle.properties').write_text('\n'.join(lines)+'\n')
project=Path(__file__).resolve().parents[1]
if not (project/'local.properties').exists():(project/'local.properties').write_text('sdk.dir='+str(sdk)+'\n')
print('Toolchains verified and available')
