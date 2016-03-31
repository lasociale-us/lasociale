package us.lasociale.lasociale;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by tomas on 31-3-16.
 */
public class BitmapScanner {

    private static Logger log = Logger.getLogger("us.lasociale.bitmapscanner");

    private Mat mInput;

    public Mat Display;


    public BitmapScanner(Mat input)
    {
        mInput = input;

    }

    public  void Scan() {

        int hueCentre1 = 146;
        int hueCentre2 = 235;


        Mat original = mInput;
        Mat display = original.clone();


        Mat matTemp = original.clone();
        Mat matTemp2 = new Mat();
        Mat matHSV = new Mat();

        Mat matCenter = new Mat();
        Mat matCenter2 = new Mat();
        Mat matSurround = new Mat();

        Imgproc.cvtColor(matTemp, matTemp2, Imgproc.COLOR_RGBA2BGR, 0);
        Imgproc.cvtColor(matTemp2, matHSV, Imgproc.COLOR_BGR2HSV, 0);

        // Core.inRange(out1, new Scalar(87, 80, 80), new Scalar(93, 255, 255), out1);

        //Imgproc.blur(matHSV, matHSV, new Size(10,10));
        // extract center by hue
        Core.inRange(matHSV, new Scalar((hueCentre1 * 180 / 256) - 6, 0, 100), new Scalar((hueCentre1 * 180 / 256) + 6, 255, 165), matCenter);
        //Core.inRange(matHSV, new Scalar((hueCentre2 * 180 / 256) - 4, 70, 70), new Scalar((hueCentre2 * 180 / 256) + 4, 255, 255), matCenter2);8
        //Core.bitwise_or(matCenter, matCenter2, matCenter);


        // extract surroung by value
        //Core.inRange(matHSV, new Scalar(0, 0, 0), new Scalar(255, 255, 60), matSurround);


        //Core.inRange(roiTmp, new Scalar(170, 70, 30), new Scalar(180, 255, 255), out2);
        Mat lines = new Mat();
        //Imgproc.HoughLinesP(matCenter, lines, 1.0f, 0.1f, );


        Imgproc.cvtColor(matCenter, display, Imgproc.COLOR_GRAY2RGBA, 0);


        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(matCenter, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);


        log.info("Countours:" + contours.size());
        for (MatOfPoint contour:
                contours) {
            Point[] pts = contour.toArray();
            double arr = Imgproc.contourArea(contour, false);
            //log.info("Contour: " + arr + "  pts=" + pts.length);
            //Imgproc.drawMarker(display, pts[0], new Scalar(255,0,0,0));
            /*
            if (pts.length < 5)
                continue;


            if (arr < 4000)
                continue;

            MatOfPoint2f  contour2f = new MatOfPoint2f( contour.toArray() );

            RotatedRect r = Imgproc.fitEllipse(contour2f);
            Point[] rectPoints = new Point[4];
            r.points(rectPoints);
            for(int n=0; n < 4; n++) {
                Imgproc.line(display, rectPoints[n], rectPoints[(n+1)%4],
                        new Scalar(0,255,0));
            }
            double ellArr = r.size.area() * 3.14 / 4;
            */

            //if (arr < ellArr *.8 || arr > ellArr*1.1)
            //    continue;

            //Point[] pts = contour.toArray();
            //
            /*


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


            this.Display = display;

        }

    }
}
