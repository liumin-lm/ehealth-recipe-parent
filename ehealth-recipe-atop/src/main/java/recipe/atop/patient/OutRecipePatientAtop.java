package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.OutPatientRecipeReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.HisErrorCodeEnum;
import recipe.core.api.IRecipeBusinessService;

import java.util.List;

/**
 * 门诊处方服务
 * @author yinsheng
 * @date 2021\7\16 0016 14:04
 */
@RpcBean("outRecipePatientAtop")
public class OutRecipePatientAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeReqVO 患者信息
     * @return  门诊处方列表
     */
    public List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO){
        logger.info("OutPatientRecipeAtop queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        validateAtop(outPatientRecipeReqVO, outPatientRecipeReqVO.getOrganId(), outPatientRecipeReqVO.getMpiId());
        try {
            List<OutPatientRecipeVO> result = recipeBusinessService.queryOutPatientRecipe(outPatientRecipeReqVO);
            logger.info("OutPatientRecipeAtop queryOutPatientRecipe result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop queryOutPatientRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop queryOutPatientRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断列表
     */
    @RpcService
    public String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OutPatientRecipeAtop getOutRecipeDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        validateAtop(organId, patientName, registerID, patientId);
        try {
            List<DiseaseInfoDTO> result = recipeBusinessService.getOutRecipeDisease(organId, patientName, registerID, patientId);
            final StringBuilder diseaseNames = new StringBuilder();
            result.forEach(diseaseInfoDTO -> diseaseNames.append(diseaseInfoDTO.getDiseaseName()).append(";"));
            if (StringUtils.isNotEmpty(diseaseNames)) {
                diseaseNames.deleteCharAt(diseaseNames.lastIndexOf(";"));
            }
            logger.info("OutPatientRecipeAtop getOutRecipeDisease diseaseNames = {}", diseaseNames);
            return diseaseNames.toString();
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop getOutRecipeDisease error", e1);
            if (HisErrorCodeEnum.HIS_PARAMETER_ERROR.getCode() == e1.getCode()) {
                return "";
            }
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop getOutRecipeDisease error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
