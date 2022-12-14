package recipe.factoryManager.button;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.OperationClient;
import recipe.constant.*;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.RecipeParameterDao;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.RecipeDistributionFlagEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 20:01
 */
public abstract class GiveModeManager implements IGiveModeBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveModeManager.class);
    private static final String LIST_TYPE_RECIPE = "1";
    private static final String LIST_TYPE_ORDER = "2";
    private static final Integer No_Show_Button = 3;

    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private PatientService patientService;

    @Override
    public void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        //处理医院配送和药企配送的药企按钮，根据该机构配置的药企配送主体来决定
        Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
        boolean showSendToEnterprises = result.containsKey("showSendToEnterprises");
        boolean showSendToHos = result.containsKey("showSendToHos");
        //如果运营平台没有配置药企配送或者医院配送，则可不用继续处理
        Integer enterprisesSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
        Integer hosSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
        if (showSendToEnterprises && enterprisesSend == 0L) {
            //表示运营平台虽然配置了药企配送但是该机构没有配置可配送的药企
            removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "showSendToEnterprises");
        }
        if (showSendToHos && hosSend == 0L) {
            //表示运营平台虽然配置了医院配送但是该机构没有配置可配送的自建药企
            removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "showSendToHos");
        }
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            //开处方时校验库存时存的只支持配送方式--不支持到院取药
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "supportToHos");
            }
            removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "supportDownload");
        }
    }

    @Override
    public GiveModeShowButtonDTO getShowButton(Recipe recipe) {
        GiveModeShowButtonDTO giveModeShowButtonDTO = new GiveModeShowButtonDTO();
        try {
            //校验数据
            validRecipeData(recipe);
        } catch (Exception e) {
            return giveModeShowButtonDTO;
        }
        //从运营平台获取配置项
        giveModeShowButtonDTO = operationClient.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(giveModeShowButtonDTO.getGiveModeButtons())) {
            return giveModeShowButtonDTO;
        }
        //设置按钮是否可点击
        setButtonOptional(giveModeShowButtonDTO, recipe);
        //设置按钮展示类型
        setButtonType(giveModeShowButtonDTO, recipe);
        //设置列表不显示的按钮
        setItemListNoShow(giveModeShowButtonDTO, recipe);
        //后置设置处理
        afterSetting(giveModeShowButtonDTO, recipe);
        return giveModeShowButtonDTO;
    }

    @Override
    public GiveModeShowButtonDTO getShowButtonV1(Recipe recipe) {
        GiveModeShowButtonDTO giveModeShowButtonDTO = getShowButton(recipe);
        setShowButton(giveModeShowButtonDTO, recipe);
        //设置其他按钮
        setOtherButton(giveModeShowButtonDTO, recipe);
        return giveModeShowButtonDTO;
    }

    protected void removeGiveModeData(List<GiveModeButtonDTO> giveModeButtonBeans, String remoteGiveMode) {
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonDTO giveModeShowButtonVO = (GiveModeButtonDTO) iterator.next();
            if (remoteGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }

    protected void saveGiveModeData(List<GiveModeButtonDTO> giveModeButtonBeans, String saveGiveMode) {
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonDTO giveModeShowButtonVO = (GiveModeButtonDTO) iterator.next();
            if (!saveGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }

    private void setOtherButton(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        LOGGER.info("setOtherButton giveModeButtons:{}", JSONUtils.toString(giveModeShowButtonVO));
    }

    private void setShowButton(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        boolean showButton = false;
        if (CollectionUtils.isNotEmpty(giveModeShowButtonVO.getGiveModeButtons())) {
            if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
                //待药师审核，审核一次不通过，待处理无订单
                if ((RecipeStatusEnum.getCheckStatusFlag(recipe.getStatus()) || RecipecCheckStatusConstant.First_Check_No_Pass.equals(recipe.getCheckStatus())) && null == recipe.getOrderCode()) {
                    showButton = true;
                }
            } else {
                if (RecipeStatusConstant.CHECK_PASS == recipe.getStatus() && null == recipe.getOrderCode()) {
                    showButton = true;
                }
            }
        }
        giveModeShowButtonVO.setShowButton(showButton);
    }

    private void validRecipeData(Recipe recipe) {
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "处方数据为空");
        }
        if (StringUtils.isEmpty(recipe.getRecipeMode())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "处方流转方式为空");
        }
    }


    private void setButtonOptional(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        boolean isOptional = !(ReviewTypeConstant.Preposition_Check == recipe.getReviewType() && (RecipeStatusConstant.SIGN_NO_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() || (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus() && RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus())));
        giveModeShowButtonVO.setOptional(isOptional);
    }


    private void setButtonType(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        String recordType = getRecordInfo(recipe).get("recordType");
        String recordStatusCode = getRecordInfo(recipe).get("recordStatusCode");
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        //添加判断，当选药按钮都不显示的时候，按钮状态为不展示
        if (CollectionUtils.isNotEmpty(giveModeButtonBeans)) {
            //查找是否包含用药指导按钮
            Map result = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonDTO::getShowButtonKey, GiveModeButtonDTO::getShowButtonName));
            boolean showSendToEnterprises = result.containsKey("showSendToEnterprises");
            boolean showSendToHos = result.containsKey("showSendToHos");
            boolean supportToHos = result.containsKey("supportToHos");
            boolean supportTFDS = result.containsKey("supportTFDS");
            boolean showUseDrugConfig = result.containsKey("supportMedicationGuide");
            //当处方在待处理、前置待审核通过时，购药配送为空不展示按钮
            Boolean noHaveBuyDrugConfig = !showSendToEnterprises && !showSendToHos && !supportTFDS && !supportToHos;

            //只有当亲处方有订单，且物流公司和订单号都有时展示物流信息
            Boolean haveSendInfo = false;
            RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipe.getRecipeId());
            if (null != order && null != order.getLogisticsCompany() && StringUtils.isNotEmpty(order.getTrackingNumber())) {
                haveSendInfo = true;
            }
            RecipePageButtonStatusEnum buttonStatus = RecipePageButtonStatusEnum.
                    fromRecodeTypeAndRecodeCodeAndReviewTypeByConfigure(recordType, Integer.parseInt(recordStatusCode), recipe.getReviewType(), showUseDrugConfig, noHaveBuyDrugConfig, haveSendInfo);
            giveModeShowButtonVO.setButtonType(buttonStatus.getPageButtonStatus());
        } else {
            LOGGER.error("当前按钮的显示信息不存在");
            giveModeShowButtonVO.setButtonType(No_Show_Button);
        }
    }

    private void setItemListNoShow(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        if (recipe.getClinicOrgan() == 1002753) {
            List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
            removeGiveModeData(giveModeButtonBeans, "supportMedicalPayment");
        }
    }


    private void afterSetting(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe) {
        List<GiveModeButtonDTO> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        LOGGER.info("afterSetting recipeId={}  giveModeButtonBeans={}", recipe.getRecipeId(), JSONUtils.toString(giveModeButtonBeans));
        //不支持配送，则按钮都不显示--包括药店取药
        String recipeSupportGiveMode = recipe.getRecipeSupportGiveMode();
        List<String> list = new ArrayList<>();

        // 从处方中获取支持的购药方式
        if (StringUtils.isNotEmpty(recipeSupportGiveMode)) {
            List<String> strings = Arrays.asList(recipeSupportGiveMode.split(","));

            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType()))) {
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getText());
            }
            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType()))) {
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getText());
            }
            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType()))) {
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText());
            }
            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType()))) {
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
            }
            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType()))) {
                list.add(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            }
            if (strings.contains(String.valueOf(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getType()))) {
                list.add(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
            }
        }
        try {
            //配置了白名单的就诊人只显示例外支付按钮，不在白名单的则隐藏例外支付按钮
            list = handleMedicalPaymentButton(recipe,list);
        }catch (Exception e){
            LOGGER.error("afterSetting saveGiveModeDatas error", e);
        }
        LOGGER.info("saveGiveModeDatas recipeId={} list={}", recipe.getRecipeId(), JSONUtils.toString(list));
        saveGiveModeDatas(giveModeButtonBeans, list);
        LOGGER.info("afterSetting saveGiveModeDatas recipeId={}  giveModeButtonBeans={}", recipe.getRecipeId(), JSONUtils.toString(giveModeButtonBeans));

        //从运营平台获取配置项和现在的按钮集合取交集
        GiveModeShowButtonDTO giveModeShowButton = operationClient.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        List<GiveModeButtonDTO> fromYyptButtons = giveModeShowButton.getGiveModeButtons();
        if (fromYyptButtons != null) {
            fromYyptButtons.retainAll(giveModeShowButtonVO.getGiveModeButtons());
            giveModeShowButtonVO.setGiveModeButtons(fromYyptButtons);
        }
        //如果是浙江省互联网的机构配置的是扁鹊的药企，则不显示任何购药方式
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
            if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
                drugsEnterprises.stream().forEach(drugsEnterprise -> {
                    if (StringUtils.isNotEmpty(drugsEnterprise.getAccount()) && "bqEnterprise".equals(drugsEnterprise.getAccount())) {
                        giveModeShowButtonVO.setGiveModeButtons(new ArrayList<>());
                    }
                });
            }
        }
    }
    /**
     * 针对绍兴市人民医院做个性化处理
     * 配置了白名单的就诊人只显示例外支付按钮，不在白名单的则隐藏例外支付按钮
     * @param recipe
     * @param list
     * @return
     */
    public List<String> handleMedicalPaymentButton(Recipe recipe,List<String> list){
        RecipeParameterDao parameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String recipeIdCardWhiteListOrgan = parameterDao.getByName("recipe_idCard_whiteList_organ");
        LOGGER.info("GiveModeManager handleMedicalPaymentButton={}", recipeIdCardWhiteListOrgan);
        if(StringUtils.isNotEmpty(recipeIdCardWhiteListOrgan)){
            List<String> organIdList = Arrays.asList(recipeIdCardWhiteListOrgan.split(","));
            String recipeIdCardWhiteList = parameterDao.getByName("recipe_idCard_whiteList");
            LOGGER.info("GiveModeManager recipeIdCardWhiteList={}", recipeIdCardWhiteList);
            if(organIdList.contains(recipe.getClinicOrgan().toString())){
                if(StringUtils.isEmpty(recipeIdCardWhiteList)){
                    list = list.stream().filter(a -> !a.equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    return list;
                }
                List<String> recipeIdCardWhiteLists = Arrays.asList(recipeIdCardWhiteList.split(","));
                PatientDTO patient = patientService.get(recipe.getMpiid());
                if (Objects.nonNull(patient)) {
                    LOGGER.info("GiveModeManager patient={}", JSONUtils.toString(patient));
                    if(recipeIdCardWhiteLists.contains(patient.getIdcard())){
                        list = list.stream().filter(a -> a.equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    }else{
                        list = list.stream().filter(a -> !a.equals(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())).collect(Collectors.toList());
                    }
                }
            }
        }
        return list;
    }

    private Map<String, String> getRecordInfo(Recipe recipe) {
        String recordType;
        Integer recordStatusCode;
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            recordType = LIST_TYPE_ORDER;
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (recipeOrder != null) {
                recordStatusCode = recipeOrder.getStatus();
            } else {
                recordStatusCode = recipe.getStatus();
            }
        } else {
            recordType = LIST_TYPE_RECIPE;
            if (RecipeStatusEnum.RECIPE_STATUS_WAIT_SEND.getType().equals(recipe.getStatus())) {
                recordStatusCode = RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType();
            } else {
                recordStatusCode = recipe.getStatus();
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("recordType", recordType);
        map.put("recordStatusCode", recordStatusCode.toString());
        return map;
    }


    private void saveGiveModeDatas(List<GiveModeButtonDTO> giveModeButtonBeans, List<String> remoteGiveMode) {
        List<String> list = new ArrayList<>();
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText())) {
            list.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
        }
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getText())) {
            list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getText());
        }
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getText())) {
            list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getText());
        }
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText())) {
            list.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText());
        }
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText())) {
            list.add(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
        }
        if (!remoteGiveMode.contains(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())) {
            list.add(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
        }
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonDTO giveModeShowButtonVO = (GiveModeButtonDTO) iterator.next();
            if (list.contains(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }
}
