package recipe.util;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.dictionary.DictionaryItem;
import ctd.dictionary.service.DictionaryLocalService;
import ctd.dictionary.service.DictionarySliceRecordSet;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取字典转换
 *
 * @author fuzi
 */
public class DictionaryUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryUtil.class);

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

    /**
     * 根据字典的value获取key
     *
     * @param strDic
     * @param value
     * @return
     */
    public static String getKeyByValue(String strDic, String value) {
        if (value == null || StringUtils.isEmpty(value.trim())) {
            throw new DAOException(DAOException.VALUE_NEEDED, " value is require");
        }
        value = value.trim();
        List<DictionaryItem> list = findAllItem(strDic);

        if (list.size() == 0) {
            throw new DAOException(" dicId is not exist");
        }
        for (DictionaryItem item : list) {
            if (value.equals(item.getText())) {
                return item.getKey();
            }
        }
        return null;
    }

    private static List<DictionaryItem> findAllItem(String strDic) {
        DictionaryLocalService ser = AppContextHolder.getBean("eh.dictionaryService", DictionaryLocalService.class);
        List<DictionaryItem> list = new ArrayList<DictionaryItem>();
        try {
            DictionarySliceRecordSet var = ser.getSlice(
                    strDic, "", 0, "", 0, 0);
            list = var.getItems();

        } catch (ControllerException e) {
            LOGGER.error("DictionaryUtil findAllItem", e);
        }
        return list;
    }

}
