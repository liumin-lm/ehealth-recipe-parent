package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 患者自选药品
 * @author： whf
 * @date： 2021-11-23 16:45
 */
@Getter
@Setter
public class PatientOptionalDrugVo implements Serializable {
    @ItemProperty(alias = "药品序号")
    private Integer drugId;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)")
    private Integer clinicId;

    @ItemProperty(alias = "机构药品编号")
    private String organDrugCode;

    @ItemProperty(alias = "患者指定的药品数量")
    private Integer patientDrugNum;
}
