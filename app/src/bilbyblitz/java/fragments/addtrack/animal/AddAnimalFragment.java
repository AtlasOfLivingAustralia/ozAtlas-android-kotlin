package fragments.addtrack.animal;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import activity.SingleFragmentActivity;
import au.csiro.ozatlas.BuildConfig;
import au.csiro.ozatlas.R;
import au.csiro.ozatlas.manager.AtlasDialogManager;
import au.csiro.ozatlas.manager.FileUtils;
import au.csiro.ozatlas.manager.Language;
import au.csiro.ozatlas.manager.MarshMallowPermission;
import au.csiro.ozatlas.manager.Utils;
import au.csiro.ozatlas.model.SearchSpecies;
import base.BaseMainActivityFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fragments.CustomSpinnerAdapter;
import fragments.addtrack.map.LocationUpdatesService;
import io.realm.RealmResults;
import model.track.SightingEvidenceTable;
import model.track.Species;

import static android.app.Activity.RESULT_OK;

/**
 * Created by sad038 on 9/10/17.
 */

public class AddAnimalFragment extends BaseMainActivityFragment {
    private static final int REQUEST_IMAGE_GALLERY = 3;
    private static final int REQUEST_IMAGE_CAPTURE = 4;
    private static final int REQUEST_AVAILABLE_SPECIES = 5;

