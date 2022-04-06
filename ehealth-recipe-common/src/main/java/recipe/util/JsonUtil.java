package recipe.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by yejunjie on 2018/2/26 0026.
 */
public class JsonUtil {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static ObjectMapper getInstance() {
        return mapper;
    }

    /**
     * 转换为 JSON 字符串
     *
     * @param o
     * @return
     * @throws Exception
     */
    public static String toString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception var2) {
            throw new IllegalStateException(var2);
        }
    }
}
