package au.csiro.ozatlas.upload;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

import javax.inject.Inject;

import au.csiro.ozatlas.OzAtlasApplication;
import au.csiro.ozatlas.R;
import au.csiro.ozatlas.manager.AtlasManager;
import au.csiro.ozatlas.model.AddSight;
import au.csiro.ozatlas.model.ImageUploadResponse;
import au.csiro.ozatlas.model.SightingPhoto;
import au.csiro.ozatlas.rest.RestClient;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import retrofit2.Response;

import static au.csiro.ozatlas.manager.FileUtils.getMultipart;

/**
 * Created by sad038 on 8/5/17.
 */

public class UploadService extends IntentService {

    @Inject
    protected RestClient restClient;

    private BroadcastNotifier mBroadcaster;
    private Realm realm;
    private int imageUploadCount;
    //private RealmList<SightingPhoto> sightingPhotos = new RealmList<>();
    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UploadService() {
        super("AtlasUploadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        OzAtlasApplication.component().inject(this);
        if (AtlasManager.isNetworkAvailable(this)) {
            realm = Realm.getDefaultInstance();
            mBroadcaster = new BroadcastNotifier(this);

            ArrayList<Long> sightPrimarykeys = null;
            if (intent != null) {
                sightPrimarykeys = (ArrayList<Long>) intent.getSerializableExtra(getString(R.string.primary_keys_parameter));
            }

        /*
        get the primary keys of the models to upload
         */
            RealmResults<AddSight> result;
            if (sightPrimarykeys == null) {
                result = realm.where(AddSight.class).findAll();
            } else {
                RealmQuery<AddSight> query = realm.where(AddSight.class);
                query.equalTo("realmId", sightPrimarykeys.get(0));
                if (sightPrimarykeys.size() > 1) {
                    for (int i = 1; i < sightPrimarykeys.size(); i++) {
                        query.or().equalTo("realmId", sightPrimarykeys.get(i));
                    }
                }
                result = query.findAll();
            }

            Iterator<AddSight> sightIterator = result.iterator();
            while (sightIterator.hasNext()) {
                AddSight addSight = sightIterator.next();
                if (addSight.isValid() && !addSight.upLoading) {
                    realm.beginTransaction();
                    addSight.upLoading = true;
                    realm.commitTransaction();

                    if (addSight.outputs.get(0).data.sightingPhoto.size() > 0) {
                        imageUploadCount = 0;
                        //sightingPhotos = addSight.outputs.get(0).data.sightingPhoto;
                        uploadPhotos(addSight);
                    } else {
                        getGUID(addSight);
                    }

                    realm.beginTransaction();
                    addSight.deleteFromRealm();
                    realm.commitTransaction();

                    mBroadcaster.notifyDataChange();
                }
            }

            Log.d("", result.size() + "");
            if (realm != null)
                realm.close();
        }
    }

    private void uploadPhotos(final AddSight addSight) {
        if (imageUploadCount < addSight.outputs.get(0).data.sightingPhoto.size()) {
            mCompositeDisposable.add(restClient.getService().uploadPhoto(getMultipart(addSight.outputs.get(0).data.sightingPhoto.get(imageUploadCount).filePath))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<ImageUploadResponse>() {
                        @Override
                        public void onNext(ImageUploadResponse value) {
                            addSight.outputs.get(0).data.sightingPhoto.get(imageUploadCount).thumbnailUrl = value.files[0].thumbnail_url;
                            addSight.outputs.get(0).data.sightingPhoto.get(imageUploadCount).url = value.files[0].url;
                            addSight.outputs.get(0).data.sightingPhoto.get(imageUploadCount).contentType = value.files[0].contentType;
                            addSight.outputs.get(0).data.sightingPhoto.get(imageUploadCount).staged = true;
                            Log.d("", value.files[0].thumbnail_url);
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {
                            imageUploadCount++;
                            if (imageUploadCount < addSight.outputs.get(0).data.sightingPhoto.size())
                                uploadPhotos(addSight);
                            else
                                getGUID(addSight);
                        }
                    }));
        }
    }

    private void getGUID(final AddSight addSight) {
        mCompositeDisposable.add(restClient.getService().getGUID()
                .subscribeWith(new DisposableObserver<JsonObject>() {
                    @Override
                    public void onNext(JsonObject value) {
                        if (value.has("outputSpeciesId")) {
                            addSight.outputs.get(0).data.species.outputSpeciesId = value.getAsJsonPrimitive("outputSpeciesId").getAsString();
                            saveData(addSight);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                }));
    }

    private void saveData(AddSight addSight) {
        mCompositeDisposable.add(restClient.getService().postSightings(getString(R.string.project_activity_id), addSight)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Response<Void>>() {
                    @Override
                    public void onNext(Response<Void> value) {
                        Log.d("", "onNext");
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.dispose();
    }
}
