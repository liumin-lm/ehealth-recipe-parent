package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 患者端提交处方
 * @author： yins
 * @date： 2022-03-28 10:31
 */
@Getter
@Setter
public class PatientSubmitRecipeVO implements Serializable {
    private static final long serialVersionUID = 2580107256079305592L;
    private List<Integer> recipeIds;
    private Integer organId;
    private String giveModeKey;
    private Integer expressFeePayType;
    private Double expressFee;
    @ItemProperty(alias = "机构ID 兼容问题 前期命名错误 后期可逐渐删除")
    private Integer orderId;
}
