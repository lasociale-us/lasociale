package us.lasociale.lasociale;

import android.os.AsyncTask;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Created by tomas on 22-3-16.
 */
public class Uploader extends AsyncTask<String, Void, String> {

    private static Logger log = Logger.getLogger("us.lasociale.uploader");
    private static String SERVER =   "http://185.77.131.115:3333/";

    protected String doInBackground(String... params) {

        StringBuilder sb = new StringBuilder();

        String doc = params[0];
        long seconds = Long.parseLong(params[1]);
        String http = SERVER + doc;


        log.info("SENDING TO" + http);

        HttpURLConnection urlConnection=null;
        try {
            URL url = new URL(http);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("PUT");
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type","application/json");

            urlConnection.setRequestProperty("Host", "android.lasociale.us");
            urlConnection.connect();

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("active", seconds);


            /*
            DataOutputStream printout = new DataOutputStream(urlConnection.getOutputStream ());
            printout.write(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
            printout.flush();
            printout.close();
*/
            OutputStreamWriter out = new   OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonParam.toString());
            out.close();

            int HttpResult =urlConnection.getResponseCode();
            if(HttpResult ==HttpURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream(),"utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                System.out.println(""+sb.toString());

            }else{
                System.out.println(urlConnection.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }finally{
            if(urlConnection!=null)
                urlConnection.disconnect();
        }

        return "";
    }

    protected void onProgressUpdate(Void... progress) {

    }

    protected void onPostExecute(String result) {

    }
}
