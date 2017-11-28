package fragments.animal;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import au.csiro.ozatlas.BuildConfig;
import au.csiro.ozatlas.R;
import au.csiro.ozatlas.adapter.SearchSpeciesAdapter;
import au.csiro.ozatlas.manager.FileUtils;
import au.csiro.ozatlas.manager.MarshMallowPermission;
import au.csiro.ozatlas.manager.Utils;
import au.csiro.ozatlas.model.SearchSpecies;
import au.csiro.ozatlas.model.SpeciesSearchResponse;
import au.csiro.ozatlas.rest.BieApiService;
import au.csiro.ozatlas.rest.NetworkClient;
import au.csiro.ozatlas.rest.SearchSpeciesSerializer;
import base.BaseMainActivityFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import model.track.SightingEvidenceTable;
import model.track.Species;

import static android.app.Activity.RESULT_OK;

/**
 * Created by sad038 on 9/10/17.
 */

public class AddAnimalFragment extends BaseMainActivityFragment {
    private static final int REQUEST_IMAGE_GALLERY = 3;
    private static final int REQUEST_IMAGE_CAPTURE = 4;
    private static final int DELAY_IN_MILLIS = 400;

    @BindView(R.id.editSpeciesName)
    AutoCompleteTextView editSpeciesName;
    @BindView(R.id.whatSeenSpinner)
    AppCompatSpinner whatSeenSpinner;
    @BindView(R.id.howRecentSpinner)
    AppCompatSpinner howRecentSpinner;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.speciesDetailLayout)
    LinearLayout speciesDetailLayout;
    @BindView(R.id.speciesURL)
    TextView speciesURL;
    @BindView(R.id.species_loading_indicator)
    ProgressBar speciesLoadingIndicator;
    @BindView(R.id.editLatitude)
    EditText editLatitude;
    @BindView(R.id.editLongitude)
    EditText editLongitude;

    private String mCurrentPhotoPath;
    private List<SearchSpecies> species = new ArrayList<>();
    private SightingEvidenceTable sightingEvidenceTable = new SightingEvidenceTable();
    private BieApiService bieApiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        //set the localized labels
        setLanguageValues();

        whatSeenSpinner.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.what_see_values, R.layout.item_textview));
        whatSeenSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sightingEvidenceTable.typeOfSign = (String) whatSeenSpinner.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        howRecentSpinner.setAdapter(ArrayAdapter.createFromResource(getContext(), R.array.how_recent_values, R.layout.item_textview));
        howRecentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sightingEvidenceTable.evidenceAgeClass = (String) howRecentSpinner.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //species search service
        Gson gson = new GsonBuilder().registerTypeAdapter(SpeciesSearchResponse.class, new SearchSpeciesSerializer()).create();
        bieApiService = new NetworkClient(getString(R.string.bie_url), gson).getRetrofit().create(BieApiService.class);

        editSpeciesName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                sightingEvidenceTable.species = new Species(species.get(position));
                speciesDetailLayout.setVisibility(View.VISIBLE);
                speciesURL.setText(String.format(Locale.getDefault(), "http://bie.ala.org.au/species/%s", sightingEvidenceTable.species.guid));
                if (BuildConfig.DEBUG) {
                    Toast.makeText(getActivity(), sightingEvidenceTable.species.commonName, Toast.LENGTH_LONG).show();
                }
            }
        });
        mCompositeDisposable.add(getSearchSpeciesResponseObserver());

        return view;
    }

    /**
     * network call for species suggestion
     *
     * @return
     */
    private DisposableObserver<SpeciesSearchResponse> getSearchSpeciesResponseObserver() {
        return RxTextView.textChangeEvents(editSpeciesName)
                .debounce(DELAY_IN_MILLIS, TimeUnit.MILLISECONDS)
                .map(textViewTextChangeEvent -> textViewTextChangeEvent.text().toString())
                .filter(s -> {
                    boolean call = s.length() > 1;
                    if (call)
                        getActivity().runOnUiThread(() -> speciesLoadingIndicator.setVisibility(View.VISIBLE));
                    return call;
                })
                .observeOn(Schedulers.io())
                .flatMap(s -> bieApiService.searchSpecies(s, "taxonomicStatus:accepted"))
                .observeOn(AndroidSchedulers.mainThread())
                .retry()
                .subscribeWith(new DisposableObserver<SpeciesSearchResponse>() {
                    @Override
                    public void onNext(SpeciesSearchResponse speciesSearchResponse) {
                        speciesLoadingIndicator.setVisibility(View.INVISIBLE);
                        species.clear();
                        species.addAll(speciesSearchResponse.results);

                        editSpeciesName.setAdapter(new SearchSpeciesAdapter(getActivity(), species));
                        if (species.size() == 0 || (sightingEvidenceTable.species != null && sightingEvidenceTable.species.name.equals(editSpeciesName.getText().toString()))) {
                            editSpeciesName.dismissDropDown();
                        } else {
                            editSpeciesName.showDropDown();
                            speciesDetailLayout.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleError(e, 0, "");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void prepareSightingEvidenceTableModel() {
        if (!Utils.isNullOrEmpty(editLatitude.getText().toString()))
            sightingEvidenceTable.observationLatitude = Double.parseDouble(editLatitude.getText().toString());
        if (!Utils.isNullOrEmpty(editLongitude.getText().toString()))
            sightingEvidenceTable.observationLongitude = Double.parseDouble(editLongitude.getText().toString());
    }

    private boolean isSightingEvidenceTableModelValid() {
        return sightingEvidenceTable.species != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                prepareSightingEvidenceTableModel();
                if (isSightingEvidenceTableModelValid()) {
                    Intent intent = new Intent();
                    intent.putExtra(getString(R.string.add_animal_parameter), sightingEvidenceTable);
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
                        FileUtils.galleryAddPic(getActivity(), mCurrentPhotoPath);
                        imageView.setImageBitmap(FileUtils.getBitmapFromFilePath(mCurrentPhotoPath));
                        mCurrentPhotoPath = null;
                    }
                    break;
                case REQUEST_IMAGE_GALLERY:
                    final Uri selectedImageUri = data.getData();
                    imageView.setImageURI(selectedImageUri);
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

    @Override
    protected void setLanguageValues() {

    }
}

