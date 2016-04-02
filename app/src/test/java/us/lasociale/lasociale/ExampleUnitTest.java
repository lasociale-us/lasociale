package us.lasociale.lasociale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;


import junit.framework.Assert;

import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.logging.Logger;



/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private static Logger log = Logger.getLogger("us.lasociale.bitmapscanner");


    @Test
    public void test_bitmapScanner() throws Exception {

        File f = new File("../test/cases");

        for (File file :f.listFiles()) {
            log.info("Scanning:" + file.getName());


            BitmapScanner scanner = new BitmapScanner(file.getPath());
            scanner.Scan();


        }

        //BitmapScanner scanner = new BitmapScanner()

        Assert.assertTrue(false);
    }
}