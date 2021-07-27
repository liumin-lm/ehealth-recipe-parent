package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.HisErrorCodeEnum;
import recipe.core.api.IRecipeBusinessService;
import recipe.util.DateConversion;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 门诊处方服务
 * @author yinsheng
 * @date 2021\7\16 0016 14:04
 */
@RpcBean(value = "outRecipePatientAtop", mvc_authentication = false)
public class OutRecipePatientAtop extends BaseAtop {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeReqVO 患者信息
     * @return  门诊处方列表
     */
    @RpcService
    public List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO){
        logger.info("OutPatientRecipeAtop queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        validateAtop(outPatientRecipeReqVO, outPatientRecipeReqVO.getOrganId(), outPatientRecipeReqVO.getMpiId());
        try {
            //设置默认查询时间3个月
            outPatientRecipeReqVO.setBeginTime(DateConversion.getDateFormatter(DateConversion.getMonthsAgo(3), DateConversion.DEFAULT_DATE_TIME));
            outPatientRecipeReqVO.setEndTime(DateConversion.getDateFormatter(new Date(), DateConversion.DEFAULT_DATE_TIME));
            List<OutPatientRecipeVO> result = recipeBusinessService.queryOutPatientRecipe(outPatientRecipeReqVO);
            result.forEach(outPatientRecipeVO -> {
                outPatientRecipeVO.setStatusText(OutRecipeStatusEnum.getName(outPatientRecipeVO.getStatus()));
                outPatientRecipeVO.setGiveModeText(OutRecipeGiveModeEnum.getName(outPatientRecipeVO.getGiveMode()));
                outPatientRecipeVO.setOrganId(outPatientRecipeReqVO.getOrganId());
            });
            result = result.stream().sorted(Comparator.comparing(OutPatientRecipeVO::getCreateDate).reversed()).collect(Collectors.toList());
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

    /**
     * 前端获取用药指导
     * @param medicationGuidanceReqVO 用药指导入参
     * @return 用药指导出参
     */
    @RpcService
    public MedicationGuideResVO getMedicationGuide(MedicationGuidanceReqVO medicationGuidanceReqVO){
        logger.info("OutPatientRecipeAtop getMedicationGuide medicationGuidanceReqVO:{}.", JSON.toJSONString(medicationGuidanceReqVO));
        try {
            MedicationGuideResVO result = recipeBusinessService.getMedicationGuide(medicationGuidanceReqVO);
            logger.info("OutPatientRecipeAtop getMedicationGuide result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop getMedicationGuide error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop getMedicationGuide error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 校验当前就诊人是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 是否有效
     */
    @RpcService
    public boolean checkCurrentPatient(OutPatientReqVO outPatientReqVO){
        logger.info("OutPatientRecipeAtop checkCurrentPatient outPatientReqVO:{}.", JSON.toJSONString(outPatientReqVO));
        validateAtop(outPatientReqVO, outPatientReqVO.getMpiId());
        try {
            return  recipeBusinessService.checkCurrentPatient(outPatientReqVO);
        } catch (DAOException e1) {
            logger.error("OutPatientRecipeAtop checkCurrentPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OutPatientRecipeAtop checkCurrentPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

}
