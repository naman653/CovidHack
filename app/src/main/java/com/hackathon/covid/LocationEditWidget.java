package com.hackathon.covid;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hackathon.covid.data.Address;
import com.hackathon.covid.data.MultiLingual;
import com.hackathon.covid.data.Point;
import com.hackathon.covid.utils.Constants;
import com.hackathon.covid.utils.LocationManagerClient;
import com.hackathon.covid.utils.LocationUtils;
import com.hackathon.covid.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;
import java.util.Locale;

import static com.hackathon.covid.utils.Constants.REQUEST_LOCATION;
import static com.hackathon.covid.utils.Constants.REQUEST_USER_LOCATION;

public class LocationEditWidget extends LinearLayout {
    ImageView ivCurrentLocation;
    EditText etAddress;
    TextView tvAddress2;
    SupportMapFragment mapFragment;
    TextView buttonUploadLocation;

    double mLatitude, currentLatitude;
    double mLongitude, currentLongitude;
    Address mAddress;
    String editableAddress, nonEditableAddress;
    GoogleMap mMap;
    Marker marker;
    Observer<Location> addCurrentLocationObserver;
    LiveData<Location> locationLiveData;
    RxPermissions rxPermissions;
    public LocationEditWidget(Context context) {
        this(context, null);
    }

    public LocationEditWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocationEditWidget(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_edit_location, this, true);

