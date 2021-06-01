package recipe.drugsenterprise.bean.yd.utils;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA非对称加解密
 * 兼容C#、.net语言
 */
public class RSAEncryptUtils {

	private static final int MAXENCRYPTSIZE = 117;
	private static final int MAXDECRYPTSIZE = 128;

	/**
	 * 字节数据转字符串专用集合
	 */
	private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * 从文件中输入流中加载公钥
	 *
	 * @param path
	 *            公钥输入流
	 * @throws Exception
	 *             加载公钥时产生的异常
	 */
	public static RSAPublicKey loadPublicKeyByFile(String path)
			throws Exception {

		return loadPublicKeyByStr(path);

	}

	/**
	 * 从字符串中加载公钥
	 *
	 * @param publicKeyStr
	 *            公钥数据字符串
	 * @throws Exception
	 *             加载公钥时产生的异常
	 */
	public static RSAPublicKey loadPublicKeyByStr(String publicKeyStr)
			throws Exception {
		try {
			byte[] buffer = Base64.decode(publicKeyStr);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
			return (RSAPublicKey) keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此算法");
		} catch (InvalidKeySpecException e) {
			throw new Exception("公钥非法");
		} catch (NullPointerException e) {
			throw new Exception("公钥数据为空");
		}
	}

	/**
	 * 从文件中加载私钥
	 *
	 * @param path
	 *            私钥文件名
	 * @return 是否成功
	 * @throws Exception
	 */
	public static RSAPrivateKey loadPrivateKeyByFile(String path)
			throws Exception {

		return loadPrivateKeyByStr(path);
	}

	public static RSAPrivateKey loadPrivateKeyByStr(String privateKeyStr)
			throws Exception {
		try {
			byte[] buffer = Base64.decode(privateKeyStr);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此算法");
		} catch (InvalidKeySpecException e) {
			try {//pem字节流时，这样处理
				byte[] keybyte = Base64.decode(privateKeyStr);
				ASN1InputStream in=new ASN1InputStream(keybyte);
				//ASN1Primitive obj=in.readObject();
				RSAPrivateKeyStructure pStruct= RSAPrivateKeyStructure.getInstance(null);
				RSAPrivateKeySpec spec=new RSAPrivateKeySpec(pStruct.getModulus(), pStruct.getPrivateExponent());
				KeyFactory keyFactory= KeyFactory.getInstance("RSA");
				return (RSAPrivateKey) keyFactory.generatePrivate(spec);
			}catch (InvalidKeySpecException ex) {
				ex.printStackTrace();
				throw new Exception("私钥非法");
			}
		} catch (NullPointerException e) {
			throw new Exception("私钥数据为空");
		}
	}

	/**
	 * RSA 加密返回16进制编码字符串
	 * @param publicKey
	 * @param plainText
	 * @return
	 */
	public static String encryptToHexString(String publicKey, String plainText){
		try {
			PublicKey publicKey1 = loadPublicKeyByStr(publicKey);
			return encryptToHexString(publicKey1,plainText);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("字符编码异常",e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static String encryptToHexString(PublicKey publicKey, String plainText){
		try {
			return encodeHexString(encrypt(publicKey,plainText.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("字符编码异常",e);
		}
	}

	/**
	 * RSA加密过程
	 * @param plainTextData
	 * @param publicKey
	 * @return Bute[] encryptData
	 * @throws Exception
	 */
	public static byte[] encrypt(PublicKey publicKey, byte[] plainTextData){
		try {
			//此处填充方式选择部填充 NoPadding，当然模式和填充方式选择其他的，在Java端可以正确加密解密，
			//但是解密后的密文提交给C#端，解密的得到的数据将产生乱码
			Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			int length = plainTextData.length;
			int offset = 0;
			byte[] cache;
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			int i = 0;
			while (length - offset > 0) {
				if (length - offset > MAXENCRYPTSIZE) {
					cache = cipher.doFinal(plainTextData, offset, MAXENCRYPTSIZE);
				} else {
					cache = cipher.doFinal(plainTextData, offset, length - offset);
				}
				outStream.write(cache, 0, cache.length);
				i++;
				offset = i * MAXENCRYPTSIZE;
			}
			return outStream.toByteArray();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("无此加密算法",e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("非法明文参数",e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("加密公钥非法,请检查",e);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException("明文长度非法",e);
		} catch (BadPaddingException e) {
			throw new RuntimeException("明文数据已损坏",e);
		}
	}

	/**
	 * RSA解密数据
	 * @param privateKey
	 * @param hexString
	 * @return
	 */
	public static String decryptFromHexString(String privateKey, String hexString) {
		try {
			RSAPrivateKey privateKey1 = loadPrivateKeyByStr(privateKey);
			return decryptFromHexString(privateKey1,hexString);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("字符编码异常",e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static String decryptFromHexString(PrivateKey privateKey, String hexString) {
		try {
			return new String(decrypt(privateKey,decodeHex(hexString)),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("字符编码异常",e);
		}
	}

		/**
         * RSA解密过程
         * @param privateKey
         * 			私钥
         * @param encryptData
         * 			解密数据
         * @return decryptData
         */
	public static byte[] decrypt(PrivateKey privateKey, byte[] encryptData){
		//此处模式选择与加密对应，但是需要添加第二个参数new org.bouncycastle.jce.provider.BouncyCastleProvider()
		//若不添加第二个参数的话，解密后的数据前面出现大段空格符
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA/ECB/NoPadding", new org.bouncycastle.jce.provider.BouncyCastleProvider());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			int length = encryptData.length;
			int offset = 0;
			int i = 0;
			byte[] cache;
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			while (length - offset > 0) {
				if (length - offset > MAXDECRYPTSIZE) {
					cache = cipher.doFinal(encryptData, offset, MAXDECRYPTSIZE);
				} else {
					cache = cipher.doFinal(encryptData, offset, length - offset);
				}
				outStream.write(cache, 0, cache.length);
				i++;
				offset = i * MAXDECRYPTSIZE;
			}
			return outStream.toByteArray();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("无此加密算法",e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("非法明文参数",e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("加密公钥非法,请检查",e);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException("明文长度非法",e);
		} catch (BadPaddingException e) {
			throw new RuntimeException("明文数据已损坏",e);
		}
	}

	/**
	 * 加密16进制字节成字符
	 * @param data
	 * @return
	 */
	protected static String encodeHexString(byte[] data) {
		return new String(encodeHex(data));
	}

	protected static char[] encodeHex(byte[] data) {
		int l = data.length;
		char[] out = new char[l << 1];
		int i = 0;

		for(int var5 = 0; i < l; ++i) {
			out[var5++] = HEX_CHAR[(240 & data[i]) >>> 4];
			out[var5++] = HEX_CHAR[15 & data[i]];
		}
		return out;
	}

	protected static int toDigit(char ch, int index){
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Illegal hexadecimal character " + ch + " at index " + index);
		} else {
			return digit;
		}
	}

	/**
	 * 解密16进制字节码
	 * @param data
	 * @return
	 */
	public static byte[] decodeHex(String data) {
		return decodeHex(data.toCharArray());
	}
	public static byte[] decodeHex(char[] data){
		int len = data.length;
		if ((len & 1) != 0) {
			throw new IllegalArgumentException("Odd number of characters.");
		} else {
			byte[] out = new byte[len >> 1];
			int i = 0;

			for(int j = 0; j < len; ++i) {
				int f = toDigit(data[j], j) << 4;
				++j;
				f |= toDigit(data[j], j);
				++j;
				out[i] = (byte)(f & 255);
			}
			return out;
		}
	}

}
