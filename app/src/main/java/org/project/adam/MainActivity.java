package org.project.adam;
/**
 * Adam project
 * Copyright (C) 2017 Orange
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import android.annotation.SuppressLint;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.project.adam.alert.AlertActivity_;
import org.project.adam.persistence.Diet;
import org.project.adam.ui.dashboard.DashboardFragment_;
import org.project.adam.ui.data.DataFragment_;
import org.project.adam.ui.diet.DietListFragment_;
import org.project.adam.ui.preferences.PrefActivity_;

import static org.project.adam.alert.AlertActivity.ACTION_STOP_ALARM;

@SuppressLint("Registered")
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity extends BaseActivity {

    static final int DEFAULT_DIET_ID = -1;

    @ViewById(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;

    @Pref
    protected Preferences_ prefs;

    private int selectedMenuId = -1;

    @AfterViews
    void setUpTabs() {
        bottomNavigationView.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    if (selectedMenuId != item.getItemId()){
                        selectMenu(item.getItemId(), false);
                    }
                    return true;
                }
            });
        showWelcomeSection();

        //sometimes the alarm cancellation can fail and there is no way to stop the alarm sound. Just to be sure
        sendBroadcast(AlertActivity_.intent(this).action(ACTION_STOP_ALARM).get());
    }

    @UiThread(propagation = UiThread.Propagation.REUSE)
    protected void selectMenu(int menuId, boolean changeMenuSelected) {
        switch (menuId) {
            case R.id.tab_dashboard:
                showDashBoard();
                break;
            case R.id.tab_data:
                showData();
                break;
            case R.id.tab_diet:
                showDiets();
                break;
            default:
                return;
        }
        if(changeMenuSelected){
            bottomNavigationView.getMenu().findItem(selectedMenuId).setChecked(true);
        }
    }

    private void showWelcomeSection() {
        final Integer currentDietId = prefs.currentDietId().get();
        if(currentDietId == DEFAULT_DIET_ID){
            selectMenu(R.id.tab_diet, true);
        } else {
            checkCurrentDietIdExists(currentDietId);
        }
    }

    @Background
    protected void checkCurrentDietIdExists(int currentDietId){
        Diet diet = AppDatabase.getDatabase(this).dietDao().findSync(currentDietId);
        if (diet == null){
            selectMenu(R.id.tab_diet, true);
        } else {
            selectMenu(R.id.tab_dashboard, true);
        }
    }

    private void showDashBoard() {
        selectedMenuId = R.id.tab_dashboard;
        showFragment(DashboardFragment_.builder().build());
    }

    private void showData() {
        selectedMenuId = R.id.tab_data;
        showFragment(DataFragment_.builder().build());
    }

    private void showDiets() {
        selectedMenuId = R.id.tab_diet;
        showFragment(DietListFragment_.builder().build());
    }


    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow();
    }

    @OptionsItem(R.id.action_settings)
    public void menuSettings() {
        PrefActivity_.intent(this).start();
    }

}
