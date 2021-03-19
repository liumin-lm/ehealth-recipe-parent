package recipe.bussutil.drugdisplay;

import ctd.schema.annotation.ItemProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * created by shiyuping on 2021/3/10
 */
@Data
@Builder
public class DrugDisplayNameInfo {

    @ItemProperty(alias = "通用名")
    private String drugName;

    @ItemProperty(alias = "商品名")
    private String saleName;

    @ItemProperty(alias = "药品规格")
    private String drugSpec;

    @ItemProperty(alias = "药品包装单位")
    private String unit;

    @ItemProperty(alias = "剂型")
    private String drugForm;

    @ItemProperty(alias = "机构配置key")
    private String configKey;

    @ItemProperty(alias = "配置信息排序")
    private Map<String, Integer> keyMap;
}
