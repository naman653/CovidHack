package com.hackathon.covid;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.Point;
import com.hackathon.covid.data.UserRequest;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.hackathon.covid.utils.Constants.REQUEST_USER_LOCATION;

public class SeekHelpActvity extends AppCompatActivity {

    @BindView(R.id.locationEditWidget)
    LocationEditWidget locationEditWidget;
    @BindView(R.id.packets)
    EditText packets;

    private FirebaseFirestore db;

    private RxPermissions rxPermissions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seek_help_actvity);
        ButterKnife.bind(this);
        rxPermissions = new RxPermissions(this);
        db = FirebaseFirestore.getInstance();
        locationEditWidget.init(rxPermissions, null, null);
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

    @OnClick(R.id.submit)
    void submit(View view) {
        if(isValidRequest()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Point point = locationEditWidget.getLocation();
            int count;
            try {
                count = Integer.parseInt(packets.getText().toString());
            } catch (Exception e) {
                count = 0;
            }
            submitRequest(new UserRequest(currentUser.getDisplayName(),
                    currentUser.getPhoneNumber(),
                    point.getLatitude(),
                    point.getLongitude(),
                    count));
        }
    }

    private boolean isValidRequest() {
        boolean valid = true;
        if(locationEditWidget.etAddress.getText().toString().isEmpty()) {
            valid = false;
            locationEditWidget.etAddress.setError(getString(R.string.location_not_selected));
        }
        if(packets.getText().toString().isEmpty()) {
           valid = false;
           packets.setError(getString(R.string.enter_packets));
        } else if(Integer.parseInt(packets.getText().toString()) < 1) {
            valid = false;
            packets.setError(getString(R.string.enter_packets));
        }
        return valid;
    }
    private void submitRequest(UserRequest userRequest){
        db.collection("UserRequests")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .set(userRequest)
                .addOnSuccessListener(aVoid -> Toast.makeText(SeekHelpActvity.this, R.string.request_submitted, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(SeekHelpActvity.this, R.string.request_not_submitted, Toast.LENGTH_SHORT).show());
    }
}
