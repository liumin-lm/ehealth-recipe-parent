package recipe.common;

/**
 * liumin
 * 20220616
 * description:文件阿波罗配置
 */
public class UrlConfig {

    public final static long VALID_TIME_SECOND = 3600 * 24 * 30;
    public static String fileViewUrl;

    public  void setFileViewUrl(String fileViewUrl) {
        UrlConfig.fileViewUrl = fileViewUrl;
    }

}
