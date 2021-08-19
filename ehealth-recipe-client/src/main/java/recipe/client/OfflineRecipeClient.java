package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OffLineRecipeDetailDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * his处方 交互处理类
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {
    /**
     * @param organId   机构id
     * @param doctorDTO 医生信息
     * @return 线下常用方对象
     */
    public List<CommonDTO> offlineCommonRecipe(Integer organId, DoctorDTO doctorDTO) {
        logger.info("OfflineRecipeClient offlineCommonRecipe organId:{}，doctorDTO:{}", organId, JSON.toJSONString(doctorDTO));
        OfflineCommonRecipeRequestTO request = new OfflineCommonRecipeRequestTO();
        request.setOrganId(organId);
        request.setDoctorId(doctorDTO.getDoctorId());
        request.setJobNumber(doctorDTO.getJobNumber());
        request.setName(doctorDTO.getName());
        try {
            HisResponseTO<List<CommonDTO>> hisResponse = recipeHisService.offlineCommonRecipe(request);
            return getResponse(hisResponse);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断列表
     */
    public List<DiseaseInfoDTO> queryPatientDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OfflineRecipeClient queryPatientDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        try{
            PatientDiseaseInfoTO patientDiseaseInfoTO = new PatientDiseaseInfoTO();
            patientDiseaseInfoTO.setOrganId(organId);
            patientDiseaseInfoTO.setPatientName(patientName);
            patientDiseaseInfoTO.setRegisterID(registerID);
            patientDiseaseInfoTO.setPatientId(patientId);
            HisResponseTO<List<DiseaseInfo>>  hisResponse = recipeHisService.queryDiseaseInfo(patientDiseaseInfoTO);
            List<DiseaseInfo> result = getResponse(hisResponse);
            return ObjectCopyUtils.convert(result, DiseaseInfoDTO.class);
        } catch (Exception e){
            logger.error("OfflineRecipeClient queryPatientDisease hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询门诊处方
     * @param outPatientRecipeReq 患者信息
     * @return 门诊处方列表
     */
    public List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReq outPatientRecipeReq){
        logger.info("OfflineRecipeClient queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReq));
        try {
            HisResponseTO<List<OutPatientRecipeTO>> hisResponse = recipeHisService.queryOutPatientRecipe(outPatientRecipeReq);
            List<OutPatientRecipeTO> result = getResponse(hisResponse);
            logger.info("OfflineRecipeClient queryOutPatientRecipe result:{}.", JSON.toJSONString(result));
            return ObjectCopyUtils.convert(result, OutPatientRecipeDTO.class);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient queryOutPatientRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     *
     * @param outRecipeDetailReq 门诊处方明细入参
     * @return 门诊处方明细信息
     */
    public OutRecipeDetailDTO queryOutRecipeDetail(OutRecipeDetailReq outRecipeDetailReq) {
        logger.info("OfflineRecipeClient queryOutPatientRecipe queryOutRecipeDetail:{}.", JSON.toJSONString(outRecipeDetailReq));
        try {
            HisResponseTO<OutRecipeDetailTO> hisResponse = recipeHisService.queryOutRecipeDetail(outRecipeDetailReq);
            OutRecipeDetailTO result = getResponse(hisResponse);
            return ObjectCopyUtils.convert(result, OutRecipeDetailDTO.class);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient queryOutPatientRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @return
     * @Author liumin
     * @Desciption 从 his查询待缴费已缴费的处方信息
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        logger.info("OfflineRecipeClient queryData param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}",organId,JSONUtils.toString(patientDTO),timeQuantum,flag,recipeCode);
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setBirthday(patientDTO.getBirthday());
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientSex(patientDTO.getPatientSex());
        patientBaseInfo.setMobile(patientDTO.getMobile());
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        patientBaseInfo.setCardID(patientDTO.getCardId());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());

        QueryRecipeRequestTO queryRecipeRequestTo = new QueryRecipeRequestTO();
        queryRecipeRequestTo.setPatientInfo(patientBaseInfo);
        if (timeQuantum != null) {
            //根据flag转化日期 1 代表一个月  3 代表三个月 6 代表6个月
            queryRecipeRequestTo.setStartDate(DateConversion.getMonthsAgo(timeQuantum));
        }
        queryRecipeRequestTo.setEndDate(new Date());
        queryRecipeRequestTo.setOrgan(organId);
        queryRecipeRequestTo.setQueryType(flag);
        if (StringUtils.isNotEmpty(recipeCode)) {
            queryRecipeRequestTo.setRecipeCode(recipeCode);
        }
        IRecipeHisService recipeHisService = AppContextHolder.getBean("his.iRecipeHisService", IRecipeHisService.class);
        logger.info("queryHisRecipeInfo input:" + JSONUtils.toString(queryRecipeRequestTo, QueryRecipeRequestTO.class));
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = recipeHisService.queryHisRecipeInfo(queryRecipeRequestTo);
        logger.info("queryHisRecipeInfo output:" + JSONUtils.toString(responseTo, HisResponseTO.class));

        return responseTo;
    }

    /**
     * 获取线下处方的发药流水号
     * @param patientName  患者姓名
     * @param patientId    患者病历号
     * @return 发药流水号
     */
    public String queryRecipeSerialNumber(Integer organId, String patientName, String patientId, String registerID){
        try {
            PatientDiseaseInfoTO patientDiseaseInfoTO = new PatientDiseaseInfoTO();
            patientDiseaseInfoTO.setOrganId(organId);
            patientDiseaseInfoTO.setPatientId(patientId);
            patientDiseaseInfoTO.setPatientName(patientName);
            patientDiseaseInfoTO.setRegisterID(registerID);
            logger.info("OfflineRecipeClient queryRecipeSerialNumber patientDiseaseInfoTO:{}.", JSON.toJSONString(patientDiseaseInfoTO));
            HisResponseTO response = recipeHisService.queryRecipeSerialNumber(patientDiseaseInfoTO);
            return getResponse(response).toString();
        } catch (Exception e) {
            logger.error("OfflineRecipeClient queryRecipeSerialNumber hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }

    }

    /**
     * 查询线下处方数据
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @param recipeCode
     * @return
     */
    public QueryHisRecipResTO queryOffLineRecipeDetail(OffLineRecipeDetailDTO offLineRecipeDetailDTO,Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        logger.info("HisRecipeManager queryOffLineRecipeDetail param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}", organId, JSONUtils.toString(patientDTO), timeQuantum, flag, recipeCode);
        List<QueryHisRecipResTO> response = null;
        HisResponseTO<List<QueryHisRecipResTO>> responseTo = null;
        try {
            responseTo = queryData(organId, patientDTO, timeQuantum, flag, recipeCode);
            //过滤数据
            HisResponseTO<List<QueryHisRecipResTO>> res = filterData(responseTo, recipeCode, flag);
            response = getResponse(res);
            if (ObjectUtils.isEmpty(response)){
                throw new DAOException(ErrorCode.SERVICE_ERROR,"His查询结果为空");
            }
        } catch (Exception e) {
            logger.error("HisRecipeManager queryOffLineRecipeDetail error",e);
            throw new DAOException(ErrorCode.SERVICE_ERROR,e.getMessage());
        }

        List<QueryHisRecipResTO> data = responseTo.getData();
        QueryHisRecipResTO queryHisRecipResTO = data.get(0);

        offLineRecipeDetailDTO.setDocIndexId(null);

        if (!ObjectUtils.isEmpty(queryHisRecipResTO)) {
            BeanUtils.copy(queryHisRecipResTO,offLineRecipeDetailDTO);
            offLineRecipeDetailDTO.setOrganDiseaseName(queryHisRecipResTO.getDiseaseName());
            offLineRecipeDetailDTO.setChronicDiseaseName(queryHisRecipResTO.getChronicDiseaseName());
            offLineRecipeDetailDTO.setCheckerName(queryHisRecipResTO.getCheckerName());
            //根据枚举设置处方类型
            Integer recipeType = queryHisRecipResTO.getRecipeType();
            String recipeTypeText = RecipeTypeEnum.getRecipeType(recipeType);
            offLineRecipeDetailDTO.setRecipeTypeText(recipeTypeText);
            //判断是否为医保处方
            Integer medicalType = queryHisRecipResTO.getMedicalType();
            if (!ObjectUtils.isEmpty(medicalType)&&medicalType.equals(2)){
                offLineRecipeDetailDTO.setMedicalTypeText("普通医保");
            }else if (!ObjectUtils.isEmpty(medicalType)&&medicalType.equals(1)){
                offLineRecipeDetailDTO.setMedicalTypeText("患者自费");
            }
        }

        return data.get(0);
    }

    /**
     * @param responseTo
     * @param flag
     * @return
     * @author liumin
     * @Description 数据过滤
     */
    private HisResponseTO<List<QueryHisRecipResTO>> filterData(HisResponseTO<List<QueryHisRecipResTO>> responseTo, String recipeCode, Integer flag) {
        logger.info("HisRecipeManager filterData responseTo:{},recipeCode:{}", JSONUtils.toString(responseTo), recipeCode);
        if (responseTo == null) {
            return responseTo;
        }
        List<QueryHisRecipResTO> queryHisRecipResTos = responseTo.getData();
        List<QueryHisRecipResTO> queryHisRecipResToFilters = new ArrayList<>();
        //获取详情时防止前置机没过滤数据，做过滤处理
        if (responseTo != null && recipeCode != null) {
            logger.info("HisRecipeManager queryHisRecipeInfo recipeCode:{}", recipeCode);
            //详情
            if (!CollectionUtils.isEmpty(queryHisRecipResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResTos) {
                    if (recipeCode.equals(queryHisRecipResTo.getRecipeCode())) {
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
                        continue;
                    }
                }
            }
            responseTo.setData(queryHisRecipResToFilters);
        }
        //列表
        if (responseTo != null && recipeCode == null) {
            //对状态过滤(1、测试桩会返回所有数据，不好测试，对测试造成干扰 2、也可以做容错处理)
            if (!CollectionUtils.isEmpty(queryHisRecipResTos)) {
                for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResTos) {
                    if (flag.equals(queryHisRecipResTo.getStatus())) {
                        queryHisRecipResToFilters.add(queryHisRecipResTo);
                    }
                }
            }
            responseTo.setData(queryHisRecipResToFilters);
        }
        logger.info("HisRecipeManager filterData:{}.", JSONUtils.toString(responseTo));
        return responseTo;
    }



}
