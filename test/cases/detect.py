

from os import listdir
import cv2
import numpy as np
import math
import binascii

def nothing(*arg):
    pass


def find_ellipse(img):


    gray = cv2.cvtColor(img.copy(), cv2.COLOR_BGR2GRAY)

    # find edges & blur
    threshed = cv2.Canny(gray, 500, 1000, apertureSize=5)
    threshed = cv2.blur(threshed, (5,5))

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

        ell_arr = 3.14 * MA * ma / 4
        if arr/ell_arr <0.9 or arr/ell_arr > 1.15:
            continue

        epsilon = 0.001 * cv2.arcLength(cont,True)
        ccnts.append(cv2.approxPolyDP(cont, 0.5, True))


    cont = sorted(ccnts, key = cv2.contourArea, reverse = True)[0]

    return cv2.fitEllipse(cont)

def lhex(x):
    return ' '.join([hex(b)[2:] for b in x])

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
    xor = 0
    hex = ''
    for n in range(10):
        sbyt = int(hsh[n*8:n*8+8],2)
        bts.append(sbyt)
        xor = xor^sbyt

    if xor == 0:
        print("FOUND correct hash:" + lhex(bts))
        return True
        

    print('XOR = ' + str(xor) + ', hash=' + lhex(bts))
    return check_hash(h, s+1)

for filename in listdir('.'):
    if filename[-4:] != '.png':
        continue
    print('loading ' + filename)

    img = cv2.imread(filename)

    
    
    (x,y),(MA,ma),angle = find_ellipse(img)

    bgr = img # cv2.cvtColor(img, cv2.COLOR_RGBA2BGR)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)

    cv2.namedWindow('edge')
        
    C = 146 * 180 / 256
    C_MIN = np.array([0,0,0],np.uint8)
    C_MAX = np.array([0,0,0],np.uint8)
    spikes = cv2.inRange(hsv.copy(), (C-15, 0, 120), (C+15,235,180))

    display  = cv2.cvtColor(spikes, cv2.COLOR_GRAY2RGB)

    blank = img.copy()

    if True:
        ma *= 0.9
        MA *= 0.9

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

            xx = xx * math.cos(ell_angle) - yy * math.sin(ell_angle)
            yy = xx * math.sin(ell_angle) + yy * math.cos(ell_angle)

            xx = xx + x
            yy = yy + y

            print(hsv[yy,xx])
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
            if (points[minval[1]][0] < 75):
                resulthash = resulthash + '1';
            else:
                resulthash = resulthash + '0';
        print('HASH=' + str(check_hash(resulthash)))


            


            




                #trg[yy,xx] = (255,255,0)


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
    cv2.imshow(filename, display)



    while(True):
        ch = cv2.waitKey(50) & 0xFF
        if ch == 27:
            break



    cv2.destroyAllWindows()
