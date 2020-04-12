package com.hackathon.covid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.hackathon.covid.utils.Constants;
import com.hackathon.covid.utils.LocationManagerClient;
import com.hackathon.covid.utils.LocationUtils;
import com.hackathon.covid.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SelectLocationActivity extends AppCompatActivity {

    private static final String TAG = SelectLocationActivity.class.getSimpleName();
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    @BindView(R.id.tvAddress)
    TextView tvAddress;
    @BindView(R.id.tvAddressAction)
    TextView tvAddressAction;
    @BindView(R.id.popUpView)
    ConstraintLayout popUpView;

    private Address mAddress;

    boolean isRetry = false;

    @Nullable
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;

    private static double latitude;
    private static double longitude;
    public String address;
    private RxPermissions rxPermissions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);
        ButterKnife.bind(this);
        rxPermissions = new RxPermissions(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.choose_location));

        String existingLatitude = null;
        String existingLongitude = null;

        if (getIntent() != null) {
            existingLatitude = getIntent().getStringExtra(Constants.LATITUDE);
            existingLongitude = getIntent().getStringExtra(Constants.LONGITUDE);
        }

        if (!Utils.isNullOrEmpty(existingLatitude) && !Utils.isNullOrEmpty(existingLongitude)) {
            setLatLng(Double.parseDouble(existingLatitude), Double.parseDouble(existingLongitude));
        } else {
            setLatLng(0.0, 0.0);
        }

        setUpPopUpView();
    }

    private void setUpPopUpView() {
        popUpView.setOnClickListener(v -> {
            if (Utils.isNetworkAvailable(this)){
                if (!isRetry) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.LATITUDE, latitude);
                    intent.putExtra(Constants.LONGITUDE, longitude);
                    intent.putExtra(Constants.ADDRESS, mAddress);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    updatePopUpStatus();
                }
            } else {
                updatePopUpStatus();
                Toast.makeText(this, getString(R.string.internet_unavailable), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLatLng(double lat, double lng) {
        latitude = lat;
        longitude = lng;
        fetchCurrentLocation();
    }

    @SuppressLint("CheckResult")
    public void fetchCurrentLocation() {
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                     .subscribe(granted -> { // will emit 2 Permission objects
                         if (granted) {
                             googleMapView();
                             LiveData<Location> locationLiveData = LocationManagerClient.getInstance(getApplicationContext())
                                     .getLocationUpdate(this);
                             Observer<Location> currentLocationObserver = new Observer<Location>() {
                                 @Override
                                 public void onChanged(Location location) {
                                     if(location != null) {
                                         if (latitude == 0.0 && longitude == 0.0) {
                                             renderMarker(location.getLatitude(), location.getLongitude());
                                         } else {
                                             renderMarker(latitude, longitude);
                                         }
                                         locationLiveData.removeObserver(this);
                                     }
                                 }
                             };
                             locationLiveData.observe(this, currentLocationObserver);
                         } else {
                             Toast.makeText(SelectLocationActivity.this, getString(R.string.location_permission_msg), Toast.LENGTH_SHORT).show();
                         }
                     });
    }

    private void renderMarker(double latitude, double longitude) {
        if(mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16.0f));
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> renderMarker(latitude, longitude), 200);
        }
    }

    private void googleMapView() {
        FragmentManager fm = getSupportFragmentManager();
        mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setScrollGesturesEnabledDuringRotateOrZoom(false);
            mMap.getUiSettings().setTiltGesturesEnabled(false);

            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(16).build();
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.setOnCameraIdleListener(() -> {
                LatLng midLatLng = mMap.getCameraPosition().target;
                latitude = midLatLng.latitude;
                longitude = midLatLng.longitude;
                fetchAddressFromGeoCoder(latitude, longitude);
                popUpView.setVisibility(View.VISIBLE);
                updatePopUpStatus();
            });
            mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                @Override
                public void onCameraMoveStarted(int i) {
                    popUpView.setVisibility(View.GONE);
                }
            });
        });
    }

    private void fetchAddressFromGeoCoder(double latitude, double longitude) {
        mAddress = LocationUtils.getAddress(this, latitude, longitude);
        if (mAddress != null) {
            address = mAddress.getAddressLine(0);
        } else {
            address = getString(R.string.current_location);
        }
    }

    private void updatePopUpStatus() {
        if (Utils.isNetworkAvailable(this)) {
            isRetry = false;
            tvAddressAction.setText(getString(R.string.select_location));
            tvAddress.setText(address);
        } else {
            isRetry = true;
            tvAddressAction.setText(getString(R.string.text_refresh));
            tvAddress.setText(getString(R.string.internet_unavailable));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_location_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menuSearch: {
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, Collections.singletonList(Place.Field.LAT_LNG))
                        .setCountry("IN")
                        .build(this);
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (place.getLatLng() != null) {
                    setLatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                } else {
                    setLatLng(0.0, 0.0);
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(this, getString(R.string.oops_something_went_wrong), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