        etAddress = findViewById(R.id.etAddress);
        tvAddress2 = findViewById(R.id.tvAddress2);
        ivCurrentLocation = findViewById(R.id.ivCurrentLocation);
        buttonUploadLocation = findViewById(R.id.buttonUploadLocation);
        if(buttonUploadLocation != null) buttonUploadLocation.setOnClickListener(v -> selectLocationMap());
        if(ivCurrentLocation != null) ivCurrentLocation.setOnClickListener(v -> addCurrentLocation());
        if(etAddress != null) etAddress.setFocusable(false);
        if(etAddress != null) etAddress.setOnClickListener(v -> {
            if (mLatitude == 0.0 || mLongitude == 0.0) {
                Toast.makeText(getContext(), getContext().getString(R.string.address_input_msg), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void init(@NonNull RxPermissions rxPermissions, @Nullable Address address, @Nullable Point location) {

        this.rxPermissions = rxPermissions;
        // Initialize map fragment
        initMapView();

        locationLiveData = LocationManagerClient.getInstance(getContext()).getLocationUpdate(getContext());
        // Set location if already exists
        if (address != null && location != null) {
            mAddress = address;
            LocationUtils.getBackendDataSegregated(address, etAddress, tvAddress2);
            etAddress.setFocusableInTouchMode(true);
            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();
            renderMarker(mLatitude, mLongitude);
        }
    }

    private void initMapView() {
        FragmentActivity fragmentActivity = (FragmentActivity) getContext();
        FragmentManager fm = fragmentActivity.getSupportFragmentManager();
        mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            if(rxPermissions.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    rxPermissions.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION))
                mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(false);
            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setZoomGesturesEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setAllGesturesEnabled(false);
        });
    }

    private void renderMarker(double latitude, double longitude) {
        if (mMap != null) {
            MarkerOptions markeroption = new MarkerOptions().position(new LatLng(latitude, longitude));
            markeroption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 13.0f));

            if (marker != null) {
                marker.remove();
            }
            marker = mMap.addMarker(markeroption);
            buttonUploadLocation.setText("");
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> renderMarker(latitude, longitude), 200);
        }
    }


    public void selectLocationMap() {
        Intent intent = new Intent(getContext(), SelectLocationActivity.class);
        FragmentActivity fragmentActivity = (FragmentActivity) getContext();
        if (mLatitude == 0.0 && mLongitude == 0.0) {
            fragmentActivity.startActivityForResult(intent, REQUEST_USER_LOCATION);
        } else {
            intent.putExtra(Constants.LATITUDE, String.valueOf(mLatitude));
            intent.putExtra(Constants.LONGITUDE, String.valueOf(mLongitude));
            fragmentActivity.startActivityForResult(intent, REQUEST_USER_LOCATION);
        }
    }


    public void updateLocation(Intent data) {
        double latitude = data.getDoubleExtra(Constants.LATITUDE, 0.0);
        double longitude = data.getDoubleExtra(Constants.LONGITUDE,  0.0);
        android.location.Address address = data.getParcelableExtra(Constants.ADDRESS);

        updateAddress(latitude, longitude, address);
    }

    private void addCurrentLocation() {
        addCurrentLocationObserver = currentLocation -> {
            if(currentLocation != null) {
                currentLatitude = currentLocation.getLatitude();
                currentLongitude = currentLocation.getLongitude();
                if (Utils.isNetworkAvailable(getContext())) {
                    updateAddress(currentLatitude, currentLongitude, null);
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.internet_unavailable), Toast.LENGTH_SHORT).show();
                }
                locationLiveData.removeObserver(addCurrentLocationObserver);
            }
        };

        // Fetch current location
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(granted -> { // will emit 2 Permission objects
                    if (granted) {
                        locationLiveData.observeForever(addCurrentLocationObserver);
                    } else {
                        Toast.makeText(getContext(), getContext().getString(R.string.location_permission_msg), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public Point getCurrentLocation() {
        Point userCurrentLocation = new Point();
        userCurrentLocation.setLatitude(currentLatitude);
        userCurrentLocation.setLongitude(currentLongitude);
        return userCurrentLocation;
    }

    public Point getLocation() {
        if (mLatitude == 0.0 && mLongitude == 0.0) {
            return null;
        }

        Point location = new Point();
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        return location;
    }

    private void updateAddress(double latitude, double longitude, @Nullable android.location.Address addressObj) {
        mLatitude = latitude;
        mLongitude = longitude;
        android.location.Address address = null;

        if (addressObj != null) {
            address = addressObj;
        } else {
            List<android.location.Address> addressList = LocationUtils.addressList(getContext(), mLatitude, mLongitude, Locale.ENGLISH);
            if (!addressList.isEmpty()) {
                address = addressList.get(0);
            }
        }

        if (address != null) {
            // Update EditText
            editableAddress = LocationUtils.getEditableAddressString(address);
            etAddress.setText(editableAddress);
            etAddress.setFocusableInTouchMode(true);

            // Update TextView
            nonEditableAddress = LocationUtils.getNonEditableAddressString(address);
            tvAddress2.setText(nonEditableAddress);

            // Update Marker
            renderMarker(mLatitude, mLongitude);

            // Save address
            MultiLingual city = Utils.getMultiLingual(address.getLocality());
            MultiLingual district = Utils.getMultiLingual(address.getSubAdminArea());
            MultiLingual state = Utils.getMultiLingual(address.getAdminArea());
            MultiLingual country = Utils.getMultiLingual(address.getCountryName());

            mAddress = new Address();
            mAddress.setCity(city);
            mAddress.setDistrict(district);
            mAddress.setState(state);
            mAddress.setCountry(country);
            mAddress.setPincode(address.getPostalCode());

        }
    }

    public Address getAddress() {
        // Update address based on user input in editText field
        if (mAddress != null) {
            int count = etAddress.getText().toString().split(",").length;
            String addressLine1Value = "";
            String addressLine2Value = "";
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    if (i <= Math.ceil(count / 2)) {
                        addressLine1Value += etAddress.getText().toString().split(",")[i] + ",";
                    } else {
                        addressLine2Value += etAddress.getText().toString().split(",")[i] + ",";
                    }
                }
            }

            if (addressLine1Value.endsWith(",")) {
                addressLine1Value = addressLine1Value.substring(0, addressLine1Value.length() - 1);
            }
            if (addressLine2Value.endsWith(",")) {
                addressLine2Value = addressLine2Value.substring(0, addressLine2Value.length() - 1);
            }

            mAddress.setAddressLine1(Utils.getMultiLingual(addressLine1Value));
            mAddress.setAddressLine2(Utils.getMultiLingual(addressLine2Value));
        }

        return mAddress;
    }

    public boolean checkValid() {
        if (etAddress.getText().toString().isEmpty()) {
            etAddress.requestFocus();
            etAddress.setError(getContext().getString(R.string.invalid_address));
            return false;
        }
        return true;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.removeAllViews();
        locationLiveData.removeObserver(addCurrentLocationObserver);
    }
}
