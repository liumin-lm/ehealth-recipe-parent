package recipe.bussutil.drugdisplay;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2021/3/11
 * 药品名排序器
 */
public class DrugDisplayNameSorter {

    /**
     * 根据机构配置展示的配置信息按要求排好序
     * 需求要求：(西药药品名和中药药品名)
     * //机构药品名称、机构商品名称在页面显示的前后顺序根据运营平台配置项顺序显示；
     * //剂型、药品规格、单位顺序固定显示并且在机构药品名称、机构商品名称后显示
     * 如若【西药药品名显示信息拼接配置】“机构药品名称、机构商品名称”都不配置会显示平台默认值：机构药品名称
     * 如若【西药商品名显示信息拼接配置】“机构商品名称”不配置会显示平台默认值：机构商品名称
     * <p>
     * 【西药药品名显示信息拼接配置】总共有[drugName,saleName,drugForm,drugSpec,unit]
     * 【西药商品名显示信息拼接配置】总共有[saleName,drugForm]
     *
     * @param keyMap    根据配置数组转换成的顺序关系 例如 drugName:0,saleName:1
     * @param configKey 机构配置key 药品名和商品名默认显示规则不同
     * @return
     */
    public static List<String> sortConfigName(Map<String, Integer> keyMap, String configKey) {
        return DisplayNameEnum.getDisplayObject(configKey).sortConfigName(keyMap);
    }
}
