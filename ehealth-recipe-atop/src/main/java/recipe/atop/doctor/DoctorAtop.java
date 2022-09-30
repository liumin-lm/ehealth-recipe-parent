package recipe.atop.doctor;

import com.ngari.recipe.entity.DoctorDefault;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.doctor.IDoctorBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.DoctorDefaultVO;

import java.util.List;

/**
 * 医生服务入口类
 * @author fuzi
 */
@RpcBean(value = "doctorAtop")
public class DoctorAtop extends BaseAtop {
    @Autowired
    private IDoctorBusinessService iDoctorBusinessService;

    /**
     * 保存医生 默认数据
     * @param  list 默认数据
     */
    @RpcService
    public boolean saveBatchDoctorDefault(List<DoctorDefaultVO> list) {
        list.forEach(a->iDoctorBusinessService.saveDoctorDefault(a));
        return true;
    }

    /**
     * 获取医生 默认数据
     * @param doctorDefault
     * @return 医生默认数据
     */
    @RpcService
    public List<DoctorDefaultVO> doctorDefaultList(DoctorDefaultVO doctorDefault){
        validateAtop(doctorDefault, doctorDefault.getOrganId(),doctorDefault.getDoctorId(),doctorDefault.getCategory());
        List<DoctorDefault> result = iDoctorBusinessService.doctorDefaultList(doctorDefault);
        return ObjectCopyUtils.convert(result, DoctorDefaultVO.class);
    }

}
