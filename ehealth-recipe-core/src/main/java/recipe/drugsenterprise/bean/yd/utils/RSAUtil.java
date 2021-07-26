package recipe.drugsenterprise.bean.yd.utils;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;


public class RSAUtil {

	//私钥
	private static String private_key = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAK5mVgu9DtA0FGNh\n" +
			"c9TzEqT0PcGdtbic+xXAkwi65xTdUD/NFa2G5ScAEA0B5VXSzLGCqrmFIwbPzHMa\n" +
			"Ifuyp935ghzURLdy1OKFSYbNU2v4XOTmtL+mJandNfMMc5IZIb0LO8UM8Atg5OjH\n" +
			"1xmbMmZk/wRtNwl2frQPGdec3NxTAgMBAAECgYBjdfmcuDW6h/kYtHta90WqzaBq\n" +
			"y4bXwq9vuGQilnUzcQRTXqL+U/BcTazZvjsMtywGEH9NTqCrQddXzY+T/E+/VnA9\n" +
			"v+hyVyWnaMQCdAvpl7BcaSMl2DkPFbUl9fFvk+qpwDvum5HjYNN7IApWM6jCvMpD\n" +
			"Spu6jcEMTeSW71rQAQJBAOh8cfU4CLGLSwdazARWCS3oCgCU0Bg/y+SNTtYc8SuR\n" +
			"kwdjy1pko/8rq6XwtIRpowKeIblRakG2OGx3wsjZcv8CQQDACemqL2gutlBhJFn2\n" +
			"ev25nwTOdN/OH+Yy7l6oJpc/WyOVjuMuKFexIPARNH02Wlz9d630/+NOPOg+ypij\n" +
			"A9qtAkBX3oErz6vpft4yv2yQzvvVL/hn09b681Ha5lW/s1yrvO+3QU6gsZ0SWq0b\n" +
			"oOh5i3ujB6VzZ4Qjpf2ZcYJba2R9AkBMbGV6Hc2nMVTBo/bNWVrZ4QfHpclfPWCe\n" +
			"CjDPWDQ+uWVq4mdUeieTzRjcr/fYhpOVJ2iqJJ9wBlsiifu+fA0tAkEAyHerMK5+\n" +
			"X9XJGPbUNZISf1yTDMuV8xZk/eWTR81dvJqHRZFC9NOyNvZBGDnR1i/HkYH+RgJw\n" +
			"lrAg3NAHNlPtdg==";


	//appid
	private static String appid = "h0Lc8CYi";

	public static String getAppid() {
		return appid;
	}

	public static String getPrivateKey() {
		return private_key;
	}

	/**
	 * 私钥加密
	 *
	 * @param content    待加密字符串
	 * @param privateKey 私钥
	 * @return 加密后的16进制字符串
	 * @throws Exception
	 */
	public static String privateEncrypt(String content, String privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");// java默认"RSA"="RSA/ECB/PKCS1Padding"
		cipher.init(Cipher.ENCRYPT_MODE, getPrivateKey(privateKey));
		byte[] doFinal = cipher.doFinal(content.getBytes("utf-8"));
		return bytesToHexString(doFinal);
	}

	/**
	 * 将base64编码后的私钥字符串转成PrivateKey实例
	 *
	 * @param privateKey
	 * @return
	 * @throws Exception
	 */
	private static PrivateKey getPrivateKey(String privateKey) throws Exception {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] keyBytes = decoder.decodeBuffer(privateKey);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}


	/**
	 * 将byte数组转换为表示16进制值的字符串
	 *
	 * @param src
	 * @return
	 */
	private static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

}
