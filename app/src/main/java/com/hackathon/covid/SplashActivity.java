package com.hackathon.covid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.User;

import static com.hackathon.covid.utils.Constants.CONSUMER;
import static com.hackathon.covid.utils.Constants.NGO;
import static com.hackathon.covid.utils.Constants.SHOPKEEPER;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 0;
    public static Intent shareIntent = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        shareIntent = getIntent();
        FirebaseApp.initializeApp(this);
        if(FirebaseAuth.getInstance().getCurrentUser() == null) {
            SPLASH_TIME_OUT = 1000;
        }
        new Handler().postDelayed(() -> {
            if(FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            } else {
                FirebaseFirestore.getInstance().collection("Users")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                startActivity(documentSnapshot.toObject(User.class).getType());
                            }
                        });
            }
        }, SPLASH_TIME_OUT);
    }

    private void startActivity(String userType) {
        Intent intent = null;
        switch (userType) {
            case CONSUMER:
                intent = new Intent(SplashActivity.this, ConsumerActivity.class);
                break;
            case SHOPKEEPER:
                intent = new Intent(SplashActivity.this, ShopActivity.class);
                break;
            case NGO:
                intent = new Intent(SplashActivity.this, NgoActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
