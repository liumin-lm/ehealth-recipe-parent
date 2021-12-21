package recipe.vo.doctor;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 购药方式对应的药品出参
 * @author： whf
 * @date： 2021-12-21 15:31
 */
@Data
public class DrugForGiveModeListVO implements Serializable {
    /**
     * 购药方式key
     */
    private String supportKey;
    /**
     * 购药方式text
     */
    private String supportKeyText;
    /**
     * 对应的药企药品信息
     */
    List<DrugForGiveModeVO> drugForGiveModeVOS;
}
