package au.csiro.ozatlas.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import au.csiro.ozatlas.R;
import au.csiro.ozatlas.base.BaseActivity;
import au.csiro.ozatlas.base.FloatingActionButtonListener;
import au.csiro.ozatlas.fragments.AddSightingFragment;
import au.csiro.ozatlas.fragments.SightingListFragment;
import au.csiro.ozatlas.fragments.WebViewFragment;
import au.csiro.ozatlas.manager.AtlasManager;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, FloatingActionButtonListener {

    private NavigationView navigationView;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new AddSightingFragment()).commit();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        updateNavigationHeader();

        if (AtlasManager.isTesting) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new SightingListFragment()).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new SightingListFragment()).commit();
        }
    }

    private void updateNavigationHeader() {
        //((TextView) navigationView.getHeaderView(0).findViewById(R.id.name)).setText(user.getDisplayName());
        ((TextView) navigationView.getHeaderView(0).findViewById(R.id.email)).setText(sharedPreferences.getUsername());
        //((CircularImageView) navigationView.getHeaderView(0).findViewById(R.id.imageView)).setImageURL(user.getPhotoUrl().toString());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            sharedPreferences.writeAuthKey(null);
            launchLoginActivity();
        } else if (id == R.id.nav_add) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new AddSightingFragment()).commit();
        } else if (id == R.id.nav_all_sighting) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, new SightingListFragment()).commit();
        } else if (id == R.id.nav_my_sighting) {
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.myview_parameter), "myrecords");
            Fragment fragment = new SightingListFragment();
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, fragment).commit();
        } else if (id == R.id.nav_about) {
            commitWebViewFragment(getString(R.string.about_us_url));
        } else if (id == R.id.nav_contact) {
            commitWebViewFragment(getString(R.string.contact_us_url));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void commitWebViewFragment(String url) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.url_parameter), url);
        Fragment fragment = new WebViewFragment();
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentHolder, fragment).commit();
    }

    @Override
    public void hideFloatingButton() {
        fab.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showFloatingButton() {
        fab.setVisibility(View.VISIBLE);
    }
}
