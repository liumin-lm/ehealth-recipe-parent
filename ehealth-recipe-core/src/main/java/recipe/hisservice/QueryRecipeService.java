package recipe.hisservice;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.base.BaseAPI;
import com.ngari.base.cdr.service.IDiseaseService;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.QuestionnaireBean;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.dto.zjs.SubCodeDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.service.zjs.SubCodeService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.hisprescription.service.IQueryRecipeService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import es.vo.DiseaseVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.*;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeService;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.RedisClient;

import javax.annotation.Nullable;
import java.util.*;

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
     * 处方撤销状态
     *
     * @param
     * @return 1正常 2撤销
     */
    private String getVerificationStatus(Recipe recipe) {
        if (RecipeStatusConstant.REVOKE == recipe.getStatus()
                || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus()
                || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus()
                || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            return "2";
        }

        return "1";
    }

    /**
     * 设置处方详情数据
     *
     * @param req
     * @param detailList
     */
    private void setDetail(RegulationRecipeIndicatorsDTO req, List<Recipedetail> detailList,
                           Dictionary usingRateDic, Dictionary usePathwaysDic, Recipe recipe) {
        RegulationRecipeDetailIndicatorsReq reqDetail;
        DrugListDAO drugListDao = DAOFactory.getDAO(DrugListDAO.class);
        List<RegulationRecipeDetailIndicatorsReq> list = new ArrayList<>(detailList.size());
        /*double dosageDay;*/
        DrugList drugList;
        for (Recipedetail detail : detailList) {
            reqDetail = new RegulationRecipeDetailIndicatorsReq();
            reqDetail.setDrcode(detail.getOrganDrugCode());
            reqDetail.setDrname(detail.getDrugName());
            reqDetail.setDrmodel(detail.getDrugSpec());
            reqDetail.setPack(detail.getPack());
            reqDetail.setPackUnit(detail.getDrugUnit());
            //频次
            reqDetail.setFrequency(detail.getUsingRate());
            //药品频次名称
            if (null != usingRateDic) {
                reqDetail.setFrequencyName(usingRateDic.getText(detail.getUsingRate()));
            }
            //用法
            reqDetail.setAdmission(detail.getUsePathways());
            //药品用法名称
            if (null != usePathwaysDic) {
                reqDetail.setAdmissionName(usePathwaysDic.getText(detail.getUsePathways()));
            }
            reqDetail.setDosage(detail.getUseDose().toString());
            reqDetail.setDrunit(detail.getUseDoseUnit());
            reqDetail.setDosageTotal(detail.getUseTotalDose().toString());
            reqDetail.setUseDays(detail.getUseDays());
            reqDetail.setRemark(detail.getMemo());
            drugList = drugListDao.getById(detail.getDrugId());
            if (drugList != null){
                //药物剂型代码
                reqDetail.setDosageForm(drugList.getDrugForm());
                //厂商
                reqDetail.setDrugManf(drugList.getProducer());
            }
            //药物使用总剂量
            reqDetail.setUseDosage("0");
            //药物日药量/DDD值
            /*dosageDay = (detail.getUseDose())*(UsingRateFilter.transDailyTimes(detail.getUsingRate()));*/
            reqDetail.setDosageDay("0");
            //中药处方详细描述
            if (RecipeUtil.isTcmType(recipe.getRecipeType())){
                reqDetail.setTcmDescribe(detail.getUsingRate()+detail.getUsePathways());
            }
            //处方明细Id
            reqDetail.setRecipeDetailId(detail.getRecipeDetailId());
            //单价
            reqDetail.setPrice(detail.getSalePrice());
            //总价
            reqDetail.setTotalPrice(detail.getDrugCost());

            list.add(reqDetail);
        }

        req.setOrderList(list);
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
                //设置药品产地名称
                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(recipe.getClinicOrgan(), detail.getOrganDrugCode());
                orderItem.setDrugManf(null != organDrugList ? organDrugList.getProducer() : null);

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
}
