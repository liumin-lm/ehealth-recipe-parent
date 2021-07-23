package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.OutRecipeDetailReqVO;
import com.ngari.recipe.vo.OutRecipeDetailVO;
import com.ngari.recipe.vo.PatientInfoVO;
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
     * @param patientInfoVO 患者信息
     * @return  诊断列表
     */
    @RpcService
    public String getOutRecipeDisease(PatientInfoVO patientInfoVO){
        logger.info("OutPatientRecipeAtop getOutRecipeDisease patientInfoVO:{}.", JSON.toJSONString(patientInfoVO));
        validateAtop(patientInfoVO, patientInfoVO.getOrganId(), patientInfoVO.getPatientName(), patientInfoVO.getPatientId(), patientInfoVO.getRegisterID());
        try {
            List<DiseaseInfoDTO> result = recipeBusinessService.getOutRecipeDisease(patientInfoVO);
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

    /**
     * 获取门诊处方详情信息
     * @param outRecipeDetailReqVO 门诊处方信息
     * @return 图片或者PDF链接等
     */
    @RpcService
    public OutRecipeDetailVO queryOutRecipeDetail(OutRecipeDetailReqVO outRecipeDetailReqVO){
        logger.info("OutPatientRecipeAtop getOutRecipeDisease queryOutRecipeDetail:{}.", JSON.toJSONString(outRecipeDetailReqVO));
        try {
            OutRecipeDetailVO result = recipeBusinessService.queryOutRecipeDetail(outRecipeDetailReqVO);
            logger.info("OutPatientRecipeAtop queryOutRecipeDetail result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop queryOutRecipeDetail error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop queryOutRecipeDetail error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
