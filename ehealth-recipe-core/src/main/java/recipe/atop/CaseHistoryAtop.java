package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.CaseHistoryVO;
import com.ngari.revisit.common.request.ValidRevisitRequest;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.cdr.api.vo.MedicalDetailBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import recipe.constant.ErrorCode;
import recipe.service.CaseHistoryService;
import recipe.util.ValidateUtil;

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
    public MedicalDetailBean getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        logger.info("CaseHistoryAtop getDocIndexInfo caseHistoryVO = {}", JSON.toJSONString(caseHistoryVO));
        if (null == caseHistoryVO || null == caseHistoryVO.getActionType()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        if (ValidateUtil.integerIsEmpty(caseHistoryVO.getClinicId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())
                && ValidateUtil.integerIsEmpty(caseHistoryVO.getDocIndexId())) {
            return new MedicalDetailBean();
        }
        try {
            MedicalDetailBean result = caseHistoryService.getDocIndexInfo(caseHistoryVO);
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

    /**
     * 获取医生下同一个患者 最新 复诊的id
     *
     * @param mpiId    患者id
     * @param doctorId 医生id
     * @return
     */
    @RpcService
    public Integer getRevisitId(String mpiId, Integer doctorId) {
        logger.info("CaseHistoryAtop getRevisitId mpiId: {},doctorId :{}", mpiId, doctorId);
        if (StringUtils.isEmpty(mpiId) || ValidateUtil.integerIsEmpty(doctorId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        ValidRevisitRequest revisitRequest = new ValidRevisitRequest();
        revisitRequest.setMpiId(mpiId);
        revisitRequest.setDoctorID(doctorId);
        try {
            Integer revisitId = revisitService.findValidRevisitByMpiIdAndDoctorId(revisitRequest);
            logger.info("CaseHistoryAtop getRevisitId revisitIds = {}", JSON.toJSONString(revisitId));
            if (ValidateUtil.integerIsEmpty(revisitId)) {
                return 0;
            }
            return revisitId;
        } catch (DAOException e1) {
            logger.warn("CaseHistoryAtop getRevisitId DAOException", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CaseHistoryAtop getRevisitId error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
