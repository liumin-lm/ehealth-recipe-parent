package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @description： 患者端续方药品校验
 * @author： whf
 * @date： 2022-05-20 10:49
 */
@Getter
@Setter
@ToString
public class PatientContinueRecipeCheckDrugReq {

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "his处方单号")
    private String recipeCode;

    @ItemProperty(alias = "处方来源类型 1 平台处方 2 线下转线上的处方")
    private Integer recipeSourceType;

    @ItemProperty(alias = "购物车类型 1 线上药品-我要配药 2 线下药品-我要配药")
    private Integer shoppingCartType;

    @ItemProperty(alias = "药品信息")
    private List<PatientOptionalDrugVo> patientOptionalDrugVo;

}
