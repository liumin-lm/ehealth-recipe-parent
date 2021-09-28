package recipe.purchase;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.DecoctionWay;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.constant.UpdateSendMsgStatusEnum;
import recipe.dao.DrugDecoctionWayDao;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.drugsenterprise.CommonRemoteService;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yinsheng
 * @date 2019\9\10 0010 13:48
 */
public class CommonOrder {

    private static final Logger LOG = LoggerFactory.getLogger(CommonOrder.class);

    public static void createDefaultOrder(Map<String, String> extInfo, OrderCreateResult result, RecipeOrder order, RecipePayModeSupportBean payModeSupport, List<Recipe> recipeList, Integer calculateFee) {
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        //设置确认订单页购药方式的key
        String giveModeKey = MapValueUtil.getString(extInfo, "giveModeKey");
        order.setGiveModeKey(giveModeKey);
        order.setGiveModeText(getGiveModeText(recipeList.get(0).getClinicOrgan(), giveModeKey));
        if (null == calculateFee || Integer.valueOf(1).equals(calculateFee)) {
            orderService.setOrderFee(result, order, recipeIds, recipeList, payModeSupport, extInfo, 1);
        } else {
            //设置默认值
            order.setExpressFee(BigDecimal.ZERO);
            order.setTotalFee(BigDecimal.ZERO);
            order.setRecipeFee(BigDecimal.ZERO);
            order.setCouponFee(BigDecimal.ZERO);
            order.setRegisterFee(BigDecimal.ZERO);
            order.setActualPrice(BigDecimal.ZERO.doubleValue());
        }
    }

