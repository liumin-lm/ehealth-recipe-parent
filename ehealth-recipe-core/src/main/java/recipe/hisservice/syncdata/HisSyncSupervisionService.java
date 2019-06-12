package recipe.hisservice.syncdata;

import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.serviceconfig.mode.ServiceConfigResponseTO;
import com.ngari.base.serviceconfig.service.IHisServiceConfigService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.RegulationRecipeDetailIndicatorsReq;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.dto.*;
import com.ngari.patient.dto.zjs.SubCodeDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.service.zjs.SubCodeService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import java.util.*;

/**
 * created by shiyuping on 2019/6/3
 * 广东省监管平台同步
 */
@RpcBean("hisSyncSupervisionService")
public class HisSyncSupervisionService implements ICommonSyncSupervisionService {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSyncSupervisionService.class);

    private static String HIS_SUCCESS = "200";

    @RpcService
    @Override
    public CommonResponse uploadRecipeIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeIndicators recipeList length={}", recipeList.size());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }
        IHisServiceConfigService configService = AppDomainContext.getBean("his.hisServiceConfig", IHisServiceConfigService.class);
        //获取所有监管平台机构列表
        List<ServiceConfigResponseTO> list = configService.findAllRegulationOrgan();
        if (CollectionUtils.isEmpty(list)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }

       /* ProvUploadOrganService provUploadOrganService =
                AppDomainContext.getBean("basic.provUploadOrganService", ProvUploadOrganService.class);
        List<ProvUploadOrganDTO> provUploadOrganList = provUploadOrganService.findByStatus(1);
        if (CollectionUtils.isEmpty(provUploadOrganList)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }*/


        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        SubCodeService subCodeService = BasicAPI.getService(SubCodeService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
       /* IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);*/

        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        Map<Integer, OrganDTO> organMap = new HashMap<>(20);
        Map<Integer, DepartmentDTO> departMap = new HashMap<>(20);
        Map<Integer, AppointDepartDTO> appointDepartMap = new HashMap<>(20);
        Map<Integer, DoctorDTO> doctorMap = new HashMap<>(20);

        Dictionary usingRateDic = null;
        Dictionary usePathwaysDic = null;
        try {
            usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
            usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
        } catch (ControllerException e) {
            LOGGER.warn("uploadRecipeIndicators dic error.");
        }

        //业务数据处理
        Date now = DateTime.now().toDate();
        RegulationRecipeIndicatorsReq req;
        OrganDTO organDTO;
        String organDiseaseName;
        DepartmentDTO departmentDTO;
        DoctorDTO doctorDTO;
        PatientDTO patientDTO;
        SubCodeDTO subCodeDTO;
        List<Recipedetail> detailList;
        AppointDepartDTO appointDepart;
        String str;
        for (Recipe recipe : recipeList) {
            req = new RegulationRecipeIndicatorsReq();

            /* req.setBussID(recipe.getRecipeId().toString());*/

            //机构处理
            organDTO = organMap.get(recipe.getClinicOrgan());
            if (null == organDTO) {
                organDTO = organService.get(recipe.getClinicOrgan());
                organMap.put(recipe.getClinicOrgan(), organDTO);
            }
            if (null == organDTO) {
                LOGGER.warn("uploadRecipeIndicators organ is null. recipe.clinicOrgan={}", recipe.getClinicOrgan());
                continue;
            }
            for (ServiceConfigResponseTO uploadOrgan : list) {
                if (uploadOrgan.getOrganid().equals(organDTO.getOrganId())) {
                    req.setOrganID(LocalStringUtil.toString(organDTO.getOrganId()));
                    //组织机构编码
                    req.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
                    req.setOrganName(organDTO.getName());
                    req.setOrganName(organDTO.getName());
                    break;
                }
            }

            /*if (StringUtils.isEmpty(req.getUnitID())) {
                LOGGER.warn("uploadRecipeIndicators minkeUnitID is not in minkeOrganList. organ.organId={}",
                        organDTO.getOrganId());
                continue;
            }*/

            //科室处理
            appointDepart = appointDepartMap.get(recipe.getDepart());
            if (null==appointDepart){
                appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
                appointDepartMap.put(recipe.getDepart(),appointDepart);
            }
            req.setDeptID((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
            req.setDeptName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
            /*req.setDeptID(recipe.getDepart().toString());*/
            departmentDTO = departMap.get(recipe.getDepart());
            if (null == departmentDTO) {
                departmentDTO = departmentService.getById(recipe.getDepart());
                departMap.put(recipe.getDepart(), departmentDTO);
            }
            if (null == departmentDTO) {
                LOGGER.warn("uploadRecipeIndicators depart is null. recipe.depart={}", recipe.getDepart());
                continue;
            }
            /*req.setDeptName(departmentDTO.getName());*/
            //设置专科编码等
            subCodeDTO = subCodeService.getByNgariProfessionCode(departmentDTO.getProfessionCode());
            if (null == subCodeDTO) {
                LOGGER.warn("uploadRecipeIndicators subCode is null. recipe.professionCode={}",
                        departmentDTO.getProfessionCode());
                continue;
            }
            req.setSubjectCode(subCodeDTO.getSubCode());
            req.setSubjectName(subCodeDTO.getSubName());

            //医生处理
            req.setDoctorId(recipe.getDoctor().toString());
            doctorDTO = doctorMap.get(recipe.getDoctor());
            if (null == doctorDTO) {
                doctorDTO = doctorService.get(recipe.getDoctor());
                doctorMap.put(recipe.getDoctor(), doctorDTO);
            }
            if (null == doctorDTO) {
                LOGGER.warn("uploadRecipeIndicators doctor is null. recipe.doctor={}", recipe.getDoctor());
                continue;
            }
            if(1 == doctorDTO.getTestPersonnel()){
                LOGGER.warn("uploadRecipeIndicators doctor is testPersonnel. recipe.doctor={}", recipe.getDoctor());
                continue;
            }

            req.setDoctorCertID(doctorDTO.getIdNumber());
            req.setDoctorName(doctorDTO.getName());
            //设置医生工号
            req.setDoctorNo(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));

            //药师处理
            if (recipe.getChecker() != null){
                doctorDTO = doctorMap.get(recipe.getChecker());
                if (null == doctorDTO) {
                    doctorDTO = doctorService.get(recipe.getChecker());
                    doctorMap.put(recipe.getChecker(), doctorDTO);
                }
                if (null == doctorDTO) {
                    LOGGER.warn("uploadRecipeIndicators checker is null. recipe.checker={}", recipe.getChecker());
                    continue;
                }
                req.setAuditDoctorCertID(doctorDTO.getIdNumber());
                req.setAuditDoctor(doctorDTO.getName());
                req.setAuditDoctorId(recipe.getChecker().toString());
            }


            //患者处理
            patientDTO = patientService.get(recipe.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("uploadRecipeIndicators patient is null. recipe.patient={}", recipe.getMpiid());
                continue;
            }

            organDiseaseName = recipe.getOrganDiseaseName().replaceAll("；", "|");
            /*req.setOriginalDiagnosis(organDiseaseName);*/
            req.setPatientCardType(LocalStringUtil.toString(patientDTO.getCertificateType()));
            req.setPatientCertID(LocalStringUtil.toString(patientDTO.getCertificate()));
            req.setPatientName(patientDTO.getPatientName());
            req.setMobile(LocalStringUtil.toString(patientDTO.getMobile()));
            req.setSex(patientDTO.getPatientSex());
            req.setAge(DateConversion.calculateAge(patientDTO.getBirthday()));
            req.setBirthDay(patientDTO.getBirthday());
            //其他信息
            //监管接收方现在使用recipeId去重
            req.setRecipeID(recipe.getRecipeId().toString());
            //处方唯一编号
            req.setRecipeUniqueID(recipe.getRecipeCode());
           /* //互联网医院处方都是经过合理用药审查
            req.setRationalFlag("0");*/

            req.setIcdCode(recipe.getOrganDiseaseId().replaceAll("；", "|"));
            req.setIcdName(organDiseaseName);
            req.setRecipeType(recipe.getRecipeType().toString());
            /*req.setPacketsNum(recipe.getCopyNum());*/
            req.setDatein(recipe.getSignDate());
            req.setEffectivePeriod(recipe.getValueDays());
            req.setStartDate(recipe.getSignDate());
            req.setEndDate(DateConversion.getDateAftXDays(recipe.getSignDate(), recipe.getValueDays()));
            req.setUpdateTime(now);
            /*req.setTotalFee(recipe.getTotalMoney().doubleValue());*/
            req.setIsPay(recipe.getPayFlag().toString());

            //过敏史标记 有无过敏史 0:无 1:有
            req.setAllergyFlag("0");
            //门诊号处理 年月日+患者身份证后5位 例：2019060407915
            /*str = patientDTO.getCertificate().substring(patientDTO.getCertificate().length()-5);*/
            req.setPatientNumber(recipe.getPatientID());

            //撤销标记
            req.setCancelFlag(getVerificationStatus(recipe));
            //详情处理
            detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isEmpty(detailList)) {
                LOGGER.warn("uploadRecipeIndicators detail is null. recipe.id={}", recipe.getRecipeId());
                continue;
            }
            setDetail(req, detailList, usingRateDic, usePathwaysDic,recipe);

            request.add(req);
        }

        try {
            IRegulationService  hisService =
                    AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            LOGGER.info("uploadRecipeIndicators request={}", JSONUtils.toString(request));
            HisResponseTO response = hisService.uploadRecipeIndicators(recipeList.get(0).getClinicOrgan(), request);
            LOGGER.info("uploadRecipeIndicators response={}", JSONUtils.toString(response));
            if (HIS_SUCCESS.equals(response.getMsgCode())) {
                //成功
                commonResponse.setCode(CommonConstant.SUCCESS);
                LOGGER.info("uploadRecipeIndicators execute success.");
            } else {
                commonResponse.setMsg(response.getMsg());
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }

        LOGGER.info("uploadRecipeIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 设置处方详情数据
     *
     * @param req
     * @param detailList
     */
    private void setDetail(RegulationRecipeIndicatorsReq req, List<Recipedetail> detailList,
                           Dictionary usingRateDic, Dictionary usePathwaysDic,Recipe recipe) {
        RegulationRecipeDetailIndicatorsReq reqDetail;
        DrugListDAO drugListDao = DAOFactory.getDAO(DrugListDAO.class);
        List<RegulationRecipeDetailIndicatorsReq> list = new ArrayList<>(detailList.size());
        double dosageDay;
        DrugList drugList;
        for (Recipedetail detail : detailList) {
            reqDetail = new RegulationRecipeDetailIndicatorsReq();
            reqDetail.setDrcode(detail.getOrganDrugCode());
            reqDetail.setDrname(detail.getDrugName());
            reqDetail.setDrmodel(detail.getDrugSpec());
            reqDetail.setPack(detail.getPack());
            reqDetail.setPackUnit(detail.getDrugUnit());
            //频次
            reqDetail.setFrequency(UsingRateFilter.transReguation(detail.getUsingRate()));
            //药品频次名称
            if (null != usingRateDic) {
                reqDetail.setFrequencyName(usingRateDic.getText(detail.getUsingRate()));
            }
            //用法
            reqDetail.setAdmission(UsePathwaysFilter.transReguation(detail.getUsePathways()));
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
            }
            //药物使用总剂量
            reqDetail.setUseDosage("0");
            //药物日药量/DDD值
            dosageDay = (detail.getUseDose())*(UsingRateFilter.transDailyTimes(detail.getUsingRate()));
            reqDetail.setDosageDay(LocalStringUtil.toString(dosageDay));
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
     * 处方核销状态判断，处方完成及开始配送都当做已核销处理
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
}
