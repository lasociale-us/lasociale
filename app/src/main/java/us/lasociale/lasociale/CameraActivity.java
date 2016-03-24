package us.lasociale.lasociale;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.logging.Logger;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static Logger log = Logger.getLogger("us.lasociale.cameraactivity");


    private CameraBridgeViewBase mOpenCvCameraView;

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


    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    log.info("OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
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

        Mat mat = inputFrame.rgba();
        Mat roiTmp = mat.clone();

        Mat out1 = new Mat();
        Mat out2 = new Mat();

        Imgproc.cvtColor(roiTmp, out1, Imgproc.COLOR_RGBA2BGR,0);
        Imgproc.cvtColor(out1, out1, Imgproc.COLOR_BGR2HSV, 0);
        Core.inRange(out1, new Scalar(88, 180, 180), new Scalar(92, 255, 255), out1);
        //Core.inRange(roiTmp, new Scalar(170, 70, 30), new Scalar(180, 255, 255), out2);

        //Mat out3 = out1 | out2;


        Imgproc.cvtColor(out1, mat, Imgproc.COLOR_GRAY2RGBA, 0);

        return mat;
    }
}
