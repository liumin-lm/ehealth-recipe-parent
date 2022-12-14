package recipe.hisservice;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.constant.RecipeHisStatusEnum;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.RecipeExtendBean;
import com.ngari.platform.recipe.mode.RecipeOrderBean;
import com.ngari.recipe.dto.EmrDetailDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.cdr.api.service.IDocIndexService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.client.DocIndexClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.PayFlagEnum;
import recipe.enumerate.type.RecipeDistributionFlagEnum;
import recipe.enumerate.type.RecipeSendTypeEnum;
import recipe.manager.DepartManager;
import recipe.manager.EmrRecipeManager;
import recipe.manager.EnterpriseManager;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.ValidateUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/9/14.
 */
@Service
public class HisRequestInit {

    @Autowired
    private DocIndexClient docIndexClient;

    @Autowired
    private IDocIndexService docIndexService;

    @Autowired
    private DepartManager departManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(HisRequestInit.class);

    public RecipeSendRequestTO initRecipeSendRequestTOForWuChang(Recipe recipe, List<Recipedetail> details, PatientBean patient) {
        RecipeSendRequestTO requestTO = new RecipeSendRequestTO();
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        try {
            String recipeIdStr = Integer.toString(recipe.getRecipeId());
            requestTO.setRecipeID(recipeIdStr);
            requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
            requestTO.setDatein(recipe.getSignDate());
            requestTO.setStartDate(recipe.getSignDate());
            requestTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe.getPayFlag()) : null);

            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            requestTO.setIcdCode(recipe.getOrganDiseaseId());
            requestTO.setIcdName(recipe.getOrganDiseaseName());
            // 简要病史
            requestTO.setDiseasesHistory(recipe.getOrganDiseaseName());
            requestTO.setDeptID("");
            requestTO.setRecipeType((null != recipe.getRecipeType()) ? recipe.getRecipeType().toString() : null);
            //new 字段
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            //组织机构编码
            requestTO.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
            //性别
            requestTO.setPatientSex(patient.getPatientSex());
            //出生日期
            requestTO.setBirthDay(DateConversion.formatDate(patient.getBirthday()));
            //科室代码
            AppointDepartDTO appointDepart = departManager.getAppointDepartByOrganIdAndDepart(recipe);
            requestTO.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
            //科室名称
            requestTO.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
            //操作时间
            requestTO.setOperationTime(DateConversion.formatDateTimeWithSec(recipe.getSignDate()));
            //操作员代码(医生身份证)
            DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
            DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
            requestTO.setDoctorIdCard(doctorDTO.getIdNumber());
            //操作员名称
            requestTO.setDoctorName(recipe.getDoctorName());
            //挂号金额
            requestTO.setRegistrationFee(new BigDecimal(0));
            //自费金额
            requestTO.setSelfPayingFee(recipe.getActualPrice());
            //医保补偿
            requestTO.setHealthCompensation(new BigDecimal(0));
            //舍入金额
            requestTO.setRoundingFee(new BigDecimal(0));
            //费别代码
            switch (patient.getPatientType()) {
                case "1"://自费
                    requestTO.setPatientTypeCode("0");
                    break;
                case "2"://医保
                    requestTO.setPatientTypeCode("2");
                    break;
            }
            //费别名称
            requestTO.setPatientTypeName(DictionaryController.instance().get("eh.mpi.dictionary.PatientType").getText(patient.getPatientType()));
            //单据类别
            if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                requestTO.setRecipeType("2");//中药处方
            } else {
                requestTO.setRecipeType("1");//西药处方
            }
            //处方类型代码
            requestTO.setRecipeTypeCode(0);
            //处方类型名称
            requestTO.setRecipeTypeName("普通");
            //药品付数、西药。中成药默认1
            requestTO.setDrugNum(1);
            //基药标志 默认0
            requestTO.setBasicmedicineFlag(0);
            //处方来源
            requestTO.setRecipeSource("04");//00：HIS 01：体检 02：签约 03：计免 04：延伸处方
            //处方号
            requestTO.setRecipeCode(recipe.getRecipeCode());
            //出参标识
            /*requestTO.setCCBS("1");//1-有库存出参 0-无库存出参*/

            if (null != patient) {
                // 患者信息
                String idCard = patient.getCertificate();
                if (StringUtils.isNotEmpty(idCard)) {
                    //没有身份证儿童的证件处理
                    String childFlag = "-";
                    if (idCard.contains(childFlag)) {
                        idCard = idCard.split(childFlag)[0];
                    }
                }
                requestTO.setCertID(idCard);
                requestTO.setPatientName(patient.getPatientName());
                requestTO.setMobile(patient.getMobile());
                requestTO.setCertificateType(patient.getCertificateType());
            }
            /*if (null != card) {
                requestTO.setCardType(card.getCardType());//2-医保卡
                requestTO.setCardNo(card.getCardId());
            }else {
                requestTO.setCardType("4");//武昌-4-身份证
                requestTO.setCardNo(patient.getIdcard());
            }*/
            requestTO.setCardType("4");//武昌-4-身份证
            requestTO.setCardNo(patient.getIdcard());
            //根据处方单设置配送方式
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                requestTO.setDeliveryType("1");
            } else {
                requestTO.setDeliveryType("0");
            }
            requestTO.setTakeMedicine(recipe.getTakeMedicine());
            // 设置结束日期
            Calendar c = Calendar.getInstance();
            c.setTime(recipe.getSignDate());
            c.add(Calendar.DATE, 3);
            requestTO.setEndDate(c.getTime());

            if (null != details && !details.isEmpty()) {
                List<OrderItemTO> orderList = new ArrayList<>();
                int i = 1;
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                OrganDrugList organDrug;
                for (Recipedetail detail : details) {
                    OrderItemTO orderItem = new OrderItemTO();
                    orderItem.setOrderID(Integer.toString(detail.getRecipeDetailId()));
                    orderItem.setDrcode(detail.getOrganDrugCode());
                    orderItem.setDrname(detail.getDrugName());
                    orderItem.setDrmodel(detail.getDrugSpec());
                    orderItem.setPackUnit(detail.getDrugUnit());
                    orderItem.setDrugId(detail.getDrugId());

                    orderItem.setAdmission(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(), detail.getUsePathways()));
                    orderItem.setFrequency(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(), detail.getUsingRate()));
                    if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                        orderItem.setDosage(detail.getUseDoseStr());
                    } else {
                        orderItem.setDosage((null != detail.getUseDose()) ? Double.toString(detail.getUseDose()) : null);
                    }
                    orderItem.setDrunit(detail.getUseDoseUnit());
                    /*
                     * //每日剂量 转换成两位小数 DecimalFormat df = new DecimalFormat("0.00");
                     * String dosageDay =
                     * df.format(getFrequency(detail.getUsingRate(
                     * ))*detail.getUseDose());
                     */
                    // 传用药总量 药品包装 * 开药数量
                    Double dos = detail.getUseTotalDose() * detail.getPack();
                    orderItem.setDosageDay(dos.toString());

                    orderItem.setRemark(detail.getMemo());
                    orderItem.setPack(detail.getPack());

                    //项目类别（0：药品 1：收费项目）
                    orderItem.setItemClass("0");
                    //项目单价
                    orderItem.setItemPrice(detail.getSalePrice());
                    //项目数量
                    orderItem.setItemCount(new BigDecimal(detail.getUseTotalDose()));
                    //金额
                    orderItem.setItemTotalPrice(detail.getDrugCost());
                    //转换系数
                    orderItem.setConversionFactor(detail.getPack());
                    //服用天数
                    //date 20200526
                    //线上推送处方到his，将用药天数big转成int
                    orderItem.setUseDays(detail.getUseDays());
                    //每天次数
                    orderItem.setDailyTimes(UsingRateFilter.transDailyTimes(detail.getUsingRate()));
                    //付数
                    if (RecipeUtil.isTcmType(recipe.getRecipeType())) {
                        orderItem.setDrugFs(detail.getUseTotalDose().intValue());//中药处方
                    } else {
                        orderItem.setDrugFs(1);//西药处方
                    }
                    //分组序号
                    orderItem.setGroupNo(1);
                    //排序序号
                    orderItem.setOrderNo(i);
                    //剂量系数
                    orderItem.setDoseFactor(1);
                    //药品转换系数
                    orderItem.setDrugConversionFactor(detail.getPack());
                    //处方标识
                    orderItem.setRecipeFlag(1);
                    organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
                    if (null != organDrug) {
                        orderItem.setUseDoseSmallestUnit(organDrug.getUseDoseSmallestUnit());
                        //生产厂家
                        orderItem.setManfcode(organDrug.getProducerCode());
                        //药房名称
                        orderItem.setPharmacy(organDrug.getPharmacyName());
                        //单价
                        orderItem.setItemPrice(organDrug.getSalePrice());
                        //产地名称
                        orderItem.setDrugManf(organDrug.getProducer());
                        //机构药品编码
                        orderItem.setDrugItemCode(organDrug.getDrugItemCode());
                    }

                    orderList.add(orderItem);
                    i += 1;
                }

                requestTO.setOrderList(orderList);
            } else {
                requestTO.setOrderList(null);
            }
            //设置医生工号
            requestTO.setDoctorID(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        } catch (Exception e) {
            LOGGER.error("initRecipeSendRequestTOForWuChang 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        return requestTO;

    }

    public RecipeSendRequestTO initRecipeSendRequestTO(Recipe recipe, List<Recipedetail> details, PatientBean patient) {
        RecipeSendRequestTO requestTO = new RecipeSendRequestTO();
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        requestTO.setRecipeID(recipe.getRecipeId().toString());
        requestTO.setMedicalFlag(recipe.getMedicalFlag());
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
        requestTO.setDatein(recipe.getSignDate());
        requestTO.setStartDate(recipe.getSignDate());
        requestTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe.getPayFlag()) : null);
        requestTO.setDeptID(Objects.nonNull(recipe.getDepart())?recipe.getDepart().toString():"");
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? recipe.getRecipeType().toString() : null);
        requestTO.setRecipeDrugForm(recipe.getRecipeDrugForm());
        //处方附带信息
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        LOGGER.info("initRecipeSendRequestTO recipeExtend={}", JSONUtils.toString(recipeExtend));
        // 简要病史
        if (null != recipeExtend) {
            EmrDetailDTO emrDetail = docIndexClient.getEmrDetails(recipeExtend.getDocIndexId());
            if (StringUtils.isNotEmpty(emrDetail.getOrganDiseaseId())) {
                recipe.setOrganDiseaseName(emrDetail.getOrganDiseaseName());
                recipe.setOrganDiseaseId(emrDetail.getOrganDiseaseId());
                recipeExtend.setMainDieaseDescribe(emrDetail.getMainDieaseDescribe());
                recipeExtend.setHistoryOfPresentIllness(emrDetail.getHistoryOfPresentIllness());
                recipeExtend.setSymptomId(emrDetail.getSymptomId());
                recipeExtend.setSymptomName(emrDetail.getSymptomName());
                recipeExtend.setCurrentMedical(emrDetail.getCurrentMedical());
                recipeExtend.setHistroyMedical(emrDetail.getHistroyMedical());
                recipeExtend.setAllergyMedical(emrDetail.getAllergyMedical());
                recipeExtend.setPhysicalCheck(emrDetail.getPhysicalCheck());
                recipeExtend.setHandleMethod(emrDetail.getHandleMethod());
                requestTO.setSymptomValue(ObjectCopyUtils.convert(emrDetail.getSymptomValue(), EmrDetailValueDTO.class));
                requestTO.setDiseaseValue(ObjectCopyUtils.convert(emrDetail.getDiseaseValue(), EmrDetailValueDTO.class));
                Map<String, Object> medicalInfoBean = docIndexService.getMedicalInfoByDocIndexId(recipeExtend.getDocIndexId());
                requestTO.setMedicalInfoBean(medicalInfoBean);
            }
            //挂号序号
            requestTO.setRegisteredId(recipeExtend.getRegisterID());
            //主诉
            requestTO.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
            //现病史
            requestTO.setHistoryOfPresentIllness(recipeExtend.getHistoryOfPresentIllness());
            //处理方法
            requestTO.setHandleMethod(recipeExtend.getHandleMethod());
            requestTO.setCardNo(recipeExtend.getCardNo());
            requestTO.setCardType(recipeExtend.getCardType());
            requestTO.setPatientId(recipe.getPatientID());
            //处方扩展信息
            requestTO.setRecipeExtend(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
            //终端是否为自助机
            requestTO.getRecipeExtend().setSelfServiceMachineFlag(new Integer(1).equals(recipeExtend.getTerminalType()));
            try {
                //制法Code 煎法Code 中医证候Code
                DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                    DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                    if (null != decoctionWay) {
                        requestTO.getRecipeExtend().setDecoctionCode(decoctionWay.getDecoctionCode());
                        requestTO.setGenerationisOfDecoction(decoctionWay.getGenerationisOfDecoction());
                        requestTO.setDecoctionCode(decoctionWay.getDecoctionCode());
                        requestTO.setDecoctionText(decoctionWay.getDecoctionText());
                        requestTO.setDecoctionUnitPrice(decoctionWay.getDecoctionPrice());
                    }
                }
                if (StringUtils.isNotBlank(recipeExtend.getDoctorIsDecoction())) {
                    requestTO.setGenerationisOfDecoction(recipeExtend.getDoctorIsDecoction().equals("1"));
                }
                if (StringUtils.isNotBlank(recipeExtend.getMakeMethodId())) {
                    DrugMakingMethodDao drugMakingMethodDao = DAOFactory.getDAO(DrugMakingMethodDao.class);
                    DrugMakingMethod drugMakingMethod = drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                    requestTO.getRecipeExtend().setMakeMethod(drugMakingMethod.getMethodCode());
                }
                if (StringUtils.isNotBlank(recipeExtend.getEveryTcmNumFre())) {
                    requestTO.getRecipeExtend().setEveryTcmNumFre(recipeExtend.getEveryTcmNumFre());
                }

                if (StringUtils.isNotBlank(recipeExtend.getRequirementsForTakingCode())) {
                    requestTO.getRecipeExtend().setRequirementsForTakingCode(recipeExtend.getRequirementsForTakingCode());
                }
                if (StringUtils.isNotBlank(recipeExtend.getRequirementsForTakingText())) {
                    requestTO.getRecipeExtend().setRequirementsForTakingText(recipeExtend.getRequirementsForTakingText());
                }
                if (StringUtils.isNotBlank(recipeExtend.getSymptomId())) {
                    requestTO.getRecipeExtend().setSymptomCode(recipeExtend.getSymptomId());
                }
            } catch (Exception e) {
                LOGGER.error("initRecipeSendRequestTO recipeid:{} error ", recipe.getRecipeId(), e);
            }
        }
        requestTO.setIcdCode(recipe.getOrganDiseaseId());
        requestTO.setIcdName(recipe.getOrganDiseaseName());
        requestTO.setDiseasesHistory(recipe.getOrganDiseaseName());
        try {
            if (recipe.getClinicId() != null) {
                requestTO.setClinicId(recipe.getClinicId());
                requestTO.setBussSource(recipe.getBussSource());
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO revisitExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                if (null != revisitExDTO) {
                    if (requestTO.getRegisteredId() == null) {
                        requestTO.setPatientChannelId(revisitExDTO.getProjectChannel());
                    }
                    requestTO.setRegisteredId(revisitExDTO.getRegisterNo());
                    requestTO.setCardType(revisitExDTO.getCardType());
                    requestTO.setCardNo(revisitExDTO.getCardId());
                    requestTO.setTreatmentId(revisitExDTO.getTreatmentId());
                    requestTO.setSerialNumberOfReception(revisitExDTO.getSerialNumberOfReception());
                }
            }
        } catch (Exception e) {
            LOGGER.error("initRecipeSendRequestTO recipeid:{}, clinicId:{} error", recipe.getRecipeId(), recipe.getClinicId(), e);
        }
        //科室代码
        AppointDepartDTO appointDepart = departManager.getAppointDepartByOrganIdAndDepart(recipe);
        requestTO.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        requestTO.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        DepartmentService departService = ApplicationUtils.getBasicService(DepartmentService.class);
        DepartmentDTO departmentDTO = departService.getById(recipe.getDepart());
        //互联网环境下没有挂号科室 取department表
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            //科室编码
            requestTO.setDepartCode((null != departmentDTO) ? departmentDTO.getCode() : "");
            //科室名称
            requestTO.setDepartName((null != departmentDTO) ? departmentDTO.getName() : "");
        }
        if (Objects.nonNull(departmentDTO)) {
            requestTO.setDeptCode(departmentDTO.getCode());
            requestTO.setDeptName(departmentDTO.getName());
        }
        if (StringUtils.isNotEmpty(recipe.getCommonRecipeCode())) {
            requestTO.setCommonRecipeCode(recipe.getCommonRecipeCode());
        }
        if (Objects.nonNull(recipe.getDecoctionNum())) {
            requestTO.setDecoctionNum(recipe.getDecoctionNum());
        }
        //医生名字
        requestTO.setDoctorName(recipe.getDoctorName());
        //设置医生工号
        requestTO.setDoctorID(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        //处方金额
        requestTO.setSelfPayingFee(recipe.getActualPrice());
        if (null != patient) {
            //患者userId
            requestTO.setUserId(patient.getLoginId());
            // 患者信息
            String idCard = patient.getCertificate();
            if (StringUtils.isNotEmpty(idCard)) {
                //没有身份证儿童的证件处理
                String childFlag = "-";
                if (idCard.contains(childFlag)) {
                    idCard = idCard.split(childFlag)[0];
                }
            }
            requestTO.setCertID(idCard);
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setMobile(patient.getMobile());
            requestTO.setCertificateType(patient.getCertificateType());
        }

        //根据处方单设置配送方式
        if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
            requestTO.setDeliveryType("1");
        } else {
            if (recipe.getGiveMode() == null) {
                requestTO.setDeliveryType("0");
            } else {
                switch (recipe.getGiveMode()) {
                    //配送到家
                    case 1:
                        requestTO.setDeliveryType("1");
                        break;
                    //到院取药
                    case 2:
                        requestTO.setDeliveryType("0");
                        break;
                    //药店取药
                    case 3:
                        requestTO.setDeliveryType("2");
                        break;
                    default:
                        requestTO.setDeliveryType("0");
                }
            }
        }
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrderDAO dao = getDAO(RecipeOrderDAO.class);
            RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
            if (order != null) {
                //第三方支付交易流水号（支付机构
                requestTO.setTradeNo(order.getTradeNo());
                //商户订单号（平台）
                requestTO.setOutTradeNo(order.getOutTradeNo());
            }
        }
        requestTO.setTakeMedicine(recipe.getTakeMedicine());
        // 设置结束日期
        Calendar c = Calendar.getInstance();
        c.setTime(recipe.getSignDate());
        c.add(Calendar.DATE, 3);
        requestTO.setEndDate(c.getTime());
        // 医嘱
        requestTO.setRecipeMemo(recipe.getRecipeMemo());
        // 剂数
        requestTO.setCopyNum(recipe.getCopyNum());
        List<OrderItemTO> orderList = new ArrayList<>();
        if (CollectionUtils.isEmpty(details)) {
            return requestTO;
        }
        try {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
            for (Recipedetail detail : details) {
                OrderItemTO orderItem = new OrderItemTO();
                orderItem.setOrderID(Integer.toString(detail.getRecipeDetailId()));
                orderItem.setDrcode(detail.getOrganDrugCode());
                orderItem.setDrname(detail.getDrugName());
                //药品规格
                orderItem.setDrmodel(detail.getDrugSpec());
                orderItem.setPackUnit(detail.getDrugUnit());
                orderItem.setDrugId(detail.getDrugId());
                //存在为了兼容
                orderItem.setAdmission(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(), detail.getUsePathways()));
                orderItem.setFrequency(UsingRateFilter.filterNgari(recipe.getClinicOrgan(), detail.getUsingRate()));
                //机构频次代码
                orderItem.setOrganUsingRate(detail.getOrganUsingRate());
                //机构用法代码
                orderItem.setOrganUsePathways(detail.getOrganUsePathways());
                orderItem.setAdmissionName(detail.getUsePathwaysTextFromHis());
                //频次名称
                orderItem.setFrequencyName(detail.getUsingRateTextFromHis());
                if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
                    orderItem.setDosage(detail.getUseDoseStr());
                } else {
                    orderItem.setDosage((null != detail.getUseDose()) ? Double.toString(detail.getUseDose()) : null);
                }
                orderItem.setDrunit(detail.getUseDoseUnit());
                // 传用药总量 药品包装 * 开药数量
                Double dos = detail.getUseTotalDose() * detail.getPack();
                orderItem.setDosageDay(dos.toString());

                orderItem.setRemark(detail.getMemo());
                orderItem.setType(detail.getType());
                orderItem.setPack(detail.getPack());
                //用药天数
                orderItem.setUseDays(detail.getUseDays());
                //药品数量
                orderItem.setItemCount(new BigDecimal(detail.getUseTotalDose()));
                //药房
                if (detail.getPharmacyId() != null) {
                    PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(detail.getPharmacyId());
                    if (pharmacyTcm != null) {
                        orderItem.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                        orderItem.setPharmacy(pharmacyTcm.getPharmacyName());
                    }
                }
                if (StringUtils.isNotEmpty(detail.getSuperScalarCode())) {
                    //设置超量编码
                    orderItem.setSuperScalarCode(detail.getSuperScalarCode());
                    //设置超量原因
                    orderItem.setSuperScalarName(detail.getSuperScalarName());
                }
                OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
                if (null != organDrug) {
                    orderItem.setUseDoseSmallestUnit(organDrug.getUseDoseSmallestUnit());
                    //生产厂家
                    orderItem.setManfcode(organDrug.getProducerCode());
                    //药房名称
                    if (StringUtils.isEmpty(orderItem.getPharmacy())) {
                        orderItem.setPharmacy(organDrug.getPharmacyName());
                    }
                    //单价
                    orderItem.setItemPrice(organDrug.getSalePrice());
                    //产地名称
                    orderItem.setDrugManf(organDrug.getProducer());
                    //机构药品编码
                    orderItem.setDrugItemCode(organDrug.getDrugItemCode());
                    //最小售卖单位/单位HIS编码
                    if(organDrug.getUnitHisCode() != null){
                        orderItem.setUnitHisCode(organDrug.getUnitHisCode());
                    }
                    //规格单位/单位HIS编码
                    if(organDrug.getUseDoseUnitHisCode() != null){
                        orderItem.setUseDoseUnitHisCode(organDrug.getUseDoseUnitHisCode());
                    }
                    //单位剂量单位（最小单位）/单位his编码
                    if(organDrug.getUseDoseSmallestUnitHisCode() != null){
                        orderItem.setUseDoseSmallestUnitHisCode(organDrug.getUseDoseSmallestUnitHisCode());
                    }
                }
                //设置单个药品医保类型
                orderItem.setDrugMedicalFlag(detail.getDrugMedicalFlag());
                orderList.add(orderItem);
            }
        } catch (Exception e) {
            LOGGER.error("initRecipeSendRequestTO error ", e);
        }
        requestTO.setOrderList(orderList);
        LOGGER.info("initRecipeSendRequestTO recipeId:{},requestTO:{}", recipe.getRecipeId(), JSONUtils.toString(requestTO));
        return requestTO;
    }

    public PayNotifyReqTO initPayNotifyReqTO(List<String> recipeIdList, Recipe recipe, PatientBean patient, HealthCardBean card) {
        PayNotifyReqTO requestTO = new PayNotifyReqTO();
        try {
            requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
            requestTO.setRecipeNo(recipe.getRecipeCode());
            requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer.toString(recipe.getRecipeType()) : null);
            // 目前都是平台代收 后面要改
            requestTO.setPayType("1");
            if (null != patient) {
                // 患者信息
                requestTO.setCertID(patient.getCertificate());
                requestTO.setCertificateType(patient.getCertificateType());
                requestTO.setPatientName(patient.getPatientName());
            }
            requestTO.setPatId(recipe.getPatientID());
            requestTO.setPayMode(recipe.getGiveMode());

            if (null != card) {
                requestTO.setCardType(card.getCardType());
                requestTO.setCardNo(card.getCardId());
            }

            requestTO.setAmount(recipe.getTotalMoney().toString());
            requestTO.setMobile(patient.getMobile());
            requestTO.setIsMedicalSettle("0");
            if (recipe.getOrderCode() != null) {
                requestTO.setOrderCode(recipe.getOrderCode());
                RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());

                if (order != null) {
                    //新参数-把整个订单信息传给前置机
                    requestTO.setRecipeOrderBean(ObjectCopyUtils.convert(order, RecipeOrderBean.class));
                    //如果预结算有值则直接返回预结算的金额
                    if (order.getPreSettletotalAmount() != null) {
                        requestTO.setAmount(order.getPreSettletotalAmount().toString());
                    }
                    //省医保订单新增逻辑
                    switch (order.getOrderType()) {
                        case 0:
                            requestTO.setIsMedicalSettle("0");
                            break;
                        case 1:
                            requestTO.setIsMedicalSettle("1");
                            break;
                        default:
                            requestTO.setIsMedicalSettle("0");
                    }
                    if ("40".equals(order.getWxPayWay())) {
                        requestTO.setPayType("C");
                    } else {
                        requestTO.setPayType("E");
                    }
                    requestTO.setRecipeNo(recipe.getRecipeId() + "");
                    requestTO.setRecipeCode(recipe.getRecipeCode());
                    requestTO.setOrganName(recipe.getOrganName());
                    RecipeExtendDAO extendDAO = getDAO(RecipeExtendDAO.class);
                    RecipeExtend extend = extendDAO.getByRecipeId(recipe.getRecipeId());
                    if (extend != null) {
                        if (StringUtils.isNotEmpty(extend.getTerminalId())) {
                            //终端id
                            requestTO.setTerminalId(extend.getTerminalId());
                        }
                        if (extend.getTerminalType() != null ) {
                            //终端类型 1 自助机
                            requestTO.setTerminalType(extend.getTerminalType());
                        }
                        //终端是否为自助机
                        requestTO.setSelfServiceMachineFlag(new Integer(1).equals(extend.getTerminalType()));

                        if (StringUtils.isNotEmpty(extend.getIllnessType())) {
                            // 大病标识
                            requestTO.setIllnessType(extend.getIllnessType());
                        }
                        //参保地行政区划代码
                        requestTO.setInsuredArea(extend.getInsuredArea());
                        //挂号序号
                        //查询已经预结算过的挂号序号
                        if (StringUtils.isNotEmpty(extend.getRegisterID())) {
                           /* List<RecipeExtend> recipeExtends = extendDAO.querySettleRecipeExtendByRegisterID(extend.getRegisterID());
                            if (CollectionUtils.isEmpty(recipeExtends)) {
                                //his作为是否返回诊察费的判断  诊察费已在预结算总金额里返回*/
                            requestTO.setRegisterID(extend.getRegisterID());
                        }

                        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
                        //获取医保支付流程配置（2-原省医保 3-长三角）
                        Integer insuredAreaType = (Integer) configService.getConfiguration(recipe.getClinicOrgan(), "provincialMedicalPayFlag");
                        if (new Integer(3).equals(insuredAreaType) && StringUtils.isNotEmpty(extend.getInsuredArea())) {
                            //省医保参保类型 1 长三角 没有赋值就是原来的省直医保
                            requestTO.setInsuredAreaType("1");
                        }
                        //自负金额
                        if (order.getCashAmount() != null) {
                            requestTO.setCashAmount(BigDecimal.valueOf(order.getCashAmount()).toPlainString());
                            //应付金额
                            requestTO.setPayAmount(BigDecimal.valueOf(order.getCashAmount()).toPlainString());
                        }
                        if (order.getPreSettletotalAmount() != null) {
                            //总金额
                            requestTO.setPreSettleTotalAmount(BigDecimal.valueOf(order.getPreSettletotalAmount()).toPlainString());
                        }
                        //his收据号
                        requestTO.setHisSettlementNo(order.getHisSettlementNo());
                    }

                    requestTO.setTradeNo(order.getTradeNo());
                    requestTO.setOutTradeNo(order.getOutTradeNo());
                    try {
                        if (Objects.nonNull(order)) {
                            requestTO.setRegisterFee(order.getRegisterFee());
                            requestTO.setRegisterFeeNo(order.getRegisterFeeNo());
                            requestTO.setTcmFee(order.getTcmFee());
                            requestTO.setTcmFeeNo(order.getTcmFeeNo());
                            requestTO.setOrderCode(order.getOrderCode());
                        }
                    } catch (Exception e) {
                        LOGGER.error("MedicalPreSettleService 代缴费用有误");
                    }
                }
                //合并支付的处方需要将所有his处方编码传过去
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                List<Recipe> recipeS = recipeDAO.findRecipeListByOrderCode(recipe.getOrderCode());
                if (CollectionUtils.isNotEmpty(recipeS)) {
                    List<String> recipeNoS = recipeS.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
                    requestTO.setRecipeNoS(recipeNoS);
                }
            }
        } catch (Exception e) {
            LOGGER.error("initPayNotifyReqTO error  recipeId={}", recipe.getRecipeId(), e);
        }
        return requestTO;
    }

    public static DrugTakeChangeReqTO initDrugTakeChangeReqTO(Recipe recipe, List<Recipedetail> list, PatientBean patient, HealthCardBean card) {
        LOGGER.info("HisRequestInit initDrugTakeChangeReqTO recipe ={}", recipe.getRecipeId());
        DrugTakeChangeReqTO requestTO = new DrugTakeChangeReqTO();
        try {
            requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
            if (null != card) {
                requestTO.setCardType(card.getCardType());
                requestTO.setCardNo(card.getCardId());
            }
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            requestTO.setPatientID(recipe.getPatientID());
            if (null != recipeExtend) {
                requestTO.setRegisterID(recipeExtend.getRegisterID());
                if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                    DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                    //是否代煎
                    requestTO.setGenerationisOfDecoction(decoctionWay.getGenerationisOfDecoction());
                    requestTO.setDecoctionCode(decoctionWay.getDecoctionCode());
                    requestTO.setDecoctionText(decoctionWay.getDecoctionText());
                }
            }

            if (null != patient) {
                requestTO.setPatientName(patient.getPatientName());
                requestTO.setCertID(patient.getCertificate());
                requestTO.setCertificateType(patient.getCertificateType());
                requestTO.setMobile(patient.getMobile());
            }
            RecipeOrder order = null;
            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
                order = dao.getByOrderCode(recipe.getOrderCode());
            }

            if (order != null) {
                //期待配送时间
                requestTO.setPlanDate(order.getExpectSendDate());
                requestTO.setPlanTime(order.getExpectSendTime());
                //设置预约取药开始和结束时间
                // 数据库默认设置取药时间为 "1970-01-01 00:00:01" 所以只有当取药时间不等于 1970-01-01 00:00:01才给前置机传
                String defaultTime = "1970-01-01 00:00:01";
                if (!defaultTime.equals(order.getExpectStartTakeTime())) {
                    requestTO.setExpectStartTakeTime(order.getExpectStartTakeTime());
                    requestTO.setExpectEndTakeTime(order.getExpectEndTakeTime());
                }
                //收货人
                requestTO.setConsignee(order.getReceiver());
                //联系电话
                requestTO.setContactTel(order.getRecMobile());
                //收货地址
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                requestTO.setAddress(commonRemoteService.getCompleteAddress(order));
                requestTO.setTrackingNumber(order.getTrackingNumber());
                if (order.getLogisticsCompany() != null) {
                    String logisticsCompany = DictionaryController.instance().get("eh.cdr.dictionary.LogisticsCompany").getText(order.getLogisticsCompany());
                    requestTO.setLogisticsCompany(logisticsCompany);
                }
                //设置省市区编码
                if (StringUtils.isNotEmpty(order.getAddress1())) {
                    requestTO.setProvinceCode(ValidateUtil.isEmpty(order.getAddress1()) + "0000");
                    requestTO.setCityCode(ValidateUtil.isEmpty(order.getAddress2()) + "00");
                    requestTO.setDistrictCode(ValidateUtil.isEmpty(order.getAddress3()));
                    requestTO.setStreetCode(ValidateUtil.isEmpty(order.getStreetAddress()));
                    requestTO.setCommunityCode(ValidateUtil.isEmpty(order.getAddress5()));
                    requestTO.setCommunityName(ValidateUtil.isEmpty(order.getAddress5Text()));
                }
                //合并支付的处方需要将所有his处方编码传过去
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                List<Recipe> recipeS = recipeDAO.findRecipeListByOrderCode(recipe.getOrderCode());
                if (CollectionUtils.isNotEmpty(recipeS)) {
                    List<String> recipeNoS = recipeS.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
                    requestTO.setRecipeNoS(recipeNoS);
                }
                if (Objects.nonNull(order.getEnterpriseId())) {
                    DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                    requestTO.setThirdEnterpriseCode(drugsEnterprise.getThirdEnterpriseCode());
                    requestTO.setEnterpriseCode(drugsEnterprise.getEnterpriseCode());
                }
                requestTO.setOrderId(order.getOrderId());
                requestTO.setOrderCode(order.getOrderCode());
            }

            //此处就行改造
            if (null != recipe.getGiveMode()) {
                if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(recipe.getGiveMode())) {
                    requestTO.setTakeDrugsType("0");
                }
                if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                    if (order != null) {
                        Integer depId = order.getEnterpriseId();
                        if (depId != null) {
                            DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                            DrugsEnterprise drugsEnterprise = enterpriseDAO.getById(depId);
                            EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
                            Integer sendType = enterpriseManager.getEnterpriseSendType(order.getOrganId(), depId);
                            if (drugsEnterprise != null && sendType == RecipeSendTypeEnum.NO_PAY.getSendType()) {
                                //药企配送
                                requestTO.setTakeDrugsType("2");
                            } else {
                                //医院配送
                                requestTO.setTakeDrugsType("1");
                            }
                        }
                    }
                }
                if (RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
                    requestTO.setTakeDrugsType("3");
                }
            }

            requestTO.setRecipeNo(recipe.getRecipeCode());
            requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer.toString(recipe.getRecipeType()) : null);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe nowRecipe = recipeDAO.getByRecipeId(recipe.getRecipeId());
            LOGGER.info("HisRequestInit initDrugTakeChangeReqTO recipe:{},order:{}.", JSONUtils.toString(nowRecipe), JSONUtils.toString(order));
            RecipeHisStatusEnum recipeHisStatusEnum = RecipeHisStatusEnum.getRecipeHisStatusEnum(nowRecipe.getStatus());
            if (Objects.nonNull(recipeHisStatusEnum)) {
                requestTO.setRecipeStatus(recipeHisStatusEnum.getValue());
            }
            if (null == requestTO.getRecipeStatus() && null != order && PayFlagEnum.PAYED.getType().equals(order.getPayFlag())
                    && (RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType().equals(nowRecipe.getStatus()) ||
                    RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType().equals(nowRecipe.getStatus()))) {
                requestTO.setRecipeStatus(0);
            }
            //设置药房信息
            requestTO.setPharmacyCode("");
            requestTO.setPharmacyName("");
            if (CollectionUtils.isNotEmpty(list)) {
                Recipedetail recipeDetail = list.get(0);
                if (null != recipeDetail && null != recipeDetail.getPharmacyId()) {
                    PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                    PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(recipeDetail.getPharmacyId());
                    if (null != pharmacyTcm) {
                        requestTO.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                        requestTO.setPharmacyName(pharmacyTcm.getPharmacyName());
                    }
                }
            }
            if (null != recipe.getFastRecipeFlag()) {
                requestTO.setFastRecipeFlag(recipe.getFastRecipeFlag());
            } else {
                requestTO.setFastRecipeFlag(0);
            }
            // 医院系统医嘱号（一张处方多条记录用|分隔）
            StringBuilder str = new StringBuilder("");
            if (null != list && list.size() != 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (i < list.size() - 1) {
                        str.append(list.get(i).getOrderNo() + "|");
                    } else {
                        str.append(list.get(i).getOrderNo());
                    }
                }
            }
            requestTO.setOrderNo(str.toString());
        } catch (Exception e) {
            LOGGER.error("initDrugTakeChangeReqTO error", e);
        }
        LOGGER.info("HisRequestInit initDrugTakeChangeReqTO requestTO:{}.", JSONUtils.toString(requestTO));
        return requestTO;
    }

    public RecipeStatusUpdateReqTO initRecipeStatusUpdateReqForWuChang(Recipe recipe, List<Recipedetail> list, PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer.toString(recipe.getRecipeType()) : null);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        //组织机构编码
        requestTO.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        requestTO.setPatientID(recipe.getPatientID());
        if (null != patient) {
            // 患者信息
            requestTO.setCertID(patient.getCertificate());
            requestTO.setCertificateType(patient.getCertificateType());
            requestTO.setPatientName(patient.getPatientName());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }
        //操作员工号
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
        requestTO.setDoctorID(doctorDTO.getIdNumber());
        //如果平台状态是 13-未支付 14-未操作 15-药师审核未通过 则武昌医院状态置为 9-作废
        if (RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.DELETE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus() || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            requestTO.setRecipeStatus("9");
        }
        return requestTO;

    }

    public RecipeStatusUpdateReqTO initRecipeStatusUpdateReqTO(Recipe recipe, List<Recipedetail> list, PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer.toString(recipe.getRecipeType()) : null);
        //his患者id和挂号序号
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        requestTO.setPatientID(recipe.getPatientID());
        if (null != recipeExtend) {
            requestTO.setRegisterID(recipeExtend.getRegisterID());
            requestTO.setRecipeCostNumber(recipeExtend.getRecipeCostNumber());
        }

        if (null != patient) {
            // 患者信息
            requestTO.setCertID(patient.getCertificate());
            requestTO.setCertificateType(patient.getCertificateType());
            requestTO.setPatientName(patient.getPatientName());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }

        // 医院系统医嘱号（一张处方多条记录用|分隔）
        StringBuilder str = new StringBuilder("");
        if (null != list && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
                if (i < list.size() - 1) {
                    str.append(list.get(i).getOrderNo() + "|");
                } else {
                    str.append(list.get(i).getOrderNo());
                }
            }
        }
        requestTO.setOrderNo(str.toString());

        //如果平台状态是 13-未支付 14-未操作 15-药师审核未通过 则医院状态置为 2-已取消
        if (RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.DELETE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus() || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus() || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            requestTO.setRecipeStatus("2");
        }
        // 如果平台状态是 6-已完成 则医院状态置为 1-已发药
        if (RecipeStatusConstant.FINISH == recipe.getStatus()) {
            requestTO.setRecipeStatus("1");
        }

        return requestTO;
    }

    public RecipeAuditReqTO recipeAudit(Recipe recipe, PatientBean patientBean, CheckYsInfoBean resutlBean) {
        RecipeAuditReqTO request = new RecipeAuditReqTO();
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeCode(recipe.getRecipeCode());
        request.setPatientName(recipe.getPatientName());
        request.setPatientId(recipe.getPatientID());
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null) {
            request.setRegisterId(recipeExtend.getRegisterID());
        }
        //获取药师工号药师姓名
        if (recipe.getChecker() != null && recipe.getChecker() != 0) {
            DoctorDTO doctor = doctorService.getByDoctorId(recipe.getChecker());
            LOGGER.info("recipeAudit doctor:{}.", JSONUtils.toString(doctor));
            if (doctor != null) {
                List<String> jobNumbers = iEmploymentService.findJobNumberByDoctorIdAndOrganId(doctor.getDoctorId(), recipe.getCheckOrgan());
                if (CollectionUtils.isNotEmpty(jobNumbers)) {
                    request.setAuditDoctorNo(jobNumbers.get(0));
                }
                request.setAuditDoctorName(doctor.getName());
            }
        }
        //添加科室代码、医生姓名、科室名称
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //科室代码
            AppointDepartDTO appointDepart = departManager.getAppointDepartByOrganIdAndDepart(recipe);
            request.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
            //科室名称
            request.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        } else {
            //互联网环境下没有挂号科室 取department表
            DepartmentService departService = ApplicationUtils.getBasicService(DepartmentService.class);
            DepartmentDTO departmentDTO = departService.getById(recipe.getDepart());
            //科室编码
            request.setDepartCode((null != departmentDTO) ? departmentDTO.getCode() : "");
            //科室名称
            request.setDepartName((null != departmentDTO) ? departmentDTO.getName() : "");
        }
        String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart());
        request.setDoctorNumber(jobNumber);
        request.setDoctorName(recipe.getDoctorName());
        request.setResult(resutlBean.getCheckResult().toString());
        request.setCheckMark(resutlBean.getCheckFailMemo());
        List<RecipeAuditDetailReqTO> detailList = Lists.newArrayList();
        request.setRecipeAuditDetailReqTO(detailList);
        List<RecipeCheckDetail> recipeCheckDetailList = resutlBean.getCheckDetailList();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipeDetailList = detailDAO.findByRecipeId(recipe.getRecipeId());
        if (CollectionUtils.isNotEmpty(recipeCheckDetailList)) {
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            List<Integer> drugIds = detailDAO.findDrugIdByRecipeId(recipe.getRecipeId());
            List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
            Map<Integer, DrugList> drugInfo = Maps.newHashMap();
            Map<Integer, String> drugCodeMap = Maps.newHashMap();
            Map<Integer, String> drugItemCodeMap = Maps.newHashMap();
            for (Recipedetail detail : recipeDetailList) {
                for (DrugList drug : drugList) {
                    if (drug.getDrugId().equals(detail.getDrugId())) {
                        drugInfo.put(detail.getRecipeDetailId(), drug);
                        break;
                    }
                }
                drugCodeMap.put(detail.getRecipeDetailId(), detail.getDrugCode());
                try {
                    OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
                    LOGGER.info("recipeAudit organDrugList={}", JSONUtils.toString(organDrugList));
                    if (null != organDrugList) {
                        drugItemCodeMap.put(detail.getRecipeDetailId(), organDrugList.getDrugItemCode());
                    }
                } catch (Exception e) {
                    LOGGER.error("recipeAudit error", e);
                }
            }
            RecipeAuditDetailReqTO auditDetail;
            List<Integer> detailIdList;
            List<Integer> reasonIdList;
            DrugList drug;
            try {
                for (RecipeCheckDetail detail : recipeCheckDetailList) {
                    reasonIdList = JSONUtils.parse(detail.getReasonIds(), List.class);
                    detailIdList = JSONUtils.parse(detail.getRecipeDetailIds(), List.class);
                    for (Integer detailId : detailIdList) {
                        auditDetail = new RecipeAuditDetailReqTO();
                        auditDetail.setReason(getReasonDicList(reasonIdList));
                        drug = drugInfo.get(detailId);
                        auditDetail.setDrugCode(drugCodeMap.get(detailId));
                        auditDetail.setDrugItemCode(drugItemCodeMap.get(detailId));
                        auditDetail.setDrugName(drug.getSaleName());
                        auditDetail.setProducer(drug.getProducer());
                        auditDetail.setSpecification(drug.getDrugSpec());
                        //TODO 智能审方数据设置
                        detailList.add(auditDetail);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("recipeAudit create his data error. recipeId={}", recipe.getRecipeId(), e);
            }
        } else {
            //审核成功后
            //创建处方新增请求体
            try {
                RecipeSendRequestTO recipeInfo = initRecipeSendRequestTO(recipe, recipeDetailList, patientBean);
                request.setRecipeInfo(recipeInfo);
            } catch (Exception e) {
                LOGGER.warn("recipeAudit create recipeSendInfo error. recipeId={}", recipe.getRecipeId(), e);
            }
        }
        return request;
    }

    private List<String> getReasonDicList(List<Integer> reList) {
        List<String> reasonList = new ArrayList<>();
        try {
            Dictionary dictionary = DictionaryController.instance().get("eh.cdr.dictionary.Reason");
            if (null != reList) {
                for (Integer key : reList) {
                    String reason = dictionary.getText(key);
                    if (StringUtils.isNotEmpty(reason)) {
                        reasonList.add(reason);
                    }
                }
            }
        } catch (ControllerException e) {
            LOGGER.error("获取审核不通过原因字典文本出错reasonIds:" + JSONUtils.toString(reList), e);
        }
        return reasonList;
    }

    public DocIndexToHisReqTO initDocIndexToHisReqTO(Recipe recipe) {
        DocIndexToHisReqTO requestTO = new DocIndexToHisReqTO();
        try {
            requestTO.setOrganId(recipe.getClinicOrgan());
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            //组织机构编码
            requestTO.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));

            IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
            PatientBean patientBean = iPatientService.get(recipe.getMpiid());
            /*HealthCardBean cardBean = iPatientService.getHealthCard(recipe.getMpiid(), recipe.getClinicOrgan(), "2");
            if (null != cardBean) {
                requestTO.setCardType(cardBean.getCardType());//2-医保卡
                requestTO.setCardCode(cardBean.getCardId());
            }else {
                requestTO.setCardType("4");//武昌-4-身份证
                requestTO.setCardCode(patientBean.getIdcard());
            }*/
            requestTO.setCardType("4");//武昌-4-身份证
            requestTO.setCardCode(patientBean.getIdcard());
            requestTO.setPatientIdCard(patientBean.getIdcard());
            requestTO.setPatientName(patientBean.getPatientName());
            requestTO.setPatientSex(patientBean.getPatientSex());
            requestTO.setPatientId(recipe.getPatientID());
            //出生日期
            requestTO.setBirthDay(DateConversion.formatDate(patientBean.getBirthday()));
            //复诊标记（0：初诊 1：复诊）
            requestTO.setClinicFlag("1");
            //就诊日期
            requestTO.setTreatmentDate(DateConversion.formatDateTimeWithSec(new Date()));
            //医生工号
            //设置医生工号
            EmploymentService employmentService = ApplicationUtils.getBasicService(EmploymentService.class);
            requestTO.setDoctorCode(employmentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));

            //获取扩展表数据
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (recipeExtend != null) {
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                //主诉
                requestTO.setMainDiseaseDescribe(StringUtils.isNotEmpty(recipeExtend.getMainDieaseDescribe()) ? recipeExtend.getMainDieaseDescribe() : "无");
                //现病史
                requestTO.setHistoryOfPresentIllness(StringUtils.isNotEmpty(recipeExtend.getCurrentMedical()) ? recipeExtend.getCurrentMedical() : "无");
                //既往史
                requestTO.setPastHistory(StringUtils.isNotEmpty(recipeExtend.getHistroyMedical()) ? recipeExtend.getHistroyMedical() : "无");
                //过敏史
                requestTO.setAllergyHistory(StringUtils.isNotEmpty(recipeExtend.getAllergyMedical()) ? recipeExtend.getAllergyMedical() : "无");
            } else {
                LOGGER.warn("recipeExtend is null . recipeId={}", recipe.getRecipeId());
            }
            //获取诊断数据
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            List<String> icdLists = Splitter.on(ByteUtils.SEMI_COLON_EN).splitToList(recipe.getOrganDiseaseId());
            List<String> nameLists = Splitter.on(ByteUtils.SEMI_COLON_EN).splitToList(recipe.getOrganDiseaseName());
            List<DocIndexInfoTO> data = Lists.newArrayList();
            if (icdLists.size() == nameLists.size()) {
                for (int i = 0; i < icdLists.size(); i++) {
                    DocIndexInfoTO docIndexInfoTO = new DocIndexInfoTO();
                    //诊断类型
                    docIndexInfoTO.setDiagnoseType("西医");
                    //发病日期
                    if (recipeExtend != null && recipeExtend.getOnsetDate() != null) {
                        docIndexInfoTO.setIllnessTime(DateConversion.formatDate(recipeExtend.getOnsetDate()));
                    }
                    //诊断码
                    docIndexInfoTO.setIcdCode(icdLists.get(i));
                    //诊断名
                    docIndexInfoTO.setIcdname(nameLists.get(i));
                    data.add(docIndexInfoTO);
                }
            }
            requestTO.setData(data);
        } catch (Exception e) {
            LOGGER.error("initDocIndexToHisReqTO 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }

        return requestTO;
    }
}
