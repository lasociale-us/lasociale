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

        log.info("findEllipse");
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
            if (x1 < 0 || y1 < 0 || x1>= width || y1 >= height)
                return null;
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
        log.info("scanPoints");
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


    public synchronized String Scan() {
        String msg = "";

        log.info("Start scan");
        int hueCentre1 = 146;
        int hueCentre2 = 235;




        Mat original = mInput;
        Display = original.clone();


        Mat matHSV = new Mat();

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



        Imgproc.putText(Display, msg, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 1, new Scalar(255, 0, 0));
        log.info("Returning scan");
        this.Display = Display;


        return null;
    }

}
