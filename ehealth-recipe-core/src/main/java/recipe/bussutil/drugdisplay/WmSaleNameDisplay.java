package recipe.bussutil.drugdisplay;

import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2021/3/11
 */
public class WmSaleNameDisplay extends CommonDrugNameDisplay {

    @Override
    public void sortDrugNameAndSaleName(List<String> sortList, Map<String, Integer> keyMap) {
        //若【西药商品名显示信息拼接配置】“机构商品名称”不配置会显示默认值：机构商品名称
        //剂型、药品规格、单位顺序固定显示并且在机构药品名称、机构商品名称后显示
        //==不管配没配默认第一个都是商品名
        sortList.add(SALE_NAME);
    }
}
