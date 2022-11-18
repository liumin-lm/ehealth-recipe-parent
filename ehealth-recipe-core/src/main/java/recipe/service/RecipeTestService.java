package recipe.service;

import com.alibaba.fastjson.JSON;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.*;
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
import com.ngari.recipe.pay.model.PayResultDTO;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.account.session.ClientSession;
import ctd.net.broadcast.MQHelper;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.msg.constant.MqConstant;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.RevisitClient;
import recipe.common.OnsConfig;
import recipe.core.api.IDrugBusinessService;
import recipe.dao.*;
import recipe.drugTool.service.DrugToolService;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.manager.StateManager;
import recipe.service.afterpay.LogisticsOnlineOrderService;
import recipe.service.paycallback.RecipePayInfoCallBackService;
import recipe.service.recipecancel.RecipeCancelService;
import recipe.util.DateConversion;
import recipe.util.RecipeMsgUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

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
    @Autowired
    private IDrugBusinessService drugBusinessService;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private DrugToolService drugToolService;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private RecipeParameterDao recipeParameterDao;



    @RpcService
    public PushRecipeAndOrder getPushRecipeAndOrder(Integer recipeId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
        return enterpriseManager.getPushRecipeAndOrder(recipe, drugsEnterprise, "");
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
    }

    /**
     * 查询sql
     select * from cdr_recipeorder o,cdr_drugsenterprise e
     where o.enterpriseId=e.id
     and o.payTime>='2022/07/28 18:17:57'
     and o.trackingNumber is null
     and e.logisticsType=1
     and o.PayFlag=1
     and o.AddressID>0
     * @param orderCodeList
     */
    @RpcService
    public void onlineOrderList(List<String> orderCodeList) {
        for (String orderCode:orderCodeList ) {
            onlineOrder(orderCode);
        }
    }

    /**
     * 通知his结算
     * @param recipeId
     */
    @RpcService
    public void recipeDrugTake(Integer recipeId){
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeResultBean result = RecipeResultBean.getSuccess();
        hisService.recipeDrugTake(recipeId, 1, result);
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

//    /**
//     * 更新快递费
//     */
//    @RpcService
//    public void updateLogisticsFee(Integer depId, List<String> addrArea, Double price){
//        addrArea.forEach(addr->{
//            DrugDistributionPrice drugDistributionPrice = drugDistributionPriceDAO.getByEnterpriseIdAndAddrArea(depId, addr);
//            if (null == price) {
//                drugDistributionPrice.setDistributionPrice(null);
//            } else {
//                drugDistributionPrice.setDistributionPrice(new BigDecimal(price));
//            }
//            drugDistributionPriceDAO.update(drugDistributionPrice);
//        });
//    }


    /**
     * @auther maoze
     * @desc 修改新的审方状态
     * REVIEWING(2, "审核中"),
     * FAIL(3, "审核未通过"),
     * FAIL_DOC_CONFIRMING(4, "未通过，医生确认中"),
     * PASS(5, "审核通过"),
     * DOC_FORCED_PASS(6, "医生强制通过"),
     * NO_REVIEW(7, "无需审核"),
     * @param
     */
    @RpcService
    @LogRecord
    public Integer updateAuditState(Integer max, Integer min) {
        int start = 1, pageSize = 1000, userNum = 0;
        IRecipeCheckService iRecipeCheckService = AppContextHolder.getBean("recipeaudit.recipeCheckServiceImpl", IRecipeCheckService.class);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        List<RecipeCheckBean> list;
        do {
            list = iRecipeCheckService.findRecipeCheck(max, min, start, pageSize);
            start += pageSize;
            if (CollectionUtils.isEmpty(list)) {
                return userNum;
            } else {
                userNum += list.size();
            }
            List<Integer> recipeIds = list.stream().map(RecipeCheckBean::getRecipeId).collect(Collectors.toList());
            List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
            HashMap<Integer, Recipe> recipeMap = (HashMap<Integer, Recipe>) recipes.stream().collect(Collectors.toMap(Recipe::getRecipeId, Recipe -> Recipe));
            for (RecipeCheckBean item : list) {
                try {
                    Recipe recipe = recipeMap.get(item.getRecipeId());
                    if (recipe == null) {
                        break;
                    }
                    if (item.getChecker() != null) {
                        // 审核未通过
                        if (item.getCheckStatus() == 0) {
                            stateManager.updateAuditState(item.getRecipeId(), RecipeAuditStateEnum.FAIL);
                        }
                        // 审核通过
                        if (item.getCheckStatus() == 1) {
                            stateManager.updateAuditState(item.getRecipeId(), RecipeAuditStateEnum.PASS);
                        }
                    }
                    // 医生确认中
                    if (recipe.getCheckStatus() == 1) {
                        stateManager.updateAuditState(item.getRecipeId(), RecipeAuditStateEnum.FAIL_DOC_CONFIRMING);
                    }
                    // 医生强制通过
                    if (recipe.getCheckStatus() == 0 && StringUtils.isNoneBlank(recipe.getSupplementaryMemo()) && recipe.getCheckFlag() == 1) {
                        stateManager.updateAuditState(item.getRecipeId(), RecipeAuditStateEnum.DOC_FORCED_PASS);
                    }
                    // 取消
                    if (recipe.getStatus() == 9) {
                        stateManager.updateAuditState(item.getRecipeId(), RecipeAuditStateEnum.NO_REVIEW);
                    }
                } catch (Exception e) {
                    LOGGER.info("recipeTestService-updateAuditState recipecheck:{} - recipe:{} Exception:", JSON.toJSONString(item), JSON.toJSONString(recipeMap.get(item.getRecipeId())), e);
                }
            }
        } while (CollectionUtils.isNotEmpty(list));
        return 0;
    }

    @RpcService
    public String getOrganDrugList(Integer organId, String organDrugCode, Integer drugId){
        OrganDrugList organDrugList = drugBusinessService.getOrganDrugList(organId, organDrugCode, drugId);
        return JSON.toJSONString(organDrugList);
    }

    @RpcService
    public String recipePdfTest(Integer recipeId, Integer type) throws Exception {
        createPdfFactory.updatePdfToImg(recipeId, type);
        return null;
    }


    @RpcService
    public Map<String, Object> readDrugExcelTest(String filePath, int organId, String operator){
        LOGGER.info("readDrugExcelTest filePath={},organId={},operator={}",filePath,organId,operator);
        Workbook wb = null;
        File file = new File(filePath);
        String fileName = file.getName();
        String subFileName = fileName.substring(fileName.lastIndexOf("."));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            if(".xls".equals(subFileName)){
                wb = new HSSFWorkbook(fileInputStream);
            }
            else if(".xlsx".equals(subFileName)){
                wb = new XSSFWorkbook(fileInputStream);
            }else return null;
            wb.write(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = os.toByteArray();
        return drugToolService.readDrugExcel(bytes,fileName,organId,operator);
    }

    @RpcService
    public String getParameterValue(String name){
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        return recipeParameterDao.getByName(name);
    }

    @RpcService
    public String updateAutoCheckValue(Date startDate,Date endDate,Integer auditOrgan){
        IRecipeCheckService recipeCheckService=  RecipeAuditAPI.getService(IRecipeCheckService.class,"recipeCheckServiceImpl");
        RecipeExtendDAO extendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        StringBuffer buffer=new StringBuffer();

        Integer autoCheck=0;
        List<Integer> noCheckRecipeIds=recipeCheckService.findByIsAutoCheck(autoCheck,auditOrgan,startDate,endDate);
        int noCheckUpdateNum = 0;
        if (0 < noCheckRecipeIds.size()) {
            int start = 0;
            int end = 0;
            while (end < noCheckRecipeIds.size()) {
                start = end;
                end = start + 100;
                if (end > noCheckRecipeIds.size()) {
                    end = noCheckRecipeIds.size();
                }
                Integer flag=extendDAO.updateAutoCheck(autoCheck,noCheckRecipeIds.subList(start, end));
                noCheckUpdateNum=noCheckUpdateNum+flag;
            }
        }
        buffer.append("更新autoCheck=0数据条数："+noCheckUpdateNum);


        autoCheck=1;
        List<Integer> hasCheckRecipeIds=recipeCheckService.findByIsAutoCheck(autoCheck,auditOrgan,startDate,endDate);
        int hasCheckUpdateNum = 0;
        if (0 < hasCheckRecipeIds.size()) {
            int start = 0;
            int end = 0;
            while (end < hasCheckRecipeIds.size()) {
                start = end;
                end = start + 100;
                if (end > hasCheckRecipeIds.size()) {
                    end = hasCheckRecipeIds.size();
                }
                Integer flag=extendDAO.updateAutoCheck(autoCheck,hasCheckRecipeIds.subList(start, end));
                hasCheckUpdateNum=hasCheckUpdateNum+flag;
            }
        }
        buffer.append("-----更新autoCheck=1数据条数："+hasCheckUpdateNum);

        return buffer.toString();
    }

    /**
     * 监管平台处方实时上传：开方/审方上传
     * @param recipeId
     * @return
     */
    @RpcService
    public RegulationRecipeIndicatorsReq getUploadRecipeIndicatorsReq(Integer recipeId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }
        List<RegulationRecipeIndicatorsReq> request = new ArrayList<>();
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        service.splicingBackRecipeData(Collections.singletonList(recipe), request);
        if(request==null || request.size()==0){
            return null;
        }
        return request.get(0);
    }

    /**
     * 监管平台处方实时上传：上传核销信息
     * @param recipeId
     * @return
     */
    @RpcService
    public RegulationRecipeVerificationIndicatorsReq getUploadRecipeVerificationIndicatorsReq(Integer recipeId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }

        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        List<RegulationRecipeVerificationIndicatorsReq> requestList=service.getRegulationRecipeVerificationIndicatorsReqs(Collections.singletonList(recipe));
        return requestList==null || requestList.size()==0?null:requestList.get(0);
    }

    /**
     * 监管平台处方实时上传：组装派药/配送完成接口
     * @param recipeId
     * @return
     */
    @RpcService
    public RegulationSendMedicineReq getRegulationSendFinishMedicineReq(Integer recipeId){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }

        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        RegulationSendMedicineReq request=service.pakRegulationSendMedicineReq(recipeId);
        return request;
    }

    /**
     * 监管平台处方实时上传：组装处方支付上传接口
     * @param recipeId
     * @return
     */
    @RpcService
    public RegulationOutpatientPayReq getRegulationOutpatientPayReq(Integer recipeId,String refundNo){
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return null;
        }
        List<Integer> recipeIds = recipeDAO.findRecipeIdsByOrderCode(recipe.getOrderCode());
        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());

        if(recipeIds==null || order==null){
            return null;
        }
        HisSyncSupervisionService service = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
        RegulationOutpatientPayReq request=service.getRegulationOutpatientPayReq(recipe.getPayFlag(),refundNo,
                recipeIds,order,recipe);
        return request;
    }

    @RpcService
    public List<RecipeOrder> findEffectiveOrderByOrderCode(String orderCode){
        Set<String> orderSet = new HashSet<>();
        orderSet.add(orderCode);
        return orderManager.findEffectiveOrderByOrderCode(orderSet);
    }

    @RpcService
    public RevisitExDTO retryGetByClinicId(Integer clinicId){
        return revisitClient.retryGetByClinicId(clinicId);
    }

    /**
     * 卫宁支付回调太慢导致患者取消，数据处理
     * @param orderIdList 订单列表
     * @param flag 是否调用支付回调接口
     */
    @RpcService
    public void updatePayBackInfo(List<Integer> orderIdList, Boolean flag){
        List<RecipeOrder> recipeOrderList = recipeOrderDAO.findByOrderId(orderIdList);
        recipeOrderList.forEach(recipeOrder -> {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            recipeIdList.forEach(recipeId->{
                Recipe recipe = recipeDAO.getByRecipeId(recipeId);
                recipe.setStatus(2);
                recipe.setGiveMode(1);
                recipe.setPayFlag(0);
                recipe.setOrderCode(recipeOrder.getOrderCode());
                recipeDAO.updateNonNullFieldByPrimaryKey(recipe);
            });
            recipeOrder.setStatus(1);
            recipeOrder.setEffective(1);
            recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
            if(flag){
                String payBackInfo = recipeParameterDao.getByName(recipeOrder.getOrderId() + "_PayInfoCallBack");
                PayResultDTO payResultDTO = JSON.parseObject(payBackInfo, PayResultDTO.class);
                RecipePayInfoCallBackService recipePayInfoCallBackService = ApplicationUtils.getRecipeService(RecipePayInfoCallBackService.class);
                recipePayInfoCallBackService.doHandleAfterPay(payResultDTO);
            }
        });
    }
}
