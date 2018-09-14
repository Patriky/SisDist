package Multicast;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyHandlerClass {

    public PublicKey encodeStringToKey (String keyString) {
        PublicKey publicKey = null;
        byte[] bKey = Base64.getDecoder().decode(keyString);
        EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bKey);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(encodedKeySpec);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return publicKey;
    }

    public String decodeKeyToString (PublicKey key) {
        String pbKString;
        byte[] bHandler = key.getEncoded();
        pbKString = Base64.getEncoder().encodeToString(bHandler);
        return pbKString;
    }
}
