package recipe.status.factory.givemodefactory.impl;

import com.ngari.recipe.vo.UpdateOrderStatusVO;
import recipe.status.factory.constant.GiveModeEnum;

/**
 * 医院取药
 *
 * @author fuzi
 */
public class HospitalDrugImp extends AbstractGiveMode {
    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {

    }
}
