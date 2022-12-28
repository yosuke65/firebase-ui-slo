package com.firebase.ui.auth.ui.phone;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.firebase.ui.auth.data.model.PhoneNumberVerificationRequiredException;
import com.firebase.ui.auth.data.model.Resource;
import com.firebase.ui.auth.viewmodel.AuthViewModelBase;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PhoneNumberVerificationHandler extends AuthViewModelBase<PhoneVerification> {
    private static final long AUTO_RETRIEVAL_TIMEOUT_SECONDS = 120;
    private static final String VERIFICATION_ID_KEY = "verification_id";

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;
    private PhoneNumberVerificationCallback mPhoneNumberVerificationCallback;


    public PhoneNumberVerificationHandler(Application application) {
        super(application);
    }

    public void setPhoneNumberVerificationCallback(PhoneNumberVerificationCallback callback) {
        mPhoneNumberVerificationCallback = callback;
    }

    public void verifyPhoneNumber(@NonNull Activity activity, final String number, boolean force) {
        setResult(Resource.forLoading());
        PhoneAuthOptions.Builder optionsBuilder = PhoneAuthOptions.newBuilder(getAuth())
                .setPhoneNumber(number)
                .setTimeout(AUTO_RETRIEVAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        if(mPhoneNumberVerificationCallback != null) {
                            mPhoneNumberVerificationCallback.onVerificationSuccess();
                        }
                        setResult(Resource.forSuccess(new PhoneVerification(
                                number, credential, true)));
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        setResult(Resource.forFailure(e));
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        if(mPhoneNumberVerificationCallback != null) {
                            mPhoneNumberVerificationCallback.onCodeSent();
                        }
                        mVerificationId = verificationId;
                        mForceResendingToken = token;
                        setResult(Resource.forFailure(
                                new PhoneNumberVerificationRequiredException(number)));
                    }
                });
        if (force) {
            optionsBuilder.setForceResendingToken(mForceResendingToken);
        }
        if (isBrowserAvailable(activity)) {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build());
        } else {
            setResult(Resource.forFailure(new ActivityNotFoundException("No browser was found in this device")));
        }
    }

    public void submitVerificationCode(String number, String code) {
        setResult(Resource.forSuccess(new PhoneVerification(
                number,
                PhoneAuthProvider.getCredential(mVerificationId, code),
                false)));
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(VERIFICATION_ID_KEY, mVerificationId);
    }

    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        if (mVerificationId == null && savedInstanceState != null) {
            mVerificationId = savedInstanceState.getString(VERIFICATION_ID_KEY);
        }
    }

    private boolean isBrowserAvailable(Activity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        return browserIntent.resolveActivity(activity.getPackageManager()) != null;
    }
}
