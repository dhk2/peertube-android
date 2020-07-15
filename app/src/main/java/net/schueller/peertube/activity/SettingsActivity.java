/*
 * Copyright 2018 Stefan Schüller <sschueller@techdroid.com>
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.schueller.peertube.activity;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.schueller.peertube.R;

import java.util.List;

public class SettingsActivity extends CommonActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        // Attaching the layout to the toolbar object
        Toolbar toolbar = findViewById(R.id.tool_bar_settings);
        // Setting toolbar as the ActionBar with setSupportActionBar() call
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // close this activity as oppose to navigating up
        return false;
    }
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            if (sharedPref.getBoolean("pref_torrent_seed",false)){
                findPreference("pref_torrent_settings").setVisible(true);
            } else {
                findPreference("pref_torrent_settings").setVisible(false);
            }
            findPreference("pref_torrent_seed").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Boolean enabled=(Boolean)newValue;
                    if (enabled){
                        findPreference("pref_torrent_settings").setVisible(true);
                    } else {
                        findPreference("pref_torrent_settings").setVisible(false);
                    }
                    return (boolean) preference.isEnabled();
                }
            });

/*
            //enable app based torrent seeding options if they are installed
            PackageManager pm = getContext().getPackageManager();
            List<ApplicationInfo> appList = pm.getInstalledApplications(0);

            for (int i = 0; i < appList.size(); i++) {
                if (appList.get(i).packageName.equals("org.proninyaroslav.libretorrent")) {
                    System.out.println(appList.get(i).packageName);
                    findPreference("pref_torrent_seed_libre_interactive").setVisible(true);
                }
                if (appList.get(i).packageName.equals("com.biglybt.android.client")) {
                    System.out.println(appList.get(i).packageName);
                    findPreference("pref_torrent_seed_bigly_interactive").setVisible(true);
                }
            }

            for (int i = 0; i < appList.size(); i++) {

            }

*/
        }
    }
}