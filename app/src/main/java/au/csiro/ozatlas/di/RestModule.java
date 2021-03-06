package au.csiro.ozatlas.di;

import javax.inject.Singleton;

import au.csiro.ozatlas.rest.RestClient;
import dagger.Module;
import dagger.Provides;

/**
 * Created by sad038 on 5/4/17.
 */

/**
 * Injecting Rest Client
 */
@Module
public class RestModule {
    private String url;

    public RestModule(String url) {
        this.url = url;
    }

    @Singleton
    @Provides
    RestClient getRestClient() {
        return new RestClient(url);
    }
}
