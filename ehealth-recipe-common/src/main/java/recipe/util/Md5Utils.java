package recipe.util;

import java.security.MessageDigest;

public class Md5Utils {
	
	/**
	 * 私有构造函数，不允许本类生成实例 
	 */
	private Md5Utils(){
	}
	
	private static final String sv = "PMP-NOW-YOU-SEE-ME.0plo98ikmju76yhnbgt54rfvcde32wsxzaq1";

	/**
	 * 用MD5算法进行加密
	 * @param pStrPW
	 * @return
	 */
	public static String crypt(String pStrPW) {
		String strCrypt = hash(pStrPW);
		if(strCrypt.length() > 0) {
			strCrypt += sv;
			strCrypt = hash(strCrypt);
		}

		return strCrypt;
	}
	
	/**
	 * MD5算法进行散列
	 * @param str
	 * @return
	 */
	public static String hash(String str) {
		String result = "";
		if (str == null || str.equals("")) { // 如果传入参数为空，则返回空字符串
			return result;
		}
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] data = str.getBytes("UTF-8");
			int l = data.length;
			for (int i = 0; i < l; i++)
				md.update(data[i]);
			byte[] digest = md.digest();
			
			result = ByteUtils.byteArrayToHexString(digest);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(),e);
		}

		return result;		
	}

}
