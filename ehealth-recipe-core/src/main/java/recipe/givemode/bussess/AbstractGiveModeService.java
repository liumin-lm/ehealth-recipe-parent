package recipe.givemode.bussess;

import com.alibaba.fastjson.JSONArray;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import eh.base.constant.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.*;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.constant.RecipeOrderStatusEnum;
import recipe.factory.status.constant.RecipeStatusEnum;
import recipe.service.RecipeServiceSub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 20:01
 */
public abstract class AbstractGiveModeService implements IGiveModeBase{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGiveModeService.class);

    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeServiceSub recipeServiceSub;

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
        //模拟数据
        String jsonData = "[{\"latexId\":82020,\"configType\":\"hospitalindex\",\"configId\":\"1\",\"tempId\":0,\"boxIcon\":\"box_1_4.png\",\"boxTxt\":\"药企配送\",\"linkType\":\"3\",\"boxLink\":\"showSendToEnterprises\",\"used\":true,\"creater\":4015241,\"createrName\":\"高剑3\",\"creatDate\":\"2020-12-0320:40:28\",\"updater\":4015241,\"updateName\":\"高剑3\",\"lastModify\":\"2020-12-0320:40:28\",\"recipeskip\":\"1\"},{\"latexId\":82021,\"configType\":\"hospitalindex\",\"configId\":\"1\",\"tempId\":0,\"boxIcon\":\"box_1_6.png\",\"boxTxt\":\"医院配送\",\"linkType\":\"3\",\"boxLink\":\"showSendToHos\",\"used\":true,\"creater\":4015241,\"createrName\":\"高剑3\",\"creatDate\":\"2020-12-0320:40:28\",\"updater\":4015241,\"updateName\":\"高剑3\",\"lastModify\":\"2020-12-0320:40:28\",\"recipeskip\":\"1\"},{\"latexId\":82021,\"configType\":\"hospitalindex\",\"configId\":\"1\",\"tempId\":0,\"boxIcon\":\"box_1_6.png\",\"boxTxt\":\"到店取药\",\"linkType\":\"3\",\"boxLink\":\"supportTFDS\",\"used\":true,\"creater\":4015241,\"createrName\":\"高剑3\",\"creatDate\":\"2020-12-0320:40:28\",\"updater\":4015241,\"updateName\":\"高剑3\",\"lastModify\":\"2020-12-0320:40:28\",\"recipeskip\":\"1\"},{\"latexId\":82021,\"configType\":\"hospitalindex\",\"configId\":\"1\",\"tempId\":0,\"boxIcon\":\"box_1_6.png\",\"boxTxt\":\"到院自取\",\"linkType\":\"3\",\"boxLink\":\"supportToHos\",\"used\":true,\"creater\":4015241,\"createrName\":\"高剑3\",\"creatDate\":\"2020-12-0320:40:28\",\"updater\":4015241,\"updateName\":\"高剑3\",\"lastModify\":\"2020-12-0320:40:28\",\"recipeskip\":\"1\"}]";
        List<GiveModeButton> buttons = JSONArray.parseArray(jsonData, GiveModeButton.class);
        buttons.forEach(giveModeButton -> {
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
    public void setSpecialItem(PatientTabStatusRecipeDTO record, GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        //处理医院配送和药企配送的药企按钮，根据该机构配置的药企配送主体来决定
        Map result = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
        boolean showSendToEnterprises = result.containsKey("showSendToEnterprises");
        boolean showSendToHos = result.containsKey("showSendToHos");
        //如果运营平台没有配置药企配送或者医院配送，则可不用继续处理
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_ONLINE);
        payModeSupport.addAll(RecipeServiceSub.getDepSupportMode(RecipeBussConstant.PAYMODE_COD));
        Long enterprisesSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Enterprise_Send);
        Long hosSend = drugsEnterpriseDAO.getCountByOrganIdAndPayModeSupportAndSendType(recipe.getClinicOrgan(), payModeSupport, EnterpriseSendConstant.Hos_Send);
        if (showSendToEnterprises && enterprisesSend == null) {
            //表示运营平台虽然配置了药企配送但是该机构没有配置可配送的药企
            removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "showSendToEnterprises");
        }
        if (showSendToHos && null == hosSend ) {
            //表示运营平台虽然配置了医院配送但是该机构没有配置可配送的自建药企
            removeGiveModeData(giveModeShowButtonVO.getGiveModeButtons(), "showSendToHos");
        }
    }

    @Override
    public void setOtherButton(PatientTabStatusRecipeDTO record, GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe){
        //设置按钮的展示类型
        Boolean showUseDrugConfig = (Boolean) configService.getConfiguration(record.getOrganId(), "medicationGuideFlag");
        //已完成的处方单设置
        if ((LIST_TYPE_ORDER.equals(record.getRecordType()) && RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(record.getStatusCode()))
                || (LIST_TYPE_RECIPE.equals(record.getRecordType()) && RecipeStatusEnum.RECIPE_STATUS_FINISH.getType() == record.getStatusCode())) {
            //设置用药指导按钮
            if (showUseDrugConfig) {
                GiveModeButtonBean giveModeButton = new GiveModeButtonBean();
                giveModeButton.setButtonSkipType("1");
                giveModeButton.setShowButtonName("用药指导");
                giveModeButton.setShowButtonKey("supportMedicationGuide");
                giveModeShowButtonVO.getGiveModeButtons().add(giveModeButton);
            }
        }
        //1.判断配置项中是否配置了下载处方签，
        //2.是否是后置的，后置的判断状态是否是已审核，已完成, 配送中，
        //3.如果不是后置的，判断实际金额是否为0：为0则ordercode关联则展示，不为0支付则展示
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            boolean isDownload = recipeServiceSub.getDownConfig(recipe, order);
            giveModeShowButtonVO.setDownloadRecipeSign(isDownload);
        }
    }

    @Override
    public void setButtonType(PatientTabStatusRecipeDTO record, GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
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
            Boolean noHaveBuyDrugConfig = !showSendToEnterprises && !showSendToHos  && !supportTFDS && !supportToHos;

            //只有当亲处方有订单，且物流公司和订单号都有时展示物流信息
            Boolean haveSendInfo = false;
            RecipeOrder order = orderDAO.getOrderByRecipeId(recipe.getRecipeId());
            if (null != order && null != order.getLogisticsCompany() && StringUtils.isNotEmpty(order.getTrackingNumber())) {
                haveSendInfo = true;
            }
            RecipePageButtonStatusEnum buttonStatus = RecipePageButtonStatusEnum.
                    fromRecodeTypeAndRecodeCodeAndReviewTypeByConfigure(record.getRecordType(), record.getStatusCode(), recipe.getReviewType(), showUseDrugConfig, noHaveBuyDrugConfig, haveSendInfo);
            giveModeShowButtonVO.setButtonType(buttonStatus.getPageButtonStatus());
        } else {
            LOGGER.error("当前按钮的显示信息不存在");
            giveModeShowButtonVO.setButtonType(No_Show_Button);
        }
    }

    @Override
    public void afterSetting(GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        //不支持配送，则按钮都不显示--包括药店取药
        if (new Integer(2).equals(recipe.getDistributionFlag())) {
            removeGiveModeData(giveModeButtonBeans, "showSendToEnterprises");
            removeGiveModeData(giveModeButtonBeans, "showSendToHos");
            removeGiveModeData(giveModeButtonBeans, "supportTFDS");
        }
    }

    @Override
    public String getGiveModeTextByRecipe(Recipe recipe) {
        GiveModeShowButtonVO giveModeShowButtonVO = this.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        String giveModeKey ;
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
            giveModeKey = "otherPay";
        }
        List<GiveModeButtonBean> giveModeButtonBeans = giveModeShowButtonVO.getGiveModeButtons();
        Map<String, String> result = giveModeButtonBeans.stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
        return result.get(giveModeKey);
    }

    protected void removeGiveModeData(List<GiveModeButtonBean> giveModeButtonBeans, String remoteGiveMode){
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeShowButtonVO = (GiveModeButtonBean) iterator.next();
            if (remoteGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }

    protected void saveGiveModeData(List<GiveModeButtonBean> giveModeButtonBeans, String saveGiveMode){
        Iterator iterator = giveModeButtonBeans.iterator();
        while (iterator.hasNext()) {
            GiveModeButtonBean giveModeShowButtonVO = (GiveModeButtonBean) iterator.next();
            if (!saveGiveMode.equals(giveModeShowButtonVO.getShowButtonKey())) {
                iterator.remove();
            }
        }
    }
}