    //订单完成更新pdf中的取药标签
    public static void finishGetDrugUpdatePdf(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.get(recipeId);
        //更新pdf
        if(null == recipe || null == recipe.getChemistSignFile()){
            return;
        }
        try {
            String newPfd = CreateRecipePdfUtil.transPdfIdForRecipePdf(recipe.getChemistSignFile());
            if (StringUtils.isNotEmpty(newPfd)){
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("ChemistSignFile",newPfd));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static String getGiveModeText(Integer organId, String key){
        try {
            IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
            GiveModeShowButtonVO giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(organId);
            Map configurations = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
            return (String)configurations.get(key);
        } catch (Exception e) {
            LOG.error("getGiveModeText organId:{}, key:{}.", organId, key);
        }
        return "";
    }

    public static HisResponseTO updateGoodsReceivingInfoToCreateOrder(Integer recipeId, Map<String, String> extInfo) {
        LOG.info("CommonOrder updateGoodsReceivingInfoToCreateOrder recipeId:{},extInfo:{}.", recipeId, JSONUtils.toString(extInfo));
        try{
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(recipeId);
            DoctorService doctorService = BasicAPI.getService(DoctorService.class);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patient = patientService.get(recipe.getMpiid());
            if (null == patient){
                throw new DAOException(ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
            }
            //患者信息
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            patientBaseInfo.setCertificateType(patient.getCertificateType());
            patientBaseInfo.setCertificate(patient.getCertificate());
            patientBaseInfo.setPatientName(patient.getPatientName());
            patientBaseInfo.setPatientID(recipe.getPatientID());

            UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
            updateTakeDrugWayReqTO.setPatientBaseInfo(patientBaseInfo);
            updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
            //医院处方号
            updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
            updateTakeDrugWayReqTO.setNgarRecipeId(recipe.getRecipeId().toString());
            //审方药师工号和姓名
            if (null != recipe.getChecker()){
                IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
                EmploymentBean primaryEmp = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
                if (null != primaryEmp){
                    updateTakeDrugWayReqTO.setCheckerId(primaryEmp.getJobNumber());
                }
                DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
                if (null != doctorDTO){
                    updateTakeDrugWayReqTO.setCheckerName(doctorDTO.getName());
                }
            }
            //处方总金额
            updateTakeDrugWayReqTO.setPayment(recipe.getActualPrice());
            //支付状态
            updateTakeDrugWayReqTO.setPayFlag(recipe.getPayFlag());
            String payMode = MapValueUtil.getString(extInfo, "payMode");
            //支付方式
            updateTakeDrugWayReqTO.setPayMode(payMode);
            RecipeOrder order = createOrderBySendMap(extInfo);
            LOG.info("组装的order信息：{}", JSONUtils.toString(order));
            if (null != order){
                //收货人
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                //联系电话
                updateTakeDrugWayReqTO.setContactTel(order.getRecMobile());
                //收货地址
                //date 20200319
                //修改推送的地址细节：address ：address4,receiveAddress:集合，receiveAddrCode：address3
                CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                updateTakeDrugWayReqTO.setAddress(order.getAddress4());
                if(null != order.getStreetAddress()){
                    updateTakeDrugWayReqTO.setReceiveAddrCode(order.getStreetAddress());
                } else {
                    updateTakeDrugWayReqTO.setReceiveAddrCode(order.getAddress3());
                }
                updateTakeDrugWayReqTO.setReceiveAddress(commonRemoteService.getCompleteAddressToSend(order));

                //date 20200314
                //新添字段更新
                if ("1".equals(payMode)) {
                    updateTakeDrugWayReqTO.setDeliveryCode(order.getHisEnterpriseCode());
                    updateTakeDrugWayReqTO.setDeliveryName(order.getHisEnterpriseName());
                } else if ("4".equals(payMode)) {
                    updateTakeDrugWayReqTO.setDeliveryCode(order.getDrugStoreCode());
                    updateTakeDrugWayReqTO.setDeliveryName(order.getDrugStoreName());
                }
                updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                updateTakeDrugWayReqTO.setContactTel(order.getRecTel());
                updateTakeDrugWayReqTO.setPlanDate(StringUtils.isNotEmpty(order.getExpectSendDate())?
                        order.getExpectSendDate() + " 00:00:00" : null);
                updateTakeDrugWayReqTO.setPlanTime(order.getExpectSendTime());

                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
                if(recipeExtend != null && recipeExtend.getDecoctionId() != null){
                    //煎法Code
                    DrugDecoctionWayDao drugDecoctionWayDao=DAOFactory.getDAO(DrugDecoctionWayDao.class);
                    DecoctionWay decoctionWay=drugDecoctionWayDao.get(Integer.parseInt(recipeExtend.getDecoctionId()));
                    updateTakeDrugWayReqTO.setDecoctionCode(decoctionWay.getDecoctionCode());
                    updateTakeDrugWayReqTO.setDecoctionFee(order.getDecoctionFee());
                }
            }else{
                LOG.info("同步配送信息，组装配送订单失败！");
                HisResponseTO hisResponseTO = new HisResponseTO();
                hisResponseTO.setMsgCode("-1");
                return hisResponseTO;
            }
            if (recipe.getClinicId() != null) {
                updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
            }
            //流转到这里来的属于物流配送
            updateTakeDrugWayReqTO.setDeliveryType("1");
            //date 2020-10-15 17:38 修改添加挂号序号
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend nowRecipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            if(null != nowRecipeExtend){
                updateTakeDrugWayReqTO.setRegisterId(nowRecipeExtend.getRegisterID());
            }else{
                LOG.info("当前处方{}没有关联的扩展信息", recipeId);
            }
            //date 20200312
            //将配送信息同步过来
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            updateTakeDrugWayReqTO.setCheckOrgan(null != recipe.getCheckOrgan() ? recipe.getCheckOrgan().toString() : null);
            updateTakeDrugWayReqTO.setCheckDate(recipe.getCheckDateYs());
            updateTakeDrugWayReqTO.setCheckerTel(recipe.getCheckerTel());
            updateTakeDrugWayReqTO.setCheckMemo(recipe.getCheckFailMemo());
            updateTakeDrugWayReqTO.setSupplementaryMemo(recipe.getSupplementaryMemo());
            //设置当前更新
            updateTakeDrugWayReqTO.setGiveMode(
                    UpdateSendMsgStatusEnum.fromGiveType(null == extInfo.get("payMode") ? null : Integer.parseInt(extInfo.get("payMode").toString())).getSendType());

            LOG.info("收货信息更新通知his. req={}", JSONUtils.toString(updateTakeDrugWayReqTO));
            HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
            LOG.info("收货信息更新通知his. res={}", JSONUtils.toString(hisResult));
            return hisResult;
        }catch (Exception e){
            LOG.error("updateGoodsReceivingInfo. error", e);
            HisResponseTO hisResponseTO = new HisResponseTO();
            hisResponseTO.setMsgCode("-1");
            return hisResponseTO;
        }

    }
    //将map中用来生成平台订单的信息组装成订单配送信息
    private static RecipeOrder createOrderBySendMap(Map<String, String> extInfo) {
        LOG.info("createOrderBySendMap. extInfo=[{}]", JSONUtils.toString(extInfo));
        RecipeOrder recipeOrder = new RecipeOrder();
        recipeOrder.setExpectSendDate(MapValueUtil.getString(extInfo, "expectSendDate"));
        recipeOrder.setExpectSendTime(MapValueUtil.getString(extInfo, "expectSendTime"));
        //判断传入的是his的药企code还是平台维护的药企id
        if(StringUtils.isNotEmpty(extInfo.get("hisDepCode"))){
            recipeOrder.setHisEnterpriseCode(MapValueUtil.getString(extInfo, "hisDepCode"));
        }else{
            recipeOrder.setHisEnterpriseCode(MapValueUtil.getString(extInfo, "depId"));
        }

        recipeOrder.setHisEnterpriseName(MapValueUtil.getString(extInfo, "depName"));
        String operMpiId = MapValueUtil.getString(extInfo, "operMpiId");
        String operAddressId = MapValueUtil.getString(extInfo, "addressId");
        AddressDTO address = null;
        AddressService addressService = ApplicationUtils.getBasicService(AddressService.class);
        if (StringUtils.isNotEmpty(operAddressId)) {
            address = addressService.get(Integer.parseInt(operAddressId));
        } else {
            address = addressService.getLastAddressByMpiId(operMpiId);
        }
        if(null != address){
            recipeOrder.setAddress1(address.getAddress1());
            recipeOrder.setAddress2(address.getAddress2());
            recipeOrder.setAddress3(address.getAddress3());
            recipeOrder.setAddress4(address.getAddress4());
            recipeOrder.setStreetAddress(address.getStreetAddress());
        }else{
            LOG.warn("当前确认订单推送没有设置配送地址");
            return null;
        }
        recipeOrder.setReceiver(address.getReceiver());
        recipeOrder.setRecMobile(address.getRecMobile());
        recipeOrder.setRecTel(address.getRecMobile());
        recipeOrder.setDrugStoreCode(MapValueUtil.getString(extInfo,"drugStoreCode"));
        recipeOrder.setDrugStoreName(MapValueUtil.getString(extInfo, "drugStoreName"));
        return recipeOrder;
    }
}