    @BindView(R.id.editSpeciesName)
    EditText editSpeciesName;
    @BindView(R.id.whatSeenSpinner)
    AppCompatSpinner whatSeenSpinner;
    @BindView(R.id.howRecentSpinner)
    AppCompatSpinner howRecentSpinner;
    @BindView(R.id.animalAgeSpinner)
    AppCompatSpinner animalAgeSpinner;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.editLatitude)
    EditText editLatitude;
    @BindView(R.id.editLongitude)
    EditText editLongitude;
    @BindView(R.id.animalAgeTextView)
    TextView animalAgeTextView;
    @BindView(R.id.whatSeenTextView)
    TextView whatSeenTextView;
    @BindView(R.id.recentTextView)
    TextView recentTextView;
    @BindView(R.id.inputLayoutLatitude)
    TextInputLayout inputLayoutLatitude;
    @BindView(R.id.inputLayoutLongitude)
    TextInputLayout inputLayoutLongitude;
    @BindView(R.id.inputLayoutSpeciesName)
    TextInputLayout inputLayoutSpeciesName;
    @BindView(R.id.addLocation)
    Button addLocation;

    private boolean needLocationUpdate = true;
    private String mCurrentPhotoPath;
    private SightingEvidenceTable sightingEvidenceTable;
    private LocationManager locationManager;
    private MyReceiver myReceiver;
    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;
    // Tracks the bound state of the service.
    private boolean mBound = false;

    @Override
    protected void setLanguageValues(Language language) {
        setTitle(localisedString("animal", R.string.animal));
        inputLayoutSpeciesName.setHint(localisedString("what_animal", R.string.what_animal));
        inputLayoutLatitude.setHint(localisedString("sign_latitude", R.string.sign_latitude));
        inputLayoutLongitude.setHint(localisedString("sign_longitude", R.string.sign_longitude));
        recentTextView.setText(localisedString("how_recent", R.string.how_recent));
        whatSeenTextView.setText(localisedString("what_you_see", R.string.what_you_see));
        animalAgeTextView.setText(localisedString("how_old_animal", R.string.how_old_animal));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        //set the localized labels
        setLanguageValues(sharedPreferences.getLanguageEnumLanguage());

        whatSeenSpinner.setAdapter(new CustomSpinnerAdapter(getContext(), getResources().getStringArray( R.array.what_see_values), R.layout.item_textview));
        howRecentSpinner.setAdapter(new CustomSpinnerAdapter(getContext(), getResources().getStringArray( R.array.how_recent_values), R.layout.item_textview));
        animalAgeSpinner.setAdapter(new CustomSpinnerAdapter(getContext(), getResources().getStringArray( R.array.animal_age_values), R.layout.item_textview));

        Bundle bundle = getArguments();
        if (bundle == null) {
            sightingEvidenceTable = new SightingEvidenceTable();
        } else {
            sightingEvidenceTable = Parcels.unwrap(bundle.getParcelable(getString(R.string.add_animal_parameter)));
            if (sightingEvidenceTable == null)
                sightingEvidenceTable = new SightingEvidenceTable();
            else
                setValues();
        }

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        myReceiver = new MyReceiver();

        return view;
    }

    @OnClick(R.id.editSpeciesName)
    void editSpeciesName() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(getString(R.string.fragment_type_parameter), SingleFragmentActivity.FragmentType.AVAILABLE_SPECIES);
        Intent intent = new Intent(getActivity(), SingleFragmentActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_AVAILABLE_SPECIES);
    }

    private void setValues() {
        if (sightingEvidenceTable.species != null) {
            editSpeciesName.setText(sightingEvidenceTable.species.name);
        }

        whatSeenSpinner.setSelection(Utils.stringSearchInArray(getResources().getStringArray(R.array.what_see_values), sightingEvidenceTable.typeOfSign));
        howRecentSpinner.setSelection(Utils.stringSearchInArray(getResources().getStringArray(R.array.how_recent_values), sightingEvidenceTable.evidenceAgeClass));
        animalAgeSpinner.setSelection(Utils.stringSearchInArray(getResources().getStringArray(R.array.animal_age_values), sightingEvidenceTable.ageClassOfAnimal));
        if (sightingEvidenceTable.mPhotoPath != null) {
            imageView.setImageBitmap(FileUtils.getBitmapFromFilePath(sightingEvidenceTable.mPhotoPath));
        }
        if (sightingEvidenceTable.observationLatitude != null)
            editLatitude.setText(String.valueOf(sightingEvidenceTable.observationLatitude));
        if (sightingEvidenceTable.observationLongitude != null)
            editLongitude.setText(String.valueOf(sightingEvidenceTable.observationLongitude));
    }

    private boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @OnClick(R.id.addLocation)
    void addLocation() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (checkPermissions()) {
                needLocationUpdate = true;
            }
        } else {
            AtlasDialogManager.alertBox(getActivity(), "Your Device's GPS or Network is Disable", "Location Provider Status", "Setting", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    dialog.cancel();
                }
            });
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mService.requestLocationUpdates();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        if (checkPermissions()) {
            getActivity().bindService(new Intent(getActivity(), LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
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

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(myReceiver, new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(myReceiver);
        if(mService!=null) {
            mService.removeLocationUpdates();
            getActivity().unbindService(mServiceConnection);
            mService = null;
            mBound = false;
        }
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void prepareSightingEvidenceTableModel() {
        if (!Utils.isNullOrEmpty(editLatitude.getText().toString()))
            sightingEvidenceTable.observationLatitude = Utils.parseDouble(editLatitude.getText().toString());
        if (!Utils.isNullOrEmpty(editLongitude.getText().toString()))
            sightingEvidenceTable.observationLongitude = Utils.parseDouble(editLongitude.getText().toString());
    }

    private boolean isSightingEvidenceTableModelValid() {
        return sightingEvidenceTable.species != null;
    }

    private void prepareData() {
        sightingEvidenceTable.typeOfSign = whatSeenSpinner.getSelectedItemPosition() == 0 ? null : (String) whatSeenSpinner.getSelectedItem();
        sightingEvidenceTable.evidenceAgeClass = howRecentSpinner.getSelectedItemPosition() == 0 ? null : (String) howRecentSpinner.getSelectedItem();
        sightingEvidenceTable.ageClassOfAnimal = animalAgeSpinner.getSelectedItemPosition() == 0 ? null : (String) animalAgeSpinner.getSelectedItem();
        sightingEvidenceTable.observationLongitude = Utils.parseDouble(editLongitude.getText().toString());
        sightingEvidenceTable.observationLatitude = Utils.parseDouble(editLatitude.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                prepareSightingEvidenceTableModel();
                if (isSightingEvidenceTableModelValid()) {
                    prepareData();
                    Intent intent = new Intent();
                    intent.putExtra(getString(R.string.add_animal_parameter), Parcels.wrap(sightingEvidenceTable));
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().onBackPressed();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called after the autocomplete activity has finished to return its result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (mCurrentPhotoPath != null) {
                        sightingEvidenceTable.mPhotoPath = mCurrentPhotoPath;
                        FileUtils.galleryAddPic(getActivity(), mCurrentPhotoPath);
                        imageView.setImageBitmap(FileUtils.getBitmapFromFilePath(mCurrentPhotoPath));
                    }
                    break;
                case REQUEST_IMAGE_GALLERY:
                    final Uri selectedImageUri = data.getData();
                    mCurrentPhotoPath = FileUtils.getPath(getActivity(), selectedImageUri);
                    sightingEvidenceTable.mPhotoPath = mCurrentPhotoPath;
                    imageView.setImageURI(selectedImageUri);
                    break;
                case REQUEST_AVAILABLE_SPECIES:
                    String id = data.getStringExtra(getString(R.string.species_parameter));
                    RealmResults<SearchSpecies> results = realm.where(SearchSpecies.class).equalTo("realmId", id).findAllAsync();
                    results.addChangeListener((collection, changeSet) -> {
                        if (isAdded() && collection.size() > 0) {
                            SearchSpecies species = collection.first();
                            sightingEvidenceTable.species = new Species(species);
                            editSpeciesName.setText(sightingEvidenceTable.species.name);
                        }
                    });
                    break;
            }
        }
    }


    /**
     * Marshmellow permission
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MarshMallowPermission.CAMERA_PERMISSION_REQUEST_CODE:
            case MarshMallowPermission.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImage();
                } else {
                    //todo permission denied, boo! Disable the functionality that depends on this permission.
                }
                break;
        }
    }

    @OnClick(R.id.addPhotoButton)
    void pickImage() {
        MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);
        if (!marshMallowPermission.isPermissionGrantedForExternalStorage()) {
            marshMallowPermission.requestPermissionForExternalStorage();
        } else {
            if (!marshMallowPermission.isPermissionGrantedForCamera()) {
                marshMallowPermission.requestPermissionForCamera();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DateTimeDialogTheme);
                builder.setItems(R.array.image_upload, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            dispatchTakePictureIntent();
                        } else if (which == 1) {
                            openGalleryLocal();
                        }
                    }
                }).show();
            }
        }
    }

    /**
     * Make a filename for the camera picture
     *
     * @return
     * @throws IOException
     */
    private File setUpPhotoFile() throws IOException {
        mCurrentPhotoPath = null;
        File f = FileUtils.createImageFile(getActivity());
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }


    /**
     * open the Gallery
     */
    private void openGalleryLocal() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_IMAGE_GALLERY);
    }

    /**
     * method to start the camera
     */
    private void dispatchTakePictureIntent() {
        File f = null;
        mCurrentPhotoPath = null;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            f = setUpPhotoFile();
            mCurrentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileUtils.getUriFromFileProvider(getActivity(), f));

        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            mCurrentPhotoPath = null;
        }

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            List<ResolveInfo> resInfoList = getActivity().getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                getActivity().grantUriPermission(packageName, FileUtils.getUriFromFileProvider(getActivity(), f), Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void setLatitudeLongitude(Double latitude, Double longitude) {
        if (needLocationUpdate) {
            needLocationUpdate = false;
            addLocation.setText(getString(R.string.update_location));
            editLatitude.setText(String.format(Locale.getDefault(), "%.4f", latitude));
            editLongitude.setText(String.format(Locale.getDefault(), "%.4f", longitude));
        }
    }

    class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location;

            location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                //if (BuildConfig.DEBUG)
                //    Toast.makeText(getContext(), location.toString(), Toast.LENGTH_SHORT).show();

                setLatitudeLongitude(location.getLatitude(), location.getLongitude());
            }
        }
    }
}

