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
    private static String PREF_NAME = "lasociale-sec.6";

    private static String S_PUB = "pub";
    private static String S_PRV = "prv";
    private static String S_HASH = "hash";


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

            byte[] hash = digest.digest(kp.getPublic().getEncoded());


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
