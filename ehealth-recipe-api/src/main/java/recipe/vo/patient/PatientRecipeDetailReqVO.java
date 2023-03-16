package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @description：患者端 处方详情入参
 * @author： whf
 * @date： 2023-03-16 9:51
 */
@Data
public class PatientRecipeDetailReqVO implements Serializable {
    private static final long serialVersionUID = 5540590420559633388L;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "处方号码，处方回写")
    private String recipeCode;

    @ItemProperty(alias = "处方业务查询来源 1 线上  2 线下 3 院内门诊")
    private Integer recipeBusType;

}
