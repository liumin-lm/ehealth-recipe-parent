package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Data
public class ClinicCartVO {

    @ItemProperty(alias = "主键Id")
    private Integer id;

    @ItemProperty(alias = "机构Id")
    private Integer organId;

    @ItemProperty(alias = "操作人员Id")
    private String userId;

    @ItemProperty(alias = "项目Id，处方organDrugCode, 检验检查ItemId")
    private String itemId;

    @ItemProperty(alias = "项目名称，处方：药品名称，检验检查：项目名称")
    private String itemName;

    @ItemProperty(alias = "项目详情说明")
    private String itemDetail;

    @ItemProperty(alias = "项目类型：1：药品，2：检查，3：检验")
    private Integer itemType;

    @ItemProperty(alias = "项目数量，处方为药品数量")
    private Integer amount;

    @ItemProperty(alias = "项目数量单位")
    private String unit;

    @ItemProperty(alias = "删除标识，0：正常，1：删除")
    private Integer deleteFlag;

    @ItemProperty(alias = "业务场景, 方便门诊:1, 便捷购药:2")
    private Integer workType;

    @ItemProperty(alias = "项目价格")
    private BigDecimal itemPrice;
}
