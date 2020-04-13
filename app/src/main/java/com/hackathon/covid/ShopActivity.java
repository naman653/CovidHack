package com.hackathon.covid;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.Point;
import com.hackathon.covid.data.Shop;
import com.hackathon.covid.data.UserRequest;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.hackathon.covid.utils.Constants.REQUEST_USER_LOCATION;

public class ShopActivity extends AppCompatActivity {

    @BindView(R.id.locationEditWidget)
    LocationEditWidget locationEditWidget;
    @BindView(R.id.masks)
    EditText masks;
    @BindView(R.id.sanitizers)
    EditText sanitizers;

    private FirebaseFirestore db;

    private RxPermissions rxPermissions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        ButterKnife.bind(this);
        rxPermissions = new RxPermissions(this);
        db = FirebaseFirestore.getInstance();
        locationEditWidget.init(rxPermissions, null, null);
    }

    @OnClick(R.id.submit)
    void submit(View view) {
        if(isValidRequest()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Point point = locationEditWidget.getLocation();
            int maskC;
            try {
                maskC = Integer.parseInt(masks.getText().toString());
            } catch (Exception e) {
                maskC = 0;
            }
            int sanitizerC;
            try {
                sanitizerC = Integer.parseInt(sanitizers.getText().toString());
            } catch (Exception e) {
                sanitizerC = 0;
            }
            submitRequest(new Shop(currentUser.getUid(),
                    currentUser.getPhoneNumber(),
                    maskC,
                    sanitizerC,
                    point.getLatitude(),
                    point.getLongitude()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_USER_LOCATION) {
            if(data != null) {
                locationEditWidget.updateLocation(data);
            }
        }
    }

    private boolean isValidRequest() {
        boolean valid = true;
        if(locationEditWidget.etAddress.getText().toString().isEmpty()) {
            valid = false;
            Toast.makeText(ShopActivity.this, R.string.location_not_selected, Toast.LENGTH_SHORT).show();
        }
        if(masks.getText().toString().isEmpty()) {
            valid = false;
            Toast.makeText(ShopActivity.this, R.string.valid_masks, Toast.LENGTH_SHORT).show();
        }
        if(sanitizers.getText().toString().isEmpty()) {
            valid = false;
            Toast.makeText(ShopActivity.this, R.string.valid_sanitizers, Toast.LENGTH_SHORT).show();
        }
        return valid;
    }
    private void submitRequest(Shop shop){
        db.collection("Shops")
                .document(shop.getLatitude() + "" + shop.getLongitude())
                .set(shop)
                .addOnSuccessListener(aVoid -> Toast.makeText(ShopActivity.this, R.string.request_submitted, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ShopActivity.this, R.string.request_not_submitted, Toast.LENGTH_SHORT).show());
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
        Intent intent = new Intent(ShopActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
