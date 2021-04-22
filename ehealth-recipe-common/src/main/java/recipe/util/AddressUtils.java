package recipe.util;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yinsheng
 * @date 2021\4\13 0013 17:13
 */
public class AddressUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddressUtils.class);

    public static String getAddressDic(String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                return DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area);
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
        return "";
    }
}
