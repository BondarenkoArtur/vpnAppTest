package com.inverita.vpnapptask;

import android.app.Activity;
import android.os.Bundle;

/**
 * This main activity is started from launcher.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                .add(R.id.container, new MainFragment())
                .commit();
        }
    }
}
