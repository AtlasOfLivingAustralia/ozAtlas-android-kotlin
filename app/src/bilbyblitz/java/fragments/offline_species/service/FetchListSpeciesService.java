package fragments.offline_species.service;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import au.csiro.ozatlas.R;
import au.csiro.ozatlas.base.BaseIntentService;
import au.csiro.ozatlas.model.SearchSpecies;
import au.csiro.ozatlas.rest.NetworkClient;
import io.reactivex.observers.DisposableObserver;
import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import rest.SpeciesListApiService;

import static model.EventBusPosts.FETCH_SPECIES_LIST;

public class FetchListSpeciesService extends BaseIntentService {

    private Realm realm;
    private SpeciesListApiService speciesListApiService;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public FetchListSpeciesService() {
        super("FetchAndSaveSpeciesService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        speciesListApiService = new NetworkClient(getString(R.string.species_list_url)).getRetrofit().create(SpeciesListApiService.class);

        realm = Realm.getDefaultInstance();
        fetchSpecies();
    }

    private void fetchSpecies() {
        mCompositeDisposable.add(speciesListApiService.getSpeciesList("dr8016")
                .subscribeWith(new DisposableObserver<List<SearchSpecies>>() {
                    @Override
                    public void onNext(List<SearchSpecies> value) {
                        realm.beginTransaction();
                        realm.delete(SearchSpecies.class);
                        realm.commitTransaction();
                        for (SearchSpecies searchSpecies : value) {
                            saveData(searchSpecies);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        EventBus.getDefault().post(FETCH_SPECIES_LIST);
                        Log.d(TAG, "onComplete");
                    }
                }));
    }

    private void saveData(final SearchSpecies species) {
        realm.executeTransactionAsync(realm -> {
            try {
                species.realmId = species.guid + species.id;
                Log.d(TAG + "ID", species.guid + "   " + species.id + "   " + species.realmId);
                realm.copyToRealm(species);
            } catch (RealmPrimaryKeyConstraintException exception) {
                Log.d(TAG, getString(R.string.duplicate_entry));
            }
        });
    }
}
