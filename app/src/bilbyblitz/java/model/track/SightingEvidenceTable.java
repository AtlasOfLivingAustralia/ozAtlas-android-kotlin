package model.track;

import org.parceler.Parcel;
import org.parceler.ParcelPropertyConverter;

import java.io.Serializable;

import au.csiro.ozatlas.manager.RealmListParcelConverter;
import au.csiro.ozatlas.model.SearchSpecies;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.annotations.RealmClass;

/**
 * Created by sad038 on 28/11/17.
 */

@Parcel
@RealmClass
public class SightingEvidenceTable extends RealmObject {
    public String typeOfSign;

    public Double observationLongitude;

    @ParcelPropertyConverter(RealmListParcelConverter.class)
    public RealmList<ImageOfSign> imageOfSign;

    public Species species;

    public Double observationLatitude;

    public String evidenceAgeClass;

    //view
    public String mPhotoPath;
}