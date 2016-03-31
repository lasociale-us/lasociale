package us.lasociale.lasociale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Created by tomas on 22-3-16.
 */
public class Uploader extends AsyncTask<Uploader.UploadParams, Void, byte[]> {

    private static Logger log = Logger.getLogger("us.lasociale.uploader");
    private static String SERVER =   "http://185.77.131.115:3333/";

    public static class UploadParams {
        public String document;
        public String jsonData;
        public Bitmap bitmapData;
        public Handler imageReceiver;

    };

    protected byte[] doInBackground(UploadParams... params) {

        StringBuilder sb = new StringBuilder();

        UploadParams param = params[0];
        String http = SERVER + param.document;


        log.info("SENDING TO:" + http);

        HttpURLConnection urlConnection=null;
        try {
            URL url = new URL(http);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("PUT");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            if (param.imageReceiver != null)
                urlConnection.setRequestProperty("Accept","image/png");
            else
                urlConnection.setRequestProperty("Accept","application/json");

            urlConnection.setRequestProperty("Host", "android.lasociale.us");
            urlConnection.connect();




            /*
            DataOutputStream printout = new DataOutputStream(urlConnection.getOutputStream ());
            printout.write(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            printout.flush();
            printout.close();
*/
            if (param.jsonData != null) {
                urlConnection.setRequestProperty("Content-Type", "application/json");

                OutputStreamWriter out = new   OutputStreamWriter(urlConnection.getOutputStream());

                out.write(param.jsonData);
                out.close();
            }
            else if (param.bitmapData != null)
            {
                urlConnection.setRequestProperty("Content-Type", "image/png");

                OutputStream sOutput = urlConnection.getOutputStream();
                param.bitmapData.compress(Bitmap.CompressFormat.PNG, 100, sOutput);
                sOutput.close();

            }



            int HttpResult =urlConnection.getResponseCode();
            log.info("HTTP RESONPOSE CODE=" + HttpResult);
            if(HttpResult ==HttpURLConnection.HTTP_OK || HttpResult == HttpURLConnection.HTTP_CREATED){

                if (param.imageReceiver == null)
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            urlConnection.getInputStream(),"utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        log.info("Reading:" + line);
                    }
                    br.close();
                }
                else
                {
                    byte[] buffer = new byte[1024*1024];
                    int offset = 0;
                    int read = 0;
                    InputStream stream = urlConnection.getInputStream();
                    while((read = stream.read(buffer, offset, buffer.length - offset)) > -1)
                    {
                        offset+=read;
                    }
                    log.info("Sending bitmap");
                    Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, offset);
                    Message msg = param.imageReceiver.obtainMessage(0);
                    msg.obj = bmp;
                    msg.sendToTarget();

                }




            } else {
                System.out.println(urlConnection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(urlConnection!=null)
                urlConnection.disconnect();
        }

        return new byte[] {0};
    }

    protected void onProgressUpdate(Void... progress) {

    }

    protected void onPostExecute(byte[] result) {

    }
}
