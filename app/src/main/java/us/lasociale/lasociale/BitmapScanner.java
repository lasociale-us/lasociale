package us.lasociale.lasociale;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.Objdetect;

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

    private static Object lock = new Object();
    static{ System.loadLibrary("opencv_java3"); }


    int pixcount = 31;
    int midpix = (pixcount-1)/2;


    public BitmapScanner(String input)
    {
        Mat mat = Imgcodecs.imread(input);
        mInput = mat;


    }

    public BitmapScanner(Mat input)
    {
        mInput = input;

    }

    public RotatedRect findEllipse(Mat input) {


        Mat matGray = new Mat();

        Imgproc.cvtColor(input, matGray, Imgproc.COLOR_BGR2GRAY, 0);

        Mat edges = new Mat();
        Imgproc.Canny(matGray, edges, 500.0, 1000.0, 5, false);

        Mat blurred = new Mat();
        Imgproc.blur(edges, blurred, new Size(5, 5));

        //Imgproc.cvtColor(blurred, Display, Imgproc.COLOR_GRAY2RGBA, 0);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(blurred, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        RotatedRect largest = null;
        double largestArea = 0.0;
        for (MatOfPoint contour:  contours) {
            Point[] pts = contour.toArray();
            double arr = Imgproc.contourArea(contour, false);

            if (pts.length < 10)
                continue;

            if (arr < 1000)
                continue;

            MatOfPoint2f contour2f = new MatOfPoint2f( contour.toArray() );


            RotatedRect r = Imgproc.fitEllipse(contour2f);
            Point[] rectPoints = new Point[4];
            r.points(rectPoints);
            double ellArr = r.size.area() * 3.14 / 4;

            if (arr < ellArr *.9 || arr > ellArr*1.15)
                continue;

            if (arr > largestArea)
            {
                largestArea = arr;
                largest = r;
            }

        }
        return largest;
    }

    public List<byte[]> FindPoints(RotatedRect rect, Mat hsv)
    {
        int n;

        int minval = 255;
        int minvaln=0;
        List<byte[]> pixs = new ArrayList<byte[]>();

        for(n=0; n < 80*pixcount; n++)
        {
            double angle = Math.PI * 2 * n / (80*pixcount);
            double x1 = Math.cos(angle) * ((rect.size.width*.9)/2);
            double y1 = Math.sin(angle) * ((rect.size.height*.9)/2);

            // rotation of ellipse
            double rot = Math.toRadians(-rect.angle);
            x1 = x1 * Math.cos(rot) - y1 * Math.sin(rot);
            y1 = x1 * Math.sin(rot) + y1 * Math.cos(rot);

            // offset of center of ellipse
            x1 += rect.center.x;
            y1 += rect.center.y;

            byte[] pix = new byte[3];
            hsv.get((int) y1, (int) x1, pix);
            //Display.put(new short[] { })
            if (pix == null)
                return null;

            if (V(pix) < minval) {
                minval = V(pix);
                minvaln = n;
            }
            pixs.add(pix);

        }
        int sz = pixs.size();
        List<byte[]> total = new ArrayList<>(pixs.subList(minvaln, sz));
        total.addAll(pixs.subList(0,minvaln));
        total.addAll(new ArrayList<byte[]>(total.subList(sz-pixcount,sz)));
        return total;
    }

    private int V(byte[] pt)
    {
        return (pt[2] &0xFF);
    }
    private int H(byte[] pt)
    {
        return (pt[0] &0xFF);
    }

    private String ScanPoints(List<byte[]> pts) {
        String r = "";
        int ctr = 0;
        for(int n=0; n < 80; n++)
        {
            int from = ctr + (pixcount*2/3);
            int to = ctr + (pixcount*4/3);

            if (to>pts.size())
                break;

            int minval = 255;
            int minn = 0;
            for(int i = from; i < to; i++)
            {
                int v = V(pts.get(i));
                if (v<minval)
                {
                    minval = v;
                    minn = i;
                }
            }
            //log.info("Minimum " +minval + " @ " + minn + " with " + pts.get(minn)[0]);

            ctr = minn;
            if (H(pts.get(minn)) < 75)
                r = r +"1";
            else
                r = r + "0";

        }
        log.info("HASH ("+ r.length() +")="+ r);
        if (r.length() == 81)
            r = r.substring(0,80);
        return r;
    }

    private String CheckHash(String bits)
    {
        return CheckHash(bits,0);
    }

    private String CheckHash(String bits, int start )
    {
        String SPEC1 = "10011000"; // 0x98
        String SPEC2 = "10111010"; // 0xBA


        int n = bits.indexOf(SPEC1, start);
        if (n<0)
        {
            n = bits.indexOf(SPEC2, start);
            if (n<0)
                return null;

        }

        byte[] bytes = new byte[10];
        String bitstriple = bits+bits+bits;
        String bitsrot = bitstriple.substring(n+32, n+112);
        byte xor = 0;
        for(int m=0; m <10; m++)
        {
            bytes[m] = (byte)Integer.parseInt(bitsrot.substring(m * 8, m * 8 + 8), 2);
            xor ^= bytes[m];
        }

        String hex = IdentityManager.toHexString(bytes);
        if (xor == 0)
            return hex;
        else
        {
            log.info("Invalid:"+hex+", xor="+xor);
            return null;
        }


    }


    public synchronized String Scan() {
        String msg = "";

        int hueCentre1 = 146;
        int hueCentre2 = 235;




        Mat original = mInput;
        Display = original.clone();


        Mat matTemp = original.clone();
        Mat matTemp2 = new Mat();
        Mat matHSV = new Mat();

        Mat matCenter = new Mat();
        Mat matCenter2 = new Mat();
        Mat matSurround = new Mat();

        Mat bgr = new Mat();
        Imgproc.cvtColor(original, bgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(bgr, matHSV, Imgproc.COLOR_BGR2HSV);
        RotatedRect ellipse = findEllipse(bgr);


        if (ellipse != null) {
            Point[] rectPoints = new Point[4];
            ellipse.points(rectPoints);
            for (int n = 0; n < 4; n++) {
                Imgproc.line(Display, rectPoints[n], rectPoints[(n + 1) % 4],
                        new Scalar(0, 255, 0));
            }
            msg += "E=Ok";

            List<byte[]> pts = FindPoints(ellipse, matHSV);

            if (pts == null)
                msg +=",P=Err";
            else
            {
                msg +=",P=Ok";
                String bits = ScanPoints(pts);
                //msg += ","+bits;
                log.info(Integer.toString(pts.size()));
                if (bits.length() != 80) {
                    msg += ",B=Err";
                }
                else
                {
                    msg += ",B=Ok";
                    String hex = CheckHash(bits);

                    if (hex==null)
                    {
                        msg +="H=Err";
                    }
                    else
                    {
                        msg+="H=Ok";
                        log.info("Returning hash="+hex);
                        return hex;
                    }

                }
            }


        }
        else
            msg += "E=err";


        /*

        // Core.inRange(out1, new Scalar(87, 80, 80), new Scalar(93, 255, 255), out1);

        Imgproc.blur(matHSV, matHSV, new Size(10,10));
        // extract center by hue
        Core.inRange(matHSV, new Scalar((hueCentre1 * 180 / 256) - 8, 0, 120), new Scalar((hueCentre1 * 180 / 256) + 8, 235, 170), matCenter);
        //Core.inRange(matHSV, new Scalar((hueCentre2 * 180 / 256) - 4, 70 70), new Scalar((hueCentre2 * 180 / 256) + 4, 255, 255), matCenter2);8
        //Core.bitwise_and(matHSV, matCenter, matHSV);
        Imgproc.Canny(matCenter, matCenter, 50f, 200f);


        // extract surroung by value
        //Core.inRange(matHSV, new Scalar(0, 0, 0), new Scalar(255, 255, 60), matSurround);


        //Core.inRange(roiTmp, new Scalar(170, 70, 30), new Scalar(180, 255, 255), out2);
        Mat lines = new Mat();
        Imgproc.HoughLinesP(matCenter, lines, 1.0f, 3.14f/180f , 0, 15f, 0);


        Imgproc.cvtColor(matCenter, display, Imgproc.COLOR_GRAY2RGBA, 0);

        Imgproc.putText(display, Integer.toString(lines.rows()), new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0));

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(matCenter, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        int rows = lines.rows();
        for(int n=0; n < rows; n++)
        {
            double[] vec = lines.get(n, 0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Imgproc.line(display, start, end, new Scalar(255,0,0));
            double dx = x1 - x2;
            double dy = y1 - y2;

        }

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


        Imgproc.putText(Display, msg, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 0, 0));

        this.Display = Display;


        return null;
    }

}
