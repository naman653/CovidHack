package com.hackathon.covid;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.Point;
import com.hackathon.covid.data.ServedArea;
import com.hackathon.covid.data.UserRequest;
import com.hackathon.covid.utils.LocationManagerClient;
import com.hackathon.covid.utils.LocationUtils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NgoActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.locationButton)
    ImageButton imageButton;

    private RxPermissions rxPermissions;
    private Point currentLocation;
    private Marker userMarker;
    private FirebaseFirestore db;
    private GoogleMap mMap;
    private ArrayList<UserRequest> userRequests;
    private ArrayList<ServedArea> servedAreas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo);
        ButterKnife.bind(this);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        userRequests = new ArrayList<>();
        servedAreas = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        rxPermissions = new RxPermissions(this);
        setCurrentLocation();
        imageButton.setOnClickListener(view -> setCurrentLocation());
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we
     * just add a marker near Africa.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        refreshMarkerOptionsServedAreas(servedAreas);
    }

    private void setCurrentLocation() {
        rxPermissions.requestEachCombined(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(permission -> {
                    LocationManagerClient.getInstance(NgoActivity.this)
                            .getLocationUpdate(NgoActivity.this)
                            .observe(this, location -> {
                                if(location != null) {
                                    currentLocation = new Point(location.getLatitude(), location.getLongitude());
                                    markUserLocation(currentLocation);
                                }
                            });
                });
    }
    
    public void getRequests(View view) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("UserRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userRequests.clear();
                    for(DocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                        UserRequest userRequest = documentSnapshot.toObject(UserRequest.class);
                        if(currentLocation != null) {
                            if (LocationUtils.distance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                    userRequest.getLatitude(), userRequest.getLongitude()) < 3) {
                                userRequests.add(userRequest);
                            }
                        }
                    }
                    refreshMarkerOptionsUserRequests(userRequests);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });
    }

    public void getServedAreas(View view) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("ServedAreas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    servedAreas.clear();
                    for(DocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                        ServedArea servedArea = documentSnapshot.toObject(ServedArea.class);
                        if(currentLocation != null) {
                            if (LocationUtils.distance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                    servedArea.getLatitude(), servedArea.getLongitude()) < 3) {
                                servedAreas.add(servedArea);
                            }
                        }
                    }
                    refreshMarkerOptionsServedAreas(servedAreas);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void refreshMarkerOptionsUserRequests(ArrayList<UserRequest> userRequests) {
        progressBar.setVisibility(View.VISIBLE);
        if (mMap != null) {
            mMap.clear();
            BitmapDescriptor unselectedMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
            LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

            for (int i = 0; i < userRequests.size(); i++) {
                UserRequest userRequest = userRequests.get(i);
                MarkerOptions markerOptions = new MarkerOptions();
                Point location = new Point(userRequest.getLatitude(), userRequest.getLongitude());
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                markerOptions.position(latLng);

                markerOptions.icon(unselectedMarker);
                markerOptions.zIndex(0);
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(i);
                marker.setSnippet("Packets: " + userRequest.getPackets());
                latLngBoundsBuilder.include(marker.getPosition());
            }
            if(currentLocation != null) {
                Marker marker = markUserLocation(currentLocation);
                latLngBoundsBuilder.include(marker.getPosition());
            }
            // Set camera
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsBuilder.build(), 100));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    private void refreshMarkerOptionsServedAreas(ArrayList<ServedArea> servedAreas) {
        progressBar.setVisibility(View.VISIBLE);
        if (mMap != null) {
            mMap.clear();
            BitmapDescriptor unselectedMarker = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

            for (int i = 0; i < servedAreas.size(); i++) {
                ServedArea servedArea = servedAreas.get(i);
                MarkerOptions markerOptions = new MarkerOptions();
                Point location = new Point(servedArea.getLatitude(), servedArea.getLongitude());
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                markerOptions.position(latLng);

                markerOptions.icon(unselectedMarker);
                markerOptions.zIndex(0);
                Marker marker = mMap.addMarker(markerOptions);
                marker.setTag(i);
                marker.setSnippet("People count: " + servedArea.getPeople());
                latLngBoundsBuilder.include(marker.getPosition());
            }
            if(currentLocation != null) {
                Marker marker = markUserLocation(currentLocation);
                latLngBoundsBuilder.include(marker.getPosition());
            }
            // Set camera
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsBuilder.build(), 100));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    private Marker markUserLocation(Point point) {
        MarkerOptions userMarkerOptions = new MarkerOptions();
        userMarkerOptions.position(new LatLng(point.getLatitude(), point.getLongitude()));
        userMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(0.15f));
        userMarkerOptions.zIndex(0);
        if(userMarker != null) userMarker.remove();
        userMarker = mMap.addMarker(userMarkerOptions);
        userMarker.setTag(-1);
        userMarker.setTitle("That's you");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), 10));
        return userMarker;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.consumer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(NgoActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
