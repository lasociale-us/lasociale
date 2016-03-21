package us.lasociale.lasociale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.logging.Logger;

/**
 * Created by stefkolman on 15/03/16.
 */
public class UserPresentListener extends BroadcastReceiver {


    private Logger log = Logger.getLogger("us.lasociale.app");

    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            // do whatever you need to do here
            log.info("Screen OFF");
            ActivityStorage.Write(context, false);
        }
        else {
            log.info("Screen ON");
            ActivityStorage.Write(context, true);
        }


    }
}
