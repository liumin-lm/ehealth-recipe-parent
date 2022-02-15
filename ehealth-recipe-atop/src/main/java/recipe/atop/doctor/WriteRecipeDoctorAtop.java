package recipe.atop.doctor;

import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRevisitBusinessService;

import java.util.List;

/**
 * 开处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("writeRecipeDoctorAtop")
public class WriteRecipeDoctorAtop extends BaseAtop {

    @Autowired
    private IRevisitBusinessService iRevisitBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 获取院内门诊
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 院内门诊处方列表
     */
    @RpcService
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) {
        return iRevisitBusinessService.findWriteDrugRecipeByRevisitFromHis(mpiId, organId, doctorId);
    }

    @RpcService
    public Boolean confirmAgain(Integer recipeId) {
        return recipeBusinessService.confirmAgain(recipeId);
    }
}
