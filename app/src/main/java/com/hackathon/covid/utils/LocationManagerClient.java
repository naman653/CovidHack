package com.hackathon.covid.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.hackathon.covid.R;

import javax.inject.Singleton;

import static com.hackathon.covid.utils.Constants.REQUEST_LOCATION_PERMISSION;
import static com.hackathon.covid.utils.LocationUtils.showToast;

@Singleton
public class LocationManagerClient {

//    private int FASTEST_INTERVAL = 8 * 1000; // 8 SECOND
//    private int UPDATE_INTERVAL = 60 * 1000; // 1 MIN
//    private int FINE_LOCATION_REQUEST = 888;

    private volatile static LocationManagerClient INSTANCE = null;

    private MutableLiveData<Location> mLocation;
    private boolean mIsLocationFetching;
    private SettingsClient mSettingsClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationManager mLocationManager;
    private LocationSettingsRequest mLocationSetting;
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location != null) {
                mLocation.setValue(location);
                removeManagerUpdates();
            } else {
                mLocation.setValue(null);
                removeManagerUpdates();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private LocationManagerClient(Context context) {
        mLocation = new MutableLiveData<>();
        mIsLocationFetching = false;
        mFusedLocationProviderClient = new FusedLocationProviderClient(context);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mSettingsClient = LocationServices.getSettingsClient(context);

        //Create LocationSettingRequest object using locationRequest
        LocationSettingsRequest.Builder mLocationSettingBuilder = new LocationSettingsRequest.Builder();
        mLocationSettingBuilder.addLocationRequest(new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY));
        mLocationSetting = mLocationSettingBuilder.build();
    }

    public static LocationManagerClient getInstance(Context context) {
        if(INSTANCE == null) {
            synchronized (LocationManagerClient.class) {
                if(INSTANCE == null) {
                    INSTANCE = new LocationManagerClient(context);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<Location> getLocationUpdate(@NonNull Context context) {

        //Need to check whether location settings are satisfied
        mSettingsClient.checkLocationSettings(mLocationSetting)
                .addOnSuccessListener(locationSettingsResponse -> requestUpdates())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult((Activity) context,
                                    REQUEST_LOCATION_PERMISSION);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    } else {
                        // Show error message to user to resolve explicitly
                        e.printStackTrace();
                        showToast(context, context.getString(R.string.unresolved_location_requirements));
                    }
                });
        return mLocation;
    }

    public void requestUpdates() {
        if(!mIsLocationFetching) {
            mIsLocationFetching = true;
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null && System.currentTimeMillis() - location.getTime() < 5 * 60 * 1000) {
                            mLocation.setValue(location);
                            removeUpdates();
                        } else {
                            requestManagerUpdates();
                        }
                    });
        }
    }

    @SuppressLint("MissingPermission")
    private void requestManagerUpdates() {
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    private void removeManagerUpdates() {
        removeUpdates();
        mLocationManager.removeUpdates(locationListener);
    }

    private void removeUpdates() {
        mIsLocationFetching = false;
    }
}
