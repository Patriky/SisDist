package Multicast;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyHandlerClass {

    public PublicKey encodeStringToKey(String keyString) {
        byte[] bKey = Base64.getDecoder().decode(keyString);
        EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bKey);
        PublicKey publicKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(encodedKeySpec);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return publicKey;
    }

    public String decodeKeyToString(PublicKey key) {
        byte[] bHandler = key.getEncoded();
        String pbKString = Base64.getEncoder().encodeToString(bHandler);
        return pbKString;
    }

    public byte[] sign(Signature signer, String content) {
        try {
            signer.update(content.getBytes());
            byte[] signature = signer.sign();
            return signature;
        } catch (SignatureException e) {
            System.out.println(e.toString());
        }
        return null;
    }
}
