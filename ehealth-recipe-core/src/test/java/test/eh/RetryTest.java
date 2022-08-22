package test.eh;

import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.mode.PayNotifyResTO;
import com.ngari.recipe.hisprescription.model.*;
import ctd.util.JSONUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import recipe.atop.greenroom.DrugsEnterpriseGmAtop;
import recipe.atop.greenroom.RecipeOrderRefundGmAtop;
import recipe.atop.open.DrugOpenAtop;
import recipe.atop.patient.DrugPatientAtop;
import recipe.client.ConsultClient;
import recipe.client.DrugClient;
import recipe.client.IConfigurationClient;
import recipe.core.api.IStockBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.factory.offlinetoonline.impl.BaseOfflineToOnlineService;
import recipe.manager.*;
import recipe.presettle.settle.MedicalSettleService;
import recipe.retry.RecipeRetryService;
import recipe.service.HosPrescriptionService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeRefundService;
import recipe.service.RecipeService;
import recipe.service.buspay.RecipeBusPayInfoService;
import recipe.service.sync.DrugSyncToEsService;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * created by shiyuping on 2020/12/9
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class RetryTest extends AbstractJUnit4SpringContextTests {

    @Resource
    private RecipeRetryService recipeSettleRetryService;
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeService recipeService;

    @Resource
    private HosPrescriptionService hosPrescriptionService;
    @Resource
    RemoteRecipeService remoteRecipeService;
    @Resource
    RecipeHisService recipeHisService;
    @Resource
    BaseOfflineToOnlineService baseOfflineToOnlineService;
    @Resource
    ButtonManager buttonManager;
    @Resource
    DrugClient drugClient;
    @Resource
    RecipeBusPayInfoService recipeBusPayInfoService;
    @Resource
    RecipeOrderDAO recipeOrderDAO;
    @Resource
    RecipeOrderPayFlowManager recipeOrderPayFlowManager;
    @Resource
    EnterpriseManager enterpriseManager;
    @Resource
    IConfigurationClient configurationClient;
    @Resource
    RecipeExtendDAO recipeExtendDAO;
    @Resource
    RemoteDrugEnterpriseService remoteDrugEnterpriseService;
    @Resource
    IStockBusinessService stockBusinessService;
    @Resource
    StateManager stateManager;
    @Resource
    OrderFeeManager orderFeeManager;
    @Resource
    OrderManager orderManager;
    @Resource
    RecipeRefundService recipeRefundService;
    @Resource
    DrugSyncToEsService drugSyncToEsService;
    @Resource
    DrugsEnterpriseGmAtop drugsEnterpriseGmAtop;
    @Resource
    ConsultClient consultClient;
    @Resource
    DrugOpenAtop drugOpenAtop;
//    @Autowired
//    protected IRecipeToTestService recipeToTestService;
    @Autowired
    protected RecipeOrderRefundGmAtop recipeOrderRefundGmAtop;
    @Autowired
    DrugPatientAtop drugPatientAtop;



    @Test
    public void testRetry() {
        PayNotifyResTO payNotifyResTO = recipeSettleRetryService.doRecipeSettle(new MedicalSettleService(), new PayNotifyReqTO());
        System.out.println(JSONUtils.toString(payNotifyResTO));
    }

    @Test
    public void testRecipe() {
//        PatientContinueRecipeCheckDrugReq drugPatient = new PatientContinueRecipeCheckDrugReq();
//        drugPatient.setOrganId(1000408);
//        List<PatientOptionalDrugVo> drug = new ArrayList<>();
//        PatientOptionalDrugVo patientOptionalDrugVo = new PatientOptionalDrugVo();
//        patientOptionalDrugVo.setDrugName("看技术框架");
//        patientOptionalDrugVo.setOrganDrugCode("31421");
//        patientOptionalDrugVo.setPatientDrugNum(2);
//        drug.add(patientOptionalDrugVo);
//        drugPatient.setPatientOptionalDrugVo(drug);
//        PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrugRes = drugPatientAtop.patientContinueRecipeCheckDrug(drugPatient);
//        System.out.println(patientContinueRecipeCheckDrugRes);
//        RecipeRefundVO applyRefund = recipeOrderRefundGmAtop.findApplyRefund(225715);
//        System.out.println(applyRefund);
//        DrugInfoRequestTO re = new DrugInfoRequestTO();
//        re.setDbType("7");
//        re.setType("1");
//        re.setOrganId(1);
//        List<DrugInfoTO> data = new ArrayList<>();
//        DrugInfoTO drugInfoTO = new DrugInfoTO();
//        drugInfoTO.setDrcode("111111");
//        drugInfoTO.setDrugType(1);
//        data.add(drugInfoTO);
//        re.setData(data);
//        HisResponseTO<List<DrugInfoTO>> hisResponseTO = recipeToTestService.hisDrugRule(re);
//        System.out.println(hisResponseTO);
//        drugOpenAtop.queryRemindRecipe();
//        String s = "1".equals("1") ? "M" : "F";
//        System.out.println(s);
//        consultClient.getTargetedDrugTypeRecipeRight(269142);
//        drugsEnterpriseBusinessService.rePushRecipeToDrugsEnterprise();
//        List<String> codes = new ArrayList<>();
//        for (int i = 0; i < 106; i++) {
//            codes.add("2257688ngari999");
//        }
//        recipeHisService.recipeListQuery(codes,1);

       //{"operMpiId":"2c9095d0774829160177be48d50b0008","addressId":"","payMode":"1","decoctionId":"","gfFeeFlag":"false","depId":"305","recipeId":"2257531","depName":"滨
        // 康大药房","hisDepFee":"0"}
//        Map<String, String> extInfo = new HashMap<>();
//        extInfo.put("operMpiId","2c9095d0774829160177be48d50b0008");
//        extInfo.put("payMode","1");
//        extInfo.put("decoctionId","");
//        extInfo.put("gfFeeFlag","false");
//        extInfo.put("depId","305");
//        extInfo.put("recipeId","2257531");
//        extInfo.put("depName","滨康大药房");
//        extInfo.put("hisDepFee","0");
//        recipeBusPayInfoService.obtainConfirmOrder("6",-1,extInfo);

//        EnterpriseDecoctionAddressReq enterprise = new EnterpriseDecoctionAddressReq();
//        enterprise.setDecoctionId(123);
//        enterprise.setEnterpriseId(321);
//        enterprise.setOrganId(1);
//        List<EnterpriseDecoctionAddressDTO> list = new ArrayList<>();
//        EnterpriseDecoctionAddressDTO enterpriseDecoctionAddressDTO = new EnterpriseDecoctionAddressDTO();
//        enterpriseDecoctionAddressDTO.setAddress("124334");
//        enterpriseDecoctionAddressDTO.setDecoctionId(123);
//        enterpriseDecoctionAddressDTO.setEnterpriseId(321);
//        enterpriseDecoctionAddressDTO.setOrganId(1);
//        enterpriseDecoctionAddressDTO.setStatus(1);
//        list.add(enterpriseDecoctionAddressDTO);
//        enterprise.setEnterpriseDecoctionAddressDTOS(list);
//        drugsEnterpriseGmAtop.addEnterpriseDecoctionAddressList(enterprise);
//        List<EnterpriseDecoctionAddressDTO> enterpriseDecoctionAddressList = drugsEnterpriseGmAtop.findEnterpriseDecoctionAddressList(enterprise);
//        System.out.println(enterprise);
//        List<OrganAndDrugsepRelationBean> organAndDrugsepRelationBean = drugsEnterpriseGmAtop.findOrganAndDrugsepRelationBean(257);
//        System.out.println(organAndDrugsepRelationBean);
//        List<EnterpriseDecoctionList> enterpriseDecoctionList = drugsEnterpriseGmAtop.findEnterpriseDecoctionList(101, 1);
//        System.out.println(enterprise);
//        drugSyncToEsService.syncOrganDrugList();
//        recipeRefundService.checkForRecipeRefund(134817,"1",null);
//        Map<String, String> map = new HashMap<>();
//        map.put("depId","3061");
//        map.put("addressId","386");
//        map.put("recipeId","136136");
//        map.put("logisticsCompany","1");
//        map.put("payMode","1");
//        orderManager.orderCanSend(map);
//        Recipe recipe = recipeDAO.get(136136);
//        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
//        orderFeeManager.setRecipePaymentFee(order,Lists.newArrayList(recipe));
//        stateManager.updateOrderState(32973, OrderStateEnum.PROCESS_STATE_CANCELLATION,OrderStateEnum.SUB_CANCELLATION_USER);
//        stockBusinessService.enterpriseStock(2257216);
//        buttonManager.enterpriseStockCheck(1);
//        ArrayList<Integer> objects = new ArrayList<>();
//        objects.add(226044);
//        recipePreSettleService.unifyRecipePreSettle(objects);

//        Recipe recipe = recipeDAO.get(226059);
//        List<Recipe> list = new ArrayList<>();
//        list.add(recipe);
//        recipeManager.getRecipeDetailSalePrice(226059,null);
        // 到院取药是否采用药企管理模式

//            List<OrganAndDrugsepRelation> relationByOrganIdAndGiveMode = organAndDrugsepRelationDAO.getRelationByOrganIdAndGiveMode(2, RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
//        System.out.println(JSONArray.toJSONString(relationByOrganIdAndGiveMode));
//        DrugQueryVO d = new DrugQueryVO();
//
//        Recipe recipe1 = recipeDAO.get(136538);
//        d.setOrganId(recipe1.getClinicOrgan());
//        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
//        List<Recipedetail> byRecipeId = detailDAO.findByRecipeId(136538);
//        d.setDrugIds(byRecipeId.stream().map(Recipedetail::getDrugId).collect(Collectors.toList()));
//        List<RecipeDetailBean> collect = byRecipeId.stream().map(recipedetail -> {
//            RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
//            BeanUtils.copy(recipedetail, recipeDetailBean);
//            return recipeDetailBean;
//        }).collect(Collectors.toList());
//        d.setRecipeDetails(collect);
//        stockBusinessService.drugForGiveMode(d);
//        remoteDrugEnterpriseService.pushSingleRecipeInfo(136279);
//        RecipeExtend e = new RecipeExtend();
//        e.setRecipeId(1111111);
//        recipeExtendDAO.save(e);

//        Boolean mergeRecipeFlag = configurationClient.getValueBooleanCatch(1, "mergeRecipeFlag", false);
//        String text = "[{\"chemistSignFile\":\"617fcec72b8e016ead7444d0\",\"clinicOrgan\":1004468,\"depart\":26470,\"doctorName\":\"小申\",\"fromFlag\":1,\"giveMode\":2,\"mpiid\":\"2c9095eb7cc0ec62017cc5ee5d43001c\",\"organDiseaseName\":\"病毒性咽喉痛\",\"organName\":\"郑州人民医院\",\"patientName\":\"申璐瑶\",\"recipeCode\":\"57269ngari999\",\"recipeId\":57269,\"recipeMode\":\"ngarihealth\",\"recipeType\":1,\"signDate\":1635765260000,\"signFile\":\"617fcc112b8e016ead7444a6\",\"status\":2},{\"chemistSignFile\":\"617fca4b2b8e016ead744482\",\"clinicOrgan\":1004468,\"depart\":26470,\"doctorName\":\"小申\",\"fromFlag\":1,\"giveMode\":2,\"mpiid\":\"2c9095eb7cc0ec62017cc5ee5d43001c\",\"organDiseaseName\":\"病毒性咽喉痛\",\"organName\":\"郑州人民医院\",\"patientName\":\"申璐瑶\",\"recipeCode\":\"57267ngari999\",\"recipeId\":57267,\"recipeMode\":\"ngarihealth\",\"recipeType\":1,\"signDate\":1635764130000,\"signFile\":\"617fc7a82b8e016ead744464\",\"status\":2},{\"chemistSignFile\":\"617fca5b2b8e016ead744484\",\"clinicOrgan\":1004468,\"depart\":26470,\"doctorName\":\"小申\",\"fromFlag\":1,\"giveMode\":2,\"mpiid\":\"2c9095eb7cc0ec62017cc5ee5d43001c\",\"organDiseaseName\":\"病毒性咽喉痛\",\"organName\":\"郑州人民医院\",\"patientName\":\"申璐瑶\",\"recipeCode\":\"57266ngari999\",\"recipeId\":57266,\"recipeMode\":\"ngarihealth\",\"recipeType\":1,\"signDate\":1635764041000,\"signFile\":\"617fc74e2b8e016ead744461\",\"status\":2}]";
//        List<RecipeListBean> recipeListBeans = JSONArray.parseArray(text, RecipeListBean.class);
//
//        Map<String, List<RecipeListBean>> collect = new HashMap<>();
//        System.out.println(collect);
//        IDrugsEnterpriseService drugsEnterpriseService = RecipeAPI.getService(IDrugsEnterpriseService.class);
//        DrugsEnterpriseBean drugsEnterpriseBean = drugsEnterpriseService.get(90);
//        System.out.println(drugsEnterpriseBean);
//        Recipe recipe = recipeDAO.get(225401);
//        RecipeBean recipeBean = new RecipeBean();
//        BeanCopyUtils.copy(recipe, recipeBean);
//        Map<String, Object> stringObjectMap = drugStockBusinessService.doSignRecipeCheckAndGetGiveMode(recipeBean);
//        Map<String, Object> stringObjectMap1 = recipeService.doSignRecipeCheck(recipeBean);
//        System.out.println(stringObjectMap);
//        List<Integer> organs = new ArrayList<>();
//        List<Integer> details = new ArrayList<>();
//        organs.add(1);
//        details.add(1);
//        List<RecipeBean> recipeBeans = remoteRecipeService.queryRecipeInfoByOrganAndRecipeType(organs, details);
//        System.out.println(recipeBeans);
//        RecipeResultBean recipeResultBean = recipeHisService.scanDrugStockByRecipeId(225491);
//        recipeHisService.recipeDrugTake(225453,1,null);
//        Integer payButton = buttonManager.getPayButton(1, "2", false);
//        System.out.println(payButton);
//        List<PatientDrugWithEsDTO> drugWithEsByPatient = drugClient.findDrugWithEsByPatient("阿莫", "1", Arrays.asList("1","2"), 0, 10);
//
//        RecipeOrderBean recipeorder = new RecipeOrderBean();
//        recipeOrderDAO.get(136372);
//        BeanCopyUtils.copy(orderIdByRecipe,recipeorder);
//        Patient p = new Patient();
//        recipeBusPayInfoService.newWnExtBusCdrRecipe(recipeorder,p);
//        Order order = new Order();
//        order.setOutTradeNo("rcp_1000423_162544297369409");
//        order.setRefundNo("rcp_1001794_1235675433111111");
//        Map<String, String> map = new HashMap<>();
//        IRecipePayCallBackService recipePayCallBackService = AppContextHolder.getBean("eh.recipePayInfoCallBackService", IRecipePayCallBackService.class);
//        recipePayCallBackService.doHandleAfterRefund(order,3,map);
//        recipeBusPayInfoService.getRecipeAuditSimpleBusObject(32827);
//
//        BigDecimal needFee = new BigDecimal(0.00);
//        needFee = new BigDecimal(11.26) .subtract(new BigDecimal(0.00));
//        List<RecipeOrderPayFlow> byOrderId = recipeOrderPayFlowManager.findByOrderId(32806);
//        if(CollectionUtils.isNotEmpty(byOrderId)){
//            Double otherFee = 0d;
//            for (RecipeOrderPayFlow recipeOrderPayFlow : byOrderId) {
//                otherFee = otherFee + recipeOrderPayFlow.getTotalFee();
//            }
//            needFee = needFee.subtract(BigDecimal.valueOf(otherFee));
//        }
//        double v1 = needFee.doubleValue();
//        double v = needFee.stripTrailingZeros().doubleValue();

//        List<RecipeOrderPayFlow> list = new ArrayList<>();
//        RecipeOrderPayFlow recipeOrderPayFlow = new RecipeOrderPayFlow();
//        recipeOrderPayFlow.setWxPayWay("q");
//        list.add(recipeOrderPayFlow);
//        System.out.println(JSONUtils.toString(list));
//        recipeOrderPayFlow.setWnPayWay("------kjsjkhf");
//        System.out.println(JSONUtils.toString(list));
//        Recipe recipe = recipeDAO.get(225804);
//        List<Integer> list = Arrays.asList(1);
//        List<DrugsEnterprise> enterpriseByOnLine = enterpriseManager.findEnterpriseByTFDS(recipe, list);
//        System.out.println(enterpriseByOnLine);
//        Recipedetail recipedetail = new Recipedetail();
//        recipedetail.setDrugId(2);
//        recipedetail.setDrugName("hjhhhjh");
//        List<Recipedetail> recipedetails = Arrays.asList(recipedetail);
//        enterpriseManager.checkEnterpriseDrugName(Arrays.asList(90,101),recipedetails);
    }

    @Test
    public void testMedicationGuideService() {
        HosPatientRecipeDTO dto = new HosPatientRecipeDTO();
        dto.setOrganId("1");
        dto.setReqType(2);
        dto.setClinicNo("123456");
        HosRecipeDTO recipe = new HosRecipeDTO();
        recipe.setDisease("M25.301");
        recipe.setDiseaseName("肩关节不稳");
        recipe.setSignTime("2019-11-13 14:55:13");
        recipe.setDepartCode("123");
        recipe.setDoctorCode("测试医生");
        recipe.setDepartName("全科");
        List<HosRecipeDetailDTO> details = new ArrayList<>();
        HosRecipeDetailDTO hosRecipeDetailDTO = new HosRecipeDetailDTO();
        hosRecipeDetailDTO.setDrugCode("6543");
        hosRecipeDetailDTO.setDrugName("测试药品1");
        hosRecipeDetailDTO.setUsingRate("每日一次");
        hosRecipeDetailDTO.setUsePathWays("口服");
        hosRecipeDetailDTO.setUseDoseUnit("g");
        hosRecipeDetailDTO.setUseDose("2");
        details.add(hosRecipeDetailDTO);
        recipe.setDetailData(details);
        dto.setRecipe(recipe);
        HosPatientDTO patient = new HosPatientDTO();
        patient.setPatientID("123");
        patient.setCertificate("110101199003073896");
        patient.setCertificateType(1);
        patient.setPatientName("测试人员");
        patient.setPatientSex("1");
        dto.setPatient(patient);
        HosRecipeResult hosRecipeResult = hosPrescriptionService.sendMedicationGuideData(dto);
        System.out.println(JSONUtils.toString(hosRecipeResult));

    }
}
