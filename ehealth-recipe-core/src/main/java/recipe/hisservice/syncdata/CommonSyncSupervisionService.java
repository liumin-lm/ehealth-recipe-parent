package recipe.hisservice.syncdata;

import com.ngari.patient.dto.*;
import com.ngari.patient.dto.zjs.SubCodeDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.service.zjs.SubCodeService;
import com.ngari.platform.sync.mode.RecipeDetailIndicatorsReq;
import com.ngari.platform.sync.mode.RecipeIndicatorsReq;
import com.ngari.platform.sync.mode.RecipeVerificationIndicatorsReq;
import com.ngari.recipe.entity.*;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ngari.openapi.Client;
import ngari.openapi.Request;
import ngari.openapi.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.common.RecipeCacheService;
import recipe.manager.EmrRecipeManager;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import java.util.*;

/**
 * @author： 0184/yu_yun
 * @date： 2019/2/14
 * @description： 同步监管数据 (openAPI调用)----原本纳里平台上传互联网平台用 现已废弃 兼容HisSyncSupervisionService
 * @version： 1.0
 */
@RpcBean("commonSyncSupervisionService")
@Deprecated
public class CommonSyncSupervisionService implements ICommonSyncSupervisionService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSyncSupervisionService.class);

    private static String HIS_SUCCESS = "200";
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    /**
     * 处方核销接口
     *
     * @param recipeList
     * @return
     */
    @Override
    @RpcService
    public CommonResponse uploadRecipeVerificationIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeVerificationIndicators recipeList length={}", recipeList.size());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }

        ProvUploadOrganService provUploadOrganService =
                AppDomainContext.getBean("basic.provUploadOrganService", ProvUploadOrganService.class);
        List<ProvUploadOrganDTO> provUploadOrganList = provUploadOrganService.findByStatus(1);
        if (CollectionUtils.isEmpty(provUploadOrganList)) {
            LOGGER.warn("uploadRecipeVerificationIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }

        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);

        Map<Integer, OrganDTO> organMap = new HashMap<>(20);
        List<RecipeVerificationIndicatorsReq> request = new ArrayList<>(recipeList.size());
        Date now = DateTime.now().toDate();
        RecipeVerificationIndicatorsReq req;
        RecipeOrder order = null;
        DrugsEnterprise enterprise;
        OrganDTO organDTO;
        for (Recipe recipe : recipeList) {
            req = new RecipeVerificationIndicatorsReq();
//            req.setBussID(LocalStringUtil.toString(recipe.getClinicId()));
            //TODO 此处与互联网分支不一致，应填复诊ID LocalStringUtil.toString(recipe.getClinicId())
            req.setBussID(recipe.getRecipeId().toString());
            //监管接收方现在使用recipeId去重
            req.setRecipeID(recipe.getRecipeId().toString());
            req.setRecipeUniqueID(recipe.getRecipeId().toString());

            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                order = orderDAO.getByOrderCode(recipe.getOrderCode());
            }

            if (null == order) {
                LOGGER.warn("uploadRecipeVerificationIndicators order is null. recipe.orderCode={}",
                        recipe.getOrderCode());
                continue;
            }

            //机构处理
            organDTO = organMap.get(recipe.getClinicOrgan());
            if (null == organDTO) {
                organDTO = organService.get(recipe.getClinicOrgan());
                organMap.put(recipe.getClinicOrgan(), organDTO);
            }
            if (null == organDTO) {
                LOGGER.warn("uploadRecipeVerificationIndicators organ is null. recipe.clinicOrgan={}", recipe.getClinicOrgan());
                continue;
            }
            for (ProvUploadOrganDTO uploadOrgan : provUploadOrganList) {
                if (uploadOrgan.getNgariOrganId().equals(organDTO.getOrganId())) {
                    req.setUnitID(uploadOrgan.getUnitId());
                    req.setOrganID(uploadOrgan.getOrganId());
                    req.setOrganName(organDTO.getName());
                    break;
                }
            }
            if (StringUtils.isEmpty(req.getUnitID())) {
                LOGGER.warn("uploadRecipeVerificationIndicators minkeUnitID is not in minkeOrganList. organ.organId={}",
                        organDTO.getOrganId());
                continue;
            }

            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                req.setDeliveryType("1");
                req.setDeliverySTDate(recipe.getStartSendDate());
                req.setDeliveryFee(order.getExpressFee().toPlainString());
                enterprise = enterpriseDAO.get(order.getEnterpriseId());
                if (null != enterprise) {
                    req.setDeliveryFirm(enterprise.getName());
                }
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())) {
                req.setDeliveryType("0");
                req.setDeliveryFirm("医院药房");
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                req.setDeliveryType("3");
                req.setDeliveryFirm(order.getDrugStoreName());
            }

            req.setVerificationTime(now);
            req.setTotalFee(recipe.getTotalMoney().doubleValue());
            if (PayConstant.PAY_FLAG_PAY_SUCCESS == order.getPayFlag()) {
                req.setIsPay("1");
            } else {
                req.setIsPay("0");
            }
            req.setUpdateTime(now);

            request.add(req);
        }
        try {
            Client client = getOpenClient();
            //X-Service-Id对应的值
            String serviceId = "his.provinceDataUploadService";
            //X-Service-Method对应的值
            String method = "uploadRecipeVerificationIndicators";
            LOGGER.warn("uploadRecipeVerificationIndicators request={}", JSONUtils.toString(request));
            List tempList = new ArrayList(1);
            tempList.add(request);
            Request hisRequest = new Request(serviceId, method, tempList);
            Response response = client.execute(hisRequest);
            LOGGER.info("uploadRecipeVerificationIndicators response={}", JSONUtils.toString(response));
            if (null != response && response.isSuccess()) {
                Map map = (Map)response.getJsonResponseBean().getBody();
                String msgCode = map.get("msgCode").toString();
                if(HIS_SUCCESS.equals(msgCode)) {
                    //成功
                    commonResponse.setCode(CommonConstant.SUCCESS);
                    LOGGER.info("uploadRecipeVerificationIndicators execute success.");
                }else{
                    commonResponse.setMsg(LocalStringUtil.toString(map.get("msg")));
                }
            } else {
                commonResponse.setMsg("上传处方核销监管平台返回异常");
            }
        } catch (Exception e) {
            LOGGER.warn("uploadRecipeVerificationIndicators HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            commonResponse.setMsg("HIS接口调用异常");
        }

        LOGGER.info("uploadRecipeVerificationIndicators commonResponse={}", JSONUtils.toString(commonResponse));
        return commonResponse;
    }

    /**
     * 同步处方数据
     *
     * @param recipeList
     * @return
     */
    @RpcService
    @Override
    public CommonResponse uploadRecipeIndicators(List<Recipe> recipeList) {
        LOGGER.info("uploadRecipeIndicators recipeList length={}", recipeList.size());
        CommonResponse commonResponse = ResponseUtils.getFailResponse(CommonResponse.class, "");
        if (CollectionUtils.isEmpty(recipeList)) {
            commonResponse.setMsg("处方列表为空");
            return commonResponse;
        }

        ProvUploadOrganService provUploadOrganService =
                AppDomainContext.getBean("basic.provUploadOrganService", ProvUploadOrganService.class);
        List<ProvUploadOrganDTO> provUploadOrganList = provUploadOrganService.findByStatus(1);
        if (CollectionUtils.isEmpty(provUploadOrganList)) {
            LOGGER.warn("uploadRecipeIndicators provUploadOrgan list is null.");
            commonResponse.setMsg("需要同步机构列表为空");
            return commonResponse;
        }


        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        SubCodeService subCodeService = BasicAPI.getService(SubCodeService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);

        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        List<RecipeIndicatorsReq> request = new ArrayList<>(recipeList.size());
        Map<Integer, OrganDTO> organMap = new HashMap<>(20);
        Map<Integer, DepartmentDTO> departMap = new HashMap<>(20);
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
        RecipeIndicatorsReq req;
        OrganDTO organDTO;
        String organDiseaseName;
        DepartmentDTO departmentDTO;
        DoctorDTO doctorDTO;
        PatientDTO patientDTO;
        SubCodeDTO subCodeDTO;
        List<Recipedetail> detailList;
        for (Recipe recipe : recipeList) {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            req = new RecipeIndicatorsReq();
            //TODO 此处与互联网分支不一致，应填复诊ID LocalStringUtil.toString(recipe.getClinicId())
            req.setBussID(recipe.getRecipeId().toString());

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
            for (ProvUploadOrganDTO uploadOrgan : provUploadOrganList) {
                if (uploadOrgan.getNgariOrganId().equals(organDTO.getOrganId())) {
                    req.setUnitID(uploadOrgan.getUnitId());
                    req.setOrganID(uploadOrgan.getOrganId());
                    req.setOrganName(organDTO.getName());
                    break;
                }
            }
            if (StringUtils.isEmpty(req.getUnitID())) {
                LOGGER.warn("uploadRecipeIndicators minkeUnitID is not in minkeOrganList. organ.organId={}",
                        organDTO.getOrganId());
                continue;
            }

            //科室处理
            req.setDeptID(recipe.getDepart().toString());
            departmentDTO = departMap.get(recipe.getDepart());
            if (null == departmentDTO) {
                departmentDTO = departmentService.getById(recipe.getDepart());
                departMap.put(recipe.getDepart(), departmentDTO);
            }
            if (null == departmentDTO) {
                LOGGER.warn("uploadRecipeIndicators depart is null. recipe.depart={}", recipe.getDepart());
                continue;
            }
            req.setDeptName(departmentDTO.getName());
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
            if(Integer.valueOf(1).equals(doctorDTO.getTestPersonnel())){
                LOGGER.warn("uploadRecipeIndicators doctor is testPersonnel. recipe.doctor={}", recipe.getDoctor());
                continue;
            }

            req.setDoctorCertID(doctorDTO.getIdNumber());
            req.setDoctorName(doctorDTO.getName());

            //药师处理
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

            //患者处理
            patientDTO = patientService.get(recipe.getMpiid());
            if (null == patientDTO) {
                LOGGER.warn("uploadRecipeIndicators patient is null. recipe.patient={}", recipe.getMpiid());
                continue;
            }

            organDiseaseName = recipe.getOrganDiseaseName().replaceAll("；", "|");
            req.setOriginalDiagnosis(organDiseaseName);
            req.setPatientCardType(LocalStringUtil.toString(patientDTO.getCertificateType()));
            req.setPatientCertID(LocalStringUtil.toString(patientDTO.getCertificate()));
            req.setPatientName(patientDTO.getPatientName());
            req.setMobile(LocalStringUtil.toString(patientDTO.getMobile()));
            req.setSex(patientDTO.getPatientSex());
            req.setAge(DateConversion.calculateAge(patientDTO.getBirthday()));
            //其他信息
            //监管接收方现在使用recipeId去重
            req.setRecipeID(recipe.getRecipeId().toString());
            req.setRecipeUniqueID(recipe.getRecipeId().toString());
            //互联网医院处方都是经过合理用药审查
            req.setRationalFlag("0");

            req.setIcdCode(recipe.getOrganDiseaseId().replaceAll("；", "|"));
            req.setIcdName(organDiseaseName);
            req.setRecipeType(recipe.getRecipeType().toString());
            req.setPacketsNum(recipe.getCopyNum());
            req.setDatein(recipe.getSignDate());
            req.setEffectivePeriod(recipe.getValueDays());
            req.setStartDate(recipe.getSignDate());
            req.setEndDate(DateConversion.getDateAftXDays(recipe.getSignDate(), recipe.getValueDays()));
            req.setUpdateTime(now);
            req.setTotalFee(recipe.getTotalMoney().doubleValue());
            req.setIsPay(recipe.getPayFlag().toString());
            req.setVerificationStatus(getVerificationStatus(recipe));
            //处方pdfId
            if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                req.setRecipeFileId(recipe.getChemistSignFile());
            } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
                req.setRecipeFileId(recipe.getSignFile());
            } else {
                LOGGER.warn("recipeId file is null  recipeId={}", recipe.getRecipeId());
            }
            //详情处理
            detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            if (CollectionUtils.isEmpty(detailList)) {
                LOGGER.warn("uploadRecipeIndicators detail is null. recipe.id={}", recipe.getRecipeId());
                continue;
            }
            setDetail(req, detailList, usingRateDic, usePathwaysDic);

            request.add(req);
        }

        try {
            Client client = getOpenClient();
            //X-Service-Id对应的值
            String serviceId = "his.provinceDataUploadService";
            //X-Service-Method对应的值
            String method = "uploadRecipeIndicators";
            LOGGER.info("uploadRecipeIndicators request={}", JSONUtils.toString(request));
            List tempList = new ArrayList(1);
            tempList.add(request);
            Request hisRequest = new Request(serviceId, method, tempList);
            Response response = client.execute(hisRequest);
            LOGGER.info("uploadRecipeIndicators response={}", JSONUtils.toString(response));
            if (null != response && response.isSuccess()) {
                Map map = (Map)response.getJsonResponseBean().getBody();
                String msgCode = map.get("msgCode").toString();
                if(HIS_SUCCESS.equals(msgCode)) {
                    //成功
                    commonResponse.setCode(CommonConstant.SUCCESS);
                    LOGGER.info("uploadRecipeIndicators execute success.");
                }else{
                    commonResponse.setMsg(LocalStringUtil.toString(map.get("msg")));
                }
            } else {
                commonResponse.setMsg("上传处方监管平台返回异常");
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
    private void setDetail(RecipeIndicatorsReq req, List<Recipedetail> detailList,
                           Dictionary usingRateDic, Dictionary usePathwaysDic) {
        RecipeDetailIndicatorsReq reqDetail;
        List<RecipeDetailIndicatorsReq> list = new ArrayList<>(detailList.size());
        for (Recipedetail detail : detailList) {
            reqDetail = new RecipeDetailIndicatorsReq();
            reqDetail.setDrcode(detail.getOrganDrugCode());
            reqDetail.setDrname(detail.getDrugName());
            reqDetail.setDrmodel(detail.getDrugSpec());
            reqDetail.setPack(detail.getPack());
            reqDetail.setPackUnit(detail.getDrugUnit());
            if (null != usingRateDic) {
                reqDetail.setFrequency(usingRateDic.getText(detail.getUsingRate()));
            }
            if (null != usePathwaysDic) {
                reqDetail.setAdmission(usePathwaysDic.getText(detail.getUsePathways()));
            }
            reqDetail.setDosage(detail.getUseDose().toString());
            reqDetail.setDrunit(detail.getUseDoseUnit());
            reqDetail.setDosageTotal(detail.getUseTotalDose().toString());
            reqDetail.setUseDays(detail.getUseDays());
            reqDetail.setRemark(detail.getMemo());

            list.add(reqDetail);
        }

        req.setOrderList(list);
    }

    /**
     * 处方核销状态判断，处方完成及开始配送都当做已核销处理
     *
     * @param status 0未核销 1已核销
     * @return
     */
    private String getVerificationStatus(Recipe recipe) {
        if (RecipeStatusConstant.FINISH == recipe.getStatus() || RecipeStatusConstant.WAIT_SEND == recipe.getStatus()
                || RecipeStatusConstant.IN_SEND == recipe.getStatus() || RecipeStatusConstant.REVOKE == recipe.getStatus()
                || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus()
                || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus()
                || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            return "1";
        }

        return "0";
    }

    /**
     * 获取openAPI客户端
     *
     * @return
     */
    private Client getOpenClient() {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        String apiUrl = cacheService.getRecipeParam("open_url", null);
        String appKey = cacheService.getRecipeParam("open_appkey", null);
        String appSecret = cacheService.getRecipeParam("open_appsecret", null);
        String encodingAesKey = cacheService.getRecipeParam("open_aeskey", null);
        return new Client(apiUrl, appKey, appSecret, encodingAesKey);
    }
}
