package fragments.offline_species;

import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import au.csiro.ozatlas.R;
import base.BaseMainActivityFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fragments.setting.SettingFragmentView;

/**
 * Created by sad038 on 19/9/17.
 */

public class OfflineSpeciesFragment extends BaseMainActivityFragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offline_species, container, false);
        setTitle(getString(R.string.setting));
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.availableSpeciesLayout)
    void availableSpeciesLayout(){
        getFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new AvailableSpeciesFragment()).addToBackStack(null).commit();
    }

    @OnClick(R.id.searchAndAddLayout)
    void searchAndAddLayout(){
        getFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new SearchAndAddFragment()).addToBackStack(null).commit();
    }

    @OnClick(R.id.downloadFromMapLayout)
    void downloadFromMapLayout(){

    }

    @OnClick(R.id.groupSpeciesLayout)
    void groupSpeciesLayout(){

    }
}