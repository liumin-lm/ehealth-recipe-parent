package recipe.vo.patient;

import lombok.Getter;
import lombok.Setter;

/**
 * @description：
 * @author： whf
 * @date： 2023-02-23 14:47
 */
@Getter
@Setter
public class FindOrganDrugsSaleConfigResVo {

    /**
     * 机构id
     */
    private Integer organId;
    /**
     * 药企id
     */
    private Integer drugsEnterpriseId;
    /**
     * 购药方式 GiveModeEnum
     */
    private Integer giveMode;
}
