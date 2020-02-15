package recipe.hisservice;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.recipe.mode.*;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.RecipeUtil;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.BusTypeEnum;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.*;
import recipe.service.RecipeCheckService;
import recipe.util.DateConversion;

import java.math.BigDecimal;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/14.
 */
public class HisRequestInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(HisRequestInit.class);

    public static RecipeSendRequestTO initRecipeSendRequestTOForWuChang(Recipe recipe, List<Recipedetail> details,
                                                              PatientBean patient, HealthCardBean card) {
        RecipeSendRequestTO requestTO = new RecipeSendRequestTO();
        try{
            String recipeIdStr = Integer.toString(recipe.getRecipeId());
            requestTO.setRecipeID(recipeIdStr);
            requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                    .toString(recipe.getClinicOrgan()) : null);
            requestTO.setDatein(recipe.getSignDate());
            requestTO.setStartDate(recipe.getSignDate());
            requestTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe
                    .getPayFlag()) : null);
            requestTO.setIcdCode(recipe.getOrganDiseaseId());
            requestTO.setIcdName(recipe.getOrganDiseaseName());
            requestTO.setDeptID("");
            requestTO.setRecipeType((null != recipe.getRecipeType()) ? recipe
                    .getRecipeType().toString() : null);
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
            switch (patient.getPatientType()){
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
                if(StringUtils.isNotEmpty(idCard)){
                    //没有身份证儿童的证件处理
                    String childFlag = "-";
                    if(idCard.contains(childFlag)){
                        idCard = idCard.split(childFlag)[0];
                    }
                }
                requestTO.setCertID(idCard);
                requestTO.setPatientName(patient.getPatientName());
                requestTO.setMobile(patient.getMobile());
                // 简要病史
                requestTO.setDiseasesHistory(recipe.getOrganDiseaseName());
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
            if (Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
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
                for (Recipedetail detail : details) {
                    OrderItemTO orderItem = new OrderItemTO();
                    orderItem.setOrderID(Integer.toString(detail
                            .getRecipeDetailId()));
                    orderItem.setDrcode(detail.getOrganDrugCode());
                    orderItem.setDrname(detail.getDrugName());
                    orderItem.setDrmodel(detail.getDrugSpec());
                    orderItem.setPackUnit(detail.getDrugUnit());
                    orderItem.setDrugId(detail.getDrugId());

                    orderItem.setAdmission(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsePathways()));
                    orderItem.setFrequency(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsingRate()));
                    orderItem.setDosage((null != detail.getUseDose()) ? Double
                            .toString(detail.getUseDose()) : null);
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

                    orderList.add(orderItem);
                    i += 1;
                }

                requestTO.setOrderList(orderList);
            } else {
                requestTO.setOrderList(null);
            }
        }catch (Exception e){
            LOGGER.error("initRecipeSendRequestTOForWuChang 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }
        return requestTO;

    }

    public static RecipeSendRequestTO initRecipeSendRequestTO(Recipe recipe, List<Recipedetail> details,
                                                              PatientBean patient, HealthCardBean card) {
        RecipeSendRequestTO requestTO = new RecipeSendRequestTO();
        String recipeIdStr = Integer.toString(recipe.getRecipeId());
        requestTO.setRecipeID(recipeIdStr);
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        requestTO.setDatein(recipe.getSignDate());
        requestTO.setStartDate(recipe.getSignDate());
        requestTO.setIsPay((null != recipe.getPayFlag()) ? Integer.toString(recipe
                .getPayFlag()) : null);
        requestTO.setIcdCode(recipe.getOrganDiseaseId());
        requestTO.setIcdName(recipe.getOrganDiseaseName());
        requestTO.setDeptID("");
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? recipe
                .getRecipeType().toString() : null);
        //设置挂号序号---如果有
        if (recipe.getClinicId() != null){
            IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
            //挂号记录
            HosrelationBean hosrelation = hosrelationService.getByBusIdAndBusType(recipe.getClinicId(), BusTypeEnum.CONSULT.getId());
            if (hosrelation != null && StringUtils.isNotEmpty(hosrelation.getRegisterId())){
                requestTO.setRegisteredId(hosrelation.getRegisterId());
            }
        }
        //科室代码
        AppointDepartService appointDepartService = ApplicationUtils.getBasicService(AppointDepartService.class);
        AppointDepartDTO appointDepart = appointDepartService.findByOrganIDAndDepartID(recipe.getClinicOrgan(), recipe.getDepart());
        requestTO.setDepartCode((null != appointDepart) ? appointDepart.getAppointDepartCode() : "");
        //科室名称
        requestTO.setDepartName((null != appointDepart) ? appointDepart.getAppointDepartName() : "");
        //互联网环境下没有挂号科室 取department表
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())){
            DepartmentService departService = ApplicationUtils.getBasicService(DepartmentService.class);
            DepartmentDTO departmentDTO = departService.getById(recipe.getDepart());
            //科室编码
            requestTO.setDepartCode((null != departmentDTO) ? departmentDTO.getCode() : "");
            //科室名称
            requestTO.setDepartName((null != departmentDTO) ? departmentDTO.getName() : "");
        }
        //医生名字
        requestTO.setDoctorName(recipe.getDoctorName());
        //处方金额
        requestTO.setSelfPayingFee(recipe.getActualPrice());
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
            requestTO.setCertID(idCard);
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setMobile(patient.getMobile());
            // 简要病史
            requestTO.setDiseasesHistory(recipe.getOrganDiseaseName());
        }
        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }

        //根据处方单设置配送方式
        if (Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
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

        //福建省立医院特殊处理
        if("1001393".equals(recipe.getClinicOrgan().toString())){
            IConsultService iConsultService = ApplicationUtils.getConsultService(IConsultService.class);
            List<Integer> consultIds = iConsultService.findApplyingConsultByRequestMpiAndDoctorId(recipe.getRequestMpiId(),
                    recipe.getDoctor(), RecipeSystemConstant.CONSULT_TYPE_RECIPE);
            Integer consultId = null;
            if (CollectionUtils.isNotEmpty(consultIds)) {
                consultId = consultIds.get(0);
            }
            if(null != consultId){
                IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
                ConsultExDTO consultExDTO = exService.getByConsultId(consultId);
                if(null != consultExDTO && StringUtils.isNotEmpty(consultExDTO.getCardId())){
                    requestTO.setCardNo(consultExDTO.getCardId());
                    requestTO.setCardType(consultExDTO.getCardType());
                }

            }
        }

        if (null != details && !details.isEmpty()) {
            List<OrderItemTO> orderList = new ArrayList<>();
            for (Recipedetail detail : details) {
                OrderItemTO orderItem = new OrderItemTO();
                orderItem.setOrderID(Integer.toString(detail
                        .getRecipeDetailId()));
                orderItem.setDrcode(detail.getOrganDrugCode());
                orderItem.setDrname(detail.getDrugName());
                //药品规格
                orderItem.setDrmodel(detail.getDrugSpec());
                orderItem.setPackUnit(detail.getDrugUnit());
                orderItem.setDrugId(detail.getDrugId());

                orderItem.setAdmission(UsePathwaysFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsePathways()));
                orderItem.setFrequency(UsingRateFilter.filterNgari(recipe.getClinicOrgan(),detail.getUsingRate()));
                orderItem.setDosage((null != detail.getUseDose()) ? Double
                        .toString(detail.getUseDose()) : null);
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
                //用药天数
                orderItem.setUseDays(detail.getUseDays());
                //药品数量
                orderItem.setItemCount(new BigDecimal(detail.getUseTotalDose()));

                orderList.add(orderItem);
            }

            requestTO.setOrderList(orderList);
        } else {
            requestTO.setOrderList(null);
        }

        return requestTO;
    }

    public static RecipeRefundReqTO initRecipeRefundReqTO(Recipe recipe, List<Recipedetail> details,
                                                          PatientBean patient, HealthCardBean card) {
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

    public static PayNotifyReqTO initPayNotifyReqTO(Recipe recipe, PatientBean patient, HealthCardBean card) {
        PayNotifyReqTO requestTO = new PayNotifyReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer
                .toString(recipe.getRecipeType()) : null);
        // 目前都是平台代收 后面要改
        requestTO.setPayType("1");
        if (null != patient) {
            // 患者信息
            requestTO.setCertID(patient.getCertificate());
            requestTO.setPatientName(patient.getPatientName());
        }

        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }

        requestTO.setAmount(recipe.getTotalMoney().toString());

        requestTO.setIsMedicalSettle("0");
        if(recipe.getOrderCode() != null){
            RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(recipe.getOrderCode());
            //省医保订单新增逻辑
            if(order != null && order.getOrderType() == null && order.getOrderType() == 1){
                requestTO.setIsMedicalSettle("1");
                if("40".equals(order.getWxPayWay())){
                    requestTO.setPayType("C");
                } else {
                    requestTO.setPayType("E");
                }
                requestTO.setRecipeNo(recipe.getRecipeId() + "");
                requestTO.setRecipeCode(recipe.getRecipeCode());
                requestTO.setOrganName(recipe.getOrganName());
                RecipeExtendDAO extendDAO = getDAO(RecipeExtendDAO.class);
                RecipeExtend extend = extendDAO.getByRecipeId(recipe.getRecipeId());
                if(extend != null && extend.getCashAmount() != null){
                    requestTO.setCashAmount(extend.getCashAmount());
                } else {
                    LOGGER.info("无法获取处方的预结算返回的自费金额，处方={}",recipe.getRecipeId());
                }
                requestTO.setTradeNo(order.getTradeNo());
                requestTO.setOutTradeNo(order.getOutTradeNo());
            }
        }


        return requestTO;
    }

    public static DrugTakeChangeReqTO initDrugTakeChangeReqTO(Recipe recipe, List<Recipedetail> list,
                                                              PatientBean patient, HealthCardBean card) {
        DrugTakeChangeReqTO requestTO = new DrugTakeChangeReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        if (null != card) {
            requestTO.setCardType(card.getCardType());
            requestTO.setCardNo(card.getCardId());
        }
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        requestTO.setPatientID(recipe.getPatientID());
        if (null != recipeExtend){
            requestTO.setRegisterID(recipeExtend.getRegisterID());
        }

        if (null != patient) {
            requestTO.setPatientName(patient.getPatientName());
            requestTO.setCertID(patient.getCertificate());
        }

        //takeDrugsType 取药方式 0-医院药房取药 1-物流配送(国药) 2-外配药(钥世圈)
        if (null != recipe.getPayMode()) {
            if (RecipeBussConstant.PAYMODE_TO_HOS.equals(recipe.getPayMode())) {
                requestTO.setTakeDrugsType("0");
            }
            if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(recipe.getPayMode())
                    || RecipeBussConstant.PAYMODE_ONLINE.equals(recipe.getPayMode())) {
                requestTO.setTakeDrugsType("1");
            }
            if (RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode())
                    || RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                requestTO.setTakeDrugsType("2");
            }
        }else {
            //默认走外配药方式
            requestTO.setTakeDrugsType("2");
        }
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer
                .toString(recipe.getRecipeType()) : null);

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

        return requestTO;
    }

    public static RecipeStatusUpdateReqTO initRecipeStatusUpdateReqForWuChang(Recipe recipe, List<Recipedetail> list,
                                                                      PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer
                .toString(recipe.getRecipeType()) : null);
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
        if (RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.DELETE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus()
                || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus()
                || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            requestTO.setRecipeStatus("9");
        }
        /*// 如果平台状态是 6-已完成 则医院状态置为 1-已发药
        if (RecipeStatusConstant.FINISH == recipe.getStatus()) {
            requestTO.setRecipeStatus("1");
        }
*/
        return requestTO;

    }

    public static RecipeStatusUpdateReqTO initRecipeStatusUpdateReqTO(Recipe recipe, List<Recipedetail> list,
                                                                      PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer
                .toString(recipe.getRecipeType()) : null);
        //his患者id和挂号序号
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        requestTO.setPatientID(recipe.getPatientID());
        if (null != recipeExtend){
            requestTO.setRegisterID(recipeExtend.getRegisterID());
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
        if (RecipeStatusConstant.REVOKE == recipe.getStatus() || RecipeStatusConstant.DELETE == recipe.getStatus() || RecipeStatusConstant.HIS_FAIL == recipe.getStatus()
                || RecipeStatusConstant.NO_DRUG == recipe.getStatus() || RecipeStatusConstant.NO_PAY == recipe.getStatus() || RecipeStatusConstant.NO_OPERATOR == recipe.getStatus()
                || RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus()) {
            requestTO.setRecipeStatus("2");
        }
        // 如果平台状态是 6-已完成 则医院状态置为 1-已发药
        if (RecipeStatusConstant.FINISH == recipe.getStatus()) {
            requestTO.setRecipeStatus("1");
        }

        return requestTO;
    }

    public static RecipeAuditReqTO recipeAudit(Recipe recipe, CheckYsInfoBean resutlBean){
        RecipeAuditReqTO request = new RecipeAuditReqTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeCode(recipe.getRecipeCode());
        request.setResult(resutlBean.getCheckResult().toString());
        request.setCheckMark(resutlBean.getCheckFailMemo());
        List<RecipeAuditDetailReqTO> detailList = Lists.newArrayList();
        request.setRecipeAuditDetailReqTO(detailList);
        List<RecipeCheckDetail> recipeCheckDetailList = resutlBean.getCheckDetailList();
        if(CollectionUtils.isNotEmpty(recipeCheckDetailList)){
            RecipeCheckService recipeCheckService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

            List<Recipedetail> recipeDetailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            List<Integer> drugIds = detailDAO.findDrugIdByRecipeId(recipe.getRecipeId());
            List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
            Map<Integer, DrugList> drugInfo = Maps.newHashMap();
            Map<Integer, String> drugCodeMap = Maps.newHashMap();
            for(Recipedetail detail : recipeDetailList){
                for(DrugList drug : drugList){
                    if(drug.getDrugId().equals(detail.getDrugId())){
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
                for(RecipeCheckDetail detail : recipeCheckDetailList){
                    reasonIdList = JSONUtils.parse(detail.getReasonIds(), List.class);
                    detailIdList = JSONUtils.parse(detail.getRecipeDetailIds(), List.class);
                    for(Integer detailId : detailIdList){
                        auditDetail = new RecipeAuditDetailReqTO();
                        auditDetail.setReason(recipeCheckService.getReasonDicList(reasonIdList));
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
        }

        return request;
    }

    public static DocIndexToHisReqTO initDocIndexToHisReqTO(Recipe recipe) {
        DocIndexToHisReqTO requestTO = new DocIndexToHisReqTO();
        try{
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
            if (recipeExtend != null){
                //主诉
                requestTO.setMainDiseaseDescribe(StringUtils.isNotEmpty(recipeExtend.getMainDieaseDescribe())?recipeExtend.getMainDieaseDescribe():"无");
                //现病史
                requestTO.setHistoryOfPresentIllness(StringUtils.isNotEmpty(recipeExtend.getCurrentMedical())?recipeExtend.getCurrentMedical():"无");
                //既往史
                requestTO.setPastHistory(StringUtils.isNotEmpty(recipeExtend.getHistroyMedical())?recipeExtend.getHistroyMedical():"无");
                //过敏史
                requestTO.setAllergyHistory(StringUtils.isNotEmpty(recipeExtend.getAllergyMedical())?recipeExtend.getAllergyMedical():"无");
            }else {
                LOGGER.warn("recipeExtend is null . recipeId={}",recipe.getRecipeId());
            }

            //获取诊断数据
            List<String> icdLists = Splitter.on("；").splitToList(recipe.getOrganDiseaseId());
            List<String> nameLists = Splitter.on("；").splitToList(recipe.getOrganDiseaseName());
            List<DocIndexInfoTO> data = Lists.newArrayList();
            if (icdLists.size() == nameLists.size()){
                for (int i = 0; i < icdLists.size();i++){
                    DocIndexInfoTO docIndexInfoTO = new DocIndexInfoTO();
                    //诊断类型
                    docIndexInfoTO.setDiagnoseType("西医");
                    //发病日期
                    if (recipeExtend!= null && recipeExtend.getOnsetDate()!=null){
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
        }catch (Exception e){
            LOGGER.error("initDocIndexToHisReqTO 组装参数错误. recipeId={}, error ", recipe.getRecipeId(), e);
        }

        return requestTO;
    }
}
