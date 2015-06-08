package axel.nennker.googlecredentialapi;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class MainActivity extends Activity {
  private static final String TAG = "GoogleCredentialPlugin";

  private static final String STATE_RESOLVING_ERROR = "resolving_error";

  // Request code to use when launching the resolution activity
  private static final int REQUEST_RESOLVE_ERROR = 1001;
  private static final int REQUEST_RESOLUTION = 0xcafebabe;

  GoogleApiClient mGoogleApiClient = null;
  boolean mResolvingError = false;

  private void saveCredentials(Credential credential) {
    Log.d(TAG, "saveCredentials");
    PendingResult<Status> result = Auth.CredentialsApi.save(mGoogleApiClient, credential);
    result.setResultCallback(new ResultCallback<Status>() {
      @Override
      public void onResult(Status status) {
        Log.d(TAG, "saveCredentials status=" + status);
        if (status.hasResolution()) {
          try {
            status.startResolutionForResult(MainActivity.this, REQUEST_RESOLUTION);
          } catch (SendIntentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    });
  }

  private void requestCredentials() {
    Log.d(TAG, "requestCredentials");
    CredentialRequest request = new CredentialRequest.Builder().setAccountTypes(IdentityProviders.YAHOO, IdentityProviders.GOOGLE).setSupportsPasswordLogin(true).build();

    PendingResult<CredentialRequestResult> result = Auth.CredentialsApi.request(mGoogleApiClient, request);
    result.setResultCallback(new ResultCallback<CredentialRequestResult>() {

      @Override
      public void onResult(CredentialRequestResult result) {
        Log.d(TAG, "requestCredentials::onResult");
        Credential credential = result.getCredential();
        if (credential != null) {
          Log.d(TAG, "credential.name: " + credential.getName());
          JSONObject resultJSON = new JSONObject();
          try {
            resultJSON.put("name", credential.getName());
            Log.d(TAG, resultJSON.toString());
          } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "onResult", e);
          }
        } else {
          Log.d(TAG, "credential is null");
          com.google.android.gms.common.api.Status status = result.getStatus();
          if (status != null) {
            Log.d(TAG, "status status.code=" + status.getStatusCode() + " status.message=" + status.getStatusMessage());
            Log.d(TAG, "status status.isSuccess=" + status.isSuccess());
            Log.d(TAG, "status.hasResolution()=" + status.hasResolution());
            if (CommonStatusCodes.SIGN_IN_REQUIRED == status.getStatusCode()) {
              Log.d(TAG, "sign in required");
              Log.d(TAG, "saving credential");
              String id = "id";
              String name = "ignisvulpis@gmail.com";
              String password = "password";
              Credential toSave = new Credential.Builder(id).setName(name).setPassword(password).build();
              saveCredentials(toSave);
            } else {
              try {
                Log.d(TAG, "starting resolution");
                status.startResolutionForResult(MainActivity.this, REQUEST_RESOLUTION);
              } catch (SendIntentException e) {
                e.printStackTrace();
                mGoogleApiClient.connect();
              }
            }
          } else {
            Log.d(TAG, "status is null");
          }
        }
      }
    });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    Log.d(TAG, "onActivityResult requestCode=" + requestCode + " resultCode=" + resultCode);
    // do something with the result
    super.onActivityResult(requestCode, resultCode, intent);
    if (resultCode == Activity.RESULT_OK) {
      Uri data = intent.getData();
      Log.d(TAG, "data=" + data);
      Bundle extras = intent.getExtras();
      JSONObject resultJSON = new JSONObject();
      for (String key : extras.keySet()) {
        Object object = extras.get(key);
        String str;
        if (object instanceof String) {
          str = (String) object;
        } else if (object instanceof Integer) {
          str = ((Integer) object).toString();
        } else {
          str = object.toString();
        }
        Log.d(TAG, "onActivityResult key=" + key + " value=" + str);
        try {
          resultJSON.put(key, object);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
      String resultString = resultJSON.toString();
      Log.d(TAG, "resultString:" + resultString);
    } else {
      Log.e(TAG, "resultCode is not ok: " + resultCode);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "onStart ");
    if (!mResolvingError) { // more about this later
      Log.d(TAG, "onStart connect");
      mGoogleApiClient.connect();
    } else {
      Log.d(TAG, "onStart already resolving error");
    }
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop ");
    if (mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    try {
      Log.d(TAG, "onCreate" + getIntent());

      mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

      ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {

        @Override
        public void onConnectionSuspended(int cause) {
          // The connection has been interrupted.
          // Disable any UI components that depend on Google APIs
          // until onConnected() is called.
          Log.d(TAG, "onConnectionSuspended cause:" + cause);
        }

        @Override
        public void onConnected(Bundle connectionHint) {
          // Connected to Google Play services!
          // The good stuff goes here.
          Log.d(TAG, "onConnected");
          requestCredentials();
        }

      };

      OnConnectionFailedListener onConnectionFailedListener = new OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult result) {
          Log.d(TAG, "onConnectionFailed result=" + result);
          if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
          } else if (result.hasResolution()) {
            try {
              mResolvingError = true;
              result.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
            } catch (SendIntentException e) {
              // There was an error with the resolution intent. Try again.
              mGoogleApiClient.connect();
            }
          } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            Log.e(TAG, "error resolving ");
            // showErrorDialog(result.getErrorCode());
            mResolvingError = true;
          }
        }

      };

      mGoogleApiClient = new GoogleApiClient.Builder(this, connectionCallbacks, onConnectionFailedListener).addApi(Auth.CREDENTIALS_API).build();

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
  }

}
