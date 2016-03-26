package us.lasociale.lasociale;

import android.content.Context;
import android.content.SharedPreferences;


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


    private static DateTime roundDateDown(final DateTime dateTime) {
        return roundDate(dateTime.minusHours(1));
    }

    private static DateTime roundDate(final DateTime dateTime) {

        return new DateTime(
                dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
                dateTime.getHourOfDay(), 0, 0, dateTime.getZone())
                .plusHours(1);


    }

    public static void Send(String doc, long seconds, long elapsed) {

        new Uploader().execute(doc, Long.toString(seconds), Long.toString(elapsed));
    }


    public static void Reset(boolean isPresent) {
    }


    public static void Log(Context context, String prefix) {
        DateTime now = new DateTime(DateTimeZone.UTC);
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        long recorded              = settings.getLong(S_RECORDED_SECS, 0);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);

        log.info(prefix + " " + lastStamp.toString() + "," + recorded + " secs, " + Boolean.toString(lastActive));
    }


    public static void Write(Context context, boolean isPresent) {

        DateTime now = new DateTime().withZone(DateTimeZone.UTC);


        String s = now.toString() + "," + (isPresent ? "ON" : "OFF") + "\n";


        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        DateTime lastStamp         = new DateTime(settings.getString(S_LASTSTAMP, now.toString())).withZone(DateTimeZone.UTC);
        long recorded              = settings.getLong(S_RECORDED_SECS, 0);
        boolean lastActive         = settings.getBoolean(S_LASTACTIVE, false);

        DateTime endOfRun = roundDate(lastStamp);

        //log.info("NOW=" + now.toString() + ",  LASTSTAMP=" + lastStamp.toString() + ", ENDOFRUN=" + endOfRun.toString());

        while (now.isAfter(endOfRun)) {
            if (lastActive) {
                recorded += new Duration(lastStamp, endOfRun).getStandardSeconds();
            }

            String user = IdentityManager.GetPublicKey(context);
            String doc = user + "-" + endOfRun.toString().substring(0,13);
            Send(doc, recorded, 3600);
            Log(context, "SENDING: ");
            log.info("USER=" + user);

            recorded =  0;
            lastStamp = endOfRun;
            endOfRun = roundDate(lastStamp);


            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(S_RECORDED_SECS, recorded);
            editor.putBoolean(S_LASTACTIVE, isPresent);
            editor.putString(S_LASTSTAMP, now.toString());
            editor.commit();
        }
        if (lastActive == isPresent)
            return;

        if (lastActive) {
            recorded += new Duration(lastStamp, now).getStandardSeconds();
        }

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

        Write(context, lastActive);

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
        /*
        Date now = new Date();
        Date last = null;
        boolean isActive = false;
        long totalSecs = 0;
        try {
            BufferedReader rdr = new BufferedReader(new InputStreamReader(context.openFileInput(FILENAME)));
            String s = "";
            while((s = rdr.readLine()) != null)
            {
                log.info("Read: " + s);
                String[] fields = s.split(",");
                Date d = new Date(Long.parseLong(fields[0]));
                if (last == null) {
                    last = d;
                    isActive = fields[1].equals("ON");
                }
                else if (fields[1].equals("ON"))
                {
                    isActive = true;
                }
                else {
                    if (isActive) {
                        // count activity seconds
                        long diff = d.getTime() - last.getTime();
                        log.info("Found active:" + diff);
                        totalSecs += (diff/1000);
                    }

                    isActive = false;
                }
                last = d;

            }
            rdr.close();
            if (last != null && isActive) {
                // count activity seconds
                long diff = now.getTime() - last.getTime();
                log.info("Found active:" + diff);
                totalSecs += (diff/1000);

            }
            float total = (float)totalSecs / (float)(60*60*24);
            log.info("Total active" + total);
            return total;
        }

        catch (IOException ex) {
            log.severe("Failed to read log " + ex.toString());
            // ignore; can't do much about it
        }
        return 0;
        */
    }

}
