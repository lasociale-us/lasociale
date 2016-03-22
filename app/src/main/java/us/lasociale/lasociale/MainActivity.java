package us.lasociale.lasociale;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Handler;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static Logger log = Logger.getLogger("us.lasociale.main");
    private static long MILLISECONDS_TILL_UPDATE = 1000 * 5;


    Handler viewHandler = new Handler();
    Runnable updateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        MainActivity act = this;
        updateView = new Runnable(){

            @Override
            public void run(){
                //log.info("Updating VIEW");

                findViewById(R.id.pieview).invalidate();
                viewHandler.postDelayed(updateView, MILLISECONDS_TILL_UPDATE);
            }
        };

        updateView.run();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewHandler.removeCallbacks(updateView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
