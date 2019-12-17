package recipe.util;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * 说明：这里最好是有两套密钥对，一套是本系统的，一套是应用系统的
 *
 * @author Eden
 *
 */
public class RSAEncryptUtils {

	//私钥
	private  static String private_key = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALGcz+NKE50fJDY0D8i4ysApvv/4/wwihFvSAZzWXnUBGkLSx/mTIeV/QSHUutcmtuoZhACruKI3VB2RBXpjjvXeZF9P4FuUmuuB91A4fOW66+EpxRPq1OoZaB5B9O60Y8AZh+V+nDK+udgI4Thl77vC6dwaZvjeEp44LdQFxBzZAgMBAAECgYAF4YVYp0lC+JcAXHTxVn0QI9G5NAtt4W60g52eDdMO2Lx/3e7VKrQCn1YOwrZ1DUkdMz8VrpnsdRyJ5hViWg2PtGstI956jcXESppeCWDP+peoG/2RBjC7wK3LAVb5qwTukxDzNJfcUVtdJBUirEcJb1PyGS03HJtGEUAMdD1KAQJBAOELWz3Nwa/Bn1Dz+zHn1gmyM8llBTkbw5AWrzTngKorVREFpGPzsXSkg8vKsNtjoRWaeCOgqTm4VQj+Gkul2uECQQDKCzTJrr8kwIQRFTA/lKpOaE+/f8pMiBSshwfPPLxfzCrbQ3YCd1IxWMAhcyql83tfDK+R3w0gagPBOPZUsTj5AkAzznh3ttlCy7EQYspOB8/nNYXkdAQKzJBtqDs3U5/0DLutin34oI4WixToIkYqizn3DjNgCElMx1mUE2McTRchAkEAtS3BY44pZ/qfM3ZtssZMxkzyHoao0WJCL8hSr3sGbV13nPHs1B9N/GRavmQ4/WHO4xhMJKIBcmy++zlqY94ceQJBALZdU+2lYEROKZ7B8C5SPHYVQzAgIwgKkbxdUT1Ztv+xMIUJRa8iAjZUGIxKk53dHMfU58kP5/ah/M2TmeAlfbc=";

	//公钥
	private static String public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrfoEAF7+NkAfTqOrakgfH3u9xsaEZxJ/3QB/m3iGSDuolmSaajsBBH1AD4Op9yOhN1mE92Fx6sosBy33XGd2YVfWxSXDFTR3vPPbDJZpJgMYeZw4tz1xn6sVP/dUg28A3w4rVQ4FuYLJ2WvdfOjiiZtWghpIBynQxcHgBW61xHQIDAQAB";

	/**
	 * 字节数据转字符串专用集合
	 */
	@SuppressWarnings("unused")
	private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * 从文件中输入流中加载公钥
	 *            公钥输入流
	 * @throws Exception
	 *             加载公钥时产生的异常
	 */
	public static RSAPublicKey loadPublicKeyByFile()
			throws Exception {
		return loadPublicKeyByStr(public_key);
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
	 *            私钥文件名
	 * @return 是否成功
	 * @throws Exception
	 */
	public static RSAPrivateKey loadPrivateKeyByFile()
			throws Exception {
		return loadPrivateKeyByStr(private_key);
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
				DERObject obj = in.readObject();
				RSAPrivateKeyStructure pStruct=RSAPrivateKeyStructure.getInstance(obj);
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
	 * 公钥加密过程
	 *
	 * @param publicKey
	 *            公钥
	 * @param plainTextData
	 *            明文数据
	 * @return
	 * @throws Exception
	 *             加密过程中的异常信息
	 */
	public static byte[] encrypt(RSAPublicKey publicKey, byte[] plainTextData)
			throws Exception {
		if (publicKey == null) {
			throw new Exception("加密公钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA",
					new org.bouncycastle.jce.provider.BouncyCastleProvider());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			int blockSize = cipher.getBlockSize();// 获得加密块大小，如：加密前数据为128个byte，而key_size=1024
			// 加密块大小为127
			// byte,加密后为128个byte;因此共有2个加密块，第一个127
			// byte第二个为1个byte
			int outputSize = cipher.getOutputSize(plainTextData.length);// 获得加密块加密后块大小
			int leavedSize = plainTextData.length % blockSize;
			int blocksSize = leavedSize != 0 ? plainTextData.length / blockSize
					+ 1 : plainTextData.length / blockSize;
			byte[] raw = new byte[outputSize * blocksSize];
			int i = 0;
			while (plainTextData.length - i * blockSize > 0) {
				if (plainTextData.length - i * blockSize > blockSize)
					cipher.doFinal(plainTextData, i * blockSize, blockSize,
							raw, i * outputSize);
				else
					cipher.doFinal(plainTextData, i * blockSize,
							plainTextData.length - i * blockSize, raw, i
									* outputSize);
				// 这里面doUpdate方法不可用，查看源代码后发现每次doUpdate后并没有什么实际动作除了把byte[]放到
				// ByteArrayOutputStream中，而最后doFinal的时候才将所有的byte[]进行加密，可是到了此时加密块大小很可能已经超出了
				// OutputSize所以只好用dofinal方法。

				i++;
			}
			return raw;
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此加密算法");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			throw new Exception("加密公钥非法,请检查");
		} catch (IllegalBlockSizeException e) {
			throw new Exception("明文长度非法");
		} catch (BadPaddingException e) {
			throw new Exception("明文数据已损坏");
		}
	}

	/**
	 * 私钥解密过程
	 *
	 * @param privateKey
	 *            私钥
	 * @param cipherData
	 *            密文数据
	 * @return 明文
	 * @throws Exception
	 *             解密过程中的异常信息
	 */
	@SuppressWarnings("static-access")
	public static byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData) throws Exception {
		if (privateKey == null) {
			throw new Exception("解密私钥为空, 请设置");
		}
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("RSA", new org.bouncycastle.jce.provider.BouncyCastleProvider());
			cipher.init(cipher.DECRYPT_MODE, privateKey);
			int blockSize = cipher.getBlockSize();
			ByteArrayOutputStream bout = new ByteArrayOutputStream(64);
			int j = 0;

			while (cipherData.length - j * blockSize > 0) {
				bout.write(cipher.doFinal(cipherData, j * blockSize, blockSize));
				j++;
			}
			return bout.toByteArray();
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("无此解密算法");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			throw new Exception("解密私钥非法,请检查");
		} catch (IllegalBlockSizeException e) {
			throw new Exception("密文长度非法");
		} catch (BadPaddingException e) {
			throw new Exception("密文数据已损坏");
		}
	}

	/**
	 * 字节数据转十六进制字符串
	 *
	 * @param data
	 *            输入数据
	 * @return 十六进制内容
	 */
	public static String bytesToHexString(byte[] data) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (data == null || data.length <= 0) {
			return null;
		}
		for (int i = 0; i < data.length; i++) {
			int v = data[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * Convert hex string to byte[]
	 *
	 * @param hexString
	 *            the hex string
	 * @return byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 *
	 * @param c
	 *            char
	 * @return byte
	 */
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

}
