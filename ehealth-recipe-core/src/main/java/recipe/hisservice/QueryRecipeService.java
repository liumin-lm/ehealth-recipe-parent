package recipe.hisservice;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.base.BaseAPI;
import com.ngari.base.cdr.service.IDiseaseService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.hisprescription.service.IQueryRecipeService;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.RegexEnum;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.RegexUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 浙江互联网医院处方查询接口
 * created by shiyuping on 2018/11/30
 */
@RpcBean("remoteQueryRecipeService")
public class QueryRecipeService implements IQueryRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRecipeService.class);

    @Override
    @RpcService
    public QueryRecipeResultDTO queryRecipeInfo(QueryRecipeReqDTO queryRecipeReqDTO){
        QueryRecipeResultDTO resultDTO = new QueryRecipeResultDTO();
        if (StringUtils.isEmpty(queryRecipeReqDTO.getOrganId())) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("缺少组织机构编码");
            return resultDTO;
        }
        String recipeCode = queryRecipeReqDTO.getRecipeID();
        if (StringUtils.isEmpty(recipeCode)){
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("缺少处方编码");
            return resultDTO;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        //转换机构组织编码
        Integer clinicOrgan = transformOrganIdToClinicOrgan(queryRecipeReqDTO.getOrganId());
        if (null == clinicOrgan) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("平台未匹配到该组织机构编码");
            return resultDTO;
        }
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCode, clinicOrgan);
        if (null == recipe) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("找不到处方");
            return resultDTO;
        }
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        PatientBean patientBean = iPatientService.get(recipe.getMpiid());
        HealthCardBean cardBean = iPatientService.getHealthCard(recipe.getMpiid(), recipe.getClinicOrgan(), "2");
        //拼接返回数据
        QueryRecipeInfoDTO infoDTO = splicingBackData(details, recipe, patientBean, cardBean);
        resultDTO.setMsgCode(0);
        resultDTO.setData(infoDTO);
        LOGGER.info("queryRecipeInfo res={}", JSONUtils.toString(resultDTO));
        return resultDTO;
    }

    @Override
    @RpcService
    public List<RegulationRecipeIndicatorsDTO> queryRegulationRecipeData(Integer organId,Date startDate,Date endDate,Boolean checkFlag) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        String start = DateConversion.formatDateTimeWithSec(startDate);
        String end = DateConversion.formatDateTimeWithSec(endDate);
        List<Recipe> recipeList = recipeDAO.findSyncRecipeListByOrganId(organId, start, end,checkFlag);
        if (CollectionUtils.isEmpty(recipeList)){
            return new ArrayList<>();
        }
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        LOGGER.info("queryRegulationRecipeData start");
        service.splicingBackRecipeData(recipeList,request);
        List<RegulationRecipeIndicatorsDTO> result = ObjectCopyUtils.convert(request, RegulationRecipeIndicatorsDTO.class);
        LOGGER.info("queryRegulationRecipeData data={}", JSONUtils.toString(result));
        return result;
    }

    /**
     * 拼接处方返回信息数据
     * @param details
     * @param recipe
     * @param patient
     * @param card
     */
    private QueryRecipeInfoDTO splicingBackData(List<Recipedetail> details, Recipe recipe, PatientBean patient, HealthCardBean card) {
        QueryRecipeInfoDTO recipeDTO = new QueryRecipeInfoDTO();
        //拼接处方信息
        recipeDTO.setRecipeID(recipe.getRecipeCode());
        recipeDTO.setPlatRecipeID(String.valueOf(recipe.getRecipeId()));
        recipeDTO.setDatein(recipe.getSignDate());
        recipeDTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe
                .getPayFlag()) : null);
        //icd诊断码
        recipeDTO.setIcdCode(getCode(recipe.getOrganDiseaseId()));
        //icd诊断名称
        recipeDTO.setIcdName(getCode(recipe.getOrganDiseaseName()));
        //返回部门code
        DepartmentService service = BasicAPI.getService(DepartmentService.class);
        DepartmentDTO departmentDTO = service.getById(recipe.getDepart());
        recipeDTO.setDeptID(departmentDTO.getCode());
        //处方类型
        recipeDTO.setRecipeType((null != recipe.getRecipeType()) ? recipe
                .getRecipeType().toString() : null);
        //获取医院诊断内码
        recipeDTO.setIcdRdn(getIcdRdn(recipe.getClinicOrgan(),recipe.getOrganDiseaseId(),recipe.getOrganDiseaseName()));
        recipeDTO.setClinicID((null != recipe.getClinicId()) ? Integer.toString(recipe
                .getClinicId()) : null);
        //转换平台医生id为工号返回his
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        if (recipe.getDoctor() != null){
            String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart());
            recipeDTO.setDoctorID(jobNumber);
        }
        //审核医生
        if (recipe.getChecker() != null){
            String jobNumberChecker = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getChecker(), recipe.getClinicOrgan(), recipe.getDepart());
            recipeDTO.setAuditDoctor(jobNumberChecker);
        }else {
            recipeDTO.setAuditDoctor(recipeDTO.getDoctorID());
        }
        //本处方收费类型 1市医保 2省医保 3自费---杭州市互联网-市医保
        recipeDTO.setMedicalPayFlag(getMedicalType(recipe.getMpiid(),recipe.getClinicOrgan()));
        //处方金额
        recipeDTO.setRecipeFee(String.valueOf(recipe.getActualPrice()));
        //主诉等等四个字段
        Integer recipeId = recipe.getRecipeId();
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (recipeExtend!=null){
            if (StringUtils.isNotEmpty(recipeExtend.getMainDieaseDescribe())){
                //主诉
                recipeDTO.setBRZS(recipeExtend.getMainDieaseDescribe());
            }
            if (StringUtils.isNotEmpty(recipeExtend.getPhysicalCheck())){
                //体格检查
                recipeDTO.setTGJC(recipeExtend.getPhysicalCheck());
            }
            if (StringUtils.isNotEmpty(recipeExtend.getHistoryOfPresentIllness())){
                //现病史
                recipeDTO.setXBS(recipeExtend.getHistoryOfPresentIllness());
            }
            if (StringUtils.isNotEmpty(recipeExtend.getHandleMethod())){
                //处理方法
                recipeDTO.setCLFF(recipeExtend.getHandleMethod());
            }
        }

        if (null != patient) {
            // 患者信息
            String idCard = patient.getCertificate();
            if(StringUtils.isNotEmpty(idCard)){
                //没有身份证儿童的证件处理
                String childFlag = "-";
                if(idCard.contains(childFlag)){
                    idCard = idCard.split(childFlag)[0];
                }
            }
            recipeDTO.setCertID(idCard);
            recipeDTO.setPatientName(patient.getPatientName());
            recipeDTO.setMobile(patient.getMobile());
            recipeDTO.setPatientSex(patient.getPatientSex());
            // 简要病史
            recipeDTO.setDiseasesHistory(recipe.getOrganDiseaseName());
        }
        //设置卡
        if (null != card) {
            recipeDTO.setCardType(card.getCardType());
            recipeDTO.setCardNo(card.getCardId());
        }


        if (recipe.getGiveMode() == null){
            //如果为nul则默认为医院取药
            recipeDTO.setDeliveryType("0");
        }else {
            //根据处方单设置配送方式
            switch(recipe.getGiveMode()){
                //配送到家
                case 1:
                    recipeDTO.setDeliveryType("1");
                    break;
                //医院取药
                case 2:
                    recipeDTO.setDeliveryType("0");
                    break;
                //药店取药
                case 3:
                    recipeDTO.setDeliveryType("2");
                    break;
            }
        }

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //拼接处方明细
        if (null != details && !details.isEmpty()) {
            List<OrderItemDTO> orderList = new ArrayList<>();
            for (Recipedetail detail : details) {
                OrderItemDTO orderItem = new OrderItemDTO();
                orderItem.setOrderID(Integer.toString(detail
                        .getRecipeDetailId()));
                orderItem.setDrcode(detail.getOrganDrugCode());
                orderItem.setDrname(detail.getDrugName());
                orderItem.setDrmodel(detail.getDrugSpec());
                orderItem.setPackUnit(detail.getDrugUnit());
                //设置用药天数
                orderItem.setUseDays(Integer.toString(detail.getUseDays()));

                orderItem.setAdmission(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsePathways()));
                orderItem.setFrequency(UsingRateFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsingRate()));
                orderItem.setDosage((null != detail.getUseDose()) ? Double
                        .toString(detail.getUseDose()) : null);
                orderItem.setDrunit(detail.getUseDoseUnit());

                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(recipe.getClinicOrgan(), detail.getOrganDrugCode());
                if (null != organDrugList){
                    //药品产地名称
                    orderItem.setDrugManf(organDrugList.getProducer());
                    //药品产地编码
                    orderItem.setDrugManfCode(organDrugList.getProducerCode());
                    //药品单价
                    orderItem.setPrice(String.valueOf(organDrugList.getSalePrice()));
                }


                /*
                 * //每日剂量 转换成两位小数 DecimalFormat df = new DecimalFormat("0.00");
                 * String dosageDay =
                 * df.format(getFrequency(detail.getUsingRate(
                 * ))*detail.getUseDose());
                 */
                // 开药数量
                orderItem.setTotalDose((null != detail.getUseTotalDose()) ? Double
                        .toString(detail.getUseTotalDose()) : null);
                //备注
                orderItem.setRemark(detail.getMemo());
                //药品包装
                orderItem.setPack(detail.getPack());
                //药品单位
                orderItem.setUnit(detail.getDrugUnit());

                orderList.add(orderItem);
            }

            recipeDTO.setOrderList(orderList);
        } else {
            recipeDTO.setOrderList(null);
        }

        return recipeDTO;
    }

    /**
     * 获取杭州市互联网的医保类型
     */
    private String getMedicalType(String mpiid, Integer clinicOrgan) {
        return RecipeServiceSub.isMedicalPatient(mpiid, clinicOrgan)?"1":"3";
    }

    //将；用|代替
    private String getCode(String code) {
        return code.replace("；","|");
    }

    //获取医院诊断内码
    private String getIcdRdn(Integer clinicOrgan, String organDiseaseId, String organDiseaseName) {
        IDiseaseService diseaseService = AppContextHolder.getBean("eh.diseasService", IDiseaseService.class);
        List<String> icd10Lists = Splitter.on("；").splitToList(organDiseaseId);
        List<String> nameLists = Splitter.on("；").splitToList(organDiseaseName);
        List<String> icdRdnList = Lists.newArrayList();
        if (icd10Lists.size() == nameLists.size()){
            for (int i = 0; i < icd10Lists.size();i++){
                String innerCode = diseaseService.getInnerCodeByNameOrCode(clinicOrgan, nameLists.get(i), icd10Lists.get(i));
                if (StringUtils.isEmpty(innerCode)){
                    innerCode = " ";
                }
                icdRdnList.add(innerCode);
            }
        }
        //若没匹配的医院诊断内码则返回空字符串
        return StringUtils.join(icdRdnList,"|");
    }

    /**
     * 转换组织机构编码
     * @param organId
     * @return
     */
    private Integer transformOrganIdToClinicOrgan(String organId){
        //需要转换组织机构编码
        Integer clinicOrgan = null;
        try {
            if (isClinicOrgan(organId)){
                return Integer.valueOf(organId);
            }
            IOrganService organService = BaseAPI.getService(IOrganService.class);
            List<OrganBean> organList = organService.findByOrganizeCode(organId);
            if (CollectionUtils.isNotEmpty(organList)) {
                clinicOrgan = organList.get(0).getOrganId();
            }
        } catch (Exception e) {
            LOGGER.warn("queryRecipeInfo 平台未匹配到该组织机构编码. organId={}", organId, e);
        }
        return clinicOrgan;
    }

    /**
     * 判断是否是平台机构id规则----长度为7的纯数字
     * @param organId
     * @return
     */
    private static boolean isClinicOrgan(String organId) {
        return RegexUtils.regular(organId, RegexEnum.NUMBER)&&(organId.length()==7);
    }
}
