package com.gintechsystems.gincose;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * Created by joeginley on 5/25/16.
 */
public class DrawerActivity extends AppCompatActivity
{
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private String[] navArray = { "Alerts", "Settings", "Share", "Help" };

    @SuppressLint("InflateParams")
    @Override
    public void setContentView(final int layoutResID) {

        mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.drawer_layout, null);
        mDrawerList = (ListView)mDrawerLayout.findViewById(R.id.left_drawer);

        FrameLayout drawerFrameLayout = (FrameLayout)mDrawerLayout.findViewById(R.id.content_frame);

        // Removes annoying may produce NullPointerException.
        assert getSupportActionBar() != null;

        setupDrawer();
        addDrawerItems();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getLayoutInflater().inflate(layoutResID, drawerFrameLayout, true);

        super.setContentView(mDrawerLayout);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);

        MenuItem miSessionStart = menu.findItem(R.id.action_start_sensor);
        MenuItem miSessionStop = menu.findItem(R.id.action_stop_sensor);

        boolean sessionExists = GINcoseWrapper.getSharedInstance().getSensorSession(this);
        if (sessionExists) {
            miSessionStart.setVisible(false);
            miSessionStop.setVisible(true);
        }
        else {
            miSessionStop.setVisible(false);
            miSessionStart.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_calibrate) {

        }
        else if (id == R.id.action_start_sensor) {
            GINcoseWrapper.getSharedInstance().defaultTransmitter.startSession();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu();
                }
            }, 500);
        }
        else if (id == R.id.action_stop_sensor) {
            GINcoseWrapper.getSharedInstance().defaultTransmitter.stopSession();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu();
                }
            }, 500);
        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void addDrawerItems() {
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, navArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                executeNavItem(position);
            }
        });

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                assert getSupportActionBar() != null;

                Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
                if (fragment == null) {
                    getSupportActionBar().setTitle("GINcose");
                }
                else {
                    if (fragment.getTag().equals("settingsFrag")) {
                        getSupportActionBar().setTitle("Settings");
                    }
                    else if (fragment.getTag().equals("shareFrag")) {
                        getSupportActionBar().setTitle("Share");
                    }
                }
            }
        });
    }

    public void showNewFragment(Fragment newFrag, String fragTag) {
        getFragmentManager().popBackStack();
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, newFrag, fragTag).addToBackStack(null).commit();
    }

    private void executeNavItem(int position) {
        String navName = mAdapter.getItem(position);

        switch (navName) {
            case "Settings":
                showNewFragment(new SettingsActivity(), "settingsFrag");
                break;
            case "Share":
                showNewFragment(new ShareActivity(), "shareFrag");
                break;
        }

        setTitle(navArray[position]);

        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

}