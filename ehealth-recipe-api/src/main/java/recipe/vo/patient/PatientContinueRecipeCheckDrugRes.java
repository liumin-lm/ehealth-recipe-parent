package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @description： 患者端校验药品信息出参
 * @author： whf
 * @date： 2022-05-20 13:51
 */
@Getter
@Setter
public class PatientContinueRecipeCheckDrugRes {

    @ItemProperty(alias = "药品校验标记 1 展示 0 不展示")
    private Integer checkFlag;

    @ItemProperty(alias = "药品校验提示信息")
    private String checkText;

    @ItemProperty(alias = "药品信息")
    private List<PatientOptionalDrugVo> patientOptionalDrugVo;

}
