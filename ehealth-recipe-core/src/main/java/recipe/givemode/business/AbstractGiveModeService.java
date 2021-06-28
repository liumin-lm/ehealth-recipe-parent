package recipe.givemode.business;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.constant.RecipeSupportGiveModeEnum;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.*;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.service.RecipeServiceSub;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 20:01
 */
public abstract class AbstractGiveModeService implements IGiveModeBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGiveModeService.class);

    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    private static final String LIST_TYPE_RECIPE = "1";

    private static final String LIST_TYPE_ORDER = "2";

    private static final Integer No_Show_Button = 3;

    @Override
    public void validRecipeData(Recipe recipe) {
        if (recipe == null) {
            LOGGER.warn("validRecipeData: recipeId:{},对应处方信息不存在,", recipe.getRecipeId());
            throw new DAOException(ErrorCode.SERVICE_ERROR,
                    "处方数据为空");
        }
        if (StringUtils.isEmpty(recipe.getRecipeMode())) {
            LOGGER.warn("validRecipeData: recipeId:{}  recipeMode:{},处方流转方式为空", recipe.getRecipeId(), recipe.getRecipeMode());
            throw new DAOException(ErrorCode.SERVICE_ERROR,
                    "处方流转方式为空");
        }
    }

    @Override
    public GiveModeShowButtonVO getGiveModeSettingFromYypt(Integer organId) {
        List<GiveModeButtonBean> giveModeButtonBeans = new ArrayList<>();
        GiveModeShowButtonVO giveModeShowButtonVO = new GiveModeShowButtonVO();
        IScratchableService scratchableService = AppContextHolder.getBean("eh.scratchableService", IScratchableService.class);
        List<ScratchableBean> scratchableBeans = scratchableService.findScratchableByPlatform("myRecipeDetailList", organId + "", 1);
        scratchableBeans.forEach(giveModeButton -> {
            GiveModeButtonBean giveModeButtonBean = new GiveModeButtonBean();
            giveModeButtonBean.setShowButtonKey(giveModeButton.getBoxLink());
            giveModeButtonBean.setShowButtonName(giveModeButton.getBoxTxt());
            giveModeButtonBean.setButtonSkipType(giveModeButton.getRecipeskip());
            if (!"listItem".equals(giveModeButtonBean.getShowButtonKey())) {
                giveModeButtonBeans.add(giveModeButtonBean);
            } else {
                giveModeShowButtonVO.setListItem(giveModeButtonBean);
            }
        });
        giveModeShowButtonVO.setGiveModeButtons(giveModeButtonBeans);
        if (giveModeShowButtonVO.getListItem() == null) {
            //说明运营平台没有配置列表
            GiveModeButtonBean giveModeButtonBean = new GiveModeButtonBean();
            giveModeButtonBean.setShowButtonKey("listItem");
            giveModeButtonBean.setShowButtonName("列表项");
            giveModeButtonBean.setButtonSkipType("1");
            giveModeShowButtonVO.setListItem(giveModeButtonBean);
        }
        return giveModeShowButtonVO;
    }

    @Override
    public void setButtonOptional(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        boolean isOptional = !(ReviewTypeConstant.Preposition_Check == recipe.getReviewType() && (RecipeStatusConstant.SIGN_NO_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ERROR_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.SIGN_ING_CODE_PHA == recipe.getStatus() || RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() || (RecipeStatusConstant.CHECK_NOT_PASS_YS == recipe.getStatus() && RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus())));
        giveModeShowButtonVO.setOptional(isOptional);
    }

    @Override
    public void setSpecialItem(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend) {
        //处理医院配送和药企配送的药企按钮，根据该机构配置的药企配送主体来决定
        Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
        boolean showSendToEnterprises = result.containsKey("showSendToEnterprises");
        boolean showSendToHos = result.containsKey("showSendToHos");
        //如果运营平台没有配置药企配送或者医院配送，则可不用继续处理
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_ONLINE);
        payModeSupport.addAll(RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD));
        Long enterprisesSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Enterprise_Send);
        Long hosSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Hos_Send);
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
    public void setOtherButton(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        String recordType = getRecordInfo(recipe).get("recordType");
        String recordStatusCode = getRecordInfo(recipe).get("recordStatusCode");
        // 按钮的展示类型
        Boolean showUseDrugConfig = (Boolean) configService.getConfiguration(recipe.getClinicOrgan(), "medicationGuideFlag");
        //已完成的处方单设置
        if ((LIST_TYPE_ORDER.equals(recordType) && RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().toString().equals(recordStatusCode))
                || (LIST_TYPE_RECIPE.equals(recordType) && RecipeStatusEnum.RECIPE_STATUS_FINISH.getType().toString().equals(recordStatusCode))) {
            //设置用药指导按钮
            if (showUseDrugConfig) {
                GiveModeButtonBean giveModeButton = new GiveModeButtonBean();
                giveModeButton.setButtonSkipType("1");
                giveModeButton.setShowButtonName("用药指导");
                giveModeButton.setShowButtonKey("supportMedicationGuide");
                giveModeShowButtonVO.getGiveModeButtons().add(giveModeButton);
                //此时将ButtonType设置为4
                giveModeShowButtonVO.setButtonType(4);
            }
        }
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
            recordStatusCode = recipe.getStatus();
        }
        Map<String, String> map = new HashMap<>();
        map.put("recordType", recordType);
        map.put("recordStatusCode", recordStatusCode.toString());
        return map;
    }

    @Override
    public void setButtonType(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        String recordType = getRecordInfo(recipe).get("recordType");
        String recordStatusCode = getRecordInfo(recipe).get("recordStatusCode");
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        //添加判断，当选药按钮都不显示的时候，按钮状态为不展示
        if (CollectionUtils.isNotEmpty(giveModeButtonBeans)) {
            //查找是否包含用药指导按钮
            Map result = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
            boolean showSendToEnterprises = result.containsKey("showSendToEnterprises");
            boolean showSendToHos = result.containsKey("showSendToHos");
            boolean supportToHos = result.containsKey("supportToHos");
            boolean supportTFDS = result.containsKey("supportTFDS");
            boolean showUseDrugConfig = result.containsKey("supportMedicationGuide");
            //当处方在待处理、前置待审核通过时，购药配送为空不展示按钮
            Boolean noHaveBuyDrugConfig = !showSendToEnterprises && !showSendToHos && !supportTFDS && !supportToHos;

            //只有当亲处方有订单，且物流公司和订单号都有时展示物流信息
            Boolean haveSendInfo = false;
            RecipeOrder order = orderDAO.getOrderByRecipeId(recipe.getRecipeId());
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

    @Override
    public void setItemListNoShow(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        if (recipe.getClinicOrgan() == 1002753) {
            List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
            removeGiveModeData(giveModeButtonBeans, "supportMedicalPayment");
        }
    }

    @Override
    public void afterSetting(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        LOGGER.info("afterSetting recipeId={}  giveModeButtonBeans={}",recipe.getRecipeId(),JSONUtils.toString(giveModeButtonBeans));
        //不支持配送，则按钮都不显示--包括药店取药
        String recipeSupportGiveMode = recipe.getRecipeSupportGiveMode();
        List<String> list = new ArrayList<>();
        if (StringUtils.isEmpty(recipeSupportGiveMode)) {
            list.add(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            // 兼容老版本处方没有写入支持的购药方式 后期删除
            if (RecipeDistributionFlagEnum.HOS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
            }
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getText());
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getText());
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText());
            }
            // 线下转线上没有 配送标记
            if (Objects.isNull(recipe.getDistributionFlag()) || recipe.getDistributionFlag() == 0) {
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getText());
                list.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getText());
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText());
                list.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText());
            }
        } else {
            // 从处方中获取支持的购药方式
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
        }
        saveGiveModeDatas(giveModeButtonBeans, list);


        //从运营平台获取配置项和现在的按钮集合取交集
        GiveModeShowButtonVO giveModeShowButton = getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        List<GiveModeButtonBean> fromYyptButtons = giveModeShowButton.getGiveModeButtons();
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

    @Override
    public void setShowButton(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        boolean showButton = false;
        if (CollectionUtils.isNotEmpty(giveModeShowButtonVO.getGiveModeButtons())) {
            if (ReviewTypeConstant.Preposition_Check == recipe.getReviewType()) {
                //待药师审核，审核一次不通过，待处理无订单
                if (RecipeStatusConstant.READY_CHECK_YS == recipe.getStatus() || RecipecCheckStatusConstant.First_Check_No_Pass == recipe.getCheckStatus() || (RecipeStatusConstant.CHECK_PASS == recipe.getStatus() && null == recipe.getOrderCode())) {
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

    @Override
    public String getGiveModeTextByRecipe(Recipe recipe) {
        if (recipe == null) {
            return "";
        }
        GiveModeShowButtonVO giveModeShowButtonVO = this.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        String giveModeKey;
        if (new Integer(1).equals(recipe.getGiveMode()) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (new Integer(1).equals(recipeOrder.getSendType())) {
                //表示医院配送
                giveModeKey = "showSendToHos";
            } else {
                //表示药企配送
                giveModeKey = "showSendToEnterprises";
            }
        } else if (new Integer(2).equals(recipe.getGiveMode())) {
            //表示到院取药
            giveModeKey = "supportToHos";
        } else if (new Integer(3).equals(recipe.getGiveMode())) {
            giveModeKey = "supportTFDS";
        } else if (new Integer(4).equals(recipe.getGiveMode())) {
            giveModeKey = "supportDownload";
        } else {
            giveModeKey = "";
        }
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        Map<String, String> result = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
        return result.get(giveModeKey);
    }

    protected void removeGiveModeData(List<GiveModeButtonBean> giveModeButtonBeans, String remoteGiveMode) {
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeShowButtonVO = (GiveModeButtonBean) iterator.next();
            if (remoteGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }

    protected void saveGiveModeDatas(List<GiveModeButtonBean> giveModeButtonBeans, List<String> remoteGiveMode) {
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
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeShowButtonVO = (GiveModeButtonBean) iterator.next();
            if (list.contains(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }

    protected void saveGiveModeData(List<GiveModeButtonBean> giveModeButtonBeans, String saveGiveMode) {
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeShowButtonVO = (GiveModeButtonBean) iterator.next();
            if (!saveGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }
}
