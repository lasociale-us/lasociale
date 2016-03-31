package us.lasociale.lasociale;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;


import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.logging.Logger;


/**
 * Created by Tomas on 15/03/16.
 */
public class ActivityStorage {


    private static String PREF_NAME = "lasociale.3";

    private static String S_RECORDED_SECS   = "recorded-secs";
    private static String S_LASTSTAMP       = "last-stamp";
    private static String S_LASTACTIVE      = "last-active";

    private static Logger log = Logger.getLogger("us.lasociale.logger");

    private static DateTime lastSend = null;

    private static DateTime roundDateDown(final DateTime dateTime) {
        return roundDate(dateTime.minusDays(1));
    }

    private static DateTime roundDate(final DateTime dateTime) {

        return new DateTime(
                dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
                0, 0, 0, dateTime.getZone())
                .plusDays(1);


    }

    public static void Send(String doc, long seconds, long elapsed, Handler imageReceiver) {

        Uploader.UploadParams params = new Uploader.UploadParams();
        params.imageReceiver = imageReceiver;

        //Create JSONObject here
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("lasociale", elapsed - seconds);
            jsonParam.put("elapsed", elapsed);
            params.jsonData = jsonParam.toString(2);
        }
        catch(JSONException ex) {
            log.severe("Can't render json");
            ex.printStackTrace();
        }
        params.bitmapData = null;
        params.document = doc;

        new Uploader().execute(params);
    }


    public static void Log(Context context, String prefix) {
        DateTime now = new DateTime(DateTimeZone.UTC);
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        long recorded              = settings.getLong(S_RECORDED_SECS, 0);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);

        log.info(prefix + " " + lastStamp.toString() + "," + recorded + " secs, " + Boolean.toString(lastActive));
    }



    public static void Write(Context context, boolean isPresent, Handler imageReceiver) {

        DateTime now = new DateTime().withZone(DateTimeZone.UTC);

        String s = now.toString() + "," + (isPresent ? "ON" : "OFF") + "\n";


        // get last saved stuff
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        long recorded              = settings.getLong(S_RECORDED_SECS, 0);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);

        DateTime endOfRun = roundDate(lastStamp);

        //log.info("NOW=" + now.toString() + ",  LASTSTAMP=" + lastStamp.toString() + ", ENDOFRUN=" + endOfRun.toString());

        while (now.isAfter(endOfRun)) {

            // active till end of day?
            if (lastActive) {
                recorded += new Duration(lastStamp, endOfRun).getStandardSeconds();
            }

            String user = IdentityManager.GetPublicKey(context);
            String doc = user + "-" + endOfRun.toString().substring(0,10);
            Send(doc, recorded, 3600 * 24, null);
            Log(context, "SENDING: ");
            log.info("USER=" + user);

            recorded =  0;
            lastStamp = endOfRun;
            endOfRun = roundDate(lastStamp);
        }

        // update until now
        if (lastActive) {
            recorded += new Duration(lastStamp, now).getStandardSeconds();
        }


        // see if we need to update the image
        if (imageReceiver != null) {
            log.info("Downloading image");
            String user = IdentityManager.GetPublicKey(context);
            String doc = user + "-" + endOfRun.toString().substring(0,10);
            DateTime startOfRun = roundDateDown(now);
            Send(doc, recorded, new Duration(startOfRun, now).getStandardSeconds(), imageReceiver);

        }

        // save in prefs
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(S_RECORDED_SECS, recorded);
        editor.putBoolean(S_LASTACTIVE, isPresent);
        editor.putString(S_LASTSTAMP, now.toString());
        editor.commit();
        Log(context, "SAVING: ");


    }


    public static void Rewrite(Context context)
    {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);

        Write(context, lastActive, null);

    }

    public static String ReadActivityState(Context context) {

        Rewrite(context);

        DateTime now = new DateTime().withZone(DateTimeZone.UTC);
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);
        long recorded = settings.getLong(S_RECORDED_SECS, 0);

        if (lastActive) {
            recorded += new Duration(lastStamp, now).getStandardSeconds();
        }


        DateTime endOfRun = roundDateDown(now);
        long elapsed = new Duration(endOfRun, now).getStandardSeconds();

        return Long.toString(recorded) + "secs of " + elapsed + " secs";

    }

    public static float ReadActivity(Context context) {

        Rewrite(context);

        DateTime now = new DateTime().withZone(DateTimeZone.UTC);
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);
        long recorded = settings.getLong(S_RECORDED_SECS, 0);

        if (lastActive) {
            recorded += new Duration(lastStamp, now).getStandardSeconds();
        }


        DateTime endOfRun = roundDateDown(now);
        long elapsed = new Duration(endOfRun, now).getStandardSeconds();


        return (float)recorded / (float)elapsed;

    }

}
