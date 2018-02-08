package fragments.addtrack;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.UUID;

import activity.SingleFragmentActivity;
import au.csiro.ozatlas.R;
import au.csiro.ozatlas.manager.AtlasDialogManager;
import au.csiro.ozatlas.manager.AtlasManager;
import au.csiro.ozatlas.manager.FileUtils;
import au.csiro.ozatlas.manager.Language;
import au.csiro.ozatlas.manager.Utils;
import au.csiro.ozatlas.model.ImageUploadResponse;
import au.csiro.ozatlas.model.Project;
import au.csiro.ozatlas.model.map.CheckMapInfo;
import au.csiro.ozatlas.model.map.MapResponse;
import base.BaseMainActivityFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import fragments.addtrack.animal.AnimalFragment;
import fragments.addtrack.country.TrackCountryFragment;
import fragments.addtrack.map.TrackMapFragment;
import fragments.addtrack.trackers.TrackersFragment;
import fragments.draft.DraftTrackListFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import model.map.Extent;
import model.map.Geometry;
import model.map.MapModel;
import model.map.Site;
import model.track.BilbyBlitzData;
import model.track.BilbyBlitzOutput;
import model.track.BilbyLocation;
import model.track.ImageModel;
import model.track.TrackModel;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

/**
 * Created by sad038 on 25/10/17.
 */

public class AddTrackFragment extends BaseMainActivityFragment {

    private final int NUMBER_OF_FRAGMENTS = 4;
    public boolean acquireGPSLocation = true;
    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    private boolean practiseView;
    private Project project;
    private int imageUploadCount = 0;
    private TrackModel trackModel = new TrackModel();
    private TrackerPagerAdapter pagerAdapter;
    private TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            Utils.closeKeyboard(getActivity(), getView().getWindowToken());
            if (tab.getPosition() == 3) {
                showFloatingButton();
            } else {
                hideFloatingButton();
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    public BilbyBlitzData getBilbyBlitzData() {
        return trackModel.outputs.get(0).data;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_track, container, false);
        setHasOptionsMenu(true);
        ButterKnife.bind(this, view);

        Bundle bundle = getArguments();
        if (bundle != null) {
            practiseView = bundle.getBoolean(getString(R.string.practise_parameter));
        }

        pager.setOffscreenPageLimit(3);

        project = sharedPreferences.getSelectedProject();
        if (project == null) {
            showSnackBarMessage(getString(R.string.project_selection_message));
        }

        //set the localized labels
        setLanguageValues(sharedPreferences.getLanguageEnumLanguage());

        getDataForEdit();

        return view;
    }


