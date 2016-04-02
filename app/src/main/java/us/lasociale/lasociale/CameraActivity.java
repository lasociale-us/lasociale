package us.lasociale.lasociale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static Logger log = Logger.getLogger("us.lasociale.cameraactivity");


    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat lastFrame = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        log.info("called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setOnClickListener(this);


    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    log.info("OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        synchronized (this) {
            lastFrame = inputFrame.rgba();
        }
        //log.info("Fount contours:" + contours.size());
        BitmapScanner scanner = new BitmapScanner(lastFrame);

        String hash = scanner.Scan();
        if (hash != null)
        {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result",hash);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }


        return scanner.Display;
    }

    @Override
    public void onClick(View v) {
        Bitmap bmp;
        log.info("ONCLICK");
        synchronized (this) {
            bmp = Bitmap.createBitmap(lastFrame.width(), lastFrame.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(lastFrame,bmp);
        }

        sendTestCase(bmp);

    }

    private void sendTestCase(Bitmap bmp)
    {
        Uploader.UploadParams params = new Uploader.UploadParams();

        params.bitmapData = bmp;
        params.jsonData = null;
        params.document = "_testcase";
        params.imageReceiver = null;

        new Uploader().execute(params);
    }
}
