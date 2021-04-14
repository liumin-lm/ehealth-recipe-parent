package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.CaseHistoryVO;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeSystemConstant;
import recipe.service.CaseHistoryService;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 电子病历服务入口类
 *
 * @author fuzi
 */
@RpcBean("caseHistoryAtop")
public class CaseHistoryAtop extends BaseAtop {


    @Autowired
    private CaseHistoryService caseHistoryService;
    @Autowired
    private IRevisitService revisitService;

    /**
     * 获取电子病历数据
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    @RpcService
    public String getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        logger.info("CaseHistoryAtop getDocIndexInfo caseHistoryVO {}", JSON.toJSONString(caseHistoryVO));
        if (null == caseHistoryVO || null == caseHistoryVO.getActionType()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        if (ValidateUtil.integerIsEmpty(caseHistoryVO.getClinicId(), caseHistoryVO.getRecipeId(), caseHistoryVO.getDocIndexId())) {
            return null;
        }
        try {
            String result = caseHistoryService.getDocIndexInfo(caseHistoryVO);
            logger.info("CaseHistoryAtop getDocIndexInfo result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.warn("CaseHistoryAtop getDocIndexInfo DAOException", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CaseHistoryAtop getDocIndexInfo error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取医生下同一个患者 最新 复诊的id
     *
     * @param mpiId    患者id
     * @param doctorId 医生id
     * @return
     */
    @RpcService
    public Integer getRevisitId(String mpiId, Integer doctorId) {
        logger.info("CaseHistoryAtop getRevisitId mpiId: {},mpiId :{}", mpiId, doctorId);
        if (StringUtils.isEmpty(mpiId) || ValidateUtil.integerIsEmpty(doctorId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            List<Integer> revisitIds = revisitService.findApplyingConsultByRequestMpiAndDoctorId(mpiId, doctorId, RecipeSystemConstant.CONSULT_TYPE_RECIPE);
            logger.info("CaseHistoryAtop getRevisitId revisitIds = {}", JSON.toJSONString(revisitIds));
            if (CollectionUtils.isEmpty(revisitIds)) {
                return null;
            }
            return revisitIds.get(0);
        } catch (DAOException e1) {
            logger.warn("CaseHistoryAtop getRevisitId DAOException", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CaseHistoryAtop getRevisitId error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
