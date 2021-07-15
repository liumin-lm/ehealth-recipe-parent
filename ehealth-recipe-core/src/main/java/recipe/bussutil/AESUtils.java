package recipe.bussutil;

import ngari.openapi.constant.Constants;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class AESUtils {

    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    public static Key toKey(byte[] key) {
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }

    public static String encrypt(String data, String key) throws Exception {
        return Base64.encodeBase64String(encrypt(data.getBytes(Constants.ENCODING), toKey(key.getBytes()), DEFAULT_CIPHER_ALGORITHM));
    }

    public static String decrypt(String data, String key) throws Exception {
        return new String(decrypt(Base64.decodeBase64(data.getBytes(Constants.ENCODING)), toKey(key.getBytes()), DEFAULT_CIPHER_ALGORITHM));
    }

    public static byte[] encrypt(byte[] data, Key key) throws Exception {
        return encrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        return encrypt(data, toKey(key), DEFAULT_CIPHER_ALGORITHM);
    }

    public static byte[] encrypt(byte[] data, Key key, String cipherAlgorithm) throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        return decrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    public static byte[] decrypt(byte[] data, Key key) throws Exception {
        return decrypt(data, key, DEFAULT_CIPHER_ALGORITHM);
    }

    public static byte[] decrypt(byte[] data, byte[] key, String cipherAlgorithm) throws Exception {
        Key k = toKey(key);
        return decrypt(data, k, cipherAlgorithm);
    }

    public static byte[] decrypt(byte[] data, Key key, String cipherAlgorithm) throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

}
