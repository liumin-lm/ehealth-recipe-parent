package recipe.util;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryItem;
import ctd.dictionary.service.DictionaryLocalService;
import ctd.dictionary.service.DictionarySliceRecordSet;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName DictionaryUtil
 * @Description
 * @Author maoLy
 * @Date 2019/8/16
 **/
public class DictionaryUtil {

    private static final Logger logger = Logger.getLogger(DictionaryUtil.class);

    public static List<DictionaryItem> findAllItem(String strDic) {
        DictionaryLocalService ser = AppContextHolder.getBean("eh.dictionaryService", DictionaryLocalService.class);
        List<DictionaryItem> list = new ArrayList<DictionaryItem>();
        try {
            DictionarySliceRecordSet var = ser.getSlice(
                    strDic, "", 0, "", 0, 0);
            list = var.getItems();

        } catch (ControllerException e) {
            logger.error(e);
        }
        return list;
    }

    /**
     * 根据字典的value获取key
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
}
