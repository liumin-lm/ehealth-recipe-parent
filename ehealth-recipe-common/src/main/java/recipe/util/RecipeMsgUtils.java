package recipe.util;

import recipe.constant.RecipeMsgEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/25
 * @description： 处方消息工具类
 * @version： 1.0
 */
public class RecipeMsgUtils {

    private static Map<Integer, RecipeMsgEnum> map = new HashMap();

    static {
        RecipeMsgEnum[] v = RecipeMsgEnum.values();
        for (RecipeMsgEnum e : v) {
            map.put(e.getStatus(), e);
        }
    }

    public static RecipeMsgEnum getEnumByStatus(int status) {
        if (map.containsKey(status)) {
            return map.get(status);
        }

        return RecipeMsgEnum.DEFAULT;
    }

    public static RecipeMsgEnum valueOf(String msgType){
        for(RecipeMsgEnum en : RecipeMsgEnum.values()){
            if(en.getMsgType().equals(msgType)){
                return en;
            }
        }

        return RecipeMsgEnum.DEFAULT;
    }
}
