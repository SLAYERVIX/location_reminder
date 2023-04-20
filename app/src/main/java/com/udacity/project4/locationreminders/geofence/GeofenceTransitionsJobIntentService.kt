package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.i("notification", "onHandleWork: ")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            Log.e("rabbit", "onHandleWork: $geofencingEvent.errorCode")
            return
        }
        val geofenceTransition = geofencingEvent.geofenceTransition
        if(geofenceTransition==Geofence.GEOFENCE_TRANSITION_ENTER){
            Log.i("rabbit", "onHandleWork: ")
            sendNotification(geofencingEvent.triggeringGeofences!!)
        }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        Log.i("rabbit", "sendNotification: ")
        val reqId:List<String> = triggeringGeofences.map { it.requestId }
        reqId.forEach{ requestId->
            //Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
            //Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                Log.i("rabbit",result.toString())
                if (result is Result.Success<ReminderDTO>) {
                    Log.i("rabbit",result.data.title.toString())
                    val reminderDTO = result.data
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }


    }


}
