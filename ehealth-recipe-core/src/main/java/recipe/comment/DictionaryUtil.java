package recipe.comment;

import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
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

    public static String getDictionary(String classId, Integer key) {
        try {
            return DictionaryController.instance().get(classId).getText(key);
        } catch (ControllerException e) {
            LOGGER.warn("DictionaryUtil DictionaryController error classId:{},key:{}", classId, key);
            return "";
        }
    }
}