    private void getDataForEdit() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            long primaryKey = bundle.getLong(getString(R.string.primary_key_parameter), -1);
            if (primaryKey != -1) {
                setTitle(getString(R.string.edit_track));
                RealmQuery<TrackModel> query = realm.where(TrackModel.class).equalTo("realmId", primaryKey);
                RealmResults<TrackModel> results = query.findAllAsync();
                results.addChangeListener(element -> {
                    trackModel = realm.copyFromRealm(element.first());
                    AtlasDialogManager.alertBox(getActivity(), getString(R.string.add_gps_location_in_edit), getString(R.string.gps_edit_title), "ADD", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            acquireGPSLocation = true;
                            tabSetup();
                        }
                    }, "NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            acquireGPSLocation = false;
                            tabSetup();
                        }
                    });
                });
            } else {
                defaultSetup();
            }
            return;
        }

        defaultSetup();
    }

    private void defaultSetup() {
        trackModel.outputs = new RealmList<>();
        if (project != null) {
            trackModel.projectName = project.name;
            trackModel.projectId = project.projectId;
            trackModel.type = getString(R.string.project_type);
            trackModel.activityId = project.projectActivityId;
        }
        BilbyBlitzOutput output = new BilbyBlitzOutput();
        output.selectFromSitesOnly = false;
        output.data = new BilbyBlitzData();
        output.name = getString(R.string.project_type);
        trackModel.outputs.add(output);
        tabSetup();
    }

    private void tabSetup() {
        pagerAdapter = new TrackerPagerAdapter();
        pager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
        tabLayout.addOnTabSelectedListener(tabSelectedListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
        if (practiseView)
            menu.findItem(R.id.save).setTitle("FINISH");
        super.onCreateOptionsMenu(menu, inflater);
    }

    public boolean isPractiseView() {
        return practiseView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //when the user will press the submit menu item
            case R.id.save:
                if (project != null) {
                    if (practiseView) {
                        AtlasDialogManager.alertBox(getActivity(), getString(R.string.close_message), getString(R.string.close_title), (dialog, which) -> {
                            AtlasManager.hideKeyboard(getActivity());
                            setDrawerMenuChecked(R.id.home);
                            setDrawerMenuClicked(R.id.home);
                        });
                    } else {
                        AtlasDialogManager.alertBox(getActivity(), getString(R.string.track_save_message),
                                getString(R.string.track_save_title),
                                getString(R.string.save), (dialog, which) -> {
                                    saveLocally(true);
                                });
                    }
                } else {
                    showSnackBarMessage(getString(R.string.project_selection_message));
                }
                break;
        }
        return true;
    }


    public void saveLocally(boolean goToDraft) {
        if (trackModel != null && !trackModel.isManaged()) {
            for (int j = 0; j < NUMBER_OF_FRAGMENTS; j++) {
                BilbyDataManager bilbyDataManager = (BilbyDataManager) pagerAdapter.getRegisteredFragment(j);
                if (bilbyDataManager != null) {
                    bilbyDataManager.prepareData();
                }
            }
            if (trackModel.realmId == null) {
                trackModel.realmId = getPrimaryKeyValue();
            }
            realm.executeTransactionAsync(realm -> {
                realm.insertOrUpdate(trackModel);
                if (goToDraft && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        AtlasManager.hideKeyboard(getActivity());
                        if (getActivity() instanceof SingleFragmentActivity) {
                            getActivity().setResult(RESULT_OK);
                            getActivity().finish();
                        } else {
                            showSnackBarMessage(getString(R.string.successful_local_save));
                            setDrawerMenuChecked(R.id.nav_review_track);
                            getFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new DraftTrackListFragment()).commit();
                        }
                    });
                }
            });
        }
    }

    private long getPrimaryKeyValue() {
        Number number = realm.where(TrackModel.class).max("realmId");
        if (number == null)
            return 1L;
        return number.longValue() + 1;
    }

    @Override
    protected void setLanguageValues(Language language) {
        if (practiseView) {
            setTitle(localisedString("practise_track", R.string.practise_track));
        } else {
            setTitle(localisedString("add_track", R.string.add_track));
        }
    }

    private MapModel getMapModel(RealmList<BilbyLocation> tempLocations) {
        /* Test Data
        tempLocations = new RealmList<>();
        tempLocations.add(new BilbyLocation(143.40, -13.27));
        tempLocations.add(new BilbyLocation(143.40, -13.25));*/
        if (tempLocations != null && tempLocations.size() > 1) {
            MapModel mapModel = new MapModel();
            mapModel.site = new Site();
            if (project != null) {
                mapModel.pActivityId = project.projectActivityId;
                mapModel.site.name = project.name + "-" + UUID.randomUUID().toString();
            }
            mapModel.site.visibility = "private";
            mapModel.site.asyncUpdate = true;
            mapModel.site.projects = new String[]{project.projectId};
            mapModel.site.extent = new Extent();
            mapModel.site.extent.source = "drawn";
            mapModel.site.extent.geometry = new Geometry();
            mapModel.site.extent.geometry.areaKmSq = 0.0;
            mapModel.site.extent.geometry.type = "LineString";
            if (tempLocations.size() > 0) {
                mapModel.site.extent.geometry.centre = new Double[2];
                mapModel.site.extent.geometry.centre[0] = tempLocations.get(0).longitude;
                mapModel.site.extent.geometry.centre[1] = tempLocations.get(0).latitude;
            }
            mapModel.site.extent.geometry.coordinates = new Double[tempLocations.size()][2];
            for (int i = 0; i < tempLocations.size(); i++) {
                BilbyLocation bilbyLocation = tempLocations.get(i);
                mapModel.site.extent.geometry.coordinates[i][0] = bilbyLocation.longitude;
                mapModel.site.extent.geometry.coordinates[i][1] = bilbyLocation.latitude;
            }
            return mapModel;
        } else {
            showSnackBarMessage(getString(R.string.at_least_two_coordinates));
        }
        return null;
    }

    private void uploadMap(MapModel mapModel) {
        Log.d("MAP_MODEL", new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(mapModel));
        mCompositeDisposable.add(restClient.getService().postMap(mapModel)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<MapResponse>() {
                    @Override
                    public void onNext(MapResponse mapResponse) {
                        trackModel.siteId = mapResponse.id;
                        trackModel.outputs.get(0).checkMapInfo = new CheckMapInfo();
                        trackModel.outputs.get(0).checkMapInfo.validation = true;
                        trackModel.outputs.get(0).data.location = mapResponse.id;
                    }

                    @Override
                    public void onError(Throwable e) {
                        hideProgressDialog();
                        handleError(e, 0, getString(R.string.map_upload_fail));//+ "\n"+ new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(mapModel)
                    }

                    @Override
                    public void onComplete() {
                        uploadPhotos();
                    }
                }));
    }

    /**
     * upload photos
     */
    private void uploadPhotos() {
        if (trackModel.outputs.get(0).data.sightingEvidenceTable != null && imageUploadCount < trackModel.outputs.get(0).data.sightingEvidenceTable.size()) {
            if (trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).mPhotoPath != null) {
                mCompositeDisposable.add(restClient.getService().uploadPhoto(FileUtils.getMultipart(trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).mPhotoPath))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<ImageUploadResponse>() {
                            @Override
                            public void onNext(ImageUploadResponse value) {
                                Log.d("uploadPhotos", value.files[0].thumbnail_url);
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign = new RealmList<>();
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.add(new ImageModel());
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).thumbnailUrl = value.files[0].thumbnail_url;
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).url = value.files[0].url;
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).contentType = value.files[0].contentType;
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).staged = true;
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).dateTaken = value.files[0].date;
                                trackModel.outputs.get(0).data.sightingEvidenceTable.get(imageUploadCount).imageOfSign.get(0).filesize = value.files[0].size;
                            }

                            @Override
                            public void onError(Throwable e) {
                                hideProgressDialog();
                                handleError(e, 0, getString(R.string.species_photo_upload_fail));
                            }

                            @Override
                            public void onComplete() {
                                imageUploadCount++;
                                if (imageUploadCount < trackModel.outputs.get(0).data.sightingEvidenceTable.size())
                                    uploadPhotos();
                                else {
                                    uploadLocationImages(trackModel);
                                }

                            }
                        }));
            } else {
                imageUploadCount++;
                if (imageUploadCount < trackModel.outputs.get(0).data.sightingEvidenceTable.size())
                    uploadPhotos();
                else {
                    uploadLocationImages(trackModel);
                }
            }
        } else {
            uploadLocationImages(trackModel);
        }
    }

    private void uploadLocationImages(final TrackModel trackModel) {
        if (trackModel.outputs.get(0).data.locationImage != null && trackModel.outputs.get(0).data.locationImage.size() > 0) {
            mCompositeDisposable.add(restClient.getService().uploadPhoto(FileUtils.getMultipart(trackModel.outputs.get(0).data.locationImage.get(0).mPhotoPath))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<ImageUploadResponse>() {
                        @Override
                        public void onNext(ImageUploadResponse value) {
                            Log.d("uploadLocationImages", value.files[0].thumbnail_url);
                            trackModel.outputs.get(0).data.locationImage.get(0).thumbnailUrl = value.files[0].thumbnail_url;
                            trackModel.outputs.get(0).data.locationImage.get(0).url = value.files[0].url;
                            trackModel.outputs.get(0).data.locationImage.get(0).contentType = value.files[0].contentType;
                            trackModel.outputs.get(0).data.locationImage.get(0).staged = true;
                            trackModel.outputs.get(0).data.locationImage.get(0).dateTaken = value.files[0].date;
                            trackModel.outputs.get(0).data.locationImage.get(0).filesize = value.files[0].size;
                        }

                        @Override
                        public void onError(Throwable e) {
                            hideProgressDialog();
                            handleError(e, 0, getString(R.string.country_photo_upload_fail));
                        }

                        @Override
                        public void onComplete() {
                            saveData();
                        }
                    }));
        } else {
            saveData();
        }
    }

    private void saveData() {
        Log.d("TRACK_MODEL", new Gson().toJson(trackModel));
        mCompositeDisposable.add(restClient.getService().postTracks(project.projectActivityId, trackModel)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Response<Void>>() {
                    @Override
                    public void onNext(Response<Void> value) {
                        if (value.isSuccessful()) {
                            showSnackBarMessage(getString(R.string.successful_submit));
                            if (trackModel.realmId != null) {
                                realm.executeTransactionAsync(realm -> {
                                    RealmResults<TrackModel> result = realm.where(TrackModel.class).equalTo("realmId", trackModel.realmId).findAll();
                                    result.deleteAllFromRealm();
                                });
                            }
                            if (getActivity() instanceof SingleFragmentActivity) {
                                getActivity().setResult(RESULT_OK);
                                getActivity().finish();
                            } else {
                                setDrawerMenuChecked(R.id.home);
                                setDrawerMenuClicked(R.id.home);
                            }
                        } else {
                            if (value.code() == 401)
                                handleError(new Throwable(""), value.code(), getString(R.string.authentication_error));
                            else
                                handleError(new Throwable(""), value.code(), "");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        hideProgressDialog();
                        handleError(e, 0, getString(R.string.track_upload_fail)); //+ "\n" + new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(trackModel)
                    }

                    @Override
                    public void onComplete() {
                        hideProgressDialog();
                    }
                }));
    }

    private class TrackerPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<Fragment> registeredFragments = new SparseArray<>();

        TrackerPagerAdapter() {
            super(getChildFragmentManager());
            registeredFragments.put(0, new TrackersFragment());
            registeredFragments.put(1, new TrackMapFragment());
            registeredFragments.put(2, new TrackCountryFragment());
            registeredFragments.put(3, new AnimalFragment());
        }

        @Override
        public Fragment getItem(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tracker);
                case 1:
                    return getString(R.string.map);
                case 2:
                    return getString(R.string.country);
                case 3:
                    return getString(R.string.animals);
                default:
                    return null;
            }
        }

        Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override
        public int getCount() {
            return NUMBER_OF_FRAGMENTS;
        }
    }
}
