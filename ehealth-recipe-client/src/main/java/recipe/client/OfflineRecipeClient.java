package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.EmrDetailValueDTO;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AppointDepartService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.RecipeBean;
import com.ngari.platform.recipe.mode.RecipeDetailBean;
import com.ngari.platform.recipe.mode.RecipeExtendBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * his处方 交互处理类
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {

    @Autowired
    private EmploymentService employmentService;
    @Autowired
    private AppointDepartService appointDepartService;

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
     * 推送处方 ，诊疗处方出参处理
     *
     * @param pushType      推送类型: 1：提交处方，2:撤销处方
     * @param recipePdfDTO  处方明细
     * @param emrDetail     电子病历
     * @param pharmacyIdMap 药房
     * @return
     * @throws Exception
     */
    public RecipeInfoDTO pushTherapyRecipe(Integer pushType, RecipeInfoDTO recipePdfDTO, EmrDetailDTO emrDetail, Map<Integer, PharmacyTcm> pharmacyIdMap) throws Exception {
        com.ngari.platform.recipe.mode.RecipeDTO hisResponseData = pushRecipe(pushType, recipePdfDTO, emrDetail, pharmacyIdMap);
        RecipeInfoDTO recipeInfoDTO = new RecipeInfoDTO();
        recipeInfoDTO.setRecipeTherapy(ObjectCopyUtils.convert(hisResponseData.getRecipeTherapy(), RecipeTherapy.class));
        recipeInfoDTO.getRecipeTherapy().setId(recipePdfDTO.getRecipeTherapy().getId());
        recipeInfoDTO.setRecipe(ObjectCopyUtils.convert(hisResponseData.getRecipeBean(), Recipe.class));
        recipeInfoDTO.setRecipeExtend(ObjectCopyUtils.convert(hisResponseData.getRecipeExtendBean(), RecipeExtend.class));
        return recipeInfoDTO;
    }


    /**
     * 查询线下门诊处方诊断信息
     *
     * @param organId     机构ID
     * @param patientName 患者名称
     * @param registerID  挂号序号
     * @param patientId   病历号
     * @return 诊断列表
     */
    public List<DiseaseInfoDTO> queryPatientDisease(Integer organId, String patientName, String registerID, String patientId) {
        logger.info("OfflineRecipeClient queryPatientDisease organId:{}, patientName:{},registerID:{},patientId:{}.", organId, patientName, registerID, patientId);
        try {
            PatientDiseaseInfoTO patientDiseaseInfoTO = new PatientDiseaseInfoTO();
            patientDiseaseInfoTO.setOrganId(organId);
            patientDiseaseInfoTO.setPatientName(patientName);
            patientDiseaseInfoTO.setRegisterID(registerID);
            patientDiseaseInfoTO.setPatientId(patientId);
            HisResponseTO<List<DiseaseInfo>> hisResponse = recipeHisService.queryDiseaseInfo(patientDiseaseInfoTO);
            List<DiseaseInfo> result = getResponse(hisResponse);
            return ObjectCopyUtils.convert(result, DiseaseInfoDTO.class);
        } catch (Exception e) {
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
            BeanUtils.copyProperties(queryHisRecipResTO, offLineRecipeDetailDTO);
            offLineRecipeDetailDTO.setOrganDiseaseName(queryHisRecipResTO.getDiseaseName());
            offLineRecipeDetailDTO.setChronicDiseaseName(queryHisRecipResTO.getChronicDiseaseName());
            offLineRecipeDetailDTO.setCheckerName(queryHisRecipResTO.getCheckerName());
            //根据枚举设置处方类型
            Integer recipeType = queryHisRecipResTO.getRecipeType();
            String recipeTypeText = RecipeTypeEnum.getRecipeType(recipeType);
            offLineRecipeDetailDTO.setRecipeTypeText(recipeTypeText);
            //判断是否为医保处方
            Integer medicalType = queryHisRecipResTO.getMedicalType();
            if (!ObjectUtils.isEmpty(medicalType) && medicalType.equals(2)) {
                offLineRecipeDetailDTO.setMedicalTypeText("普通医保");
            } else if (!ObjectUtils.isEmpty(medicalType) && medicalType.equals(1)) {
                offLineRecipeDetailDTO.setMedicalTypeText("患者自费");
            }
        }

        return data.get(0);
    }

    /**
     * 推送处方
     *
     * @param pushType      推送类型: 1：提交处方，2:撤销处方
     * @param recipePdfDTO  处方明细
     * @param emrDetail     电子病历
     * @param pharmacyIdMap 药房
     * @return
     * @throws Exception
     */
    private com.ngari.platform.recipe.mode.RecipeDTO pushRecipe(Integer pushType, RecipeInfoDTO recipePdfDTO, EmrDetailDTO emrDetail, Map<Integer, PharmacyTcm> pharmacyIdMap) throws Exception {
        com.ngari.platform.recipe.mode.RecipeDTO recipeDTO = new com.ngari.platform.recipe.mode.RecipeDTO();
        recipeDTO.setPushType(pushType);
        recipeDTO.setOrganId(recipePdfDTO.getRecipe().getClinicOrgan());
        recipeDTO.setRecipeExtendBean(ObjectCopyUtils.convert(recipePdfDTO.getRecipeExtend(), RecipeExtendBean.class));
        recipeDTO.setPatientDTO(ObjectCopyUtils.convert(recipePdfDTO.getPatientBean(), PatientDTO.class));
        com.ngari.platform.recipe.mode.EmrDetailDTO emrDetailDTO = new com.ngari.platform.recipe.mode.EmrDetailDTO();
        BeanUtils.copyProperties(emrDetail, emrDetailDTO);
        emrDetailDTO.setSymptomValue(ObjectCopyUtils.convert(emrDetail.getSymptomValue(), EmrDetailValueDTO.class));
        emrDetailDTO.setDiseaseValue(ObjectCopyUtils.convert(emrDetail.getDiseaseValue(), EmrDetailValueDTO.class));
        recipeDTO.setEmrDetailDTO(emrDetailDTO);
        RecipeBean recipe = ObjectCopyUtils.convert(recipePdfDTO.getRecipe(), RecipeBean.class);
        //医生工号
        recipe.setDoctorCode(employmentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
        //科室代码
        recipe.setDepartCode(appointDepart.getAppointDepartCode());
        //科室名称
        recipe.setDepartName(appointDepart.getAppointDepartName());
        recipeDTO.setRecipeBean(recipe);
        List<RecipeDetailBean> detailList = ObjectCopyUtils.convert(recipePdfDTO.getRecipeDetails(), RecipeDetailBean.class);
        detailList.forEach(a -> {
            PharmacyTcm pharmacyTcm = pharmacyIdMap.get(a.getPharmacyId());
            if (null != pharmacyTcm) {
                a.setPharmacyCode(pharmacyTcm.getPharmacyCode());
            }
        });
        recipeDTO.setRecipeDetails(detailList);
        logger.info("OfflineRecipeClient pushRecipe recipeDTO：{}", JSON.toJSONString(recipeDTO));
        try {
            HisResponseTO<com.ngari.platform.recipe.mode.RecipeDTO> hisResponse = recipeHisService.pushRecipe(recipeDTO);
            return getResponse(hisResponse);
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
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
