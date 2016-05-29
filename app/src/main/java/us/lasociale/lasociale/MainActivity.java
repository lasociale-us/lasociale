package us.lasociale.lasociale;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static Logger log = Logger.getLogger("us.lasociale.main");
    private static long MILLISECONDS_TILL_UPDATE = 1000 * 5;


    Handler viewHandler;
    Runnable updateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MainActivity act = this;
        // Defines a Handler object that's attached to the UI thread
        viewHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message inputMessage) {
                // Gets the image task from the incoming Message object.
               Bitmap img = (Bitmap) inputMessage.obj;
                ImageView view = (ImageView)findViewById(R.id.imageView);
                view.setImageBitmap(img);
                log.info("Received image: " + img.getWidth() + "x" + img.getHeight());

            }
        };


    }

    private void loadImage() {
        ActivityStorage.Write(this, true, viewHandler);



    }

    private void StartCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, 1);
        //startActivity(intent);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_camera:
                StartCamera();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImage();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                String hash=data.getStringExtra("result");

                String msg = "";
                if (IdentityManager.IsHash(hash))
                    msg = "Hash scanned: " + hash;
                else if (IdentityManager.IsNonce(hash)) {
                    msg = "Nonce scanned: " + hash;
                    Send(hash);
                }
                else
                    msg = "Not understood: " +hash;

                log.info(msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    public void Send(String nonce) {

        Uploader.UploadParams params = new Uploader.UploadParams();

        //Create JSONObject here
        try {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("nonce", nonce);
            jsonParam.put("hash", IdentityManager.GetPublicKey(this));
            params.jsonData = jsonParam.toString(2);
        }
        catch(JSONException ex) {
            log.severe("Can't render json");
            ex.printStackTrace();
        }
        params.bitmapData = null;
        params.document = "_link";

        new Uploader().execute(params);
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewHandler.removeCallbacks(updateView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        log.info("**** ADDING MENU***");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



}
