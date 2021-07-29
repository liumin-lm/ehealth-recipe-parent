package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.OutPatientReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeBusinessService;

/**
 * @author yinsheng
 * @date 2021\7\29 0029 19:57
 */
@RpcBean(value = "patientAtop", mvc_authentication = false)
public class PatientAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 校验当前就诊人是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 是否有效
     */
    @RpcService
    public Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO){
        logger.info("PatientInfoAtop checkCurrentPatient outPatientReqVO:{}.", JSON.toJSONString(outPatientReqVO));
        validateAtop(outPatientReqVO, outPatientReqVO.getMpiId());
        try {
            Integer result = recipeBusinessService.checkCurrentPatient(outPatientReqVO);
            logger.info("PatientInfoAtop checkCurrentPatient result:{}.", result);
            return result;
        } catch (DAOException e1) {
            logger.error("PatientInfoAtop checkCurrentPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("PatientInfoAtop checkCurrentPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
