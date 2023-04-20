package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

class SaveReminderFragment : BaseFragment(), EasyPermissions.PermissionCallbacks{
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem:ReminderDataItem
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment())

        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val id=UUID.randomUUID().toString()

            reminderDataItem=ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude,
                id
            )

            val isReminderValid=_viewModel.validateEnteredData(reminderDataItem)
            if(isReminderValid){
                checkLocationPermissionsAndAddGeofence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(){
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(reminderDataItem.latitude!!, reminderDataItem.longitude!!, Constants.GEOFENCE_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Constants.GEOFENCE_TRANSITION_TYPES)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireActivity().applicationContext,GeofenceBroadcastReceiver::class.java)
        val geofencePendingIntent=
            PendingIntent.getBroadcast(requireActivity().applicationContext, 0, intent,
                if (Constants.RUNNING_S_OR_LATER) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("rabbit", "Geofence added")
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                Log.e("rabbit", "Failed to add geofence: ${it.message}")
                Toast.makeText(requireContext(), "Failed to add geofence", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isForegroundLocationPermissionGranted():Boolean{
        return EasyPermissions.hasPermissions(
            requireContext(),
            Constants.FINE_LOCATION,
            Constants.COARSE_LOCATION
        )
    }

    private fun requestForegroundLocationPermission(){
        EasyPermissions.requestPermissions(
            this,
            "The application needs foreground permission to work properly",
            Constants.REQUEST_LOCATION_PERMISSION_CODE,
            Constants.FINE_LOCATION,
            Constants.COARSE_LOCATION
        )
    }
    private fun isBackgroundLocationPermissionGranted():Boolean{
        if(Constants.RUNNING_Q_OR_LATER) {
            return EasyPermissions.hasPermissions(requireContext(), Constants.BACKGROUND_LOCATION)
        }
        else return true
    }

    private fun requestBackgroundPermission(){
        EasyPermissions.requestPermissions(
            this,
            "The application needs background permission to work properly",
            Constants.REQUEST_BACKGROUND_LOCATION_PERMISSION_CODE,
            Constants.BACKGROUND_LOCATION
        )
    }

    private fun areForegroundAndBackgroundPermissionsGranted():Boolean {
        return isBackgroundLocationPermissionGranted() && isForegroundLocationPermissionGranted()
    }

    private fun requestForegroundAndBackgroundPermissions(){
        if(!isForegroundLocationPermissionGranted())requestForegroundLocationPermission()
        if(!isBackgroundLocationPermissionGranted())requestBackgroundPermission()
    }

    private fun checkLocationPermissionsAndAddGeofence(){
        if(areForegroundAndBackgroundPermissionsGranted()){
            if(isGPSEnabled()){
                addGeofence()
            }
            else turnOnGps()
        }
        else requestForegroundAndBackgroundPermissions()
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun turnOnGps(){
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task= client.checkLocationSettings(builder.build())
        task.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    startIntentSenderForResult(it.resolution.intentSender, Constants.REQUEST_TURN_ON_GPS_CODE,null,0,0,0,null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("rabbit", "Cannot get location settings resolution: " + sendEx.message)
                }
            }
        }.addOnSuccessListener {
            addGeofence()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,this)
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        checkLocationPermissionsAndAddGeofence()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        when(requestCode){
            Constants.REQUEST_BACKGROUND_LOCATION_PERMISSION_CODE->{
                if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
                    AppSettingsDialog.Builder(this).build().show()
                }
                else requestForegroundAndBackgroundPermissions()
            }
            Constants.REQUEST_LOCATION_PERMISSION_CODE-> {
                requestForegroundLocationPermission()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // called after user returned from app settings screen
        when(requestCode){
            Constants.REQUEST_TURN_ON_GPS_CODE->{
                if(resultCode==Activity.RESULT_OK)addGeofence()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
