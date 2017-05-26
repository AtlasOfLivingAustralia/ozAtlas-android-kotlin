package au.csiro.ozatlas.base;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

import java.net.UnknownHostException;

import javax.inject.Inject;

import au.csiro.ozatlas.OzAtlasApplication;
import au.csiro.ozatlas.R;
import au.csiro.ozatlas.activity.LoginActivity;
import au.csiro.ozatlas.activity.SingleFragmentActivity;
import au.csiro.ozatlas.manager.AtlasSharedPreferenceManager;
import au.csiro.ozatlas.rest.RestClient;
import io.reactivex.disposables.CompositeDisposable;
import io.realm.Realm;

/**
 * Created by sad038 on 5/4/17.
 */

public class BaseActivity extends AppCompatActivity implements BaseActivityFragmentListener, RestClientListener {
    @Inject
    protected AtlasSharedPreferenceManager sharedPreferences;

    @Inject
    protected RestClient restClient;

    private ProgressDialog mProgressDialog;
    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    protected Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initializing dagger
        OzAtlasApplication.component().inject(this);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        //checking the authkey from sharedpreference. Launch LoginActivity in case there is not key
        if (!(this instanceof LoginActivity) && sharedPreferences.getAuthKey().equals("")) {
            launchLoginActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideProgressDialog();
    }

    /**
     * handling home button press
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * show the spinning dialog
     */
    @Override
    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog = new ProgressDialog(this, R.style.OSSProgressBarTheme);
            mProgressDialog.getWindow().setDimAmount(0.2f);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
        }

        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    /**
     * hide the spinning dialog
     */
    @Override
    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    /**
     * @param editText to check the empty content
     * @return
     */
    @Override
    public boolean validate(EditText editText) {
        return editText.getText().toString().length() > 0;
    }

    /**
     * @param coordinatorLayout
     * @param string            message to show
     */
    public void showSnackBarMessage(CoordinatorLayout coordinatorLayout, String string) {
        Snackbar.make(coordinatorLayout, string, Snackbar.LENGTH_LONG).show();
    }

    /**
     * launch Login Activity from anywhere
     */
    @Override
    public void launchLoginActivity() {
        sharedPreferences.writeAuthKey("");
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.deleteAll();
            }
        });
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * @param url   for the webview fragment
     * @param title activity title
     */
    @Override
    public void startWebViewActivity(String url, String title) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.url_parameter), url);
        bundle.putString(getString(R.string.title_parameter), title);
        bundle.putSerializable(getString(R.string.fragment_type_parameter), SingleFragmentActivity.FragmentType.WEB_FRAGMENT);
        Intent intent = new Intent(this, SingleFragmentActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * show a toast message
     *
     * @param str
     */
    @Override
    public void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * handle the error response from a HTTP call
     *
     * @param coordinatorLayout
     * @param e
     * @param code
     * @param message
     */
    @Override
    public void handleError(CoordinatorLayout coordinatorLayout, Throwable e, int code, String message) {
        if (e instanceof UnknownHostException) {
            showSnackBarMessage(coordinatorLayout, getString(R.string.not_internet_title));
        } else if (e instanceof HttpException && ((HttpException) e).code() == code) {
            showSnackBarMessage(coordinatorLayout, message);
        } else {
            showSnackBarMessage(coordinatorLayout, getString(R.string.generic_error));
        }
    }

    /**
     * disposing RxDisposable.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCompositeDisposable != null)
            mCompositeDisposable.dispose();
        if(realm!=null)
            realm.close();
    }

    /**
     * restClient
     * mainly for the fragments
     *
     * @return
     */
    @Override
    public RestClient getRestClient() {
        return restClient;
    }
}