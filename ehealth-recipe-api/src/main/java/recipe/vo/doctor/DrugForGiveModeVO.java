package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 医生端购药方式下展示药品
 * @author： whf
 * @date： 2021-12-07 16:27
 */
@Getter
@Setter
public class DrugForGiveModeVO implements Serializable {

    /**
     * 购药按钮key
     */
    private String giveModeKey;
    /**
     * 购药按钮key text
     */
    private String giveModeKeyText;

    /**
     * 药企名称
     */
    private String enterpriseName;

    /**
     * 药品名称
     */
    private List<String> drugsName;


}

