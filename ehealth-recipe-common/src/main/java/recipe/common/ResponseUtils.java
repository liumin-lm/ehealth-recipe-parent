package recipe.common;

import recipe.common.response.CommonResponse;

import java.lang.reflect.Method;

/**
 * @author： 0184/yu_yun
 * @date： 2018/3/13
 * @description：
 * @version： 1.0
 */
public class ResponseUtils {
    /**
     * 获取失败实例
     *
     * @param clazz
     * @param msg   错误信息
     * @param <T>
     * @return
     */
    public static <T> T getFailResponse(Class<? extends CommonResponse> clazz, String msg) {
        T object = null;
        try {
            object = (T) clazz.newInstance();
            Method method = clazz.getMethod("setCode", String.class);
            method.invoke(object, CommonConstant.FAIL);
            Method setMsg = clazz.getMethod("setMsg", String.class);
            setMsg.invoke(object, (null == msg) ? "" : msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return object;
    }
}
