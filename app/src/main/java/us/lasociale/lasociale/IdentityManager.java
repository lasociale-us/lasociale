package us.lasociale.lasociale;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.spec.ECGenParameterSpec;

/**
 * Created by tomas on 22-3-16.
 */
public class IdentityManager {
    private static String PREF_NAME = "lasociale-sec.12";

    private static String S_PUB = "pub";
    private static String S_PRV = "prv";
    private static String S_HASH = "hash";


    public static boolean IsNonce(String hash) {
        return hash.substring(12,14).equals("ba");
    }

    public static boolean IsHash(String hash) {
        return hash.substring(12,14).equals("98");
    }

    public static String GetPublicKey(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);

        String key = settings.getString(S_HASH, "");

        if (key.equals(""))
        {
            GenerateKeys(context);
            key = settings.getString(S_HASH, "");
        }

        return key;

    }

    public static void SetChecksum(byte[] bytes)
    {
        byte[] cs = GetChecksum(bytes);

        bytes[8] = cs[0];
        bytes[9] = cs[1];

    }

    public static boolean CheckChecksum(byte[] bytes)
    {
        byte[] cs = GetChecksum(bytes);
        return cs[0] == bytes[8] && cs[1] == bytes[9];

    }

    public static byte[] GetChecksum(byte[] bytes)
    {
        long n1 = 0;
        long n2;
        int[] mult = new int[] { 17, 73, 14, 2, 27, 99, 101, 7};

        for(int n =0; n < 8; n++)
            n1 = n1 + ((bytes[n]&0xff) + ((bytes[n]&0xff) * mult[n])) & 0xFFFF;

        return new byte[] { (byte)(n1 & 0xFF), (byte)((n1>>8)&0xFF)};

    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private static void GenerateKeys(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();

        try {


            //ECGenParameterSpec ecParamSpec = new ECGenParameterSpec("secp224k1");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            //kpg.initialize(ecParamSpec);
            KeyPair kp = kpg.generateKeyPair();

            MessageDigest digest=null;
            digest = MessageDigest.getInstance("SHA-256");

            byte[] id = new byte[10];
            byte[] hash = digest.digest(kp.getPublic().getEncoded());

            // 3 reserved bytes
            for(int n=0; n<10; n++) {
                if (hash[n] == (byte)0x98 || hash[n] == (byte)0xBA || hash[n] == (byte)0xDC)
                    hash[n]= (byte)0xFE;
            }

            hash[6] = (byte) 0x98; // identifier
            SetChecksum(hash);

            if (!CheckChecksum(hash))
            {
                throw new RuntimeException("Checksum FAILS!");
            }

            editor.putString(S_PUB, Base64.encodeToString(kp.getPublic().getEncoded(), Base64.DEFAULT));
            editor.putString(S_HASH, toHexString(hash).trim().substring(0,20));
            editor.putString(S_PRV, Base64.encodeToString(kp.getPrivate().getEncoded(), Base64.DEFAULT));
            editor.commit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

    }
}
