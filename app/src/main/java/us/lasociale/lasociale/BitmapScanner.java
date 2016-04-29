package us.lasociale.lasociale;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.LineSegmentDetector;
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


    int pixcount = 71;
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

        log.info("findEllipse");
        Mat matGray = new Mat();

        Imgproc.cvtColor(input, matGray, Imgproc.COLOR_BGR2GRAY, 0);
        Imgproc.blur(matGray, matGray, new Size(5, 5));

        Mat edges = new Mat();
        Imgproc.Canny(matGray, edges, 500.0, 1500.0, 5, false);

        Mat blurred = new Mat();
        Imgproc.blur(edges, blurred, new Size(5, 5));
        Imgproc.blur(blurred, blurred, new Size(5, 5));


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

            if (arr < 2500)
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

    public RotatedRect findEllipse2(Mat input) {

        log.info("findEllipse");
        Mat matGray = new Mat();

        int c = 42 * 180 / 255;

        Core.inRange(input, new Scalar(c - 15, 10, 100), new Scalar(c + 15, 255, 240), matGray);


        //Mat gblurred = new Mat();
        //Imgproc.GaussianBlur(matGray, gblurred, new Size(5,5),0.0);

        //Mat edges = new Mat();
        //Imgproc.Canny(matGray, edges, 0.0, 50.0, 3, false);

        //Display = matGray;
        /*Mat blurred = new Mat();
        Imgproc.blur(edges, blurred, new Size(5, 5));
        Imgproc.blur(blurred, blurred, new Size(5, 5));
        Imgproc.blur(blurred, blurred, new Size(5, 5));
        Imgproc.blur(blurred, blurred, new Size(5, 5));
*/
        //Imgproc.cvtColor(blurred, Display, Imgproc.COLOR_GRAY2RGBA, 0);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(matGray.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        RotatedRect largest = null;
        double largestArea = 0.0;
        for (MatOfPoint contour:  contours) {
            Point[] pts = contour.toArray();
            double arr = Imgproc.contourArea(contour, false);

            if (pts.length < 10)
                continue;

            if (arr < 25)
                continue;

            MatOfPoint2f contour2f = new MatOfPoint2f( contour.toArray() );


            RotatedRect r = Imgproc.fitEllipse(contour2f);
            Point[] rectPoints = new Point[4];
            r.points(rectPoints);
            double ellArr = r.size.area() * 3.14 / 4;

            if (arr < ellArr *.95 || arr > ellArr*1.05)
                continue;

            if (arr > largestArea)
            {
                largestArea = arr;
                largest = r;
            }

        }

        //byte[] pix = new byte[3];
        //input.get((int) largest.center.y, (int) largest.center.x, pix);

        //log.info("H=" + H(pix) + ",S=" + S(pix) + "V=" + V(pix));

        return largest;
    }

    public double findAngle(RotatedRect rect, Mat hsv)
    {
        log.info("findAngle");
        int n;

        int minval = 255;
        int minvaln=0;
        double minangle = 0.0;
        List<byte[]> pixs = new ArrayList<byte[]>();

        int width = hsv.width();
        int height = hsv.height();


        for(n=0; n < 1000; n++)
        {
            double angle = Math.PI * 2 * n / (1000);
            double x1 = Math.cos(angle) * ((rect.size.width*8.7)/2);
            double y1 = Math.sin(angle) * ((rect.size.height*8.7)/2);

            // rotation of ellipse
            double rot = Math.toRadians(-rect.angle);
            double xx1 = x1 * Math.cos(rot) - y1 * Math.sin(rot);
            double yy1 = x1 * Math.sin(rot) + y1 * Math.cos(rot);

            // offset of center of ellipse
            x1 = xx1 + rect.center.x;
            y1 = yy1 + rect.center.y;

            byte[] pix = new byte[3];
            if (x1 < 0 || y1 < 0 || x1>= width || y1 >= height)
                return 0.0;
            hsv.get((int) y1, (int) x1, pix);
            Display.put((int)y1, (int)x1, new byte[] { (byte)255,(byte)255,(byte)0, (byte)255});
            if (pix == null)
                return 0.0;

            //log.info("V="+V(pix) +",H= "+H(pix) + ",S="+S(pix));
            if (V(pix) < minval) {
                minval = V(pix);
                minvaln = n;
                minangle = angle;
            }


        }
        return minangle;

    }

    public List<byte[]> FindPoints(RotatedRect rect, Mat hsv)
    {
        log.info("findPoints");
        int n;

        int minval = 255;
        int minvaln=0;
        List<byte[]> pixs = new ArrayList<byte[]>();

        int width = hsv.width();
        int height = hsv.height();

        for(n=0; n < 80*pixcount; n++)
        {
            double angle = Math.PI * 2 * n / (80*pixcount);
            double x1 = Math.cos(angle) * ((rect.size.width*8.7)/2);
            double y1 = Math.sin(angle) * ((rect.size.height*8.7)/2);

            // rotation of ellipse
            double rot = Math.toRadians(-rect.angle);
            double xx1 = x1 * Math.cos(rot) - y1 * Math.sin(rot);
            double yy1 = x1 * Math.sin(rot) + y1 * Math.cos(rot);

            // offset of center of ellipse
            x1 = xx1 + rect.center.x;
            y1 = yy1 + rect.center.y;

            byte[] pix = new byte[3];
            if (x1 < 0 || y1 < 0 || x1>= width || y1 >= height)
                return null;
            hsv.get((int) y1, (int) x1, pix);
            Display.put((int)y1, (int)x1, new byte[] { (byte)255,(byte)255,(byte)0, (byte)255});
            if (pix == null)
                return null;

            //log.info("V="+V(pix) +",H= "+H(pix) + ",S="+S(pix));
            if (V(pix) < minval) {
                minval = V(pix);
                minvaln = n;
            }
            pixs.add(pix);

        }
        int sz = pixs.size();
        List<byte[]> total = new ArrayList<>(pixs.subList(minvaln, sz));
        total.addAll(pixs.subList(0, minvaln));
        total.addAll(new ArrayList<byte[]>(total.subList(sz - pixcount, sz)));
        return total;
    }

    private int S(byte[] pt)
    {
        return (pt[1] &0xFF);
    }
    private int V(byte[] pt)
    {
        return (pt[2] &0xFF);
    }
    private int H(byte[] pt)
    {
        return (pt[0] &0xFF);
    }

    private int R(byte[] pt) {
        double H = (double)(H(pt))*2.0;   // 0-360
        double S = (double)(S(pt))/255.0; //0-1
        double V = (double)(V(pt))/255.0; //0-1

        double C = V * S;
        double X = C * (1 - Math.abs(H / 60) % 2 - 1);
        double m = V - C;

        double R1;
        if ( H < 60 || H>=300)
            R1 = C;
        else if (H >=240 || H < 120)
            R1 = X;
        else
            R1 = 0;

        return (int)((R1+m)*255.0);
    }

    private int B(byte[] pt) {
        double H = (double)(H(pt))*2.0;   // 0-360
        double S = (double)(S(pt))/255.0; //0-1
        double V = (double)(V(pt))/255.0; //0-1

        double C = V * S;
        double X = C * (1 - Math.abs((int)H / 60) % 2 - 1);
        double m = V - C;

        double B1;
        if ( H >=180 && H < 300)
            B1 = C;
        else if (H >=120)
            B1 = X;
        else
            B1 = 0;

        return (int)((B1+m)*255.0);
    }

    private String ScanPoints(List<byte[]> pts) {
        log.info("scanPoints");
        String r = "";
        int ctr = 0;
        int hues[] = new int[80];
        int minhue = -1;
        int maxhue = -1;

        for(int n=0; n < 80; n++)
        {
            // we find the lowest value approximately pixcount ahead
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
            int redness = R(pts.get(minn)) - B(pts.get(minn));

            if (redness > 0)
            {
                r = r + "0";

            }
            else
            {
                r = r + "1";
            }

            log.info("Minimum " +minval + " @ " + minn + " with R=" + R(pts.get(minn)) +",B=" + B(pts.get(minn)) + ",H=" + H(pts.get(minn)) );

            ctr = minn;
            /*
            hues[n] = V(pts.get(minn));
            if (minhue == -1 || hues[n] < minhue)
                minhue = hues[n];
            if (maxhue == -1 || hues[n] > maxhue)
                maxhue = hues[n];

            */
        }
        /*
        log.info("Miiddle-hiue" + (maxhue+minhue)/2);
        for(int n=0; n < 80; n++)
            if (hues[n] < (maxhue+minhue)/2)
                r = r +"1";
            else
                r = r + "0";
*/
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
            if (n<0) {
                log.info("No marker");
                return null;
            }

        }

        byte[] bytes = new byte[10];
        String bitstriple = bits+bits+bits;
        String bitsrot = bitstriple.substring(n+32, n+112);
        for(int m=0; m <10; m++)
        {
            bytes[m] = (byte)Integer.parseInt(bitsrot.substring(m * 8, m * 8 + 8), 2);
        }

        String hex = IdentityManager.toHexString(bytes);
        if (IdentityManager.CheckChecksum(bytes))
            return hex;
        else
        {
            log.info("Invalid:"+hex);
            return CheckHash(bits, n+1);
        }


    }

    private Point fromPolar(RotatedRect root, double angle, double r)
    {
        double x1 = Math.cos(angle) * ((root.size.width*r)/2);
        double y1 = Math.sin(angle) * ((root.size.height*r)/2);

        // rotation of ellipse
        double rot = Math.toRadians(-root.angle);
        double xx1 = x1 * Math.cos(rot) - y1 * Math.sin(rot);
        double yy1 = x1 * Math.sin(rot) + y1 * Math.cos(rot);

        // offset of center of ellipse
        x1 = xx1 + root.center.x;
        y1 = yy1 + root.center.y;

        return new Point(x1,y1);
    }

    public synchronized String Scan() {
        String msg = "";

        log.info("Start scan");
        int hueCentre1 = 146;
        int hueCentre2 = 235;




        Mat original = mInput;
        Display = original.clone();


        Mat matHSV = new Mat();
        Mat blur = new Mat();
        Mat bgr = new Mat();
        Imgproc.cvtColor(original, bgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(bgr, matHSV, Imgproc.COLOR_BGR2HSV);

        Imgproc.blur(bgr, blur, new Size(10, 10));
        Imgproc.blur(blur, blur, new Size(10,10));
        //RotatedRect ellipse = findEllipse(bgr);
        RotatedRect ellipse = findEllipse2(matHSV);


        if (ellipse == null) {
            log.info("NO Ellipse");
            return null;
        }


            //for (int n = 0; n < 4; n++) {
            //    Imgproc.line(Display, rectPoints[n], rectPoints[(n + 1) % 4],
            //            new Scalar(0, 255, 0));
           // }


            //ellipse = new RotatedRect(ellipse.center,
            //        new Size(ellipse.size.width*8.6, ellipse.size.height*8.6), ellipse.angle);
            //Imgproc.ellipse(mask, ellipse, new Scalar(255, 255, 255), 15);

        double base_angle = findAngle(ellipse, matHSV);

        String bits = "";
        for(int n=0; n < 80; n++)
        {
            base_angle += Math.PI * 2.0 / 80;

            // distance betwee two lines = 0.078
            // we find the darkest point
            int minv = 255;
            double mina = base_angle;
            byte[] pix = new byte[3];
            String ss = "";
            for(double a = base_angle - 0.02; a <= base_angle+0.02; a+= 0.002)
            {

                Point pt = fromPolar(ellipse, a, 9.0);
                matHSV.get((int) pt.y, (int) pt.x, pix);
                //ss += V(pix) +",";
                if (V(pix) <minv)
                {
                    minv = V(pix);
                    mina = a;
                }

            }

            double angle = mina;
            base_angle = angle;
            //log.info("base=" + base_angle + ",v="+minv + ",ss="+ss);

            double width = 0.018;
            Point pt1 = fromPolar(ellipse, angle-width, 9.4);
            Point pt2 = fromPolar(ellipse, angle-width, 10.4);
            Point pt3 = fromPolar(ellipse, angle+width, 10.4);
            Point pt4 = fromPolar(ellipse, angle+width, 9.4);
/*              Imgproc.line(Display, pt1,pt2, new Scalar(0,255,0));
            Imgproc.line(Display, pt2,pt3, new Scalar(0,255,0));
            Imgproc.line(Display, pt3,pt4, new Scalar(0,255,0));
            Imgproc.line(Display, pt4,pt1, new Scalar(0,255,0));
*/
            {
                Mat msk = Mat.zeros(matHSV.rows(), matHSV.cols(), CvType.CV_8UC1);
                MatOfPoint pts = new MatOfPoint(pt1, pt2, pt3, pt4);
                Imgproc.fillConvexPoly(msk, pts, new Scalar(255));
                Imgproc.fillConvexPoly(Display, pts, new Scalar(0,255,255));
                Scalar s = Core.mean(bgr, msk);

//                    msg += s.val[0]+",";
//                  log.info(Double.toString(s.val[0]- s.val[2]));
                bits += (s.val[0]- s.val[2] < 0.0 ? "0" : "1");
            }

            //ellipse = new RotatedRect(ellipse.center,
            //        new Size(ellipse.size.width*8.6, ellipse.size.height*8.6), ellipse.angle);
            //Imgproc.ellipse(mask, ellipse, new Scalar(255, 255, 255), 15);




        }


            //List<byte[]> pts = FindPoints(ellipse, matHSV);


            //Imgproc.ellipse(mask,ellipse, new Scalar(255,255,255), 5);
            /*{
                Mat spikes = new Mat();
                Core.inRange(matHSV, new Scalar(0, 0, 0), new Scalar(0,0,200), spikes);
                Display = spikes;
                Mat spikesMasked = new Mat(matHSV.rows(), matHSV.cols(), Display.type());
                //Imgproc.rectangle(spik);
                //original.copyTo(Display, mask);

            }*/





           // Imgproc.cvtColor(mask, Display, Imgproc.COLOR_GRAY2BGR);
            //LineSegmentDetector detect = Imgproc.createLineSegmentDetector();
            //detect.detect(Display, )


        if (bits.length() != 80) {
            log.info("Invalid length bits: " + bits.length());
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



        Imgproc.putText(Display, msg, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 0, 0));
        log.info("Returning scan");


        return null;
    }

}
