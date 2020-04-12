package com.hackathon.covid;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.Point;
import com.hackathon.covid.data.Shop;
import com.hackathon.covid.utils.LocationManagerClient;
import com.hackathon.covid.utils.Utils;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;

public class GetMedsFragment extends Fragment implements OnMapReadyCallback{


    private ProgressBar progressBar;
    private MapView map;
    private ImageButton imageButton;
    private Button refresh;

    private FirebaseFirestore db;
    private GoogleMap mGoogleMap;
    private Point currentLocation;
    private ArrayList<Shop> shops;
    private Marker userMarker;
    private RxPermissions rxPermissions;
    private Shop selectedShop;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_get_meds, container, false);
        progressBar = root.findViewById(R.id.progress_bar);
        map = root.findViewById(R.id.map);
        imageButton = root.findViewById(R.id.locationButton);
        refresh = root.findViewById(R.id.refresh);
        shops = new ArrayList<>();
        map.onCreate(null);
        map.getMapAsync(this::onMapReady);
        db = FirebaseFirestore.getInstance();
        rxPermissions = new RxPermissions(this);
        fetchShops();
        setCurrentLocation();
        imageButton.setOnClickListener(view -> setCurrentLocation());
        refresh.setOnClickListener(view -> fetchShops());
        return root;
    }


    private void fetchShops() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Shops")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for(DocumentSnapshot documentSnapshot: queryDocumentSnapshots) {
                        Shop shop = documentSnapshot.toObject(Shop.class);
                        shops.add(shop);
                    }
                    refreshMarkerOptions(shops);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void refreshMarkerOptions(ArrayList<Shop> shops) {
        progressBar.setVisibility(View.VISIBLE);
        if (mGoogleMap != null) {
            mGoogleMap.clear();
            BitmapDescriptor unselectedMarker = Utils.bitmapDescriptorFromVector(getContext(), R.drawable.ic_circle);
            LatLngBounds.Builder latLngBoundsBuilder = new LatLngBounds.Builder();

            for (int i = 0; i < shops.size(); i++) {
                Shop shop = shops.get(i);
                MarkerOptions markerOptions = new MarkerOptions();
                Point location = new Point(shop.getLatitude(), shop.getLongitude());
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                markerOptions.position(latLng);

                markerOptions.icon(unselectedMarker);
                markerOptions.zIndex(0);
                Marker marker = mGoogleMap.addMarker(markerOptions);
                marker.setTag(i);
                marker.setSnippet("Sanitizers: " + shop.getSanitizer() +
                        "\nMasks: " + shop.getMasks());
                latLngBoundsBuilder.include(marker.getPosition());
            }
            if(currentLocation != null) {
                Marker marker = markUserLocation(currentLocation);
                latLngBoundsBuilder.include(marker.getPosition());
            } else {
                setCurrentLocation();
            }
            // Set camera
            try {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBoundsBuilder.build(), 100));
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
        userMarker = mGoogleMap.addMarker(userMarkerOptions);
        int c = 0;
        if(shops != null)
            c = shops.size();
        userMarker.setTag(c);
        userMarker.setTitle("That's you");
        return userMarker;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(marker -> {
            if((int)marker.getTag() == shops.size()) {
                // do something
            } else {
                selectedShop = shops.get((int)marker.getTag());
            }
            return false;
        });
        refreshMarkerOptions(shops);
    }

    private void setCurrentLocation() {
        rxPermissions.requestEachCombined(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(permission -> {
                    LocationManagerClient.getInstance(getContext())
                            .getLocationUpdate(getContext())
                            .observe(getViewLifecycleOwner(), location -> {
                                if(location != null) {
                                    currentLocation = new Point(location.getLatitude(), location.getLongitude());
                                    refreshMarkerOptions(shops);
                                }
                            });
                });
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }

    @Override
    public void onStop() {
        super.onStop();
        map.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        map.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        map.onDestroy();
    }
}
