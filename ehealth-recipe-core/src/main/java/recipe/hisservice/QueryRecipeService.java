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
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.hisprescription.service.IQueryRecipeService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import recipe.ApplicationUtils;
import com.ngari.recipe.common.OrganDrugChangeBean;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.RegexEnum;
import recipe.dao.*;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.service.RecipeServiceSub;
import recipe.util.DateConversion;
import recipe.util.RegexUtils;

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
        LOGGER.info("queryRecipeInfo入參：{}", JSONUtils.toString(queryRecipeReqDTO));
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
        //date 20200222杭州市互联网(添加诊断)
//        List<DiseaseInfo> diseaseInfos = new ArrayList<>();
//        DiseaseInfo diseaseInfo;
//        if(StringUtils.isNotEmpty(recipe.getOrganDiseaseId()) && StringUtils.isNotEmpty(recipe.getOrganDiseaseName())){
//            String [] diseaseIds = recipe.getOrganDiseaseId().split("；");
//            String [] diseaseNames = recipe.getOrganDiseaseName().split("；");
//            for (int i = 0; i < diseaseIds.length; i++){
//                diseaseInfo = new DiseaseInfo();
//                diseaseInfo.setDiseaseCode(diseaseIds[i]);
//                diseaseInfo.setDiseaseName(diseaseNames[i]);
//                diseaseInfos.add(diseaseInfo);
//            }
//            infoDTO.setDiseaseInfo(diseaseInfos);
//
//        }
        resultDTO.setMsgCode(0);
        resultDTO.setData(infoDTO);
        LOGGER.info("queryRecipeInfo res={}", JSONUtils.toString(resultDTO));
        return resultDTO;
    }

    @Override
    @RpcService
    public QueryRecipeResultDTO queryPlatRecipeByRecipeId(QueryPlatRecipeInfoByDateDTO req){
        LOGGER.info("queryPlatRecipeByPatientNameAndDate req={}",JSONUtils.toString(req));
        QueryRecipeResultDTO resultDTO = new QueryRecipeResultDTO();
        if (StringUtils.isEmpty(req.getRecipeId())){
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("处方序号不能为空");
            return resultDTO;
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        /*//根据患者姓名和时间查询未支付处方
        List<Recipe> recipes = recipeDAO.findNoPayRecipeListByPatientNameAndDate(req.getPatientName(), req.getOrganId(), req.getStartDate(), req.getEndDate());*/
        Recipe recipe = recipeDAO.getByRecipeId(Integer.valueOf(req.getRecipeId()));
        if (null == recipe) {
            resultDTO.setMsgCode(-1);
            resultDTO.setMsg("找不到处方");
            return resultDTO;
        }
        List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        PatientBean patientBean = iPatientService.get(recipe.getMpiid());
        //拼接返回数据
        QueryRecipeInfoDTO infoDTO = splicingBackData(details, recipe, patientBean, null);
        resultDTO.setMsgCode(0);
        resultDTO.setData(infoDTO);
        LOGGER.info("queryPlatRecipeByPatientNameAndDate res={}", JSONUtils.toString(resultDTO));
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
        QueryRecipeInfoDTO recipeDTO = null;
        try {
            recipeDTO = new QueryRecipeInfoDTO();
            //拼接处方信息
            //处方号
            recipeDTO.setRecipeID(recipe.getRecipeCode());
            //处方id
            recipeDTO.setPlatRecipeID(String.valueOf(recipe.getRecipeId()));
            //挂号序号
            recipeDTO.setRegisterId(String.valueOf(recipe.getClinicId()));
            //签名日期
            recipeDTO.setDatein(recipe.getSignDate());
            //是否支付
            recipeDTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe
                    .getPayFlag()) : null);
            //icd诊断码
            recipeDTO.setIcdCode(getCode(recipe.getOrganDiseaseId()));
            //icd诊断名称
            recipeDTO.setIcdName(getCode(recipe.getOrganDiseaseName()));
            //返回部门code
            DepartmentService service = BasicAPI.getService(DepartmentService.class);
            DepartmentDTO departmentDTO = service.getById(recipe.getDepart());
            if (departmentDTO != null){
                recipeDTO.setDeptID(departmentDTO.getCode());
                //科室名
                recipeDTO.setDeptName(departmentDTO.getName());
            }
            //处方类型
            recipeDTO.setRecipeType((null != recipe.getRecipeType()) ? recipe
                    .getRecipeType().toString() : null);
            //获取医院诊断内码
            recipeDTO.setIcdRdn(getIcdRdn(recipe.getClinicOrgan(),recipe.getOrganDiseaseId(),recipe.getOrganDiseaseName()));
            //复诊id
            recipeDTO.setClinicID((null != recipe.getClinicId()) ? Integer.toString(recipe
                    .getClinicId()) : null);
            //转换平台医生id为工号返回his
            EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
            if (recipe.getDoctor() != null){
                String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart());
                //医生工号
                recipeDTO.setDoctorID(jobNumber);
                //医生姓名
                recipeDTO.setDoctorName(recipe.getDoctorName());
            }
            //审核医生
            if (recipe.getChecker() != null){
                String jobNumberChecker = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getChecker(), recipe.getClinicOrgan(), recipe.getDepart());
                recipeDTO.setAuditDoctor(jobNumberChecker);
                //审核状态
                recipeDTO.setAuditCheckStatus("1");
                //date 20200225 审方时间
                recipeDTO.setCheckDate(recipe.getCheckDate());
            }else {
                recipeDTO.setAuditDoctor(recipeDTO.getDoctorID());
                recipeDTO.setAuditCheckStatus("0");
                //date 20200225 审方时间
                recipeDTO.setCheckDate(new Date());
            }
            //本处方收费类型 1市医保 2省医保 3自费---杭州市互联网-市医保
            recipeDTO.setMedicalPayFlag(getMedicalType(recipe.getMpiid(),recipe.getClinicOrgan()));
            //处方金额
            recipeDTO.setRecipeFee(String.valueOf(recipe.getActualPrice()));
            //自付比例
            /*recipeDTO.setPayScale("");*/
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

            splicingBackDataForRecipeDetails(recipe.getClinicOrgan(),details,recipeDTO);
        } catch (Exception e) {
            LOGGER.error("queryRecipe splicingBackData error",e);
        }

        return recipeDTO;
    }

    private void splicingBackDataForRecipeDetails(Integer clinicOrgan, List<Recipedetail> details, QueryRecipeInfoDTO recipeDTO) throws ControllerException {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //拼接处方明细
        if (null != details && !details.isEmpty()) {
            List<OrderItemDTO> orderList = new ArrayList<>();
            for (Recipedetail detail : details) {
                OrderItemDTO orderItem = new OrderItemDTO();
                //处方明细id
                orderItem.setOrderID(Integer.toString(detail
                        .getRecipeDetailId()));
                //医院药品代码
                orderItem.setDrcode(detail.getOrganDrugCode());
                //医院药品名
                orderItem.setDrname(detail.getDrugName());
                //药品规格
                orderItem.setDrmodel(detail.getDrugSpec());
                //包装单位
                orderItem.setPackUnit(detail.getDrugUnit());
                //用药天数
                orderItem.setUseDays(Integer.toString(detail.getUseDays()));
                //用法
                orderItem.setAdmission(UsePathwaysFilter.filterNgari(clinicOrgan,detail.getUsePathways()));
                //频次
                orderItem.setFrequency(UsingRateFilter.filterNgari(clinicOrgan,detail.getUsingRate()));
                //医保频次
                orderItem.setMedicalFrequency(UsingRateFilter.filterNgariByMedical(clinicOrgan,detail.getUsingRate()));
                //单次剂量
                orderItem.setDosage((null != detail.getUseDose()) ? Double
                        .toString(detail.getUseDose()) : null);
                //剂量单位
                orderItem.setDrunit(detail.getUseDoseUnit());

                OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCode(clinicOrgan, detail.getOrganDrugCode());
                if (null != organDrugList){
                    //药品产地名称
                    orderItem.setDrugManf(organDrugList.getProducer());
                    //药品产地编码
                    orderItem.setDrugManfCode(organDrugList.getProducerCode());
                    //药品单价
                    orderItem.setPrice(String.valueOf(organDrugList.getSalePrice()));
                    //医保对应代码
                    orderItem.setMedicalDrcode(organDrugList.getMedicalDrugCode());
                    //剂型代码 --
                    orderItem.setDrugFormCode(organDrugList.getDrugFormCode());
                    //医保剂型代码--
                    orderItem.setMedicalDrugFormCode(organDrugList.getMedicalDrugFormCode());
                    //剂型名称
                    orderItem.setDrugFormName(organDrugList.getDrugForm());
                }
                /*
                 * //每日剂量 转换成两位小数 DecimalFormat df = new DecimalFormat("0.00");
                 * String dosageDay =
                 * df.format(getFrequency(detail.getUsingRate(
                 * ))*detail.getUseDose()
                 *
                 *
                 * );
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
                //放最后
                //用法名称
                orderItem.setAdmissionName(DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(detail.getUsingRate()));
                //频次名称
                orderItem.setFrequencyName(DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(detail.getUsePathways()));
                orderList.add(orderItem);
            }
            recipeDTO.setOrderList(orderList);
        } else {
            recipeDTO.setOrderList(null);
        }
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
    private boolean isClinicOrgan(String organId) {
        return RegexUtils.regular(organId, RegexEnum.NUMBER)&&(organId.length()==7);
    }


    public static String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    //杭州市互联网医院可外配药品更新上传接口
    @Override
    @RpcService
    public RecipeResultBean updateOrSaveOrganDrug(OrganDrugChangeBean organDrugChangeBean){
        LOGGER.info("updateOrSaveOrganDrug 更新药品信息入参{}", JSONUtils.toString(organDrugChangeBean));
        RecipeResultBean result = RecipeResultBean.getFail();
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = DAOFactory.getDAO(SaleDrugList.class);
        //根据你操作的方式，判断药品修改的方式（机构药品目录）
        //根据省监管药品代码，关联到对应的organDrugList,saleDrugList
        Integer operationCode = organDrugChangeBean.getOperationCode();
        //1新增 2修改 3停用
        OrganDrugList organDrugList = new OrganDrugList();
        BeanUtils.copyProperties(organDrugChangeBean, organDrugList);

        if(!validDrugMsg(organDrugChangeBean, result)){
            return result;
        }
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organDrugChangeBean.getOrganId(), 1);
        if(CollectionUtils.isEmpty(drugsEnterprises)){
            result.setMsg("当前医院"+ organDrugChangeBean.getOrganId() +"没有关联药企，无法操作关联的配送药品！");
            return result;
        }
        Integer drugsEnterpriseId = drugsEnterprises.get(0).getId();
        Date now = DateTime.now().toDate();

        switch(operationCode){
            //新增
            case 1:
                //校验是否可以新增
                if(!validDrugAdd(organDrugChangeBean, result)){
                    return result;
                }
                //新增1条organDrugList
                //新增1条saleDrugList
                List<OrganDrugList> organDrugsNo = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                        (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                                organDrugChangeBean.getOrganDrugCode(), 0);
                //判断有没有失效的
                if(CollectionUtils.isNotEmpty(organDrugsNo)){
                    //查询对应的配送药品设置为启用
                    List<SaleDrugList> saleDrugLists = saleDrugListDAO.
                            findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus(drugsEnterpriseId, organDrugsNo.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 0);
                    if(CollectionUtils.isEmpty(saleDrugLists)){
                        result.setMsg("当前没有停用配送药品");
                        return result;
                    }

                    //将设置为启用
                    OrganDrugList organDrugListAdd = organDrugsNo.get(0);
                    BeanUtils.copyProperties(organDrugList, organDrugListAdd, getNullPropertyNames(organDrugList));
                    organDrugListAdd.setStatus(1);
                    organDrugListAdd.setLastModify(now);
                    LOGGER.info("updateOrSaveOrganDrug 更新机构药品信息{}", JSONUtils.toString(organDrugListAdd));
                    OrganDrugList nowOrganDrugList = organDrugListDAO.update(organDrugListAdd);

                    SaleDrugList nowSaleDrugList = saleDrugLists.get(0);
                    nowSaleDrugList.setStatus(1);
                    nowSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                    nowSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                    nowSaleDrugList.setOrganId(drugsEnterpriseId);
                    nowSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                    nowSaleDrugList.setLastModify(now);
                    LOGGER.info("updateOrSaveOrganDrug 更新配送药品信息{}", JSONUtils.toString(nowSaleDrugList));
                    saleDrugListDAO.update(nowSaleDrugList);

                }else{
                    //没有失效的新增
                    organDrugList.setStatus(1);
                    organDrugList.setCreateDt(now);
                    LOGGER.info("updateOrSaveOrganDrug 添加机构药品信息{}", JSONUtils.toString(organDrugList));
                    OrganDrugList nowOrganDrugList = organDrugListDAO.save(organDrugList);

                    //填充配送药品信息
                    SaleDrugList newSaleDrugList = new SaleDrugList();
                    newSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                    newSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                    newSaleDrugList.setOrganId(drugsEnterpriseId);
                    newSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                    newSaleDrugList.setStatus(1);
                    newSaleDrugList.setCreateDt(now);
                    LOGGER.info("updateOrSaveOrganDrug 添加配送药品信息{}", JSONUtils.toString(newSaleDrugList));
                    saleDrugListDAO.save(newSaleDrugList);

                }

                result = RecipeResultBean.getSuccess();
                break;
            //修改
            case 2:
                //校验是否可以修改
                if(!validDrugChange(organDrugChangeBean, result)){
                    return result;
                }
                //修改1条organDrugList
                //修改1条saleDrugList
                List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                        (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                                organDrugChangeBean.getOrganDrugCode(), 1);
                List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus
                        ( drugsEnterpriseId, organDrugs.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 1);
                if(CollectionUtils.isEmpty(saleDrugLists)){
                    result.setMsg("当前没有启用配送药品");
                    return result;
                }
                //将设置为启用

                OrganDrugList organDrugListChange = organDrugs.get(0);
                BeanUtils.copyProperties(organDrugList, organDrugListChange, getNullPropertyNames(organDrugList));
                organDrugListChange.setStatus(1);
                organDrugListChange.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 更新机构药品信息{}", JSONUtils.toString(organDrugListChange));
                OrganDrugList nowOrganDrugList = organDrugListDAO.update(organDrugListChange);

                SaleDrugList nowSaleDrugList = saleDrugLists.get(0);
                nowSaleDrugList.setStatus(1);
                nowSaleDrugList.setDrugId(organDrugChangeBean.getDrugId());
                nowSaleDrugList.setOrganDrugCode(organDrugChangeBean.getCloudPharmDrugCode());
                nowSaleDrugList.setOrganId(drugsEnterpriseId);
                nowSaleDrugList.setPrice(organDrugChangeBean.getSalePrice());
                nowSaleDrugList.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 更新配送药品信息{}", JSONUtils.toString(nowSaleDrugList));
                saleDrugListDAO.update(nowSaleDrugList);

                result = RecipeResultBean.getSuccess();
                break;
            //停用
            case 3:
                //校验是否可以停用
                if(!validDrugDown(organDrugChangeBean, result)){
                    return result;
                }
                //停用1条organDrugList
                //停用1条saleDrugList
                List<OrganDrugList> organDrugsDown = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                        (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                                organDrugChangeBean.getOrganDrugCode(), 1);
                List<SaleDrugList> saleDrugListsDown = saleDrugListDAO.findByDrugIdAndOrganIdAndOrganDrugCodeAndStatus
                        (drugsEnterpriseId, organDrugsDown.get(0).getDrugId(), organDrugChangeBean.getCloudPharmDrugCode(), 1);
                if(CollectionUtils.isEmpty(saleDrugListsDown)){
                    result.setMsg("当前没有启用配送药品");
                    return result;
                }

                OrganDrugList organDrugListDown = organDrugsDown.get(0);
                organDrugListDown.setStatus(0);
                organDrugListDown.setLastModify(now);
                LOGGER.info("updateOrSaveOrganDrug 停用机构药品信息{}", JSONUtils.toString(organDrugListDown));
                organDrugListDAO.update(organDrugListDown);

                SaleDrugList saleDrugListDown = saleDrugListsDown.get(0);
                saleDrugListDown.setLastModify(now);
                saleDrugListDown.setStatus(0);
                LOGGER.info("updateOrSaveOrganDrug 停用配送药品信息{}", JSONUtils.toString(saleDrugListDown));
                saleDrugListDAO.update(saleDrugListDown);

                result = RecipeResultBean.getSuccess();

                break;
            default:
        }
        return result;


    }

    private boolean validDrugMsg(OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {
        if(null == organDrugChangeBean.getDrugId() || null == organDrugChangeBean.getOrganId() ||
                null == organDrugChangeBean.getOrganDrugCode() || null == organDrugChangeBean.getCloudPharmDrugCode()){
            result.setMsg("当前新增药品信息，信息缺失,无法操作！");
            return false;
        }

        return true;
    }


    private boolean validDrugAdd(OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                        organDrugChangeBean.getOrganDrugCode(), 1);
        //如果已经有了启用的
        if(CollectionUtils.isNotEmpty(organDrugs)){
            result.setMsg("当前药品信息系统已存在，无法重复新增！");
            return false;
        }

        return true;
    }

    private boolean validDrugChange(OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                        organDrugChangeBean.getOrganDrugCode(), 1);
        //如果没有启用的
        if(CollectionUtils.isEmpty(organDrugs)){
            result.setMsg("当前药品信息系统没有启用的,无法修改药品信息！");
            return false;
        }

        return true;
    }

    private boolean validDrugDown(OrganDrugChangeBean organDrugChangeBean, RecipeResultBean result) {

        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //判断当前机构药品是否已经存在
        List<OrganDrugList> organDrugs = organDrugListDAO.findByOrganIdAndDrugIdAndOrganDrugCodeAndStatus
                (organDrugChangeBean.getOrganId(), organDrugChangeBean.getDrugId(),
                        organDrugChangeBean.getOrganDrugCode(), 1);
        //如果没有启用的
        if(CollectionUtils.isEmpty(organDrugs)){
            result.setMsg("当前药品信息系统没有启用的,无法停用药品信息！");
            return false;
        }
        return true;
    }

    //杭州市互联网医院查询基础药品目录
    @Override
    @RpcService
    public List<DrugListBean> getDrugList(String organId, String organName, Integer start, Integer limit){
        LOGGER.info("当前请求参数：{},{},{},{}", organId, organName, start, limit);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugList = drugListDAO.findAllForPage(start, limit);
        if(CollectionUtils.isEmpty(drugList)){
            return new ArrayList<DrugListBean>();
        }
        LOGGER.info("当前返回结果", JSONUtils.toString(drugList));
        return ObjectCopyUtils.convert(drugList, DrugListBean.class);

    }
}
