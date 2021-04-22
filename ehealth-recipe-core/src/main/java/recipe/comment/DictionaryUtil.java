package recipe.comment;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.ErrorCode;
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
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DictionaryController is null");
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
     * value为null 返回key
     *
     * @param classId
     * @param key
     * @return
     */
    public static String getDictionary(String classId, String key) {
        if (null == key) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DictionaryController is null");
        }
        try {
            String value = DictionaryController.instance().get(classId).getText(key);
            if (StringUtils.isEmpty(value)) {
                return key;
            } else {
                return value;
            }
        } catch (ControllerException e) {
            LOGGER.warn("DictionaryUtil DictionaryController error classId:{},key:{}", classId, key);
            return "";
        }
    }
}
