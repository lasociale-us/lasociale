

from os import listdir
import sys
import cv2
import numpy as np
import math
import binascii

def nothing(*arg):
    pass


def show(img):
    cv2.imshow('file', img)



    while(True):
        ch = cv2.waitKey(50) & 0xFF
        if ch == 27:
            break


    cv2.destroyAllWindows()


def auto_canny(image, sigma=0.33):
        # compute the median of the single channel pixel intensities
        v = np.median(image)
 
        # apply automatic Canny edge detection using the computed median
        lower = int(max(0, (1.0 - sigma) * v))
        upper = int(min(255, (1.0 + sigma) * v))
        edged = cv2.Canny(image, 0, 500)
 
        # return the edged image
        return edged

def find_ellipse(img, mx=400, app=5,blr=10):



    gray = cv2.cvtColor(img.copy(), cv2.COLOR_BGR2GRAY)
    
    #331/360
    #128/260
    #C_MIN = np.array([0,0,0],np.uint8)
    #C_MAX = np.array([0,0,0],np.uint8)
    
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edged = cv2.Canny(blurred, 0, 50, apertureSize=3)
    

    if False:
        # interesting approach but difficult to make circle large enough
        hsv  = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

        hsv = cv2.blur(hsv, (15,15))
        C = 128/2
        spikes = cv2.inRange(hsv.copy(), (C-10, 5, 20), (C+10,255,185))
        C = 331/2
        spikes2 = cv2.inRange(hsv.copy(), (C-10, 5, 20), (C+10,255,185))

        result = cv2.bitwise_or(spikes, spikes2)


    threshed = edged
    threshed = cv2.blur(edged, (5,5))
    threshed = cv2.blur(threshed, (5,5))
    threshed = cv2.blur(threshed, (5,5))
    threshed = cv2.blur(threshed, (5,5))

    #cv2.imshow('file1', threshed)

    (contours, c2)= cv2.findContours( threshed.copy(), 
            cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    cnts = sorted(contours, key = cv2.contourArea, reverse = True)[:10]
    
    ccnts = []
    for cont in cnts:
        arr = cv2.contourArea(cont)
        #print('cont', cv2.contourArea(cont)) 
        if (len(cont)<10): 
            continue
        (x,y),(MA,ma),angle = cv2.fitEllipse(cont)

        if (arr<2500): 
            continue


        ell_arr = 3.14 * MA * ma / 4
        print('  ' + str(arr/ell_arr) + ' ; ' + str(cv2.contourArea(cont)))
        if arr/ell_arr <0.9 or arr/ell_arr > 1.10:
            continue

        epsilon = 0.001 * cv2.arcLength(cont,True)
        ccnts.append(cv2.approxPolyDP(cont, 0.5, True))


    if len(ccnts) < 1:
        return (0,0),(0,0),None
    cont = sorted(ccnts, key = cv2.contourArea, reverse = True)[0]
    #cv2.drawContours(img, [cont], -1, (0,255,0), 3)



    return cv2.fitEllipse(cont)

def lhex(x):
    return ' '.join([hex(b)[2:] for b in x])


def check_checksum(bts):
    mult = [ 17, 73, 14, 2, 27, 99, 101, 7];
    n1 = 0

    for n in range(8):
        n1 = n1 + (bts[n] & 0xff) + ((bts[n]&0xff) * mult[n]) & 0xFFFF;

    chk1 = n1 & 0xFF
    chk2 = (n1>>8) & 0xFF
    return (bts[8] == chk1) and (bts[9] == chk2)


def check_hash(h, skip=0):

    SPEC1 = '10011000'; # 0x98
    SPEC2 = '10111010'; # 0xBA

    s = h.find(SPEC1,skip)
    if s < 0:
        s = h.find(SPEC2,skip)
        if s < 0:
            return False

    hsh = h + h + h
    hsh = hsh[s+32:s+112]
    bts = []
    hex = ''
    for n in range(10):
        sbyt = int(hsh[n*8:n*8+8],2)
        bts.append(sbyt)

    print("TRY:"+ lhex(bts))
    if check_checksum(bts):
        print("FOUND correct hash:" + lhex(bts))
        return True
        

    print('INCORRECT CHECKSUM hash=' + lhex(bts))
    return check_hash(h, s+1)



for filename in sorted(listdir('.')):
    if filename[-4:] != '.png':
        continue
    if len(sys.argv)>1 and sys.argv[1] != filename:
        continue

    print('loading ' + filename)

    img = cv2.imread(filename)

    (height, width, channels) = img.shape
    
    
    (x,y),(MA,ma),angle = find_ellipse(img)
    if angle == None:
        print("NO ELLIPSE")
        continue


    print("ELLIPSE @" + str(ma) + ','+str(MA) + ',' + str(angle))


    bgr = img # cv2.cvtColor(img, cv2.COLOR_RGBA2BGR)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)

    cv2.namedWindow('edge')
        

    #display  = cv2.cvtColor(spikes, cv2.COLOR_GRAY2RGB)
    display = img.copy()

    blank = img.copy()

    if True:



        ma *= .90
        MA *= .90

        cv2.ellipse(display,(int(x),int(y)),(int(MA/2), int(ma/2)),angle,0,360,(0,0,255))
        
        ell_angle = angle * math.pi / 180
        minval=(255,0)
        trg = hsv.copy()
        points = []
        pixcount=31
        midpix = (pixcount-1)/2
        TRES = 160
        online = False
        counton = 0
        resulthash = ''
        for a in range(80 * pixcount ):
            ang = math.pi * 2 * a / (80*pixcount)
            xx = math.cos(ang) * (MA/2)
            yy = math.sin(ang) * (ma/2)


            xxx = xx * math.cos(ell_angle) - yy * math.sin(ell_angle)
            yyy = xx * math.sin(ell_angle) + yy * math.cos(ell_angle)

            xx = xxx + x
            yy = yyy + y

            if xx < 0 or yy < 00 or xx > width or yy > height:
                continue
            #print(hsv[yy,xx])
            if hsv[yy,xx][2] < minval[0]:
                minval = (hsv[yy,xx][2], a)



            points.append(hsv[yy,xx])
            display[yy,xx] = (0,255,255) 


        a = minval[0]
        if a > 0:
            a = a -1
        xpts = points[a:]
        xpts.extend(points[:a])
        xpts.extend(points[-pixcount:])
        points = xpts


        print(len(points))

        ctr = 0
        for x in range(80):
            minval = (255,0)

            # we look from pixcount*.7 to xpi
            frm = ctr + int(pixcount *2/3)
            to = ctr + int( pixcount *4/3)

            if to > len(points):
                break

            print('from='+str(frm) + ', to='+str(to))
            for n in range(frm,to):
                v = points[n][2]
                if v < minval[0]:
                    minval = (v, n)

            print('local minimum='+str(minval) + ' with ' + 
                    str(points[minval[1]][0]))

            ctr = minval[1]
            if (points[minval[1]][0] > 50):
                resulthash = resulthash + '1';
            else:
                resulthash = resulthash + '0';
        print(resulthash)
        print('HASH=' + str(check_hash(resulthash)))

    if False:
        x = int(x) 
        y = int(y)
        ma = int(ma/2)
        MA = int(MA/2)
            #cv2.ellipse(img, (100,100),(100,100), 0,360,(0,0,255))
        cv2.ellipse(blank,(x,y),(MA,ma),0,0,360,(255,255,255),10,0)

        cv2.ellipse(img,(x,y),(MA,ma),0,0,360,(255,255,255),10,0)

        target = img.copy()
        #(height, width, channels) = img.shape
        #cv2.rectangle(target, (0,0), (width,height), (255,255,255),-1)
        #for i in range(height):

        #    for j in range(width):
        #        if blank[i,j] > 0:
        #            target[i,j] = (255,255,255)
        #        else:
        #            target[i,j] = img[i,j]

        rows,cols,channels = img.shape
        roi = target[0:rows, 0:cols ]

        mask_inv = cv2.bitwise_not(blank)
        img_bg = cv2.bitwise_and(roi,roi, mask=mask_inv)
        img_fg = cv2.bitwise_and(img, img, mask = blank)


        result = cv2.bitwise_or(img_bg, img_fg)

    #cv2.floodFill(img2, blank, (1,1), (255,255,255))
    #target = cv2.bitwise_or(target, img_fg, mask_inv)
    show(display)
