package us.lasociale.lasociale;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.logging.Logger;

/**
 * Created by tomas on 22-3-16.
 */
public class MainApp extends Application {

    private static Logger log = Logger.getLogger("us.lasociale.main");

    private static UserPresentListener userPresentListener = new UserPresentListener();

    static{ System.loadLibrary("opencv_java3"); }

    @Override
    public void onCreate() {
        super.onCreate();

        log.info("APP CREATE");

        initScreenOffListener();
        JodaTimeAndroid.init(this);

    }

    private void initScreenOffListener() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(userPresentListener, filter);

    }
}
