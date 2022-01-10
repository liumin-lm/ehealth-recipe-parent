package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.CaseHistoryVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
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
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    @RpcService
    public MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        logger.info("CaseHistoryAtop getDocIndexInfo caseHistoryVO = {}", JSON.toJSONString(caseHistoryVO));
        validateAtop(caseHistoryVO, caseHistoryVO.getActionType());
        if (ValidateUtil.integerIsEmpty(caseHistoryVO.getClinicId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getDocIndexId())) {
            return new MedicalDetailVO();
        }
        try {
            MedicalDetailVO result = recipeBusinessService.getDocIndexInfo(caseHistoryVO);
            logger.info("CaseHistoryAtop getDocIndexInfo result = {}", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("CaseHistoryAtop getDocIndexInfo DAOException", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CaseHistoryAtop getDocIndexInfo error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
