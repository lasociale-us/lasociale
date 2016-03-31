package us.lasociale.lasociale;

import android.net.Uri;
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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
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

        int hueCentre = 76;
        Mat original = inputFrame.rgba();
        Mat display = original.clone();


        Mat matTemp = original.clone();
        Mat matTemp2 = new Mat();
        Mat matHSV = new Mat();

        Mat matCenter = new Mat();
        Mat matSurround = new Mat();

        Imgproc.cvtColor(matTemp, matTemp2, Imgproc.COLOR_RGBA2BGR, 0);
        Imgproc.cvtColor(matTemp2, matHSV, Imgproc.COLOR_BGR2HSV, 0);

        // Core.inRange(out1, new Scalar(87, 80, 80), new Scalar(93, 255, 255), out1);

        // extract center by hue
        Core.inRange(matHSV, new Scalar((hueCentre / 2) - 4, 70, 70), new Scalar((hueCentre / 2) + 4, 255, 255), matCenter);


        // extract surroung by value
        Core.inRange(matHSV, new Scalar(0, 0, 0), new Scalar(255, 255, 60), matSurround);

        //Core.inRange(roiTmp, new Scalar(170, 70, 30), new Scalar(180, 255, 255), out2);


        //Imgproc.cvtColor(matCenter, display, Imgproc.COLOR_GRAY2RGBA, 0);


        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(matCenter, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);


        log.info("Countours:" + contours.size());
        for (MatOfPoint contour:
             contours) {

            Point[] pts = contour.toArray();
            Imgproc.drawMarker(display, pts[0], new Scalar(255,0,0,0));
            /*

            if (pts.length < 5)
                continue;

            double arr = Imgproc.contourArea(contour, false);

            MatOfPoint2f  contour2f = new MatOfPoint2f( contour.toArray() );

            RotatedRect r = Imgproc.fitEllipse(contour2f);
            Point[] rectPoints = new Point[4];
            r.points(rectPoints);
            for(int n=0; n < 4; n++) {
                Imgproc.line(display, rectPoints[n], rectPoints[(n+1)%4],
                    new Scalar(0,255,0));
            }
            double ellArr = r.size.area() * 3.14 / 4;

            if (arr < ellArr *.9 || arr > ellArr*1.1)
                continue;

            log.info("CONTOUR2:" + r.size.width + ", " + r.size.height +","+ r.angle);


            Point pt = r.center;

            double angle = 0;
            double step = (Math.PI * 2.0) / 80.0;

            double rot = Math.toRadians(-r.angle);

            for(int n=0; n < 80; n++)
            {
                /*
                pt2.x += Math.cos(angle) * (r.size.width/2);
                pt2.y += Math.sin(angle) * (r.size.height/2);

                double x1 = Math.cos(angle) * (r.size.width/2);
                double y1 = Math.sin(angle) * (r.size.height/2);

                // rotation of ellipse
                x1 = x1 * Math.cos(rot) - y1 * Math.sin(rot);
                y1 = x1 * Math.sin(rot) + y1 * Math.cos(rot);

                // offset of center of ellipse
                x1 += r.center.x;
                y1 += r.center.y;

                Imgproc.drawMarker(display, new Point(x1,y1), new Scalar(0,0,255,0));

                angle += step;

            }
            */




        }

        //log.info("Fount contours:" + contours.size());


        return display;
    }

}
