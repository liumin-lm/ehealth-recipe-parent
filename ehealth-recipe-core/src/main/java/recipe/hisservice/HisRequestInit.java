package recipe.hisservice;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.RecipeExtendBean;
import com.ngari.platform.recipe.mode.RecipeOrderBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.constant.RecipeSendTypeEnum;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import static ctd.persistence.DAOFactory.getDAO;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.*;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.service.manager.EmrRecipeManager;
import static recipe.service.manager.EmrRecipeManager.getMedicalInfo;
import recipe.util.DateConversion;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/9/14.
 */
public class HisRequestInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(HisRequestInit.class);
    @Autowired
    private EmrRecipeManager emrRecipeManager;

    public static RecipeSendRequestTO initRecipeSendRequestTOForWuChang(Recipe recipe, List<Recipedetail> details, PatientBean patient, HealthCardBean card) {
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
            getMedicalInfo(recipe, recipeExtend);
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
            AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
            AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
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

    public static RecipeSendRequestTO initRecipeSendRequestTO(Recipe recipe, List<Recipedetail> details, PatientBean patient, HealthCardBean card) {
        RecipeSendRequestTO requestTO = new RecipeSendRequestTO();
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        String recipeIdStr = Integer.toString(recipe.getRecipeId());
        requestTO.setRecipeID(recipeIdStr);
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
        requestTO.setDatein(recipe.getSignDate());
        requestTO.setStartDate(recipe.getSignDate());
        requestTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe.getPayFlag()) : null);
        requestTO.setDeptID("");
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? recipe.getRecipeType().toString() : null);
        //处方附带信息
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        getMedicalInfo(recipe, recipeExtend);
        requestTO.setIcdCode(recipe.getOrganDiseaseId());
        requestTO.setIcdName(recipe.getOrganDiseaseName());
        // 简要病史
        requestTO.setDiseasesHistory(recipe.getOrganDiseaseName());
        if (recipeExtend != null) {
            //挂号序号
            requestTO.setRegisteredId(recipeExtend.getRegisterID());
            //主诉
            requestTO.setMainDieaseDescribe(recipeExtend.getMainDieaseDescribe());
            //现病史
            requestTO.setHistoryOfPresentIllness(recipeExtend.getHistoryOfPresentIllness());
            //处理方法
            requestTO.setHandleMethod(recipeExtend.getHandleMethod());
            //处方扩展信息
            requestTO.setRecipeExtend(ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class));
            try {
                //制法Code 煎法Code 中医证候Code
                DrugDecoctionWayDao drugDecoctionWayDao = DAOFactory.getDAO(DrugDecoctionWayDao.class);
                DrugMakingMethodDao drugMakingMethodDao = DAOFactory.getDAO(DrugMakingMethodDao.class);
                SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
                if (StringUtils.isNotBlank(recipeExtend.getDecoctionId())) {
                    DecoctionWay decoctionWay = drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                    requestTO.getRecipeExtend().setDecoctionCode(decoctionWay.getDecoctionCode());
                }
                if (StringUtils.isNotBlank(recipeExtend.getMakeMethodId())) {
                    DrugMakingMethod drugMakingMethod = drugMakingMethodDao.get(Integer.parseInt(recipeExtend.getMakeMethodId()));
                    requestTO.getRecipeExtend().setMakeMethod(drugMakingMethod.getMethodCode());

                }
                if (StringUtils.isNotBlank(recipeExtend.getSymptomId())) {
                    Symptom symptom = symptomDAO.get(Integer.parseInt(recipeExtend.getSymptomId()));
                    requestTO.getRecipeExtend().setSymptomCode(symptom.getSymptomCode());
                }
            } catch (Exception e) {
                LOGGER.error("initRecipeSendRequestTO recipeid:{} error :{}", recipe.getRecipeId(), e);
            }
        }
        // 从复诊获取患者渠道id
        try {
            if (recipe.getClinicId() != null) {
                IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                LOGGER.info("queryPatientChannelId req={}", recipe.getClinicId());
                RevisitExDTO revisitExDTO = exService.getByConsultId(recipe.getClinicId());
                if (revisitExDTO != null) {
                    LOGGER.info("queryPatientChannelId res={}", JSONObject.toJSONString(revisitExDTO));
                    requestTO.setPatientChannelId(revisitExDTO.getProjectChannel());
                }
            }
        } catch (Exception e) {
            LOGGER.error("queryPatientChannelId error:", e);
        }
        //设置挂号序号---如果有
        //处方扩展表没有再冲复诊取得
        if (requestTO.getRegisteredId() == null && recipe.getClinicId() != null) {
            IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
            RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
            if (consultExDTO != null) {
                requestTO.setRegisteredId(consultExDTO.getRegisterNo());
                requestTO.setCardType(consultExDTO.getCardType());
                requestTO.setCardNo(consultExDTO.getCardId());
            }
        }
        //科室代码
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
        requestTO.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        requestTO.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        //互联网环境下没有挂号科室 取department表
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            DepartmentService departService = ApplicationUtils.getBasicService(DepartmentService.class);
            DepartmentDTO departmentDTO = departService.getById(recipe.getDepart());
            //科室编码
            requestTO.setDepartCode((null != departmentDTO) ? departmentDTO.getCode() : "");
            //科室名称
            requestTO.setDepartName((null != departmentDTO) ? departmentDTO.getName() : "");
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
        }
        if (null != card && StringUtils.isEmpty(requestTO.getCardNo())) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }

        //根据处方单设置配送方式
        if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
            requestTO.setDeliveryType("1");
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
        //福建省立医院特殊处理
        if ("1001393".equals(recipe.getClinicOrgan().toString())) {
            IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
            List<Integer> consultIds = iRevisitService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(), recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
            Integer consultId = null;
            if (CollectionUtils.isNotEmpty(consultIds)) {
                consultId = consultIds.get(0);
            }
            if (null != consultId) {
                IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = exService.getByConsultId(consultId);
                if (null != consultExDTO && StringUtils.isNotEmpty(consultExDTO.getCardId())) {
                    requestTO.setCardNo(consultExDTO.getCardId());
                    requestTO.setCardType(consultExDTO.getCardType());
                }

            }
        }

        if (null != details && !details.isEmpty()) {
            List<OrderItemTO> orderList = new ArrayList<>();
            try {
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                OrganDrugList organDrug;
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
                    orderItem.setPack(detail.getPack());
                    //用药天数
                    //date 20200526
                    //线上推送处方到his，将用药天数big转成int
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
                    organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(), detail.getOrganDrugCode(), detail.getDrugId());
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
                    }
                    orderList.add(orderItem);
                }
            } catch (Exception e) {
                LOGGER.error("initRecipeSendRequestTO error ", e);
            }
            requestTO.setOrderList(orderList);
        } else {
            requestTO.setOrderList(null);
        }

        return requestTO;
    }

    public static RecipeRefundReqTO initRecipeRefundReqTO(Recipe recipe, List<Recipedetail> details, PatientBean patient, HealthCardBean card) {
        RecipeRefundReqTO requestTO = new RecipeRefundReqTO();
        if (null != recipe) {
            requestTO.setOrganID(String.valueOf(recipe.getClinicOrgan()));
        }

        if (null != details && !details.isEmpty()) {
            requestTO.setInvoiceNo(details.get(0).getPatientInvoiceNo());
        }

        if (null != patient) {
            requestTO.setCertID(patient.getCertificate());
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setPatientSex(patient.getPatientSex());
            requestTO.setMobile(patient.getMobile());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }

        requestTO.setHoscode("");
        requestTO.setEmpId("");

        return requestTO;
    }

    public static PayNotifyReqTO initPayNotifyReqTO(List<String> recipeIdList, Recipe recipe, PatientBean patient, HealthCardBean card) {
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
                requestTO.setPatientName(patient.getPatientName());
            }
            requestTO.setPatId(recipe.getPatientID());
            requestTO.setPayMode(recipe.getGiveMode());

            if (null != card) {
                requestTO.setCardType(card.getCardType());
                requestTO.setCardNo(card.getCardId());
            }

            requestTO.setAmount(recipe.getTotalMoney().toString());

            requestTO.setIsMedicalSettle("0");
            if (recipe.getOrderCode() != null) {
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
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            requestTO.setPatientID(recipe.getPatientID());
            if (null != recipeExtend) {
                requestTO.setRegisterID(recipeExtend.getRegisterID());
            }

            if (null != patient) {
                requestTO.setPatientName(patient.getPatientName());
                requestTO.setCertID(patient.getCertificate());
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
                //合并支付的处方需要将所有his处方编码传过去
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                List<Recipe> recipeS = recipeDAO.findRecipeListByOrderCode(recipe.getOrderCode());
                if (CollectionUtils.isNotEmpty(recipeS)) {
                    List<String> recipeNoS = recipeS.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
                    requestTO.setRecipeNoS(recipeNoS);
                }
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
                            if (drugsEnterprise != null && drugsEnterprise.getSendType() == RecipeSendTypeEnum.NO_PAY.getSendType()) {
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

        return requestTO;
    }

    public static RecipeStatusUpdateReqTO initRecipeStatusUpdateReqForWuChang(Recipe recipe, List<Recipedetail> list, PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer.toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer.toString(recipe.getRecipeType()) : null);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        //组织机构编码
        requestTO.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));

        if (null != patient) {
            // 患者信息
            requestTO.setCertID(patient.getCertificate());
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

    public static RecipeStatusUpdateReqTO initRecipeStatusUpdateReqTO(Recipe recipe, List<Recipedetail> list, PatientBean patient, HealthCardBean card) {
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

    public static RecipeAuditReqTO recipeAudit(Recipe recipe, PatientBean patientBean, CheckYsInfoBean resutlBean) {
        RecipeAuditReqTO request = new RecipeAuditReqTO();
        EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeCode(recipe.getRecipeCode());
        request.setPatientName(recipe.getPatientName());
        request.setPatientId(recipe.getPatientID());
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null) {
            request.setRegisterId(recipeExtend.getRegisterID());
        }
        //获取药师工号药师姓名
        if (recipe.getChecker() != null && recipe.getChecker() != 0) {
            DoctorDTO doctor = doctorService.getByDoctorId(recipe.getChecker());
            if (doctor != null) {
                request.setAuditDoctorNo(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), doctor.getDepartment()));
                request.setAuditDoctorName(doctor.getName());
            }
        }
        //添加科室代码、医生姓名、科室名称
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //科室代码
            AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
            AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
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
            for (Recipedetail detail : recipeDetailList) {
                for (DrugList drug : drugList) {
                    if (drug.getDrugId().equals(detail.getDrugId())) {
                        drugInfo.put(detail.getRecipeDetailId(), drug);
                        break;
                    }
                }
                drugCodeMap.put(detail.getRecipeDetailId(), detail.getDrugCode());
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
                RecipeSendRequestTO recipeInfo = HisRequestInit.initRecipeSendRequestTO(recipe, recipeDetailList, patientBean, null);
                request.setRecipeInfo(recipeInfo);
            } catch (Exception e) {
                LOGGER.warn("recipeAudit create recipeSendInfo error. recipeId={}", recipe.getRecipeId(), e);
            }
        }
        return request;
    }

    public static List<String> getReasonDicList(List<Integer> reList) {
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

    public static DocIndexToHisReqTO initDocIndexToHisReqTO(Recipe recipe) {
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
                getMedicalInfo(recipe, recipeExtend);
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
            getMedicalInfo(recipe, recipeExtend);
            List<String> icdLists = Splitter.on("；").splitToList(recipe.getOrganDiseaseId());
            List<String> nameLists = Splitter.on("；").splitToList(recipe.getOrganDiseaseName());
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
