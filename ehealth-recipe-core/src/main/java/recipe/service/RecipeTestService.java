package recipe.service;

import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.RegulationDrugCategoryReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.platform.recipe.mode.PushRecipeAndOrder;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.entity.*;
import ctd.account.session.ClientSession;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.msg.constant.MqConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import recipe.ApplicationUtils;
import recipe.common.OnsConfig;
import recipe.dao.*;
import recipe.manager.EnterpriseManager;
import recipe.service.afterpay.LogisticsOnlineOrderService;
import recipe.service.recipecancel.RecipeCancelService;
import recipe.util.DateConversion;
import recipe.util.RecipeMsgUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author yu_yun
 * @date 2016/7/13
 * 用于测试处方流程
 */
@RpcBean(value = "recipeTestService", mvc_authentication = false)
public class RecipeTestService {
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private LogisticsOnlineOrderService logisticsOnlineOrderService;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @RpcService
    public PushRecipeAndOrder getPushRecipeAndOrder(Integer recipeId, Integer depId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        return enterpriseManager.getPushRecipeAndOrder(recipe, drugsEnterprise);
    }

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTestService.class);

    @RpcService
    public String testanyway() {
        ClientSession clientSession = ClientSession.getCurrent();
        return JSONUtils.toString(clientSession);
    }

    @RpcService
    public int checkPassFail(Integer recipeId, Integer errorCode, String msg) {
        HisCallBackService.checkPassFail(recipeId, errorCode, msg);
        return 0;
    }

    /**
     * 测试用-将处方单改成已完成状态
     */
    @RpcService
    public int changeRecipeToFinish(String recipeCode, int organId) {
        HisCallBackService.finishRecipesFromHis(Arrays.asList(recipeCode), organId);
        return 0;
    }

    @RpcService
    public int changeRecipeToPay(String recipeCode, int organId) {
        HisCallBackService.havePayRecipesFromHis(Arrays.asList(recipeCode), organId);
        return 0;
    }

    @RpcService
    public int changeRecipeToHisFail(Integer recipeId) {
        HisCallBackService.havePayFail(recipeId);
        return 0;
    }

    @RpcService
    public void testSendMsg(String bussType, Integer bussId, Integer organId) {
        SmsInfoBean info = new SmsInfoBean();
        // 业务表主键
        info.setBusId(bussId);
        // 业务类型
        info.setBusType(bussType);
        info.setSmsType(bussType);
        info.setStatus(0);
        // 短信服务对应的机构， 0代表通用机构
        info.setOrganId(organId);
        info.setExtendValue("康复药店");
        info.setExtendWithoutPersist(JSONUtils.toString(Arrays.asList("2c9081814d720593014d758dd0880020")));
        ISmsPushService smsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);
        smsPushService.pushMsg(info);
    }

    @RpcService
    public void testSendMsg4new(Integer bussId, String bussType) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(bussId);
        RecipeMsgService.sendRecipeMsg(RecipeMsgUtils.valueOf(bussType), recipe);
    }


    @RpcService
    public void testSendMsgForRecipe(Integer recipeId, int afterStatus) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeMsgService.batchSendMsg(recipe, afterStatus);
    }

    @RpcService
    public void testSendMqMsg(Integer recipeId, String status) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        NoticeNgariRecipeInfoReq notice = new NoticeNgariRecipeInfoReq();
        Recipe recipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        notice.setOrganId(recipe.getClinicOrgan());
        notice.setRecipeID(recipe.getRecipeCode());
        notice.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        notice.setRecipeStatus(status);
        MQHelper.getMqPublisher().publish(OnsConfig.hisCdrinfo, notice, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM);
    }


    @RpcService(timeout = 1000)
    public Map<String, Object> analysisDrugList(List<Integer> drugIdList, int organId, boolean useFile) {
        DrugsEnterpriseTestService testService = new DrugsEnterpriseTestService();
        try {
            return testService.analysisDrugList(drugIdList, organId, useFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @RpcService
    public List<SearchDrugDetailDTO> findDrugListsByNameOrCodePageStaitc(
            Integer organId, int drugType, String drugName, int start) {
        DrugListExtService drugListExtService = ApplicationUtils.getRecipeService(DrugListExtService.class, "drugList");

        return drugListExtService.findDrugListsByNameOrCodePageStaitc(organId, drugType, drugName, start);
    }

    @RpcService
    public void insertDrugCategoryByOrganId(Integer organId, String createDate) {
        IRegulationService hisService =
                AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndCreateDt(organId, DateConversion.parseDate(createDate, "yyyy-MM-dd HH:mm:ss"));
        LOGGER.info("RecipeTestService-insertDrugCategoryByOrganId organDrugLists count:{}.", organDrugLists.size());
        for (OrganDrugList organDrugList : organDrugLists) {
            List<RegulationDrugCategoryReq> drugCategoryReqs = new ArrayList<>();
            RegulationDrugCategoryReq drugCategoryReq = packingDrugCategoryReq(organDrugList);
            drugCategoryReqs.add(drugCategoryReq);

            try {
                HisResponseTO hisResponseTO = hisService.uploadDrugCatalogue(organDrugList.getOrganId(), drugCategoryReqs);
                LOGGER.info("RecipeTestService-insertDrugCategoryByOrganId hisResponseTO parames:" + JSONUtils.toString(hisResponseTO));
            } catch (Exception e) {
                LOGGER.error("RecipeTestService-insertDrugCategoryByOrganId hisResponseTO error:" + JSONUtils.toString(organDrugList) + JSONUtils.toString(e.getStackTrace()));
            }
        }

    }

    private RegulationDrugCategoryReq packingDrugCategoryReq(OrganDrugList organDrugList) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        IMinkeOrganService minkeOrganService = AppContextHolder.getBean("jgpt.minkeOrganService", IMinkeOrganService.class);
        OrganDTO organ = organService.getByOrganId(organDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
        RegulationDrugCategoryReq drugCategoryReq = new RegulationDrugCategoryReq();
        String organId = minkeOrganService.getRegisterNumberByUnitId(organ.getMinkeUnitID());
        drugCategoryReq.setUnitID(organ.getMinkeUnitID());
        drugCategoryReq.setOrganID(organId);
        drugCategoryReq.setOrganName(organ.getName());
        Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(organDrugList.getDrugId());
        if (targetDrugId != null) {
            drugCategoryReq.setPlatDrugCode(targetDrugId.toString());
        } else {
            drugCategoryReq.setPlatDrugCode(organDrugList.getDrugId().toString());
        }
        drugCategoryReq.setPlatDrugName(organDrugList.getDrugName());
        if (StringUtils.isNotEmpty(organDrugList.getOrganDrugCode())) {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugCode());
        } else {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugId().toString());
        }

        drugCategoryReq.setHospDrugName(organDrugList.getDrugName());
        drugCategoryReq.setHospTradeName(organDrugList.getSaleName());
        drugCategoryReq.setHospDrugPacking(organDrugList.getDrugSpec());

        drugCategoryReq.setHospDrugManuf(organDrugList.getProducer());

        drugCategoryReq.setUseFlag(organDrugList.getStatus() + "");
        drugCategoryReq.setDrugClass("1901");
        drugCategoryReq.setUpdateTime(new Date());
        drugCategoryReq.setCreateTime(new Date());
        LOGGER.info("RecipeTestService-packingDrugCategoryReq drugCategoryReq:" + JSONUtils.toString(drugCategoryReq));
        return drugCategoryReq;
    }

    @RpcService
    public Integer updateIn(Integer organId, Integer drugId, Double userTotalDose) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        BigDecimal totalDose = new BigDecimal(userTotalDose);
        return saleDrugListDAO.updateInventoryByOrganIdAndDrugId(organId, drugId, totalDose);
    }

    @RpcService
    public void insertOrganDrugList(Integer organId, Integer targetOrganId, String date) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganId(organId);
        for (OrganDrugList organDrugList : organDrugLists) {
            organDrugList.setOrganId(targetOrganId);
            organDrugList.setCreateDt(DateConversion.getCurrentDate(date, DateConversion.DEFAULT_DATE_TIME));
            organDrugList.setLastModify(DateConversion.getCurrentDate(date, DateConversion.DEFAULT_DATE_TIME));
            organDrugListDAO.save(organDrugList);
        }
        insertDrugCategoryByOrganId(targetOrganId, date);
    }

    @RpcService
    public void saveOrUpdateRecipeParames(RecipeParameter recipeParameter, Integer flag) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        if (1 == flag) {
            recipeParameterDao.save(recipeParameter);
        } else {
            recipeParameterDao.update(recipeParameter);
        }
    }

    @RpcService
    public void updateRecipeOrder(String orderCode) {
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Map map = new HashMap<>();
        map.put("status", 1);
        map.put("effective", 0);
        map.put("payFlag", 0);
        recipeOrderDAO.updateByOrdeCode(orderCode, map);
    }


    /**
     * 处方退费应该按取消处方处理通知给药企--test
     *
     * @param
     */
    @RpcService
    public HisResponseTO doCancelRecipeForEnterprise(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeCancelService recipeCancelService = ApplicationUtils.getRecipeService(RecipeCancelService.class);
        HisResponseTO response = recipeCancelService.doCancelRecipeForEnterprise(recipe);
        return response;
    }

    /**
     * 手动物流下单
     *
     * @param orderCode 订单编号
     */
    @RpcService
    public void onlineOrder(String orderCode) {
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIdList);
        logisticsOnlineOrderService.onlineOrder(order, recipes);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeResultBean result = RecipeResultBean.getSuccess();
        hisService.recipeDrugTake(recipes.get(0).getRecipeId(), order.getPayFlag(), result);
    }

    /**
     * 迁移ext数据
     */
    @RpcService
    public void moveOldData() {
        List<RecipeOrder> moveData = recipeDAO.findMoveData();
        moveData.forEach(order -> {
            Map<String, Double> map = new HashMap<>();
            map.put("preSettleTotalAmount",order.getPreSettletotalAmount());
            map.put("fundAmount",order.getFundAmount());
            map.put("cashAmount",order.getCashAmount());
            recipeOrderDAO.updateByOrdeCode(order.getOrderCode(), map);
        });
    }

    @RpcService
    public String getThreadPoolInfo(){
        ThreadPoolTaskExecutor service = AppContextHolder.getBean("busTaskExecutor", ThreadPoolTaskExecutor.class);
        ThreadPoolExecutor threadPoolExecutor = service.getThreadPoolExecutor();
        return "当前线程池排队线程数:"+threadPoolExecutor.getQueue().size()+",当前线程池活动线程数:"+threadPoolExecutor.getActiveCount()+",当前线程池完成线程数:"+threadPoolExecutor.getCompletedTaskCount()+",当前线程池总线程数:"+threadPoolExecutor.getTaskCount();
    }
}
