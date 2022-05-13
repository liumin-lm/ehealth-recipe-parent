package recipe.vo.doctor;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 前端药品信息出参
 * @author： whf
 * @date： 2022-04-27 22:52
 */
@Getter
@Setter
public class DrugsResVo implements Serializable {
    @ItemProperty(alias = "机构药品序号(自增主键)")
    private Integer organDrugId;

    @ItemProperty(alias = "医疗机构代码(organ表自增主键)")
    private Integer organId;

    @ItemProperty(alias = "平台药品编码(druglist表自增主键)")
    private Integer drugId;

    @ItemProperty(alias = "机构药品唯一索引")
    private String organDrugCode;

    @ItemProperty(alias = "机构药品编码")
    private String drugItemCode;

    @ItemProperty(alias="是否靶向药  0否  1是 ")
    private Integer targetedDrugType;

    @ItemProperty(alias = "最小销售倍数")
    private Integer smallestSaleMultiple;

    @ItemProperty(alias = "药品类型")
    @Dictionary(id = "eh.base.dictionary.DrugType")
    private Integer drugType;
}
