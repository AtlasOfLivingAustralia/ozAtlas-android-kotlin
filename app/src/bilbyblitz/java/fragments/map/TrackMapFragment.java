package fragments.map;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import au.csiro.ozatlas.R;
import base.BaseMainActivityFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fragments.ValidationCheck;

/**
 * Created by sad038 on 9/10/17.
 */

public class TrackMapFragment extends BaseMainActivityFragment implements ValidationCheck, OnMapReadyCallback {
    private final float INITIAL_ZOOM = 10.2f;

    @BindView(R.id.surveySpinner)
    AppCompatSpinner surveySpinner;
    @BindView(R.id.startGPSButton)
    Button startGPSButton;
    @BindView(R.id.editCentroidLatitude)
    EditText editCentroidLatitude;
    @BindView(R.id.editCentroidLongitude)
    EditText editCentroidLongitude;

    private List<Location> locations = new ArrayList<>();
    private GoogleMap googleMap;

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mService.requestLocationUpdates();
            setButtonsState(true);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
            setButtonsState(false);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_track_map, container, false);
        ButterKnife.bind(this, view);

        myReceiver = new MyReceiver();
        // Check that the user hasn't revoked permissions by going to Settings.
        if (!checkPermissions()) {
            requestPermissions();
        }

        surveySpinner.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.survey_type, R.layout.item_textview));

        //set the localized labels
        setLanguageValues();

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        // Restore the state of the buttons when the activity (re)launches.
        //setButtonsState(false);

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        //if (checkPermissions())
        //    getActivity().bindService(new Intent(getActivity(), LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(myReceiver, new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            getActivity().unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * look for hardware GPS location
     */
    @OnClick(R.id.startGPSButton)
    public void lookForGPSLocation() {
        if (startGPSButton.getText().equals(getString(R.string.start_gps))) {
            if (!checkPermissions()) {
                requestPermissions();
            } else {
                getActivity().bindService(new Intent(getActivity(), LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        } else {
            mService.removeLocationUpdates();
            setButtonsState(false);
        }
    }


    private void setGoogleMapMarker(LatLng latLng) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, INITIAL_ZOOM));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fm.findFragmentByTag("mapFragment");
        if (mapFragment == null) {
            mapFragment = new SupportMapFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.mapLayout, mapFragment, "mapFragment");
            ft.commit();
            fm.executePendingTransactions();
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void setLanguageValues() {

    }

    @Override
    public String getValidationMessage() {
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                getActivity().bindService(new Intent(getActivity(), LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                //mService.requestLocationUpdates();
            } else {
                // Permission denied.
                setButtonsState(false);
            }
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(getContext(), location.toString(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            startGPSButton.setText(getString(R.string.stop_gps));
        } else {
            startGPSButton.setText(getString(R.string.start_gps));
        }
    }

}
