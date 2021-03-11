package recipe.bussutil.drugdisplay;


import org.apache.commons.lang3.StringUtils;
import recipe.util.MapValueUtil;

import java.util.List;

/**
 * created by shiyuping on 2021/3/10
 */
public class DrugDisplayNameProducer {

    public static String getDrugName(DrugDisplayNameInfo drugDisplayNameInfo) {
        StringBuilder splicedName = new StringBuilder();
        //排好序的配置name列表
        List<String> sortConfigList = DrugDisplayNameSorter.sortConfigName(drugDisplayNameInfo.getKeyMap(), drugDisplayNameInfo.getConfigKey());
        String value;
        //依次拼接
        for (String name : sortConfigList) {
            //排序中有空格要添加空格
            if (StringUtils.SPACE.equals(name)) {
                splicedName.append(StringUtils.SPACE);
            }
            //通过字段名取值
            value = MapValueUtil.getFieldValueByName(name, drugDisplayNameInfo);
            if (StringUtils.isNotEmpty(value)) {
                splicedName.append(value);
            }
        }
        return splicedName.toString();
    }
}
