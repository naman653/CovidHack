package com.hackathon.covid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hackathon.covid.data.User;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hackathon.covid.utils.Constants.*;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int RC_SIGN_IN = 1;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAuth.AuthStateListener authStateListener;
    // [END declare_auth]

    List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.GoogleBuilder().build());

    private static Boolean phoneSignIn = false;

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private TextView mDetailText;

    private View view;
    private ProgressBar progressBar;

    private EditText mPhoneNumberField;
    private EditText mVerificationField;

    private Button mStartButton;
    private Button mVerifyButton;
    private Button mResendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mPhoneNumberField = findViewById(R.id.phone_edit_text);
        mVerificationField = findViewById(R.id.otp_edit_text);

        mDetailText = findViewById(R.id.detail);
        view = findViewById(R.id.loginView);
        progressBar = findViewById(R.id.progress_bar);

        view.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        mStartButton = findViewById(R.id.get_otp);
        mVerifyButton = findViewById(R.id.verify);
        mResendButton = findViewById(R.id.resend);

        // Assign click listeners
        mStartButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);
        mResendButton.setOnClickListener(this);

        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(mAuth.getCurrentUser() == null) {
                    AuthUI.getInstance().signOut(getBaseContext());
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                    phoneSignIn = false;
                }
            }
        };
        // [END initialize_auth]

        mPhoneNumberField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateUI(STATE_INITIALIZED);
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUI(STATE_INITIALIZED);
            }
        });
        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                phoneSignIn = true;
                // [END_EXCLUDE]

                // [START_EXCLUDE silent]
                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential);
                // [END_EXCLUDE]
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    mPhoneNumberField.setError("Invalid phone number.");
                    updateUI(STATE_VERIFY_FAILED);
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    mPhoneNumberField.setError("Quota exceeded.");
                    // [END_EXCLUDE]
                }

                // Show a message and update the UI
                // [START_EXCLUDE]
                updateUI(STATE_VERIFY_FAILED);
                // [END_EXCLUDE]
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                updateUI(STATE_CODE_SENT);
            }
        };
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            phoneSignIn = true;
            updateUI(STATE_SIGNIN_SUCCESS);
        }

        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification("+91" + mPhoneNumberField.getText().toString());
        }
        // [END_EXCLUDE]
    }

    @Override
    public void onBackPressed() {
        mAuth.removeAuthStateListener(authStateListener);
        mAuth.signOut();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        if(!phoneSignIn) {
            mAuth.removeAuthStateListener(authStateListener);
            mAuth.signOut();
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        mAuth.removeAuthStateListener(authStateListener);
        if(!phoneSignIn)
            mAuth.signOut();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mAuth.addAuthStateListener(authStateListener);
        updateUI(STATE_INITIALIZED);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(!phoneSignIn){
            mAuth.signOut();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Successfully signed in
            } else {
                finish();
            }
        }
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        if(mAuth.getCurrentUser().getPhoneNumber() == null)
            mAuth.getCurrentUser().linkWithCredential(credential)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            phoneSignIn = true;
                            updateUserInfo();
                            updateUI(STATE_SIGNIN_SUCCESS, mAuth.getCurrentUser());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            phoneSignIn = false;
                            if(e instanceof FirebaseAuthUserCollisionException) {
                                mPhoneNumberField.setError("User linked with other account");
                                updateUI(STATE_INITIALIZED);
                            }
                            else if(e instanceof FirebaseAuthInvalidCredentialsException) {
                                mVerificationField.setError("Invalid Code");
                                updateUI(STATE_VERIFY_FAILED);
                            }
                            else{
                                finish();
                            }
                        }
                    });
        else if (("+91" + mPhoneNumberField.getText().toString()).equals(mAuth.getCurrentUser().getPhoneNumber())) {
            phoneSignIn = true;
            updateUI(STATE_SIGNIN_SUCCESS, mAuth.getCurrentUser());
            updateUserInfo();
        }
        else {
            phoneSignIn = false;
            mPhoneNumberField.setError("User registered with other number");
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user);
        } else {
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {
            case STATE_INITIALIZED:
                enableViews(mStartButton, mPhoneNumberField);
                disableViews(mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(null);
                break;
            case STATE_CODE_SENT:
                // Code sent state, show the verification field, the
                enableViews(mVerifyButton, mResendButton, mPhoneNumberField, mVerificationField);
                disableViews(mStartButton);
                mDetailText.setText(R.string.status_code_sent);
                break;
            case STATE_VERIFY_FAILED:
                // Verification has failed, show all options
                enableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField);
                break;
            case STATE_VERIFY_SUCCESS:
                // Verification has succeeded, proceed to firebase sign in
                disableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField);
                mDetailText.setText(R.string.status_verification_succeeded);

                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        mVerificationField.setText(cred.getSmsCode());
                    } else {
                        mVerificationField.setText(R.string.instant_validation);
                    }
                }break;
            case STATE_SIGNIN_FAILED:
                mDetailText.setText(R.string.status_sign_in_failed);
                break;
            case STATE_SIGNIN_SUCCESS:
                break;
        }
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = "+91" + mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField.setError("Invalid phone number.");
            return false;
        }

        if(mAuth.getCurrentUser().getPhoneNumber() != null) {
            if (!mAuth.getCurrentUser().getPhoneNumber().equals(phoneNumber)){
                mPhoneNumberField.setError("Enter previously registered number");
                return false;
            }
        }
        return true;
    }

    private void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    private void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.get_otp:
                if (!validatePhoneNumber()) {
                    return;
                }
                startPhoneNumberVerification("+91" + mPhoneNumberField.getText().toString());
                break;
            case R.id.verify:
                hideKeyboard(this);
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;
            case R.id.resend:
                resendVerificationCode("+91" + mPhoneNumberField.getText().toString(), mResendToken);
                break;
        }
    }

    private void updateUserInfo(){
        final FirebaseUser user = mAuth.getCurrentUser();
        view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(!documentSnapshot.exists()){
                            showDialog();
                        } else {
                            startActivity(documentSnapshot.toObject(User.class).getType());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mAuth.signOut();
                        e.printStackTrace();
                        finish();
                    }
                });
    }

    private void showDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_role,null);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(v);
        dialog.setTitle("Choose your role");
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        v.findViewById(R.id.consumer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setNewUser(new User(CONSUMER, user.getUid()));
                dialog.dismiss();
            }
        });
//        v.findViewById(R.id.shopkeeper).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setNewUser(new User(SHOPKEEPER, user.getUid()));
//                dialog.dismiss();
//            }
//        });
//        v.findViewById(R.id.ngo).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                setNewUser(new User(NGO, user.getUid()));
//                dialog.dismiss();
//            }
//        });
        dialog.show();
    }

    private void setNewUser(final User user) {
        db.collection("Users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        startActivity(user.getType());
                    }
                });
    }
    private void startActivity(String userType) {
        Intent intent = null;
        switch (userType) {
            case CONSUMER:
                intent = new Intent(LoginActivity.this, ConsumerActivity.class);
                break;
            case SHOPKEEPER:
//                intent = new Intent(LoginActivity.this, ConsumerActivity.class);
                break;
            case NGO:
//                intent = new Intent(LoginActivity.this, ConsumerActivity.class);
                break;
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}