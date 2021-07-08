package recipe.comment;

import com.ngari.recipe.entity.RecipeOrder;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.hisservice.EleInvoiceService;

/**
 * 获取字典转换
 *
 * @author fuzi
 */
public class DictionaryUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(EleInvoiceService.class);

    /**
     * 根据key 查询字典数据 value
     *
     * @param classId
     * @param key
     * @return
     */
    public static String getDictionary(String classId, Integer key) {
        if (null == key) {
            return "";
        }
        try {
            return DictionaryController.instance().get(classId).getText(key);
        } catch (ControllerException e) {
            LOGGER.warn("DictionaryUtil DictionaryController error classId:{},key:{}", classId, key);
            return "";
        }
    }

    /**
     * 根据key 查询字典数据 value
     * value为null
     *
     * @param classId
     * @param key
     * @return
     */
    public static String getDictionary(String classId, String key) {
        if (null == key) {
            return "";
        }
        try {
            String value = DictionaryController.instance().get(classId).getText(key);
            if (StringUtils.isEmpty(value)) {
                return "";
            } else {
                return value;
            }
        } catch (ControllerException e) {
            LOGGER.warn("DictionaryUtil DictionaryController error classId:{},key:{}", classId, key);
            return "";
        }
    }

    public static String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress1()));
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress2()));
            address.append(getDictionary("eh.base.dictionary.AddrArea", order.getAddress3()));
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }
}
