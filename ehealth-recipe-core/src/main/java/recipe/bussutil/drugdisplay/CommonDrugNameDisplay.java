package recipe.bussutil.drugdisplay;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2021/3/11
 */
public class CommonDrugNameDisplay implements IDrugNameDisplay {
    private final static String DRUG_NAME = "drugName";
    protected final static String SALE_NAME = "saleName";
    private final static String DRUG_FORM = "drugForm";
    private final static String DRUG_SPEC = "drugSpec";
    private final static String UNIT = "unit";

    @Override
    public List<String> sortConfigName(Map<String, Integer> keyMap) {
        List<String> sortList = Lists.newArrayList();
        //机构药品名称、机构商品名称在页面显示的前后顺序根据运营平台配置项顺序显示；
        this.sortDrugNameAndSaleName(sortList, keyMap);
        //剂型、药品规格、单位顺序固定显示并且在机构药品名称、机构商品名称后显示
        //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
        if (keyMap.containsKey(DRUG_FORM)) {
            sortList.add(DRUG_FORM);
        }
        sortList.add(StringUtils.SPACE);
        if (keyMap.containsKey(DRUG_SPEC)) {
            sortList.add(DRUG_SPEC);
        }
        //【“药品规格”、“单位”】中间要加/
        if (keyMap.containsKey(DRUG_SPEC) && keyMap.containsKey(UNIT)) {
            sortList.add("/");
        }
        if (keyMap.containsKey(UNIT)) {
            sortList.add(UNIT);
        }

        return sortList;
    }

    protected void sortDrugNameAndSaleName(List<String> sortList, Map<String, Integer> keyMap) {
        //药品名、商品名都有
        if (keyMap.containsKey(DRUG_NAME) && keyMap.containsKey(SALE_NAME)) {
            //排序 越小说明越靠前
            //药品名与商品名之间要有个空格
            if (keyMap.get(DRUG_NAME) < keyMap.get(SALE_NAME)) {
                sortList.add(DRUG_NAME);
                sortList.add(StringUtils.SPACE);
                sortList.add(SALE_NAME);
            } else {
                sortList.add(SALE_NAME);
                sortList.add(StringUtils.SPACE);
                sortList.add(DRUG_NAME);
            }
        }
        //只有药品名
        if (keyMap.containsKey(DRUG_NAME) && !keyMap.containsKey(SALE_NAME)) {
            sortList.add(DRUG_NAME);
        }
        //只有商品名
        if (!keyMap.containsKey(DRUG_NAME) && keyMap.containsKey(SALE_NAME)) {
            sortList.add(SALE_NAME);
        }
        //两个名称都没有
        //机构药品名称、机构商品名称”都不配置会显示平台默认值：机构药品名称
        if (!keyMap.containsKey(DRUG_NAME) && !keyMap.containsKey(SALE_NAME)) {
            sortList.add(DRUG_NAME);
        }
    }
}
