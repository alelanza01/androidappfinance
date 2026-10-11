import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val keyConfig = System.getenv("V2_SIGNING_PROPERTIES")?.let { file(it) }?.takeIf { it.isFile }
android {
 namespace="it.privato.disponibile.v2"; compileSdk=35
 defaultConfig { applicationId="it.privato.disponibile.v2";minSdk=26;targetSdk=35;versionCode=200;versionName="2.0.0" }
 if(keyConfig!=null) {
  val key=Properties().apply { keyConfig.inputStream().use { load(it) } }
  signingConfigs.create("privateV2") { storeFile=file(key.getProperty("storeFile"));storePassword=key.getProperty("storePassword");keyAlias=key.getProperty("keyAlias");keyPassword=key.getProperty("keyPassword") }
  buildTypes.getByName("release") {signingConfig=signingConfigs.getByName("privateV2");isMinifyEnabled=false}
 }
 compileOptions {sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17}
 kotlinOptions {jvmTarget="17"}
 testOptions {
  unitTests.isIncludeAndroidResources=true
  unitTests.all {
   val testHome=layout.buildDirectory.dir("test-home").get().asFile;testHome.mkdirs()
   it.systemProperty("user.home",testHome.absolutePath)
   it.systemProperty("posso.screenshots.dir",layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
   it.systemProperty("robolectric.dependency.repo.url","https://repo.maven.apache.org/maven2")
   listOf("http.proxyHost","http.proxyPort","https.proxyHost","https.proxyPort","javax.net.ssl.trustStore").forEach {name ->System.getProperty(name)?.let {v->it.systemProperty(name,v)}}
  }
 }
}
dependencies {testImplementation("junit:junit:4.13.2");testImplementation("org.robolectric:robolectric:4.14.1")}
