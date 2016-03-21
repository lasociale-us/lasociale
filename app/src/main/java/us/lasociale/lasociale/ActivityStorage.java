package us.lasociale.lasociale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Created by stefkolman on 15/03/16.
 */
public class ActivityStorage {

    private static String FILENAME = "activity2.log";
    private static Logger log = Logger.getLogger("us.lasociale.logger");

    public static void Write(Context context, boolean isPresent) {
        String s = Long.toString(new Date().getTime()) + "," + (isPresent ? "ON" : "OFF") + "\n";


        log.info(s);
        try {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_APPEND);
            fos.write(s.getBytes());
            fos.close();
        }
        catch (IOException ex) {
            log.severe("Failed to write log " + ex.toString());
            // ignore; can't do much about it
        }

    }

    public static float ReadActivity(Context context) {
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
    }

}
