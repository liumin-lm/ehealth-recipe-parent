package recipe.hisservice;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.his.recipe.mode.*;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeCheckDetail;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.CheckYsInfoBean;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.service.RecipeCheckService;
import recipe.service.RecipeService;
import recipe.util.ApplicationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/14.
 */
public class HisRequestInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(HisRequestInit.class);

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

        if (null != details && !details.isEmpty()) {
            List<OrderItemTO> orderList = new ArrayList<>();
            for (Recipedetail detail : details) {
                OrderItemTO orderItem = new OrderItemTO();
                orderItem.setOrderID(Integer.toString(detail
                        .getRecipeDetailId()));
                orderItem.setDrcode(detail.getOrganDrugCode());
                orderItem.setDrname(detail.getDrugName());
                orderItem.setDrmodel(detail.getDrugSpec());
                orderItem.setPackUnit(detail.getDrugUnit());
                orderItem.setDrugId(detail.getDrugId());

                orderItem.setAdmission(detail.getUsePathways());
                orderItem.setFrequency(detail.getUsingRate().toUpperCase());
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

    public static RecipeStatusUpdateReqTO initRecipeStatusUpdateReqTO(Recipe recipe, List<Recipedetail> list,
                                                                      PatientBean patient, HealthCardBean card) {
        RecipeStatusUpdateReqTO requestTO = new RecipeStatusUpdateReqTO();
        requestTO.setOrganID((null != recipe.getClinicOrgan()) ? Integer
                .toString(recipe.getClinicOrgan()) : null);
        requestTO.setRecipeNo(recipe.getRecipeCode());
        requestTO.setRecipeType((null != recipe.getRecipeType()) ? Integer
                .toString(recipe.getRecipeType()) : null);

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
}
