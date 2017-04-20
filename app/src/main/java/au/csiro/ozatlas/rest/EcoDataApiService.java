package au.csiro.ozatlas.rest;

import com.google.gson.JsonObject;

import au.csiro.ozatlas.model.SightList;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Created by sad038 on 5/4/17.
 */

public interface EcoDataApiService {
    @GET("user/getKey")
    Observable<JsonObject> login(@Header("userName") String username, @Header("password") String password);

    @GET("ws/record/listRecordsForDataResourceId")
    Observable<SightList> getSightings(@Query("id") String id, @Query("max") Integer max, @Query("offset") Integer offset, @Query("sort") String sort, @Query("status") String status);
}