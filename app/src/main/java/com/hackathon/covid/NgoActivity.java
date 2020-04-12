package com.hackathon.covid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

public class NgoActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    double[] latArray = new  double[]{24.4604, 24.4831, 24.5023, 24.5373, 24.4699};
    double[] lonArray = new  double[]{72.7665, 72.7840, 72.7910, 72.7950, 72.7690};
    double[] distArray = new double[4];
    private GoogleMap mMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        distArray[0] = distance(latArray[0], lonArray[0], latArray[1], lonArray[1]);
        distArray[1] = distance(latArray[0], lonArray[0], latArray[2], lonArray[2]);
        distArray[2] = distance(latArray[0], lonArray[0], latArray[3], lonArray[3]);
        distArray[3] = distance(latArray[0], lonArray[0], latArray[4], lonArray[4]);
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we
     * just add a marker near Africa.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        int[] population = new int[]{600, 900, 1500, 2000};
        String[] mMarkerPos = new String[]{"Riico Colony", "Sadar Bazar", "Manpur",  "talheti", "santpur"};
        int totalPacks = 10000;
        //String mkitchenMarker = "Riico Colony, Community Kitchen, Packets: " + totalPacks;
        String mGrowthCenterMarker = "Growth Center, Needy ";
        map.addMarker(new MarkerOptions().position(new LatLng(24.4604, 72.7665  )).title("Kitchen")
                .snippet(mMarkerPos[0] + " " + totalPacks)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        map.addMarker(new MarkerOptions().position(new LatLng(24.4831, 72.7840  )).title("Needy")
                .snippet(mMarkerPos[1] + " " + population[0])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        map.addMarker(new MarkerOptions().position(new LatLng(24.5023, 72.7910  )).title("Needy")
                .snippet(mMarkerPos[2] + " " + population[1])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        map.addMarker(new MarkerOptions().position(new LatLng(24.5373, 72.7950  )).title("Needy")
                .snippet(mMarkerPos[3] + " " + population[2])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        map.addMarker(new MarkerOptions().position(new LatLng(24.4699, 72.7690  )).title("Needy")
                .snippet(mMarkerPos[4] + " " + population[3])
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
    }

    public double distance(double lat1, double lon1, double lat2, double lon2){
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double r = 6371;
        return(c * r);
    }

    public void path(View v){
        int i=1;
        String link = "http://maps.google.com/maps?saddr=" + latArray[0] + "," + lonArray[0] + "&daddr=" + latArray[i] + "," + lonArray[i];


        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(link));
        startActivity(intent);
    }

    public void getRoute(View v){
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?saddr=24.4604,72.7665&daddr=24.4845,72.7932"));
        startActivity(intent);
    }

    public void getRequest(View v){

        mMap.addMarker(new MarkerOptions().position(new LatLng(24.4845, 72.7932  )).title("Name")
                .snippet("Luniapur" + " " + "5")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        mMap.addMarker(new MarkerOptions().position(new LatLng(24.5010, 72.7622  )).title("Name")
                .snippet("New Town" + " " + "5")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
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
