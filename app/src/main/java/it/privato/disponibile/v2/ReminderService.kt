package it.privato.disponibile.v2

import android.app.job.JobService
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobInfo
import android.content.ComponentName
import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import java.time.LocalDate

class ReminderService:JobService() {
    override fun onStartJob(params:JobParameters):Boolean {
        Thread {
            try {
                val store=Store(this)
                val ledger=try {store.snapshot()} finally {store.close()}
                val manager=getSystemService(NotificationManager::class.java)
                val due=ledger.pending.filter {it.kind==Kind.EXPENSE && it.date<=LocalDate.now().plusDays(1)}
                if(ledger.settings.reminders && due.isNotEmpty() && (android.os.Build.VERSION.SDK_INT<33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED) && manager.areNotificationsEnabled()) {
                    manager.createNotificationChannel(NotificationChannel("deadlines","Scadenze economiche",NotificationManager.IMPORTANCE_DEFAULT))
                    val intent=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    manager.notify(20,Notification.Builder(this,"deadlines").setSmallIcon(R.drawable.ic_notification).setContentTitle("${due.size} pagamenti entro domani").setContentText("Totale ${Money.format(due.sumOf {it.cents})}. Apri il calendario.").setContentIntent(intent).setAutoCancel(true).build())
                } else manager.cancel(20)
            } catch(_:Exception) { /* JobScheduler retries a failed local read on the next periodic run. */ }
            finally {jobFinished(params,false)}
        }.start()
        return true
    }
    override fun onStopJob(params:JobParameters)=true
    companion object {
        fun schedule(context:Context,enabled:Boolean) {
            val scheduler=context.getSystemService(JobScheduler::class.java)
            if(enabled) {
                val result=scheduler.schedule(JobInfo.Builder(20,ComponentName(context,ReminderService::class.java)).setPersisted(true).setPeriodic(24*60*60*1000L).build())
                check(result==JobScheduler.RESULT_SUCCESS) {"Android non ha accettato gli avvisi. Riprova."}
            } else {scheduler.cancel(20);context.getSystemService(NotificationManager::class.java).cancel(20)}
        }
    }
}
