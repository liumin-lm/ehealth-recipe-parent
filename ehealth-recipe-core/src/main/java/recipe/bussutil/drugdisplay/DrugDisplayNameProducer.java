package recipe.bussutil.drugdisplay;


import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import org.apache.commons.lang3.StringUtils;
import recipe.util.MapValueUtil;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2021/3/10
 */
public class DrugDisplayNameProducer {

    public static final String ENGLISH_REG = "[a-zA-Z]+";

    /*public static String getDrugName(DrugDisplayNameInfo drugDisplayNameInfo) {
        return getDrugName(drugDisplayNameInfo, drugDisplayNameInfo.getKeyMap(), drugDisplayNameInfo.getConfigKey());
    }*/

    /**
     * 获取拼接药品名称
     *
     * @param drugInfoObject 药品对象
     * @param keyMap         根据机构配置value转换的map--用来比较顺序用
     * @param configKey      机构配置key
     * @return
     */
    public static String getDrugName(Object drugInfoObject, Map<String, Integer> keyMap, String configKey) {
        if (keyMap == null || StringUtils.isEmpty(configKey)) {
            return "";
        }
        StringBuilder splicedName = new StringBuilder();
        //排好序的配置name列表
        List<String> sortConfigList = DrugDisplayNameSorter.sortConfigName(keyMap, configKey);
        String value;
        //依次拼接
        for (String name : sortConfigList) {
            //是否是字段名
            if (matchEnglishName(name)) {
                //常用药或者处方明细的单位是drugUnit 需要特殊处理下
                if (((drugInfoObject instanceof CommonRecipeDrugDTO) || (drugInfoObject instanceof RecipeDetailBean)) && "unit".equals(name)) {
                    name = "drugUnit";
                }
                //通过字段名取值
                value = MapValueUtil.getFieldValueByName(name, drugInfoObject);
                if (StringUtils.isNotEmpty(value)) {
                    splicedName.append(value);
                }
            } else {
                //排序中有空格要添加空格 有/添加/
                splicedName.append(name);
            }
        }
        return splicedName.toString().replace(StringUtils.SPACE + "/",StringUtils.SPACE);
    }

    public static boolean matchEnglishName(String name) {
        return name.matches(ENGLISH_REG);
    }
}
