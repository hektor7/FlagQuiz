// MainActivity.java
// Hosts the QuizFragment on a phone and both the 
// QuizFragment and SettingsFragment on a tablet
package com.hektor7.flagquiz;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {
    // keys for reading data from SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; // used to force portrait mode
    private boolean preferencesChanged = true; // did preferences change?

    private QuizFragment quizFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setupPreferences();
        this.setupScreen();

    }

    // called after onCreate completes execution
    @Override
    protected void onStart() {
        super.onStart();

        if (this.preferencesChanged) {
            this.reconfigureQuizFragment();
        }
    }

    // show menu if app is running on a phone or a portrait-oriented tablet
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // display the app's menu only in portrait orientation
        if (this.screenIsPortrait()) {
            getMenuInflater().inflate(R.menu.main, menu); // inflate the menu
            return true;
        } else {
            return false;
        }
    }

    // displays SettingsActivity when running on a phone
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }

    // listener for changes to the app's SharedPreferences
    private OnSharedPreferenceChangeListener preferenceChangeListener =
            new OnSharedPreferenceChangeListener() {
                //TODO: Refactor
                // called when the user changes the app's preferences
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    preferencesChanged = true; // user changed app settings

                    QuizFragment quizFragment = (QuizFragment)
                            getFragmentManager().findFragmentById(R.id.quizFragment);

                    if (key.equals(CHOICES)) // # of choices to display changed
                    {
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    } else if (key.equals(REGIONS)) // regions to include changed
                    {
                        Set<String> regions =
                                sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && regions.size() > 0) {
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        } else // must select one region--set North America as default
                        {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            regions.add(
                                    getResources().getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.commit();
                            Toast.makeText(MainActivity.this,
                                    R.string.default_region_message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(MainActivity.this,
                            R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
                } // end method onSharedPreferenceChanged
            }; // end anonymous inner class

    /**
     * Sets screen configuration.
     */
    private void setupScreen() {
        // determine screen size
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // if device is a tablet, set phoneDevice to false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            this.phoneDevice = false; // not a phone-sized device
        }

        // if running on phone-sized device, allow only portrait orientation
        if (this.phoneDevice) {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * Configure preferences
     */
    private void setupPreferences() {
        // set default values in SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // register listener for SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(
                        this.preferenceChangeListener);
    }

    /**
     * Reconfigure the quiz fragment, reset and show it.
     */
    private void reconfigureQuizFragment() {
        this.guessRowsChanges();
        this.regionsChanges();
        this.getQuizFragment().resetQuiz();
        this.preferencesChanged = false;
    }

    /**
     * Reconfigure regions
     */
    private void regionsChanges() {
        this.getQuizFragment().updateRegions(
                PreferenceManager.getDefaultSharedPreferences(this));
    }

    /**
     * Reconfigure rows
     */
    private void guessRowsChanges() {
        this.getQuizFragment().updateGuessRows(
                        PreferenceManager.getDefaultSharedPreferences(this));
    }

    /**
     * Determines if the screen is Portrait or not.
     *
     * @return true if is portrait.
     */
    private boolean screenIsPortrait() {
        boolean isPortrait = false;
        // get the default Display object representing the screen
        Display display = ((WindowManager)
                getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Point screenSize = new Point(); // used to store screen size
        display.getRealSize(screenSize); // store size in screenSize

        if (screenSize.x < screenSize.y) {
            isPortrait = true;
        }

        return isPortrait;
    }

    private QuizFragment getQuizFragment() {
        if (this.quizFragment == null) {
            this.quizFragment = (QuizFragment) getFragmentManager().findFragmentById(R.id.quizFragment);
        }
        return this.quizFragment;
    }

}


