package it.privato.disponibile.v2
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.Config
import android.app.job.JobScheduler

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReminderTest {
 @Test fun periodicLocalJobIsPersistedAndCanBeCancelled() {
  val context=RuntimeEnvironment.getApplication();val scheduler=context.getSystemService(JobScheduler::class.java)
  ReminderService.schedule(context,true);val job=requireNotNull(scheduler.getPendingJob(20))
  assertNotNull(job);assertTrue(job.isPersisted);assertTrue(job.isPeriodic);assertEquals(24*60*60*1000L,job.intervalMillis)
  ReminderService.schedule(context,false);assertNull(scheduler.getPendingJob(20))
 }
 @Test fun noInternetPermissionIsRequested() {
  val context=RuntimeEnvironment.getApplication();val info=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS)
  assertFalse(info.requestedPermissions?.contains(android.Manifest.permission.INTERNET) ?: false)
 }
}
