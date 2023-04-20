package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.res.Resources
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.Constants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

class SelectLocationFragment : BaseFragment(), LocationListener,
    EasyPermissions.PermissionCallbacks {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var locationManager: LocationManager
    private var latLng: LatLng? = null
    private var reminderSelectedLocationStr: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        initMap()

        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    }


    private fun initMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            getCurrentLocation()
            setPoiClick(map)
            changeMapStyle(map)
            setOnMapLongClick(map)
        }
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = latLng?.latitude
        _viewModel.longitude.value = latLng?.longitude
        _viewModel.reminderSelectedLocationStr.value = reminderSelectedLocationStr
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun setOnMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            this.latLng = latLng
            reminderSelectedLocationStr = getAddress(latLng)
            showSaveLocationButton()
            addMarker(latLng)
            addCircle(latLng)
        }
    }

    private fun showSaveLocationButton() {
        binding.btnSave.visibility = View.VISIBLE
    }

    private fun addMarker(latLng: LatLng) {
        val snippet = String.format(
            Locale.getDefault(), "Lat: %1$.5f , Long: %2$.5f",
            latLng.latitude,
            latLng.longitude
        )
        map.addMarker(
            MarkerOptions().position(latLng).title(getAddress(latLng))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .snippet(snippet)
        )
    }

    private fun getAddress(latLng: LatLng): String {
        val geocoder = Geocoder(requireActivity(), Locale.getDefault())
        val province = try {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.get(0)?.adminArea
        } catch (e: Exception) {
            "null"
        }
        val city = try {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.get(0)?.locality
        } catch (e: Exception) {
            "null"
        }
        return "$province - $city"
    }

    private fun changeMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) Log.e("rabbit", "Styling failed !.")

        } catch (e: Resources.NotFoundException) {
            Log.e("rabbit", "style not found", e)
        }

    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            latLng = poi.latLng
            reminderSelectedLocationStr = poi.name
            showSaveLocationButton()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            addCircle(poi.latLng)
        }
    }

    private fun addCircle(latLng: LatLng) {
        map.addCircle(
            CircleOptions()
                .center(latLng)
                .radius(500.0)
                .fillColor(Color.argb(50, 255, 126, 56))
                .strokeColor(Color.YELLOW)
        )
    }

    private fun isForegroundLocationPermissionGranted(): Boolean {
        return EasyPermissions.hasPermissions(
            requireContext(),
            Constants.FINE_LOCATION,
            Constants.COARSE_LOCATION
        )
    }

    private fun requestForegroundLocationPermission() {
        EasyPermissions.requestPermissions(
            this,
            "The application needs foreground permission to work properly",
            Constants.REQUEST_LOCATION_PERMISSION_CODE,
            Constants.FINE_LOCATION,
            Constants.COARSE_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (isForegroundLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
            map.setOnMyLocationButtonClickListener {
                if (!isGPSEnabled()) turnOnGps()
                false
            }
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, Constants.MIN_TIME_UPDATES,
                Constants.MIN_DISTANCE_UPDATES, this
            )
        }
        else requestForegroundLocationPermission()
    }

    // To support devices less than 29
    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        val zoomLevel = 15f
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun turnOnGps() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())
        task.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        it.resolution.intentSender,
                        Constants.REQUEST_TURN_ON_GPS_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("rabbit", "Cant get location setting resolution: " + sendEx.message)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        getCurrentLocation()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        when (requestCode) {
            Constants.REQUEST_LOCATION_PERMISSION_CODE -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                    AppSettingsDialog.Builder(this).build().show()
                }
                else requestForegroundLocationPermission()
            }
        }
    }
}

