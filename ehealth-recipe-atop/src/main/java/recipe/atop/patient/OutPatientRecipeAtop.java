package recipe.atop.patient;

import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IOutPatientRecipeService;
import recipe.util.ValidateUtil;

/**
 * 门诊病人处方服务
 * @author yinsheng
 * @date 2021\7\16 0016 14:04
 */
@RpcBean("outPatientRecipeAtop")
public class OutPatientRecipeAtop extends BaseAtop {

    @Autowired
    private IOutPatientRecipeService outPatientRecipeService;

    /**
     * 查询线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断列表
     */
    @RpcService
    public String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OutPatientRecipeAtop getOutRecipeDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        if (ValidateUtil.integerIsEmpty(organId) || StringUtils.isEmpty(patientName) || StringUtils.isEmpty(registerID) || StringUtils.isEmpty(patientId)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            String result = outPatientRecipeService.getOutRecipeDisease(organId, patientName, registerID, patientId);
            logger.info("OutPatientRecipeAtop getOutRecipeDisease result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop getOutRecipeDisease error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop getOutRecipeDisease error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
