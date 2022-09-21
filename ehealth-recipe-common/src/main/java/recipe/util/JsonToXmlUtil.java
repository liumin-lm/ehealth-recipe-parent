package recipe.util;

import java.util.Map;

/**
 * @Description EbsRemoteService中搬运出来
 * @Author yzl
 * @Date 2022-09-21
 */
public class JsonToXmlUtil {

    public static String jsonToXml(Map<String, Object> params) {
        StringBuilder result = new StringBuilder("<root><body><params>");
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</").append(entry.getKey()).append(">");
            }
        }
        result.append("</params></body></root>");
        return result.toString();
    }
}
