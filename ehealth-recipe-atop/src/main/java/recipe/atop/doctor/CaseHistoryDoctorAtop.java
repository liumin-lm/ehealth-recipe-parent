package recipe.atop.doctor;

import com.ngari.recipe.vo.CaseHistoryVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.util.ValidateUtil;
import recipe.vo.second.MedicalDetailVO;

/**
 * 电子病历服务入口类
 *
 * @author fuzi
 */
@RpcBean("caseHistoryAtop")
public class CaseHistoryDoctorAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;
    /**
     * 获取电子病历数据
     * 新方法迁移 getDocIndexInfoV1
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    @RpcService
    @Deprecated
    public MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        validateAtop(caseHistoryVO, caseHistoryVO.getActionType());
        if (ValidateUtil.integerIsEmpty(caseHistoryVO.getClinicId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getDocIndexId())) {
            return new MedicalDetailVO();
        }
        return recipeBusinessService.getDocIndexInfo(caseHistoryVO);
    }


    /**
     * 获取医生下同一个患者 最新 复诊的id
     * todo兼容老代码 默认返回 无复诊 前端改完删除
     *
     * @param mpiId    患者id
     * @param doctorId 医生id
     * @return
     */
    @RpcService
    @Deprecated
    public Integer getRevisitId(String mpiId, Integer doctorId) {
        return 0;
    }

}
