package recipe.vo.patient;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description：
 * @author： whf
 * @date： 2022-04-11 10:26
 */
@Getter
@Setter
public class CheckAddressRes implements Serializable {

    /**
     * 配送状态
     */
    private Boolean sendFlag;

    /**
     * 可配送区域
     */
    private List<AddressAreaVo> areaList;
}
