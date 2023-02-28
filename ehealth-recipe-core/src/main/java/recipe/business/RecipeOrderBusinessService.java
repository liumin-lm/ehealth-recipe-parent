package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.common.dto.CheckRequestCommonOrderItemDTO;
import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.RecipeCashPreSettleInfo;
import com.ngari.his.recipe.mode.RecipeCashPreSettleReqTO;
import com.ngari.infra.invoice.mode.InvoiceRecordDto;
import com.ngari.infra.invoice.service.InvoiceRecordService;
import com.ngari.infra.logistics.service.IWaybillService;
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.InvoiceInfoResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.vo.LogisticsMergeVO;
import com.ngari.recipe.vo.PreOrderInfoReqVO;
import com.ngari.recipe.vo.ShoppingCartReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.account.UserRoleToken;
import ctd.account.thirdparty.ThirdPartyMappingController;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import easypay.entity.vo.param.bus.HlwTbParamReq;
import easypay.entity.vo.param.bus.MedicalPreSettleQueryReq;
import easypay.entity.vo.param.bus.SelfPreSettleQueryReq;
import eh.cdr.constant.RecipeConstant;
import eh.entity.bus.pay.BusTypeEnum;
import eh.utils.BeanCopyUtils;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.*;
import recipe.common.CommonConstant;
import recipe.constant.*;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.*;
import recipe.enumerate.type.*;
import recipe.factory.status.givemodefactory.GiveModeProxy;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.manager.*;
import recipe.presettle.IRecipePreSettleService;
import recipe.presettle.factory.PreSettleFactory;
import recipe.purchase.CommonOrder;
import recipe.purchase.PurchaseService;
import recipe.service.*;
import recipe.serviceprovider.recipeorder.service.RemoteRecipeOrderService;
import recipe.third.IFileDownloadService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.*;
import recipe.vo.ResultBean;
import recipe.vo.base.BaseRecipeDetailVO;
import recipe.vo.greenroom.ImperfectInfoVO;
import recipe.vo.greenroom.InvoiceRecordVO;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;
import recipe.vo.greenroom.RefundResultNotifyVO;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.CheckOrderAddressVo;
import recipe.vo.second.OrderPharmacyVO;
import recipe.vo.second.enterpriseOrder.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 处方订单处理实现类 （新增）
 *
 * @author fuzi
 */
@Service
public class RecipeOrderBusinessService extends BaseService implements IRecipeOrderBusinessService {
    private final static long VALID_TIME_SECOND = 3600 * 24 * 30;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private ConfigStatusCheckDAO configStatusCheckDAO;
    @Autowired
    private GiveModeProxy giveModeProxy;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private InfraClient infraClient;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private OrganClient organClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private InvoiceRecordService invoiceRecordService;
    @Autowired
    private RecipeOrderService orderService;
    @Autowired
    private SmsClient smsClient;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeBeforeOrderDAO recipeBeforeOrderDAO;
    @Autowired
    private AddressService addressService;
    @Autowired
    private PharmacyDAO pharmacyDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;
    @Autowired
    private OrderFeeManager orderFeeManager;
    @Autowired
    private HisRecipeManager hisRecipeManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private PharmacyManager pharmacyManager;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private BeforeOrderManager beforeOrderManager;
    @Autowired
    private HisRecipeDAO hisRecipeDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private RemoteRecipeOrderService recipeOrderService;
    @Autowired
    private PayClient payClient;
    @Autowired
    private RecipeLogDAO recipeLogDAO;
    @Autowired
    private RecipeHisService recipeHisService;
    @Autowired
    private RecipeOrderPayFlowManager recipeOrderPayFlowManager;
    @Autowired
    private RecipeRefundDAO recipeRefundDAO;


    @Override
    public ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser) {
        ResultBean result = ResultBean.serviceError("参数错误");
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return result;
        }
        recipe.setGiveUser(giveUser.toString());
        if (StringUtils.isEmpty(recipe.getOrderCode())) {
            return result;
        }
        try {
            //更新订单表字段 兼容老版本
            ApothecaryDTO apothecaryDTO = doctorClient.getGiveUser(recipe);
            recipeOrderDAO.updateApothecaryByOrderId(recipe.getOrderCode(), apothecaryDTO.getGiveUserName(), apothecaryDTO.getGiveUserIdCardCleartext());
            logger.info("RecipeOrderTwoService updateRecipeGiveUser OrderCode{}, apothecaryVO:{} ", recipe.getOrderCode(), JSONUtils.toString(apothecaryDTO));
        } catch (Exception e) {
            logger.error("RecipeOrderTwoService updateRecipeGiveUser ", e);
        }

        //更新pdf
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setGiveUser(recipe.getGiveUser());
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        createPdfFactory.updateGiveUser(recipe);
        return ResultBean.succeed();
    }


    @Override
    public ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO orderStatus) {
        logger.info("RecipeOrderTwoService updateRecipeOrderStatus orderStatus = {}", JSON.toJSONString(orderStatus));
        ResultBean result = ResultBean.serviceError("参数错误");
        Recipe recipe = recipeDAO.getByRecipeId(orderStatus.getRecipeId());
        if (null == recipe || StringUtils.isEmpty(recipe.getOrderCode())) {
            return result;
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        //校验订单状态可否流转
        List<ConfigStatusCheck> statusList = configStatusCheckDAO.findByLocationAndSource(recipe.getGiveMode(), recipeOrder.getStatus());
        boolean status = statusList.stream().anyMatch(a -> a.getTarget().equals(orderStatus.getTargetRecipeOrderStatus()));
        result = ResultBean.succeed();
        if (!status) {
            updateOrderStatus(orderStatus);
            return result;
        }
        //工厂代理处理 按照购药方式 修改订单信息
        orderStatus.setSourceRecipeOrderStatus(recipeOrder.getStatus());
        orderStatus.setOrderId(recipeOrder.getOrderId());
        orderStatus.setSourceRecipeStatus(recipe.getStatus());
        orderStatus.setOrganName(recipe.getOrganName());
        giveModeProxy.updateOrderByGiveMode(recipe.getGiveMode(), orderStatus);
        if (NeedSendTypeEnum.NO_NEED_SEND_TYPE.getType().equals(orderStatus.getNeedSendType())) {
            orderStatus.setTargetRecipeOrderStatus(RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType());
            giveModeProxy.updateOrderByGiveMode(recipe.getGiveMode(), orderStatus);
        }
        logger.info("RecipeOrderTwoService updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
        return result;
    }


    /**
     * todo 方法需要优化 原方法需要删除
     *
     * @param skipThirdReqVO
     */
    @Override
    public SkipThirdDTO uploadRecipeInfoToThird(SkipThirdReqVO skipThirdReqVO) {
        return enterpriseManager.uploadRecipeInfoToThird(skipThirdReqVO.getOrganId(), skipThirdReqVO.getGiveMode(), skipThirdReqVO.getRecipeIds(), skipThirdReqVO.getEncData());
    }

    /**
     * 获取第三方跳转链接
     * TODO 七月大版本将会去掉bqEnterprise标志,会对此处代码进行重构,由于涉及改动较大,本次小版本不做处理
     *
     * @param skipThirdReqVO
     * @return
     */
    @Override
    public SkipThirdDTO getSkipUrl(SkipThirdReqVO skipThirdReqVO) {
        return orderManager.getThirdUrl(skipThirdReqVO.getRecipeIds().get(0), GiveModeTextEnum.getGiveMode(skipThirdReqVO.getGiveMode()));
    }

    @Override
    public List<RecipeFeeDTO> findRecipeOrderDetailFee(String orderCode) {
        return orderManager.findRecipeOrderDetailFee(orderCode);
    }

    @Override
    public RecipeOrderDto getRecipeOrderByBusId(Integer orderId) {
        return orderManager.getRecipeOrderByBusId(orderId);
    }

    @Override
    public CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request) {
        logger.info("getRecipePageForCommonOrder param ={}", JSON.toJSONString(request));
        CheckRequestCommonOrderPageDTO pageDTO = new CheckRequestCommonOrderPageDTO();
        if (request.getPage() == null || request.getSize() == null) {
            return pageDTO;
        }
        Integer start = (request.getPage() - 1) * request.getSize();
        Integer limit = request.getSize();
        QueryResult<RecipeOrder> queryResult = recipeOrderDAO.queryPageForCommonOrder(request.getStartDate(),
                request.getEndDate(), start, limit);
        if (queryResult == null) {
            return pageDTO;
        }
        if (CollectionUtils.isEmpty(queryResult.getItems())) {
            return pageDTO;
        }
        pageDTO.setTotal(Integer.parseInt(String.valueOf(queryResult.getTotal())));
        pageDTO.setPage(request.getPage());
        pageDTO.setSize(request.getSize());
        List<CheckRequestCommonOrderItemDTO> order = new ArrayList<>();
        PatientService patientService = BasicAPI.getService(PatientService.class);
        for (RecipeOrder recipeOrder : queryResult.getItems()) {
            CheckRequestCommonOrderItemDTO orderItem = new CheckRequestCommonOrderItemDTO();
            String userId = patientService.getLoginIdByMpiId(recipeOrder.getMpiId());
            orderItem.setUserId(userId);
            orderItem.setMpiId(recipeOrder.getMpiId());
            orderItem.setBusType(BusTypeEnum.RECIPE.getCode());
            orderItem.setBusId(recipeOrder.getOrderId());
            orderItem.setBusStatus(recipeOrder.getStatus());
            orderItem.setBusDate(recipeOrder.getCreateTime());
            orderItem.setCreateDate(recipeOrder.getCreateTime());
            orderItem.setLastModify(recipeOrder.getLastModifyTime());
            orderItem.setDbData(recipeOrder);
            order.add(orderItem);
        }
        pageDTO.setOrder(order);
        logger.info("getRecipePageForCommonOrder result ={}", JSON.toJSONString(pageDTO));
        return pageDTO;
    }

    /**
     * 患者提交订单时更新pdf
     *
     * @param recipeId
     */
    @Override
    public void updatePdfForSubmitOrderAfter(Integer recipeId) {
        createPdfFactory.updateCodePdfExecute(recipeId);
    }

    /**
     * 根据订单号更新物流单号
     *
     * @param orderCode      订单号
     * @param trackingNumber 物流单号
     * @return 是否成功
     */
    @Override
    public Boolean updateTrackingNumberByOrderCode(String orderCode, String trackingNumber) {
        recipeOrderDAO.updateTrackingNumberByOrderCode(orderCode, trackingNumber);
        return true;
    }

    /**
     * 第三方查询平台处方订单信息
     *
     * @param downOrderRequestVO 请求入参
     * @return 处方订单列表
     */
    @Override
    public EnterpriseDownDataVO findOrderAndRecipes(DownOrderRequestVO downOrderRequestVO) {
        EnterpriseDownDataVO result = new EnterpriseDownDataVO();
        List<DownRecipeOrderVO> downRecipeOrderVOList = new ArrayList<>();
        result.setCode(200);
        //根据appKey查询药企，查询不到给出提示
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByAppKey(downOrderRequestVO.getAppKey());
        List<Integer> enterpriseIdList = drugsEnterpriseList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
        logger.info("findOrderAndRecipes enterpriseIdList:{}.", JSON.toJSONString(enterpriseIdList));
        if (CollectionUtils.isEmpty(enterpriseIdList)) {
            result.setCode(-1);
            result.setMsg("根据appKey查询不到药企信息");
            logger.warn("findOrderAndRecipes 根据appKey查询不到药企信息 enterpriseIdList:{}.", JSON.toJSONString(enterpriseIdList));
            return result;
        }

        //获取患者信息
        List<String> mpiIdList = new ArrayList<>();
        List<PatientDTO> patientList = new ArrayList<>();
        if (StringUtils.isNotEmpty(downOrderRequestVO.getIdCard())) {
            com.ngari.recipe.dto.PatientDTO patientDTO = new com.ngari.recipe.dto.PatientDTO();
            patientDTO.setIdcard(downOrderRequestVO.getIdCard());
            patientList = patientClient.patientByIdCard(patientDTO);
            logger.info("findOrderAndRecipes patientList:{}.", JSON.toJSONString(patientList));
            if (CollectionUtils.isEmpty(patientList)) {
                result.setCode(-1);
                result.setMsg("根据当前患者身份证信息没有获取到患者信息,请检查后重新获取");
                logger.warn("findOrderAndRecipes 根据当前患者身份证信息没有获取到患者信息,请检查后重新获取");
                return result;
            }
            mpiIdList = patientList.stream().map(PatientDTO::getMpiId).collect(Collectors.toList());
        }
        if (null != downOrderRequestVO.getOrganId() && StringUtils.isNotEmpty(downOrderRequestVO.getRecipeCode())) {
            Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(downOrderRequestVO.getRecipeCode(), downOrderRequestVO.getOrganId());
            if (null == recipe) {
                result.setCode(-1);
                result.setMsg("根据当前患者信息没有获取到处方信息,请检查后重新获取");
                return result;
            }
            mpiIdList.add(recipe.getMpiid());
        }
        logger.info("findOrderAndRecipes mpiIdList:{}", JSON.toJSONString(mpiIdList));
        Date beginDate = DateConversion.parseDate(downOrderRequestVO.getBeginTime(), DateConversion.DEFAULT_DATE_TIME);
        Date endDate = DateConversion.parseDate(downOrderRequestVO.getEndTime(), DateConversion.DEFAULT_DATE_TIME);
        List<DownLoadRecipeOrderDTO> downLoadRecipeOrderDTOList = orderManager.findOrderAndRecipes(enterpriseIdList, mpiIdList, beginDate, endDate);
        downLoadRecipeOrderDTOList.forEach(downLoadRecipeOrderDTO -> {
            DownRecipeOrderVO downRecipeOrderVO = new DownRecipeOrderVO();
            //订单信息
            DownOrderVO downOrderVO = new DownOrderVO();
            //收件人信息
            ReceiverInfoVO receiverInfo = new ReceiverInfoVO();
            //处方信息
            List<DownRecipeVO> downRecipeVOList = new ArrayList<>();

            RecipeOrder recipeOrder = downLoadRecipeOrderDTO.getRecipeOrder();
            List<Recipe> recipeList = downLoadRecipeOrderDTO.getRecipeList();
            List<RecipeExtend> recipeExtendList = downLoadRecipeOrderDTO.getRecipeExtendList();
            Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a -> a, (k1, k2) -> k1));
            List<Recipedetail> recipeDetailList = downLoadRecipeOrderDTO.getRecipeDetailList();
            Map<Integer, List<Recipedetail>> recipeDetailListMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));

            List<SaleDrugList> saleDrugLists = downLoadRecipeOrderDTO.getSaleDrugLists();
            Map<Integer, SaleDrugList> saleDrugListMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, a -> a, (k1, k2) -> k1));
            ObjectCopyUtils.copyProperties(downOrderVO, recipeOrder);
            downRecipeOrderVO.setOrder(downOrderVO);
            ObjectCopyUtils.copyProperties(receiverInfo, recipeOrder);
            //设置省市区街道相关配送信息
            String province = LocalStringUtil.getAddressDic(recipeOrder.getAddress1());
            String city = LocalStringUtil.getAddressDic(recipeOrder.getAddress2());
            String district = LocalStringUtil.getAddressDic(recipeOrder.getAddress3());
            String street = LocalStringUtil.getAddressDic(recipeOrder.getStreetAddress());
            receiverInfo.setProvince(province);
            receiverInfo.setCity(city);
            receiverInfo.setDistrict(district);
            receiverInfo.setStreetCode(recipeOrder.getStreetAddress());
            receiverInfo.setStreet(street);
            receiverInfo.setTrackingNumber(recipeOrder.getTrackingNumber());
            if (!ValidateUtil.integerIsEmpty(recipeOrder.getLogisticsCompany())) {
                receiverInfo.setLogisticsCompany(recipeOrder.getLogisticsCompany());
                String company = DictionaryUtil.getDictionary("eh.infra.dictionary.LogisticsCode", recipeOrder.getLogisticsCompany());
                receiverInfo.setLogisticsCompanyName(company);
            }
            receiverInfo.setAddress(recipeOrder.getAddress4());
            if (StringUtils.isNotEmpty(recipeOrder.getAddress1())) {
                receiverInfo.setProvinceCode(StringUtils.isNotEmpty(recipeOrder.getAddress1()) ? recipeOrder.getAddress1() + "0000" : "");
                receiverInfo.setCityCode(StringUtils.isNotEmpty(recipeOrder.getAddress2()) ? recipeOrder.getAddress2() + "00" : "");
                receiverInfo.setDistrictCode(recipeOrder.getAddress3());
            }
            receiverInfo.setCommunityCode(ValidateUtil.isEmpty(recipeOrder.getAddress5()));
            receiverInfo.setCommunityName(ValidateUtil.isEmpty(recipeOrder.getAddress5Text()));
            downRecipeOrderVO.setReceiverInfo(receiverInfo);
            if (null != recipeOrder.getInvoiceRecordId()) {
                InvoiceRecordDto invoiceRecordDto = invoiceRecordService.findInvoiceRecordInfo(recipeOrder.getInvoiceRecordId());
                InvoiceRecordVO invoiceRecordVO = new InvoiceRecordVO();
                ObjectCopyUtils.copyProperties(invoiceRecordVO, invoiceRecordDto);
                downRecipeOrderVO.setInvoiceRecord(invoiceRecordVO);
            }
            //设置处方信息
            recipeList.forEach(recipe -> {
                DownRecipeVO downRecipeVO = new DownRecipeVO();
                List<BaseRecipeDetailVO> baseRecipeDetailVOList = new ArrayList<>();
                ObjectCopyUtils.copyProperties(downRecipeVO, recipe);
                downRecipeVO.setOrganId(recipe.getClinicOrgan());
                OrganDTO organDTO = organClient.organDTO(recipe.getClinicOrgan());
                if (null != organDTO) {
                    downRecipeVO.setOrganizeCode(organDTO.getOrganizeCode());
                }
                if (null != recipe.getCheckOrgan()) {
                    downRecipeVO.setCheckerName(recipe.getCheckerText());
                    com.ngari.recipe.dto.OrganDTO checkOrgan= organClient.organDTO(recipe.getCheckOrgan());
                    downRecipeVO.setCheckOrganName(checkOrgan.getName());
                }
                //设置签名文件的url
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    downRecipeVO.setSignFileUrl(recipeManager.getRecipeSignFileUrl(recipe.getChemistSignFile(), VALID_TIME_SECOND));
                } else {
                    downRecipeVO.setSignFileUrl(recipeManager.getRecipeSignFileUrl(recipe.getSignFile(), VALID_TIME_SECOND));
                }
                //设置签名图片的url
                if (StringUtils.isNotEmpty(recipe.getSignImg())) {
                    downRecipeVO.setRecipeSignImgUrl(recipeManager.getRecipeSignFileUrl(recipe.getSignImg(), VALID_TIME_SECOND));
                }
                //设置base64位图片
                String ossId = recipe.getSignImg();
                if (StringUtils.isNotEmpty(ossId)) {
                    try {
                        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                        String imgStr = "data:image/jpeg;base64," + fileDownloadService.downloadImg(ossId);
                        downRecipeVO.setRecipeSignImg(imgStr);
                    } catch (Exception e) {
                        logger.error("findOrderAndRecipes recipeId{}处方，下载处方笺服务异常：{}.", recipe.getRecipeId(), e.getMessage(), e);
                    }
                }
                if (RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())) {
                    HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                    if (Objects.nonNull(hisRecipe) && Objects.nonNull(hisRecipe.getSendType())) {
                        downRecipeVO.setSendType(hisRecipe.getSendType());
                    }
                }
                //设置订单的购药方式
                downOrderVO.setGiveMode(recipe.getGiveMode());
                //处方患者信息
                PatientDTO patient = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
                logger.info("findOrderAndRecipes patient:{} .", JSONUtils.toString(patient));
                downRecipeVO.setBirthday(patient.getBirthday());
                downRecipeVO.setSexCode("1".equals(patient.getPatientSex()) ? "M" : "F");
                downRecipeVO.setGender(patient.getPatientSex());
                try {
                    downRecipeVO.setSexName(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));
                } catch (ControllerException e) {
                    logger.error("findOrderAndRecipes recipeId:{}, error ", recipe.getRecipeId(), e);
                }
                //设置处方费用
                downRecipeVO.setRecipeFee(null);
                BigDecimal recipeFee = new BigDecimal(BigInteger.ZERO);
                if (null != recipe.getEnterpriseId()) {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipe.getEnterpriseId());
                    if (null != drugsEnterprise && drugsEnterprise.getSettlementMode() == 1) {
                        downRecipeVO.setRecipeFee(recipe.getTotalMoney());
                    }
                }
                RecipeExtend recipeExtend = recipeExtendMap.get(recipe.getRecipeId());
                ObjectCopyUtils.copyProperties(downRecipeVO, recipeExtend);
                List<Recipedetail> recipeDetailListFromMap = recipeDetailListMap.get(recipe.getRecipeId());
                for (Recipedetail recipeDetail : recipeDetailListFromMap) {
                    BaseRecipeDetailVO baseRecipeDetailVO = new BaseRecipeDetailVO();
                    baseRecipeDetailVO.setUnit(recipeDetail.getDrugUnit());
                    ObjectCopyUtils.copyProperties(baseRecipeDetailVO, recipeDetail);
                    SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
                    if (null != saleDrugList) {
                        baseRecipeDetailVO.setSaleDrugCode(saleDrugList.getOrganDrugCode());
                    }
                    if (null != recipeDetail.getActualSalePrice()) {
                        recipeFee = recipeFee.add(recipeDetail.getActualSalePrice().multiply(new BigDecimal(recipeDetail.getUseTotalDose())).setScale(4, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP);
                        baseRecipeDetailVO.setSalePrice(recipeDetail.getActualSalePrice());
                    }

                    Map<String, OrganDrugList> organDrugMap = organDrugListManager.getOrganDrugByIdAndCode(recipe.getClinicOrgan(), Collections.singletonList(recipeDetail.getDrugId()));
                    if (!organDrugMap.isEmpty()) {
                        OrganDrugList organDrug = organDrugMap.get(recipeDetail.getDrugId() + recipeDetail.getOrganDrugCode());
                        if (null != organDrug) {
                            baseRecipeDetailVO.setDrugForm(organDrug.getDrugForm());
                        }
                    }

                    baseRecipeDetailVOList.add(baseRecipeDetailVO);
                }
                if (null == downRecipeVO.getRecipeFee()) {
                    downRecipeVO.setRecipeFee(recipeFee);
                }
                downRecipeVO.setRecipeDetailList(baseRecipeDetailVOList);
                downRecipeVOList.add(downRecipeVO);
            });
            downRecipeOrderVO.setRecipeList(downRecipeVOList);
            downRecipeOrderVOList.add(downRecipeOrderVO);
        });
        result.setRecipeOrderList(downRecipeOrderVOList);
        return result;
    }

    @Override
    public RecipeResultBean cancelOrderByRecipeId(Integer recipeId, Integer status) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (null == recipeId || null == status) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("缺少参数");
            return result;
        }
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        List<Recipe> recipeList = recipeDAO.findRecipeListByOrderCode(recipe.getOrderCode());
        orderManager.cancelOrder(recipeOrder, recipeList, true, 2);
        return result;
    }

    @Override
    public RecipeOrder getTrackingNumber(String recipeCode, Integer organId) {
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(recipeCode, organId);
        if (null == recipe || StringUtils.isEmpty(recipe.getOrderCode())) {
            return null;
        }
        return recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
    }

    /**
     * todo 需要修改成 新模式
     * 不在新增逻辑内的状态流转 走老方法
     *
     * @param orderStatus
     */
    private void updateOrderStatus(UpdateOrderStatusVO orderStatus) {
        RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put("status", orderStatus.getTargetRecipeOrderStatus());
        recipeOrderService.updateOrderStatus(orderStatus.getRecipeId(), attrMap);
    }

    @Override
    public List<ReimbursementDTO> findReimbursementList(ReimbursementListReqVO reimbursementListReq) {
        logger.info("findReimbursementList reimbursementListReq={}", JSONUtils.toString(reimbursementListReq));
        return orderManager.findReimbursementList(ObjectCopyUtils.convert(reimbursementListReq, ReimbursementListReqDTO.class));
    }

    @Override
    public ReimbursementDTO findReimbursementDetail(Integer recipeId) {
        logger.info("findReimbursementDetail recipeId={}", JSONUtils.toString(recipeId));
        return orderManager.findReimbursementDetail(recipeId);
    }

    @Override
    public List<RecipeOrderWaybillDTO> findOrderByMpiId(String mpiId) {
        Date date = DateUtils.addDays(new Date(), -1);
        List<RecipeOrder> orders = recipeOrderDAO.findByMpiIdAndDate(mpiId, date);
        List<RecipeOrderWaybillDTO> recipeOrderWaybillDTOS = orders.stream().map(order -> {
            RecipeOrderWaybillDTO recipeOrderWaybillDTO = BeanCopyUtils.copyProperties(order, RecipeOrderWaybillDTO::new);
            if (StringUtils.isNotEmpty(order.getTrackingNumber())) {
                return recipeOrderWaybillDTO;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return recipeOrderWaybillDTOS;
    }

    @Override
    public void updateTrackingNumberByOrderId(UpdateOrderStatusVO updateOrderStatusVO) {
        recipeOrderDAO.updateTrackingNumberByOrderId(updateOrderStatusVO.getOrderId(), updateOrderStatusVO.getLogisticsCompany(), updateOrderStatusVO.getTrackingNumber());
    }

    @Override
    public Integer thirdCreateOrder(ThirdCreateOrderReqDTO thirdCreateOrderReqDTO) {
        checkOrderParams(thirdCreateOrderReqDTO);
        setUrtToContext(thirdCreateOrderReqDTO.getAppkey(), thirdCreateOrderReqDTO.getTid());
        String mpiId = getOwnMpiId();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(thirdCreateOrderReqDTO.getRecipeIds());

        List<String> orderCodes = recipeList.stream().map(Recipe::getOrderCode).filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(orderCodes)) {
            logger.info("thirdCreateOrder orderCode is not null orderCode = {}", JsonUtil.toString(orderCodes));
            throw new DAOException(609, "处方单已存在订单");
        }
        Recipe recipe = recipeList.get(0);
        RecipeOrder order = new RecipeOrder();
        order.setMpiId(mpiId);
        order.setOrganId(recipe.getClinicOrgan());
        order.setOrderCode(orderService.getOrderCode(order.getMpiId()));
        if (StringUtils.isEmpty(thirdCreateOrderReqDTO.getSendMethod())) {
            order.setStatus(RecipeOrderStatusEnum.ORDER_STATUS_READY_PAY.getType());
        } else {
            if ("1".equals(thirdCreateOrderReqDTO.getSendMethod())) {
                //设置配送到家的待配送状态
                order.setStatus(3);
            } else if ("2".equals(thirdCreateOrderReqDTO.getSendMethod())) {
                //设置到院取药的状态
                order.setStatus(2);
            } else if ("3".equals(thirdCreateOrderReqDTO.getSendMethod())) {
                //设置到店取药的待取药状态
                order.setStatus(12);
            } else {
                //设置到院取药的状态
                order.setStatus(2);
            }
        }
        //设置配送信息
        if (StringUtils.isNotEmpty(thirdCreateOrderReqDTO.getAddressId())) {
            order.setAddressID(Integer.parseInt(thirdCreateOrderReqDTO.getAddressId()));
            AddressService addressService = BasicAPI.getService(AddressService.class);
            AddressDTO addressDTO = addressService.getByAddressId(Integer.parseInt(thirdCreateOrderReqDTO.getAddressId()));
            if (addressDTO != null) {
                order.setAddress1(addressDTO.getAddress1());
                order.setAddress2(addressDTO.getAddress2());
                order.setAddress3(addressDTO.getAddress3());
                order.setAddress4(addressDTO.getAddress4());
                order.setReceiver(addressDTO.getReceiver());
                order.setRecMobile(addressDTO.getRecMobile());
            }
        }
        order.setWxPayWay(thirdCreateOrderReqDTO.getPayway());
        if (StringUtils.isNotEmpty(thirdCreateOrderReqDTO.getDepId())) {
            order.setEnterpriseId(Integer.parseInt(thirdCreateOrderReqDTO.getDepId()));
        }
        if (StringUtils.isNotEmpty(thirdCreateOrderReqDTO.getGysCode())) {
            order.setDrugStoreCode(thirdCreateOrderReqDTO.getGysCode());
        }
        order.setEffective(1);
        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipeList)));
        order.setPayFlag(0);
        //设置订单各个费用
        thirdOrderSetFee(order, recipeList, thirdCreateOrderReqDTO);
        order.setWxPayWay(thirdCreateOrderReqDTO.getPayway());
        order.setCreateTime(new Date());
        order.setPayTime(new Date());
        order.setPushFlag(1);
        order.setSendTime(new Date());
        order.setLastModifyTime(new Date());
        order.setOrderType(0);
        order.setExpectEndTakeTime("");
        order.setExpectStartTakeTime("");
        order.setPayMode(PayModeEnum.OFFLINE_PAY.getType());
        RecipeOrder recipeOrder = recipeOrderDAO.save(order);
        logger.info("RecipeOrderBusinessService thirdCreateOrder recipeOrder:{}.", JSONUtils.toString(recipeOrder));
        if (recipeOrder != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("orderCode", recipeOrder.getOrderCode());
            map.put("enterpriseId", recipeOrder.getEnterpriseId());
            recipeList.forEach(recipe1 -> {
                recipeDAO.updateRecipeInfoByRecipeId(recipe1.getRecipeId(), map);
            });
            return recipeOrder.getOrderId();
        }
        return 0;
    }

    @Override
    public ResultBean updateOrderGiveUser(Integer orderId, Integer giveUser) {
        RecipeOrder recipeOrder = recipeOrderDAO.get(orderId);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        recipeIdList.forEach(id -> {
            updateRecipeGiveUser(id, giveUser);
        });
        return ResultBean.succeed();
    }

    /**
     * 判断处方是否有效(到院取药-存储药柜)
     *
     * @param cabinetVO
     * @return
     */
    @Override
    public CabinetVO validateCabinetRecipeStatus(CabinetVO cabinetVO) {
        logger.info("validateCabinetRecipeStatus req:{}.",JSONUtils.toString(cabinetVO));
        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(cabinetVO.getRecipeCode(),cabinetVO.getOrganId());
        if (null == recipe || StringUtils.isEmpty(recipe.getOrderCode())) {
            throw new DAOException(609,"当前处方单未找到");
        }

        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (null == recipeExtend ) {
            throw new DAOException(609,"当前处方单未找到");
        }

        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (null == recipeOrder ) {
            throw new DAOException(609,"当前处方单未支付");
        }

        //到院取药+待取药+未申请退费
        Boolean effectiveFlag=recipe.getGiveMode()==2 && recipeOrder.getStatus()==2 && recipeExtend.getRefundNodeStatus()==null;
        cabinetVO.setEffectiveFlag(effectiveFlag);

        return  cabinetVO;
    }

    /**
     * 存储药柜放入通知
     *
     * @param cabinetVO
     * @return
     */
    @Override
    public void putInCabinetNotice(CabinetVO cabinetVO) {
        logger.info("putInCabinetNotice req:{}.",JSONUtils.toString(cabinetVO));

        Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(cabinetVO.getRecipeCode(),cabinetVO.getOrganId());
        if (null == recipe || StringUtils.isEmpty(recipe.getOrderCode())) {
            throw new DAOException(609,"当前处方单未找到");
        }

        Map<String, String> changeAttr= Maps.newHashMap();
        changeAttr.put("medicineCode",cabinetVO.getMedicineCode());

        //拼地址
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String medicineAddressTpl=recipeParameterDao.getByName("medicine_address_tpl_"+recipe.getClinicOrgan());
        if(StringUtils.isEmpty(medicineAddressTpl)){
            medicineAddressTpl=recipeParameterDao.getByName("medicine_address_tpl_0");
        }

        Map<String,Object> props = MapValueUtil.beanToMap(cabinetVO);
        String medicineAddress=LocalStringUtil.processTemplate(medicineAddressTpl,props);

        changeAttr.put("medicineAddress",medicineAddress);
        recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(),changeAttr);



        //药品放入存储药柜通知
        SmsInfoBean smsInfoBean=new SmsInfoBean();
        smsInfoBean.setBusType("recipePutInCabinetNotice");
        smsInfoBean.setSmsType("recipePutInCabinetNotice");
        smsInfoBean.setBusId(recipe.getRecipeId());
        smsInfoBean.setOrganId(recipe.getClinicOrgan());
        smsInfoBean.setExtendValue(JSONUtils.toString(cabinetVO));
        smsClient.pushMsgData2OnsExtendValue(smsInfoBean);

        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "药品存入药柜，取药码："+cabinetVO.getMedicineCode()+"，取药地址："+medicineAddress);

    }

    @Override
    public Boolean makeUpInvoice(String orderCode) {
        InvoiceInfoResTO invoiceInfoResTO = orderManager.makeUpInvoice(orderCode);
        return invoiceInfoResTO.getSuccess();
    }

    @Override
    public SelfPreSettleQueryReq selfPreSettleQueryInfo(Integer busId) {
        SelfPreSettleQueryReq selfPreSettleQueryReq = new SelfPreSettleQueryReq();
        RecipeOrder recipeOrder = recipeOrderDAO.get(busId);
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);

        if (recipeOrder == null) {
            throw new DAOException("订单信息不存在");
        }
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        if (CollectionUtils.isEmpty(recipeIdList)) {
            throw new DAOException("处方信息不存在");
        }
        Recipe recipe = recipeList.get(0);
        try {
            RecipeCashPreSettleReqTO request = new RecipeCashPreSettleReqTO();
            //购药方式
            if (GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType().equals(recipe.getGiveMode())) {
                //配送到家
                request.setDeliveryType("1");
            } else if (GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType().equals(recipe.getGiveMode())) {
                //到院取药
                request.setDeliveryType("0");
            }
            RecipeExtend ext = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (ext != null && StringUtils.isNotEmpty(ext.getIllnessType())) {
                // 大病标识
                request.setIllnessType(ext.getIllnessType());

            }
            Integer cashDeskSettleUseCode = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "cashDeskSettleUseCode", CashDeskSettleUseCodeTypeEnum.HIS_RECIPE_CODE.getType());
            List<String> recipeCodeS = new ArrayList<>();
            if (CashDeskSettleUseCodeTypeEnum.HIS_RECIPE_CODE.getType().equals(cashDeskSettleUseCode)) {
                recipeCodeS = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
            } else {
                List<String> chargeItemCodeList = recipeManager.getChargeItemCode(recipeExtendList);
                recipeCodeS.addAll(chargeItemCodeList);
            }
            String join = Joiner.on(",").join(recipeCodeS);
            List<String> list = Arrays.asList(join.split(","));
            request.setHisRecipeNoS(list);
            request.setClinicOrgan(recipe.getClinicOrgan());
            request.setRecipeId(String.valueOf(recipe.getRecipeId()));
            request.setHisRecipeNo(recipe.getRecipeCode());
            //患者信息
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientBean = patientService.get(recipe.getMpiid());
            request.setPatientName(patientBean.getPatientName());
            request.setIdcard(patientBean.getIdcard());
            request.setCertificate(patientBean.getCertificate());
            request.setCertificateType(patientBean.getCertificateType());
            request.setMobile(patientBean.getMobile());
            request.setPatientId(recipe.getPatientID());
            request.setDepartId(null != recipe.getDepart() ? recipe.getDepart().toString() : "");
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);

            try {
                if (Objects.nonNull(recipeOrder)) {
                    request.setRegisterFee(recipeOrder.getRegisterFee());
                    request.setRegisterFeeNo(recipeOrder.getRegisterFeeNo());
                    request.setTcmFee(recipeOrder.getTcmFee());
                    request.setTcmFeeNo(recipeOrder.getTcmFeeNo());
                    request.setOrderCode(recipeOrder.getOrderCode());
                }
            } catch (Exception e) {
                logger.error("MedicalPreSettleService 代缴费用有误");
            }

            RecipeExtend ext2 = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (ext2 != null) {
                if (StringUtils.isNotEmpty(ext2.getRegisterID())) {
                    request.setRegisterID(ext2.getRegisterID());
                    selfPreSettleQueryReq.setRegisterNo(ext2.getRegisterID());
                }
            }
            List<String> supportRecharge = configurationClient.getValueListCatch(recipe.getClinicOrgan(), "supportRecharge", null);
            Boolean supportRecipeRecharge = supportRecharge.contains("recipe");
            //是否支持就诊卡充值支付
            Integer supportRecipeRechargeFlag = supportRecipeRecharge ? 1 : 0;
            request.setSupportRecipeRechargeFlag(supportRecipeRechargeFlag);
            logger.info("selfPreSettleQueryInfo busId={} req={}", busId, JSONUtils.toString(request));
            HisResponseTO<RecipeCashPreSettleInfo> hisResult = service.recipeCashPreSettleHis(request);
            logger.info("selfPreSettleQueryInfo busId={} res={}", busId, JSONUtils.toString(hisResult));
            if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
                if (hisResult.getData() != null) {
                    //总金额
                    selfPreSettleQueryReq.setRecipeNos(join);
                    String totalAmount = hisResult.getData().getZje();
                    String accountBalance = hisResult.getData().getZhye();
                    String rechargeAmount = hisResult.getData().getYfje();
                    //his收据号
                    String hisSettlementNo = hisResult.getData().getSjh();

                    selfPreSettleQueryReq.setOrganId(recipe.getClinicOrgan());
                    selfPreSettleQueryReq.setOutTradeNo(recipeOrder.getOutTradeNo());
                    //获取就诊卡号--一般来说处方里已经保存了复诊里的就诊卡号了取不到再从复诊里取
                    selfPreSettleQueryReq.setMrn(getMrnForRecipe(recipe));
                    selfPreSettleQueryReq.setClinicNo(String.valueOf(recipe.getClinicId()));
                    selfPreSettleQueryReq.setHisSettlementNo(hisSettlementNo);
                    if (StringUtils.isNotEmpty(totalAmount)) {
                        selfPreSettleQueryReq.setTotalAmount(new BigDecimal(totalAmount));
                    }
                    if (StringUtils.isNotEmpty(accountBalance)) {
                        selfPreSettleQueryReq.setAccountBalance(new BigDecimal(accountBalance));
                    }
                    if (StringUtils.isNotEmpty(rechargeAmount)) {
                        selfPreSettleQueryReq.setRechargeAmount(new BigDecimal(rechargeAmount));
                    }

                    selfPreSettleQueryReq.setIsInHosPay(supportRecipeRechargeFlag);
                }

            }
        } catch (Exception e) {
            logger.error("selfPreSettleQueryInfo busId={} error", busId, e);
        }
        return selfPreSettleQueryReq;
    }

    @Override
    public MedicalPreSettleQueryReq medicalPreSettleQueryInfo(Integer busId) {
        logger.info("medicalPreSettleQueryInfo busId={}", busId);
        RecipeOrder recipeOrder = recipeOrderDAO.get(busId);
        if (null == recipeOrder) {
            throw new DAOException("订单信息不存在");
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        if (CollectionUtils.isEmpty(recipeIdList)) {
            throw new DAOException("处方信息不存在");
        }
        Recipe recipe = recipeList.get(0);
        MedicalPreSettleQueryReq medicalPreSettleQueryReq = new MedicalPreSettleQueryReq();
        if (Objects.isNull(recipe)) {
            throw new DAOException("未获取到处方信息！");
        }
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (Objects.isNull(recipeExtend)) {
            throw new DAOException("未获取到处方扩展信息！");
        }
        try {
            medicalPreSettleQueryReq.setOrganId(recipe.getClinicOrgan());
            medicalPreSettleQueryReq.setClinicNo(String.valueOf(recipe.getClinicId()));
            medicalPreSettleQueryReq.setMrn(recipeExtend.getMedicalRecordNumber());
            medicalPreSettleQueryReq.setRegisterNo(recipeExtend.getRegisterID());
            medicalPreSettleQueryReq.setPatId(recipe.getPatientID());
            if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                if (Objects.isNull(recipeOrder)) {
                    throw new DAOException("未获取到处方订单信息！");
                }
                medicalPreSettleQueryReq.setHisSettlementNo(recipeOrder.getHisSettlementNo());
                medicalPreSettleQueryReq.setTotalAmount(recipeOrder.getTotalFee());
                Integer cashDeskSettleUseCode = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "cashDeskSettleUseCode", CashDeskSettleUseCodeTypeEnum.HIS_RECIPE_CODE.getType());
                List<String> recipeCodeS = new ArrayList<>();
                if (CashDeskSettleUseCodeTypeEnum.HIS_RECIPE_CODE.getType().equals(cashDeskSettleUseCode)) {
                    recipeCodeS = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
                } else {
                    List<String> chargeItemCodeList = recipeManager.getChargeItemCode(recipeExtendList);
                    recipeCodeS.addAll(chargeItemCodeList);
                }
                String registerFeeNo = recipeOrder.getRegisterFeeNo();
                if (StringUtils.isNotEmpty(registerFeeNo)) {
                    recipeCodeS.add(registerFeeNo);
                }
                String tcmFeeNo = recipeOrder.getTcmFeeNo();
                if (StringUtils.isNotEmpty(tcmFeeNo)) {
                    recipeCodeS.add(tcmFeeNo);
                }
                String join = Joiner.on(",").join(recipeCodeS);
                medicalPreSettleQueryReq.setRecipeNos(join);
            }
        } catch (Exception e) {
            logger.error("medicalPreSettleQueryInfo error", e);
        }
        return medicalPreSettleQueryReq;
    }
    @Override
    public ThirdOrderPreSettleRes thirdOrderPreSettle(ThirdOrderPreSettleReq thirdOrderPreSettleReq) {
        ThirdOrderPreSettleRes thirdOrderPreSettleRes = new ThirdOrderPreSettleRes();
        checkParams(thirdOrderPreSettleReq);
        setUrtToContext(thirdOrderPreSettleReq.getAppkey(), thirdOrderPreSettleReq.getTid());
        List<Recipe> recipes = recipeDAO.findByRecipeIds(thirdOrderPreSettleReq.getRecipeIds());
        if(CollectionUtils.isEmpty(recipes)){
            logger.info("ThirdOrderPreSettle recipes is null");
            throw new DAOException(609,"处方不存在");
        }
        List<String> recipeNoS = recipes.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipes.get(0).getOrderCode());
        logger.info("unifyRecipePreSettle recipeOrder:{}", JSON.toJSONString(recipeOrder));
        if (recipeOrder == null) {
            logger.info("ThirdOrderPreSettle order is null");
            throw new DAOException(609,"订单不存在");
        }
        List<RecipeExtend> recipeExtends = recipeExtendDAO.queryRecipeExtendByRecipeIds(thirdOrderPreSettleReq.getRecipeIds());
        if (CollectionUtils.isEmpty(recipeExtends)) {
            logger.info("ThirdOrderPreSettle extend is null");
            throw new DAOException(609,"补充信息不存在");
        }
        List<String> recipeCostNumber = recipeExtends.stream().map(RecipeExtend::getRecipeCostNumber).filter(Objects::nonNull).collect(Collectors.toList());
        RecipeExtend extend = recipeExtends.get(0);
        if (!RecipeBussConstant.PAYMODE_ONLINE.equals(recipeOrder.getPayMode())) {
            logger.info("ThirdOrderPreSettle no support. recipeId={}", JSONUtils.toString(recipes.get(0).getRecipeId()));
            throw new DAOException(609,"不是线上支付订单");
        }
        Integer depId = recipeOrder.getEnterpriseId();
//        Integer orderType = recipeOrder.getOrderType() == null ? 0 : recipeOrder.getOrderType();
        String insuredArea = extend.getInsuredArea();
        Map<String, Object> param = com.google.common.collect.Maps.newHashMap();
        param.put("depId", depId);
        param.put("insuredArea", insuredArea);
        param.put("recipeNoS", JSONUtils.toString(recipeNoS));
        param.put("payMode", recipeOrder.getPayMode());
        param.put("recipeIds", recipes.get(0).getRecipeId());
        if(CollectionUtils.isNotEmpty(recipeCostNumber)) {
            param.put("recipeCostNumber", JSONUtils.toString(recipeCostNumber));
        }
        //获取对应预结算服务
        // 提供给金投的接口,预算写死走杭州互联网
        IRecipePreSettleService preSettleService = PreSettleFactory.getPreSettleService(recipes.get(0).getClinicOrgan(),2);
        if (preSettleService != null){
            Map<String, Object> map = preSettleService.recipePreSettle(recipes.get(0).getRecipeId(), param);
            logger.info("ThirdOrderPreSettle recipePreSettle map={}", JSONUtils.toString(map));
            thirdOrderPreSettleRes.setPreSettleTotalAmount(map.get("totalAmount").toString());
            thirdOrderPreSettleRes.setCashAmount(map.get("cashAmount").toString());
            thirdOrderPreSettleRes.setFundAmount(map.get("fundAmount").toString());
        }
        return thirdOrderPreSettleRes;
    }

    /**
     * 第三方设置订单费用
     *
     * @param order
     * @param recipeList
     * @param thirdCreateOrderReqDTO
     */
    private void thirdOrderSetFee(RecipeOrder order, List<Recipe> recipeList, ThirdCreateOrderReqDTO thirdCreateOrderReqDTO) {
        //设置挂号费
        if (thirdCreateOrderReqDTO.getRegisterFee() != null && thirdCreateOrderReqDTO.getRegisterFee().compareTo(BigDecimal.ZERO) > 0) {
            order.setRegisterFee(thirdCreateOrderReqDTO.getRegisterFee());
        } else {
            order.setRegisterFee(BigDecimal.ZERO);
        }
        //设置快递费
        if (thirdCreateOrderReqDTO.getExpressFee() != null && thirdCreateOrderReqDTO.getRecipeFee().compareTo(BigDecimal.ZERO) > 0) {
            order.setExpressFee(thirdCreateOrderReqDTO.getExpressFee());
        } else {
            order.setExpressFee(BigDecimal.ZERO);
        }
        //设置代煎费
        if (thirdCreateOrderReqDTO.getDecoctionFee() != null && thirdCreateOrderReqDTO.getRecipeFee().compareTo(BigDecimal.ZERO) > 0) {
            order.setDecoctionFee(thirdCreateOrderReqDTO.getDecoctionFee());
        } else {
            order.setDecoctionFee(BigDecimal.ZERO);
        }
        //设置审方费
        if (thirdCreateOrderReqDTO.getAuditFee() != null && thirdCreateOrderReqDTO.getRecipeFee().compareTo(BigDecimal.ZERO) > 0) {
            order.setAuditFee(thirdCreateOrderReqDTO.getAuditFee());
        } else {
            order.setAuditFee(BigDecimal.ZERO);
        }
        //设置处方费
        if (thirdCreateOrderReqDTO.getRecipeFee() != null && thirdCreateOrderReqDTO.getRecipeFee().compareTo(BigDecimal.ZERO) > 0) {
            order.setRecipeFee(thirdCreateOrderReqDTO.getRecipeFee());
        } else {
            List<BigDecimal> totalMoneys = recipeList.stream().map(Recipe::getTotalMoney).filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(totalMoneys)) {
                BigDecimal totalMoney = BigDecimal.ZERO;
                totalMoneys.forEach(t -> {
                    totalMoney.add(t);
                });
                order.setRecipeFee(totalMoney);
            } else {
                order.setRecipeFee(BigDecimal.ZERO);
            }
        }
        //设置优惠费用
        order.setCouponId(0);
        order.setCouponFee(BigDecimal.ZERO);
        //设置总费用
        order.setTotalFee(order.getRegisterFee().add(order.getExpressFee()).add(order.getDecoctionFee()).add(order.getAuditFee().add(order.getRecipeFee())));
        //设置实际支付
        order.setActualPrice(order.getTotalFee().doubleValue());
    }

    /**
     * 获取当前登录用户的MPIID
     * @return 当前用户的MPIID
     */
    private String getOwnMpiId(){
        UserRoleToken userRoleToken = UserRoleToken.getCurrent();
        logger.info("RecipeOrderBusinessService.getOwnMpiId userRoleToken:{}.", JSONUtils.toString(userRoleToken));
        return userRoleToken.getOwnMpiId();
    }

    /**
     * 模拟登录
     * @param thirdParty 第三方标识
     * @param tid 第三方唯一标识
     */
    private void setUrtToContext(String thirdParty, String tid){
        ThirdPartyMappingController.instance().setUrtToContext(thirdParty, tid);
    }

    /**
     * 参数校验
     * @param request
     */
    private void checkOrderParams(ThirdCreateOrderReqDTO request){
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (Objects.isNull(request.getRecipeIds())) {
            throw new DAOException(609, "处方单ID为空");
        }
        if (StringUtils.isEmpty(request.getPayway())) {
            throw new DAOException(609, "支付类型为空");
        }
    }

    /**
     * 参数校验
     * @param request
     */
    private void checkParams(ThirdOrderPreSettleReq request){
        if (StringUtils.isEmpty(request.getTid())) {
            throw new DAOException(609, "用户为空");
        }
        if (Objects.isNull(request.getRecipeIds())) {
            throw new DAOException(609, "处方单ID为空");
        }

    }

    /**
     * 获取支付传给卫宁付用的就诊卡号
     * 一般来说处方里已经保存了复诊里的就诊卡号了取不到再从复诊里取
     *
     * @param recipe
     * @return //-1表示获取不到身份证，默认用身份证获取患者信息
     */
    private String getMrnForRecipe(Recipe recipe) {
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        String mrn = null;
        if (StringUtils.isNotEmpty(recipeExtend.getCardNo())) {
            mrn = recipeExtend.getCardNo();
        } else {
            //复诊
            if (new Integer(2).equals(recipe.getBussSource())) {
                IRevisitExService revisitExService = RevisitAPI.getService(IRevisitExService.class);
                if (recipe.getClinicId() != null) {
                    RevisitExDTO revisitExDTO = revisitExService.getByConsultId(recipe.getClinicId());
                    if (revisitExDTO != null && revisitExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = revisitExDTO.getCardId();
                    }
                }
                //咨询
            } else if (new Integer(1).equals(recipe.getBussSource())) {
                IConsultExService consultExService = ConsultAPI.getService(IConsultExService.class);
                if (recipe.getClinicId() != null) {
                    ConsultExDTO consultExDTO = consultExService.getByConsultId(recipe.getClinicId());
                    if (consultExDTO != null && consultExDTO.getCardId() != null) {
                        //就诊卡号
                        mrn = consultExDTO.getCardId();
                    }
                }
            }
        }

        //-1表示获取不到身份证，默认用身份证获取患者信息
        if (StringUtils.isEmpty(mrn)) {
            mrn = "-1";
        }
        return mrn;
    }

    /**
     * 获取未完善或完善标识
     * @param recipeBean
     * @return
     */
    @Override
    public Integer getImperfectFlag(com.ngari.recipe.recipe.model.RecipeBean recipeBean) {
        logger.info("getImperfectFlag recipeBean={}",recipeBean);
        try{
            RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getByOrganIdAndRecipeCode(recipeBean.getClinicOrgan(),recipeBean.getRecipeCode(),recipeBean.getMpiid());
            if(recipeBeforeOrder != null){
                return recipeBeforeOrder.getIsReady();
            }
        }catch (Exception e){
            return 0;
        }
        logger.info("getImperfectFlag recipeBeforeOrder为null");
        return 0;
    }

    /**
     * 获取购物车信息
     * @param mpiId
     * @return
     */
    @Override
    public List<ShoppingCartDetailDTO> getShoppingCartDetail(String mpiId) {
        logger.info("getShoppingCartDetail mpiId={}",mpiId);
        List<ShoppingCartDetailDTO> shoppingCartDetailDTOList = new ArrayList<>();
        List<RecipeBeforeOrder> recipeBeforeOrderList = recipeBeforeOrderDAO.findByMpiId(mpiId);
        logger.info("getShoppingCartDetail recipeBeforeOrderList={}",JSONUtils.toString(recipeBeforeOrderList));
        if(CollectionUtils.isNotEmpty(recipeBeforeOrderList)){
            //需要合并的处方
            List<List<RecipeBeforeOrder>> mergeBeforeOrder = orderManager.mergeBeforeOrder(recipeBeforeOrderList);
            logger.info("getShoppingCartDetail mergeBeforeOrder ={}",JSONUtils.toString(mergeBeforeOrder));
            for(List<RecipeBeforeOrder> recipeBeforeOrders : mergeBeforeOrder){
                ShoppingCartDetailDTO shoppingCartDetailDTO = new ShoppingCartDetailDTO();
                List<RecipeBeforeOrderDTO> recipeBeforeOrderDTOList = ObjectCopyUtils.convert(recipeBeforeOrders, RecipeBeforeOrderDTO.class);
                RecipeBeforeOrderDTO beforeOrder = recipeBeforeOrderDTOList.get(0);
                RecipeOrder recipeOrder = new RecipeOrder();
                if(beforeOrder.getEnterpriseId() != null){
                    DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(beforeOrder.getEnterpriseId());
                    logger.info("getShoppingCartDetail enterprise ={}",JSONUtils.toString(enterprise));
                    if(Objects.nonNull(enterprise)){
                        beforeOrder.setExpressFeePayWay(enterprise.getExpressFeePayWay());
                        recipeOrder.setExpressFeePayWay(enterprise.getExpressFeePayWay());
                        //购药方式为配送到家时返回药企名称和电话，或到院取药并有药企ID时
                        if(new Integer(1).equals(beforeOrder.getGiveMode()) || new Integer(2).equals(beforeOrder.getGiveMode()) ){
                            beforeOrder.setOrganName(enterprise.getName());
                            beforeOrder.setOrganPhone(enterprise.getTel());
                        }
                    }
                }
                //购药方式为到院取药没有药企ID时返回机构名称和电话
                if(new Integer(2).equals(beforeOrder.getGiveMode()) && beforeOrder.getEnterpriseId() == null){
                    OrganService organDAO = AppContextHolder.getBean("basic.organService", OrganService.class);
                    com.ngari.patient.dto.OrganDTO organDTO = organDAO.getByOrganId(beforeOrder.getOrganId());
                    logger.info("getShoppingCartDetail organDTO ={}",JSONUtils.toString(organDTO));
                    if(Objects.nonNull(organDTO)){
                        beforeOrder.setOrganName(organDTO.getName());
                        beforeOrder.setOrganPhone(organDTO.getPhoneNumber().split("\\|")[0]);
                    }
                }
                BigDecimal recipeFee = BigDecimal.ZERO;
                BigDecimal tcmFee = BigDecimal.ZERO;
                BigDecimal decoctionFee = BigDecimal.ZERO;
                BigDecimal auditFee = BigDecimal.ZERO;
                BigDecimal expressFee = BigDecimal.ZERO;
                List<RecipeDTO> recipeDTOList = new ArrayList<>();
                for(RecipeBeforeOrderDTO recipeBeforeOrder : recipeBeforeOrderDTOList){
                    Map<String,String> extInfo = new HashMap<>();
                    extInfo.put("operMpiId",recipeBeforeOrder.getOperMpiId());
                    if(recipeBeforeOrder.getEnterpriseId() != null){
                        extInfo.put("depId",recipeBeforeOrder.getEnterpriseId().toString());
                    }
                    //当购药方式为配送到家以及已经保存过地址时，需要把保存的addressId传去查地址是否配送
                    if(recipeBeforeOrder.getAddressId() != null && (recipeBeforeOrder.getGiveMode().equals(GiveModeTextEnum.getGiveMode("showSendToEnterprises"))
                            || recipeBeforeOrder.getGiveMode().equals(GiveModeTextEnum.getGiveMode("showSendToHos")))){
                        extInfo.put("addressId",recipeBeforeOrder.getAddressId().toString());
                    }
                    recipeOrder.setOrganId(beforeOrder.getOrganId());
                    recipeOrder.setEnterpriseId(beforeOrder.getEnterpriseId());
                    Recipe recipe = recipeDAO.getByRecipeId(recipeBeforeOrder.getRecipeId());
                    Integer payMode = PayModeGiveModeUtil.getPayMode(1, recipeBeforeOrder.getGiveMode());
                    RecipePayModeSupportBean payModeSupportBean = orderService.setPayModeSupport(recipeOrder, payMode);
                    logger.info("getShoppingCartDetail payModeSupportBean ={}",JSONUtils.toString(payModeSupportBean));
                    if(recipe != null){
                        List<Integer> recipeIds = new ArrayList<>();
                        List<Recipe> recipeList = new ArrayList<>();
                        RecipeDTO recipeDTO = new RecipeDTO();
                        recipeDTO.setIsLock(recipeBeforeOrder.getIsLock());
                        recipeList.add(recipe);
                        recipeIds.add(recipe.getRecipeId());
                        recipeDTO.setRecipe(recipe);
                        //防止当西药没有某些费用时还有值
                        recipeOrder.setRecipeFee(BigDecimal.ZERO);
                        recipeOrder.setAuditFee(BigDecimal.ZERO);
                        recipeOrder.setTcmFee(BigDecimal.ZERO);
                        recipeOrder.setExpressFee(BigDecimal.ZERO);
                        recipeOrder.setDecoctionFee(BigDecimal.ZERO);
                        orderService.setOrderFee(new OrderCreateResult(200),recipeOrder,recipeIds,recipeList,payModeSupportBean,extInfo,0);
                        logger.info("getShoppingCartDetail recipeOrder ={}",JSONUtils.toString(recipeOrder));
                        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeBeforeOrder.getRecipeId());
                        if(CollectionUtils.isNotEmpty(recipeDetailList)){
                            for(Recipedetail recipedetail : recipeDetailList){
                                if(recipedetail.getActualSalePrice() != null){
                                    recipedetail.setSalePrice(recipedetail.getActualSalePrice());
                                }
                            }
                            recipeDTO.setRecipeDetails(recipeDetailList);
                        }
                        RecipeExtend recipeExtendVO = recipeExtendDAO.getByRecipeId(recipeBeforeOrder.getRecipeId());
                        RecipeExtend recipeExtend = new RecipeExtend();
                        recipeExtend.setRegisterID(recipeExtendVO.getRegisterID());
                        if(recipeExtendVO != null && recipeExtendVO.getDecoctionId() != null){
                            recipeExtend.setDecoctionId(recipeExtendVO.getDecoctionId());
                            recipeDTO.setRecipeExtend(recipeExtend);
                        }
                        recipeDTOList.add(recipeDTO);
                    }
                    try {
                        //当购药方式为配送到家（药企配送、医院配送）和获取到了默认地址时才保存地址
                        if(new Integer(1).equals(recipeBeforeOrder.getGiveMode()) && recipeBeforeOrder.getAddressId() == null){
                            //找该患者为已完善的地址，再赋值给后面加入购物车的单子，保证配送地址唯一
                            List<RecipeBeforeOrder> beforeOrderList = recipeBeforeOrderList.stream().filter(a -> new Integer(1).equals(a.getIsReady())).collect(Collectors.toList());
                            if(beforeOrderList.size() > 0){
                                orderManager.processShoppingCartAddress(recipeBeforeOrder,beforeOrderList.get(0),new RecipeOrder());
                                recipeBeforeOrder.setCompleteAddress(beforeOrderList.get(0).getCompleteAddress());
                            }else{
                                if(recipeOrder.getAddressID() != null){
                                    orderManager.processShoppingCartAddress(recipeBeforeOrder,new RecipeBeforeOrder(),recipeOrder);
                                    recipeBeforeOrder.setCompleteAddress(orderManager.getCompleteAddress(recipeOrder));
                                }
                            }
                            recipeBeforeOrder.setIsReady(0);
                        }
                    }catch (Exception e){
                        logger.info("getShoppingCartDetail processShoppingCartAddress error",e);
                    }
                    recipeBeforeOrder.setRecipeFee(recipeOrder.getRecipeFee());
                    recipeBeforeOrder.setAuditFee(recipeOrder.getAuditFee());
                    recipeBeforeOrder.setTcmFee(recipeOrder.getTcmFee());
                    recipeBeforeOrder.setExpressFee(recipeOrder.getExpressFee());
                    recipeBeforeOrder.setDecoctionFee(recipeOrder.getDecoctionFee());
                    recipeBeforeOrder.setAddressCanSend(recipeOrder.getAddressCanSend());
                    recipeBeforeOrder.setUpdateTime(new Date());
                    recipeBeforeOrderDAO.updateNonNullFieldByPrimaryKey(ObjectCopyUtils.convert(recipeBeforeOrder,RecipeBeforeOrder.class));
                    if(recipeOrder.getRecipeFee() != null){
                        recipeFee = recipeFee.add(recipeOrder.getRecipeFee());
                    }
                    if(recipeOrder.getTcmFee() != null){
                        tcmFee = tcmFee.add(recipeOrder.getTcmFee());
                    }
                    if(recipeOrder.getDecoctionFee() != null){
                        decoctionFee = decoctionFee.add(recipeOrder.getDecoctionFee());
                    }
                    if(recipeOrder.getAuditFee() != null){
                        auditFee = auditFee.add(recipeOrder.getAuditFee());
                    }
                    if(recipeOrder.getExpressFee() != null){
                        expressFee = recipeOrder.getExpressFee();
                    }else{
                        expressFee = null;
                    }
                }
                if(new Integer(3).equals(beforeOrder.getGiveMode())){
                    if(beforeOrder.getDrugStoreCode() != null){
                        Pharmacy pharmacy = pharmacyDAO.getPharmacyByPharmacyCode(beforeOrder.getDrugStoreCode());
                        if(Objects.nonNull(pharmacy)){
                            beforeOrder.setDrugStorePhone(pharmacy.getPharmacyPhone());
                        }
                    }
                }
                beforeOrder.setExpressFeePayMethod(recipeOrder.getExpressFeePayMethod());
                //处方费
                beforeOrder.setRecipeFee(recipeFee);
                beforeOrder.setAuditFee(auditFee);
                //为了判断总的运费是不是包邮
                recipeOrder.setRecipeFee(recipeFee);
                recipeOrder.setExpressFee(expressFee);
                recipeOrder.setAddress3(beforeOrder.getAddress3());
                orderFeeManager.setExpressFee(recipeOrder);
                beforeOrder.setExpressFee(recipeOrder.getExpressFee());
                beforeOrder.setTcmFee(tcmFee);
                beforeOrder.setDecoctionFee(decoctionFee);
                if(CollectionUtils.isNotEmpty(recipeDTOList)){
                    shoppingCartDetailDTO.setRecipeDTO(recipeDTOList);
                }
                shoppingCartDetailDTO.setRecipeBeforeOrder(beforeOrder);
                shoppingCartDetailDTOList.add(shoppingCartDetailDTO);
                logger.info("getShoppingCartDetail shoppingCartDetailDTOList={}",JSONUtils.toString(shoppingCartDetailDTOList));
            }
        }
        //根据购药方式进行排序（把配送方式排在最前面，因为对象包了两层，排序比较麻烦）
        Map<RecipeBeforeOrderDTO, ShoppingCartDetailDTO> shoppingCartDetailDTOMap = shoppingCartDetailDTOList.stream()
                .collect(Collectors.toMap(ShoppingCartDetailDTO::getRecipeBeforeOrder, a -> a, (k1, k2) -> k1));
        List<RecipeBeforeOrderDTO> beforeOrderList = shoppingCartDetailDTOList.stream().map(ShoppingCartDetailDTO::getRecipeBeforeOrder).collect(Collectors.toList());
        List<RecipeBeforeOrderDTO> recipeBeforeOrders = beforeOrderList.stream().sorted(Comparator.comparing(RecipeBeforeOrderDTO::getGiveMode)).collect(Collectors.toList());
        List<ShoppingCartDetailDTO> shoppingCartDetailDTOs = new ArrayList<>();
        recipeBeforeOrders.forEach(recipeBeforeOrder -> {
            shoppingCartDetailDTOs.add(shoppingCartDetailDTOMap.get(recipeBeforeOrder));
        } );
        return shoppingCartDetailDTOs;
}

    @Override
    public void saveRecipeBeforeOrderInfo(ShoppingCartReqVO shoppingCartReqVO) {
        logger.info("saveRecipeBeforeOrderInfo shoppingCartReqVO={}",JSONUtils.toString(shoppingCartReqVO));
        orderManager.saveRecipeBeforeOrderInfo(Objects.requireNonNull(ObjectCopyUtils.convert(shoppingCartReqVO, ShoppingCartReqDTO.class)));
        //根据购药方式更新处方详情
        purchaseService.updateRecipeDetail(shoppingCartReqVO.getRecipeId(),shoppingCartReqVO.getEnterpriseId());
    }

    @Override
    public String improvePreOrderInfo(PreOrderInfoReqVO preOrderInfoReqVO) {
        logger.info("improvePreOrderInfo preOrderInfoReqVO={}",JSONUtils.toString(preOrderInfoReqVO));
        if(preOrderInfoReqVO.getRecipeId() != null){
            List<Recipe> recipeList = recipeDAO.findByRecipeIds(preOrderInfoReqVO.getRecipeId());
            if(CollectionUtils.isNotEmpty(recipeList)){
                for(Recipe recipe : recipeList){
                    if(new Integer(1).equals(recipe.getReviewType()) && new Integer(8).equals(recipe.getStatus())){
                        throw new DAOException(ErrorCode.SERVICE_ERROR, "处方待药师审核");
                    }
                    else {
                        RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(recipe.getRecipeId());
                        if(Objects.nonNull(recipeBeforeOrder)){
                            Integer giveMode = recipeBeforeOrder.getGiveMode();
                            //购药方式为配送到家（医院配送、药企配送）
                            if(new Integer(1).equals(giveMode)){
                                if(preOrderInfoReqVO.getAddressId() != null){
                                    RecipeOrder recipeOrder = new RecipeOrder();
                                    recipeBeforeOrder.setAddressId(preOrderInfoReqVO.getAddressId());
                                    AddressDTO addressDTO = addressService.getByAddressId(preOrderInfoReqVO.getAddressId());
                                    if (addressDTO != null) {
                                        recipeBeforeOrder.setIsReady(1);
                                        recipeBeforeOrder.setAddress1(addressDTO.getAddress1());
                                        recipeBeforeOrder.setAddress2(addressDTO.getAddress2());
                                        recipeBeforeOrder.setAddress3(addressDTO.getAddress3());
                                        recipeBeforeOrder.setStreetAddress(addressDTO.getStreetAddress());
                                        recipeBeforeOrder.setAddress4(addressDTO.getAddress4());
                                        recipeBeforeOrder.setAddress5(addressDTO.getAddress5());
                                        recipeBeforeOrder.setAddress5Text(addressDTO.getAddress5Text());
                                        recipeBeforeOrder.setReceiver(addressDTO.getReceiver());
                                        recipeBeforeOrder.setRecMobile(addressDTO.getRecMobile());
                                        recipeBeforeOrder.setRecTel(addressDTO.getRecTel());
                                        recipeBeforeOrder.setZipCode(addressDTO.getZipCode());
                                        recipeOrder.setAddress1(addressDTO.getAddress1());
                                        recipeOrder.setAddress2(addressDTO.getAddress2());
                                        recipeOrder.setAddress3(addressDTO.getAddress3());
                                        if (StringUtils.isNotEmpty(addressDTO.getAddress5Text())) {
                                            recipeOrder.setAddress4(addressDTO.getAddress5Text() + addressDTO.getAddress4());
                                        } else {
                                            recipeOrder.setAddress4(addressDTO.getAddress4());
                                        }
                                        recipeOrder.setStreetAddress(addressDTO.getStreetAddress());
                                        recipeBeforeOrder.setCompleteAddress(orderManager.getCompleteAddress(recipeOrder));
                                    }else {
                                        logger.info("improvePreOrderInfo addressDTO为null");
                                    }
                                    List<Integer> recipeIds = new ArrayList<>();
                                    Integer recipeId = recipeBeforeOrder.getRecipeId();
                                    recipeIds.add(recipeId);
                                    if(recipeBeforeOrder.getEnterpriseId() != null){
                                        recipeOrder.setEnterpriseId(recipeBeforeOrder.getEnterpriseId());
                                        orderService.setOrderAddress(new OrderCreateResult(200),recipeOrder,recipeIds, new RecipePayModeSupportBean(),null,0,addressDTO);
                                    }
                                    //运费
                                    recipeBeforeOrder.setExpressFee(recipeOrder.getExpressFee());
                                }
                            }
                            //购药方式为到店取药
                            else if (new Integer(3).equals(giveMode)){
                                recipeBeforeOrder.setDrugStoreName(preOrderInfoReqVO.getDrugStoreName());
                                recipeBeforeOrder.setDrugStoreCode(preOrderInfoReqVO.getDrugStoreCode());
                                recipeBeforeOrder.setDrugStoreAddr(preOrderInfoReqVO.getDrugStoreAddr());
                                recipeBeforeOrder.setIsReady(1);
                            }
                            logger.info("improvePreOrderInfo recipeBeforeOrder={}",JSONUtils.toString(recipeBeforeOrder));
                            recipeBeforeOrder.setUpdateTime(new Date());
                            recipeBeforeOrderDAO.update(recipeBeforeOrder);
                        }
                    }
                }
            }
        }
        return "success";
    }

    @Override
    public Boolean getPreOrderFlag(Integer recipeId,String mpiId) {
        RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeIdAndMpiId(recipeId,mpiId);
        if(Objects.nonNull(recipeBeforeOrder)){
            return true;
        }
        return false;
    }

    @Override
    public List<ImperfectInfoVO> batchGetImperfectFlag(List<com.ngari.recipe.recipe.model.RecipeBean> recipeBeans) {
        logger.info("batchGetImperfectFlag recipeBeans={}",JSONUtils.toString(recipeBeans));
        List<ImperfectInfoVO> imperfectInfoVOS = new ArrayList<>();
        List<String> recipeCodes = recipeBeans.stream().map(com.ngari.recipe.recipe.model.RecipeBean::getRecipeCode).collect(Collectors.toList());
        List<Integer> recipeIds = recipeBeans.stream().map(com.ngari.recipe.recipe.model.RecipeBean::getRecipeId).collect(Collectors.toList());
        Set<Integer> organIds = recipeBeans.stream().map(com.ngari.recipe.recipe.model.RecipeBean::getClinicOrgan).collect(Collectors.toSet());
        Set<String> operMpiIds = recipeBeans.stream().map(com.ngari.recipe.recipe.model.RecipeBean::getMpiid).collect(Collectors.toSet());
        List<RecipeBeforeOrder> recipeBeforeOrders = recipeBeforeOrderDAO.findByRecipeCodesAndOrganIds(recipeCodes,organIds,operMpiIds);
        List<RecipeBeforeOrder> recipeBeforeOrderList = recipeBeforeOrderDAO.findByRecipeIds(recipeIds,operMpiIds);
        recipeBeforeOrders.addAll(recipeBeforeOrderList);
        if(CollectionUtils.isEmpty(recipeBeforeOrders)){
            List<ImperfectInfoVO> collect = recipeBeans.stream().map(recipeBean -> {
                ImperfectInfoVO imperfectInfoVO = new ImperfectInfoVO();
                imperfectInfoVO.setOrganId(recipeBean.getClinicOrgan());
                imperfectInfoVO.setRecipeCode(recipeBean.getRecipeCode());
                imperfectInfoVO.setImperfectFlag(0);
                imperfectInfoVO.setRecipeId(recipeBean.getRecipeId());
                return imperfectInfoVO;
            }).collect(Collectors.toList());
            return collect;
        }
        List<String> recipeCodeList = recipeBeforeOrders.stream().map(RecipeBeforeOrder::getRecipeCode).collect(Collectors.toList());

        recipeBeforeOrders.forEach(recipeBeforeOrder -> {
            ImperfectInfoVO imperfectInfoVO = new ImperfectInfoVO();
            imperfectInfoVO.setOrganId(recipeBeforeOrder.getOrganId());
            imperfectInfoVO.setRecipeCode(recipeBeforeOrder.getRecipeCode());
            imperfectInfoVO.setImperfectFlag(recipeBeforeOrder.getIsReady());
            imperfectInfoVO.setRecipeId(recipeBeforeOrder.getRecipeId());
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeBeforeOrder.getRecipeId());
            if (recipeExtend != null) {
                imperfectInfoVO.setRecipeCostNumber(recipeExtend.getRecipeCostNumber());
            }
            Recipe recipe = recipeDAO.getByRecipeId(recipeBeforeOrder.getRecipeId());
            if (!new Integer(3).equals(recipe.getWriteHisState())) {
                // 如果处方没写入his,视为未完善
                logger.info("RecipeOrderBusinessService batchGetImperfectFlag WriteHisState={}", recipe.getWriteHisState());
                imperfectInfoVO.setImperfectFlag(0);
            }
            imperfectInfoVOS.add(imperfectInfoVO);
        });

        //处理不存在预订单信息中的处方（例：线下处方）
        Map<String, Integer> collectMap = recipeBeans.stream().collect(Collectors.toMap(com.ngari.recipe.recipe.model.RecipeBean::getRecipeCode, com.ngari.recipe.recipe.model.RecipeBean::getClinicOrgan));
        List<String> recipeCodeLists = recipeCodes.stream().filter(a -> !recipeCodeList.contains(a)).collect(Collectors.toList());
        recipeCodeLists.forEach(a -> {
            ImperfectInfoVO imperfectInfoVO = new ImperfectInfoVO();
            imperfectInfoVO.setOrganId(collectMap.get(a));
            imperfectInfoVO.setRecipeCode(a);
            imperfectInfoVO.setImperfectFlag(0);
            Recipe recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(a, collectMap.get(a));
            if(recipe != null){
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                if(recipeExtend != null){
                    imperfectInfoVO.setRecipeCostNumber(recipeExtend.getRecipeCostNumber());
                }
            }
            imperfectInfoVOS.add(imperfectInfoVO);
        });
        logger.info("batchGetImperfectFlag imperfectInfoVOS={}",JSONUtils.toString(imperfectInfoVOS));
        return imperfectInfoVOS;
    }

    @Override
    public String batchCheckSendAddressForOrder(List<CheckOrderAddressVo> checkOrderAddressVoList) {
        //0可以配送，1不能配送
        int flags = 0;
        String msg = "【";
        boolean bool = true;
        if (CollectionUtils.isNotEmpty(checkOrderAddressVoList)){
            for(CheckOrderAddressVo checkOrderAddressVo : checkOrderAddressVoList){
                Integer flag = enterpriseBusinessService.checkSendAddressForOrder(checkOrderAddressVo);
                if(new Integer(0).equals(flag)){
                    flags = 0;
                }else{
                    flags =  1;
                    bool = false;
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(checkOrderAddressVo.getEnterpriseId());
                    msg = msg.equals("【") ? (msg + drugsEnterprise.getName() + "】") : (msg + "【" + drugsEnterprise.getName() + "】");
                }
            }
        }
        if(bool){
            return String.valueOf(flags);
        }else {
            return msg + "不支持本收获地址";
        }
    }

    @Override
    public ImperfectInfoVO getImperfectInfo(RecipeBean recipeBean) {
        logger.info("RecipeOrderBusinessService getImperfectInfo recipeBean={}",JSONUtils.toString(recipeBean));
        ImperfectInfoVO imperfectInfoVO = new ImperfectInfoVO();
        Integer imperfectFlag = getImperfectFlag(recipeBean);
        imperfectInfoVO.setImperfectFlag(imperfectFlag);
        Recipe recipe = null;
        if (Objects.nonNull(recipeBean.getRecipeId())) {
            recipe = recipeDAO.getByRecipeId(recipeBean.getRecipeId());
        } else {
            recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeBean.getRecipeCode(), recipeBean.getClinicOrgan());
        }
        if(Objects.isNull(recipe)){
            imperfectInfoVO.setImperfectFlag(0);
            return imperfectInfoVO;
        }
        if (!new Integer(3).equals(recipe.getWriteHisState())) {
            // 如果处方没写入his,视为未完善
            logger.info("RecipeOrderBusinessService getImperfectInfo WriteHisState={}",recipe.getWriteHisState());
            imperfectInfoVO.setImperfectFlag(0);
        }
        if(recipe != null){
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if(recipeExtend != null){
                imperfectInfoVO.setRecipeCostNumber(recipeExtend.getRecipeCostNumber());
            }
        }
        logger.info("RecipeOrderBusinessService getImperfectInfo ImperfectInfoVO={}", JSONUtils.toString(imperfectInfoVO));
        return imperfectInfoVO;
    }

    @Override
    public Integer getRecipeRefundCount(RecipeRefundInfoReqVO recipeRefundCountVO) {
        logger.info("RecipeOrderBusinessService getRecipeRefundCount recipeRefundCountVO={}", JSONUtils.toString(recipeRefundCountVO));
        return recipeDAO.getRecipeRefundCount(recipeRefundCountVO.getDoctorId(), recipeRefundCountVO.getStartTime(), recipeRefundCountVO.getEndTime());
    }

    @Override
    public List<RecipeOrder> orderListByClinicId(Integer clinicId, Integer bussSource) {
        return orderManager.orderListByClinicId(clinicId, bussSource);
    }

    @Override
    public String logisticsOrderNo(String orderCode) {
        return infraClient.logisticsOrderNo(orderCode);
    }

    @Override
    public void patientFinishOrder(String orderCode) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (null == recipeOrder) {
            throw new DAOException("没有查询到订单信息");
        }
        if (OrderStateEnum.PROCESS_STATE_DISPENSING.getType().equals(recipeOrder.getProcessState())) {
            throw new DAOException("当前订单已完成，不允许再次更新");
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        Date finishDate = new Date();
        boolean isSendFlag = false;
        if (!RecipeSupportGiveModeEnum.SUPPORT_TFDS.getText().equals(recipeOrder.getGiveModeKey())) {
            isSendFlag = true;
        }
        //更新处方
        recipeManager.finishRecipes(recipeIdList, finishDate);
        //更新订单完成
        recipeOrder.setEffective(1);
        recipeOrder.setPayFlag(PayFlagEnum.PAYED.getType());
        recipeOrder.setFinishTime(finishDate);
        recipeOrder.setStatus(RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType());
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
        syncFinishOrderHandle(recipeIdList, recipeOrder, isSendFlag);
        stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_DISPENSING, OrderStateEnum.SUB_DONE_SEND);

    }

    @Override
    public void finishRecipeOrderJob() {
        // 获取所有 有 配送中订单 的机构
        List<Integer> organIds = recipeOrderDAO.getOrganIdByStatus();
        if (CollectionUtils.isEmpty(organIds)) {
            return;
        }
        for (Integer organId : organIds) {
            logger.info("开始执行完成订单定时任务 执行机构id=" + organId);
            Integer recipeAutoFinishTime = configurationClient.getValueCatchReturnInteger(organId, "recipeAutoFinishTime", 14);
            if (new Integer(0).equals(recipeAutoFinishTime)) {
                continue;
            }
            Date date = DateUtils.addDays(new Date(), -recipeAutoFinishTime);
            List<RecipeOrder> recipeOrders = recipeOrderDAO.findByOrganIdAndStatus(organId, date);
            if (CollectionUtils.isEmpty(recipeOrders)) {
                continue;
            }
            recipeOrders.forEach(recipeOrder -> {
                try {
                    patientFinishOrder(recipeOrder.getOrderCode());
                } catch (Exception e) {
                    logger.info("完成处方失败 orderCode=" + recipeOrder.getOrderCode());
                }
            });
            logger.info("完成订单定时任务结束 执行机构id=" + organId);

        }

    }

    @Override
    public void submitRecipeHisV1(List<Integer> recipeIds) {
        AtomicReference<Boolean> msgFlag = new AtomicReference<>(false);
        //推送his
        recipeIds.forEach(recipeId -> {
            logger.info("submitRecipeHisV1 pushRecipe recipeId={}", recipeId);
            RecipeInfoDTO recipePdfDTO = recipeTherapyManager.getRecipeTherapyDTO(recipeId);
            Recipe recipe = recipePdfDTO.getRecipe();
            if (!RecipeStateEnum.PROCESS_STATE_ORDER.getType().equals(recipe.getProcessState())) {
                logger.info("RecipeBusinessService pushRecipe 当前处方不是待下单状态");
                return ;
            }
            if (new Integer(3).equals(recipe.getWriteHisState())) {
                logger.info("RecipeBusinessService pushRecipe 当前处方已写入his");
                return ;
            }
            //同时set最小售卖单位/单位HIS编码等
            organDrugListManager.setDrugItemCode(recipe.getClinicOrgan(), recipePdfDTO.getRecipeDetails());
            Map<Integer, PharmacyTcm> pharmacyIdMap = pharmacyManager.pharmacyIdMap(recipe.getClinicOrgan());
            RecipeBeforeOrder orderByRecipeId = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(recipeId);
            try {
                RecipeInfoDTO result = hisRecipeManager.pushRecipe(recipePdfDTO, CommonConstant.RECIPE_PUSH_TYPE, pharmacyIdMap, CommonConstant.RECIPE_PATIENT_TYPE, orderByRecipeId.getGiveModeKey(), null);
                logger.info("submitRecipeHisV1 pushRecipe result={}", ngari.openapi.util.JSONUtils.toString(result));
                result.getRecipe().setBussSource(recipe.getBussSource());
                result.getRecipe().setClinicId(recipe.getClinicId());
                recipeManager.updatePushHisRecipe(result.getRecipe(), recipeId, CommonConstant.RECIPE_PUSH_TYPE);
                recipeManager.updatePushHisRecipeExt(result.getRecipeExtend(), recipeId, CommonConstant.RECIPE_PUSH_TYPE);
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_ORDER, RecipeStateEnum.SUB_ORDER_READY_SUBMIT_ORDER);
                beforeOrderManager.updateRecipeHisStatus(result.getRecipe(),recipe.getClinicOrgan(),recipeId,CommonConstant.RECIPE_PUSH_TYPE);
                logger.info("submitRecipeHisV1 pushRecipe end recipeId:{}", recipeId);
            } catch (Exception e) {
                logger.error("submitRecipeHisV1 pushRecipe error,sysType={},recipeId:{}", CommonConstant.RECIPE_PATIENT_TYPE, recipeId, e);
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "当前处方推送his失败:" + e.getMessage());
                msgFlag.set(true);
            }
            createPdfFactory.updateCodePdfExecute(recipeId);
        });
        if (msgFlag.get()) {
            throw new DAOException(609, "锁定失败请重新锁定");
        }
    }

    @Override
    public Boolean interceptPatientApplyRefund(String orderCode){
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (Objects.isNull(recipeOrder.getEnterpriseId())) {
            return true;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrder.getEnterpriseId());
        if (!DrugEnterpriseConstant.LOGISTICS_PLATFORM.equals(drugsEnterprise.getLogisticsType())) {
            return true;
        }
        if (StringUtils.isEmpty(recipeOrder.getTrackingNumber())) {
            return true;
        }
        List<RecipeOrder> recipeOrderList = recipeOrderDAO.findRecipeOrderByLogisticsCompanyAndTrackingNumber(recipeOrder.getLogisticsCompany(), recipeOrder.getTrackingNumber());
        List<Integer> orderProcessStateList = Arrays.asList(OrderStateEnum.PROCESS_STATE_ORDER_PLACED.getType(), OrderStateEnum.PROCESS_STATE_ORDER.getType(), OrderStateEnum.PROCESS_STATE_DISPENSING.getType());
        List<RecipeOrder> recipeOrders = recipeOrderList.stream().filter(order->!orderCode.equals(order.getOrderCode())).filter(order->orderProcessStateList.contains(order.getProcessState())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(recipeOrders)) {
            return false;
        }
        //查询该物流是否揽件
        return infraClient.cancelLogisticsOrder(recipeOrder, false);
    }

    @Override
    public void findHisSettle() {
        String hisSettleOrgan = recipeParameterDao.getByName("his_settle_zhuji");
        if (StringUtils.isEmpty(hisSettleOrgan)) {
            return;
        }
        List<Integer> organIds = JSONUtils.parse(hisSettleOrgan, ArrayList.class);
        organIds.forEach(organId -> {
            List<RecipeOrder> list = recipeOrderDAO.findByOrganIdAndPayStatus(organId);
            List<String> orderCodes = orderManager.hisSettleByOrder(list);
            if (CollectionUtils.isNotEmpty(orderCodes)) {
                orderCodes.forEach(orderCode -> {
                    recipeOrderService.finishOrderPay(orderCode, PayConstant.PAY_FLAG_PAY_SUCCESS, RecipeConstant.PAYMODE_ONLINE);
                });
            }
        });

    }

    @Override
    public Integer checkOrderPayState(Integer orderId){
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(orderId);
        if (Objects.nonNull(recipeOrder) && StringUtils.isEmpty(recipeOrder.getOutTradeNo())) {
            return 0;
        }
        Integer payQuery = payClient.payQuery(orderId);
        // 需要查询是否在线下已经支付
        if (new Integer(0).equals(payQuery)) {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            for (Integer recipeId : recipeIdList) {
                Integer query = recipeHisService.getRecipeSinglePayStatusQuery(recipeId);
                if (query != null && (query == eh.cdr.constant.RecipeStatusConstant.HAVE_PAY || query == eh.cdr.constant.RecipeStatusConstant.FINISH)) {
                    payQuery = 2;
                }
            }
        }
        return payQuery;
    }

    @Override
    public List<OrderPharmacyVO> getPharmacyByOrderCode(String orderCode) {
        RecipeOrder order = recipeOrderDAO.getByOrderCode(orderCode);
        if (Objects.isNull(order)) {
            logger.info("getPharmacyByOrderCode 订单信息不存在 orderCode={}", orderCode);
            return null;
        }
        if (StringUtils.isEmpty(order.getRecipeIdList())) {
            logger.info("getPharmacyByOrderCode 订单没有绑定处方信息 orderCode={}", orderCode);
            return null;
        }
        List<Integer> recipeIdList = JSONUtils.parse(order.getRecipeIdList(), List.class);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIdList);
        if (CollectionUtils.isEmpty(recipeDetails)){
            logger.info("getPharmacyByOrderCode 订单没有药品详细信息 orderCode={}", orderCode);
            return null;
        }
        List<OrderPharmacyVO> pharmacyVOS = recipeDetails.stream().map(recipeDetail -> {
            OrderPharmacyVO orderPharmacyVO = new OrderPharmacyVO();
            BeanCopyUtils.copy(recipeDetail, orderPharmacyVO);
            return orderPharmacyVO;
        }).filter(e -> Objects.nonNull(e.getPharmacyId())).filter(distinctByKey(e -> e.getPharmacyId())).collect(Collectors.toList());
        return pharmacyVOS;
    }

    private void syncFinishOrderHandle(List<Integer> recipeIdList, RecipeOrder recipeOrder, boolean isSendFlag) {
        logger.info("syncFinishOrderHandle recipeIdList:{}, recipeOrder:{}", recipeIdList, JSON.toJSONString(recipeOrder));
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIdList);
        RecipeBusiThreadPool.execute(() -> {
            recipeList.forEach(recipe -> {
                stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_SEND);
                //HIS消息发送
                hisService.recipeFinish(recipe.getRecipeId());
                //更新pdf
                CommonOrder.finishGetDrugUpdatePdf(recipe.getRecipeId());
                HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                if (isSendFlag) {
                    hisSyncService.uploadFinishMedicine(recipe.getRecipeId());
                } else {
                    SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                    syncExecutorService.uploadRecipeVerificationIndicators(recipe.getRecipeId());
                }
            });
            Recipe recipe = recipeList.get(0);
            if (isSendFlag) {
                //配送到家
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
            } else {
                //发送取药完成消息
                RecipeMsgService.batchSendMsg(recipe.getRecipeId(), RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH);
            }
        });
    }

    @Override
    public Boolean updateInvoiceStatus(String orderCode, Integer invoiceType) {
        logger.info("updateInvoiceStatus orderCode:{},invoiceType:{}", orderCode,invoiceType);
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (recipeOrder == null){
            logger.info("updateInvoiceStatus 当前订单不存在 orderCode:{}", orderCode);
            return false;
        }
        logger.info("updateInvoiceStatus recipeOrder:{}", JSONUtils.toString(recipeOrder));
        if(new Integer(1).equals(invoiceType)){
            if(recipeOrder.getPrintDrugDistributionListFlag() == null || !recipeOrder.getPrintDrugDistributionListFlag()){
                recipeOrder.setPrintDrugDistributionListFlag(true);
                recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
            }
        }
        else if(new Integer(2).equals(invoiceType)){
            IWaybillService iWaybillService = AppContextHolder.getBean("infra.waybillService", IWaybillService.class);
            if(recipeOrder.getPrintExpressBillFlag() == null || !recipeOrder.getPrintExpressBillFlag()){
                recipeOrder.setPrintExpressBillFlag(true);
                recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
                iWaybillService.updatePrintStatus(orderCode);
            }
        }
        return true;
    }

    @Override
    public String refundResultNotify(RefundResultNotifyVO refundResultNotifyVO) {
        Recipe recipe;
        if (Objects.nonNull(refundResultNotifyVO.getRecipeId())) {
            recipe = recipeDAO.getByRecipeId(refundResultNotifyVO.getRecipeId());
        } else {
            recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(refundResultNotifyVO.getRecipeCode(), refundResultNotifyVO.getOrganId());
        }
        if (Objects.isNull(recipe) || StringUtils.isEmpty(recipe.getOrderCode())) {
            return "-1";
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (Objects.isNull(recipeOrder)) {
            return "-1";
        }
        recipeOrderService.finishOrderPayByRefund(recipeOrder.getOrderCode(), refundResultNotifyVO.getRefundState(), RecipeConstant.PAYMODE_ONLINE, refundResultNotifyVO.getRefundNo());
        StringBuilder memo = new StringBuilder("订单=" + recipeOrder.getOrderCode() + " ");
        Integer targetPayFlag = refundResultNotifyVO.getRefundState();
        switch (targetPayFlag) {
            case 3:
                memo.append("退款成功");
                break;
            case 4:
                memo.append("退款失败");
                break;
            case 5:
                memo.append("退费审核不通过");
                break;
            default:
                memo.append("支付 未知状态，payFlag:").append(targetPayFlag);
                break;
        }
        if (StringUtils.isNotEmpty(recipeOrder.getRecipeIdList())) {
            List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
            if (5 == targetPayFlag) {
                RecipeRefund recipeRefund = new RecipeRefund();
                recipeRefund.setTradeNo(recipeOrder.getTradeNo());
                recipeRefund.setPrice(recipeOrder.getActualPrice());
                recipeRefund.setStatus(2);
                recipeRefund.setNode(RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_THIRD);
                recipeRefund.setReason(refundResultNotifyVO.getRemark());
                orderFeeManager.recipeReFundSave(recipeOrder.getOrderCode(), recipeRefund);
                //退费审核不通过 需要看是否管理员可强制退费
                Boolean forceRecipeRefundFlag = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "forceRecipeRefundFlag", false);
                if (forceRecipeRefundFlag) {
                    //表示配置管理员可强制
                    recipeRefund.setStatus(0);
                    recipeRefund.setReason("");
                    recipeRefund.setNode(RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_ADMIN);
                    orderFeeManager.recipeReFundSave(recipeOrder.getOrderCode(), recipeRefund);
                }
            }
            if (CollectionUtils.isNotEmpty(recipeIdList)) {
                Integer recipeId = recipeIdList.get(0);
                //调用回调处方退费
                recipeOrderService.refundCallback(recipeId, targetPayFlag, recipeOrder.getOrderId(), PayBusTypeEnum.RECIPE_BUS_TYPE.getType(),refundResultNotifyVO.getRefundAmount());
            }
        }
        //更新处方日志
        recipeLogDAO.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo.toString());
        return "000";
    }

    @Override
    public Date getRevisitRemindTime(List<Integer> recipeIds) {
        try {
            List<Date> remindDates = new ArrayList<>();
            Date sourceTime = new Date();
            List<String> revisitRementAppointDepart =new ArrayList<>();
            List<Recipe> recipes = recipeDAO.findByRecipeIds(recipeIds);
            if(CollectionUtils.isEmpty(recipes)){
                return null;
            }
            List<Recipe> tcmRecipeList = recipes.stream().filter(recipe -> RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType())).collect(Collectors.toList());
            List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIds);
            //获取长处方的处方单号
            List<Integer> longRecipeIds = recipeExtendList.stream().filter(recipeExtend -> "1".equals(recipeExtend.getIsLongRecipe())).map(RecipeExtend::getRecipeId).collect(Collectors.toList());
            //获取全部的处方明细
            List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIds(recipeIds);
            logger.info("getRevisitRemindTime recipeDetailList：{}", JSON.toJSONString(recipeDetailList));
            if (CollectionUtils.isNotEmpty(tcmRecipeList)) {
                //中药处方 1帖=1天
                for (Recipe tcmRecipe : tcmRecipeList) {
                    for (Recipedetail recipeDetail : recipeDetailList) {
                        if (tcmRecipe.getRecipeId().equals(recipeDetail.getRecipeId())) {
                            recipeDetail.setUseDays(tcmRecipe.getCopyNum());
                        }
                    }
                }
                logger.info("getRevisitRemindTime convert recipeDetailList:{}", JSON.toJSONString(recipeDetailList));
            }
            Map<Integer, List<Recipedetail>> recipeDetailMap = recipeDetailList.stream().collect(Collectors.groupingBy(Recipedetail::getRecipeId));
            String config = configurationClient.getValueCatch(recipes.get(0).getClinicOrgan(), "revisitRemindNotify", "");
            if(StringUtils.isNotEmpty(config)){
                revisitRementAppointDepart=Arrays.asList(config.split(","));
            }
            logger.info("getRevisitRemindTime revisitRementAppointDepart:{}", JSON.toJSONString(revisitRementAppointDepart));
            Integer recipeId=recipes.get(0).getRecipeId();
            List<String> finalRevisitRementAppointDepart = revisitRementAppointDepart;
            recipes.forEach(recipe -> {
                if(CollectionUtils.isEmpty(finalRevisitRementAppointDepart)
                        || (CollectionUtils.isNotEmpty(finalRevisitRementAppointDepart) && !finalRevisitRementAppointDepart.contains(recipe.getAppointDepart()))){
                    return;
                }
                List<Recipedetail> recipeDetails = recipeDetailMap.get(recipe.getRecipeId());
                //筛选出用药天数大于4天的最小日期
                recipeDetails = recipeDetails.stream().filter(x -> x.getUseDays() > 4).collect(Collectors.toList());
                Recipedetail minRecipeDetail = recipeDetails.stream().min(Comparator.comparing(Recipedetail::getUseDays)).orElse(null);
                if (null == minRecipeDetail) {
                    return;
                }
                LocalDateTime payDate = Instant.ofEpochMilli(sourceTime.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                //三种推送时间方案，按订单号除以3取余
                int pushMode = recipeId % 3 + 1;
                switch (pushMode) {
                    case 1:
                        //方案一：长处方提前1天和3天， 非长处方提前1天
                        remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),1));
                        if (longRecipeIds.contains(recipe.getRecipeId())) {
                            remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),3));
                        }
                        break;
                    case 2:
                        //方案二：长处方提前2天和4天， 非长处方提前2天
                        remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),2));
                        if (longRecipeIds.contains(recipe.getRecipeId())) {
                            remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),4));
                        }
                        break;
                    case 3:
                        //方案三：长处方提前3天和5天， 非长处方提前3天
                        remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),3));
                        if (longRecipeIds.contains(recipe.getRecipeId())) {
                            remindDates.add(DateConversion.minusDays(payDate,minRecipeDetail.getUseDays(),5));
                        }
                        break;
                    default:
                        break;
                }
            });
            if(CollectionUtils.isEmpty(remindDates)){
                return null;
            }
            remindDates.sort(Comparator.naturalOrder());
            //取最小日期返回
            return remindDates.get(0);
        } catch (DAOException e) {
            e.printStackTrace();
            logger.error("getRevisitRemindTime",e);
            return null;
        }
    }

    @Override
    public LogisticsMergeVO mergeTrackingNumber(Integer addressId, Integer enterpriseId, List<Integer> recipeIdList) {
        AddressService addressService = ApplicationUtils.getBasicService(AddressService.class);
        AddressDTO address;
        LogisticsMergeVO logisticsMerge = new LogisticsMergeVO();
        if (Objects.isNull(addressId)) {
            address = addressService.getDefaultAddressDTO();
        } else {
            address = addressService.getByAddressId(addressId);
        }
        RecipeOrder recipeOrder = new RecipeOrder("");
        recipeOrder.setEnterpriseId(enterpriseId);
        recipeOrder.setRecipeIdList(JSONUtils.toString(recipeIdList));
        recipeOrder.setAddressID(address.getAddressId());
        recipeOrder.setAddress1(address.getAddress1());
        recipeOrder.setAddress2(address.getAddress2());
        recipeOrder.setAddress3(address.getAddress3());
        if (StringUtils.isNotEmpty(address.getAddress5Text())) {
            recipeOrder.setAddress4(address.getAddress5Text() + address.getAddress4());
        } else {
            recipeOrder.setAddress4(address.getAddress4());
        }
        recipeOrder.setStreetAddress(address.getStreetAddress());
        recipeOrder.setRecMobile(address.getRecMobile());
        recipeOrder.setReceiver(address.getReceiver());
        RecipeOrder order = orderManager.getMergeTrackingNumber(recipeOrder);
        if (Objects.nonNull(order)) {
            logisticsMerge.setLogisticsMergeFlag(true);
            logisticsMerge.setLogisticsCompany(order.getLogisticsCompany());
            String logisticsCompanyName = DictionaryUtil.getDictionary("eh.infra.dictionary.LogisticsCode", order.getLogisticsCompany());
            logisticsMerge.setLogisticsCompanyName(logisticsCompanyName);
            return logisticsMerge;
        }
        logisticsMerge.setLogisticsMergeFlag(false);
        return logisticsMerge;
    }

    @Override
    public HlwTbParamReq getHlwYbInfo(Integer busId) {
        RecipeOrder recipeOrder = recipeOrderDAO.get(busId);
        if (Objects.isNull(recipeOrder)) {
            return null;
        }
        List<Integer> recipeIdList = JSONUtils.parse(recipeOrder.getRecipeIdList(), List.class);
        HisSettleReqDTO settleReqDTO = recipeManager.getHisOrderCode(recipeOrder.getOrganId(), recipeIdList);
        HlwTbParamReq hlwTbParamReq = new HlwTbParamReq();
        hlwTbParamReq.setHisBusId(settleReqDTO.getHisBusId());
        hlwTbParamReq.setYbId(settleReqDTO.getYbId());
        return hlwTbParamReq;
    }

    @Override
    public boolean orderRefund(String orderCode) {
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(orderCode);
        if (Objects.isNull(recipeOrder)) {
            return false;
        }
        recipeOrder.setOrderRefundWay(OrderRefundWayTypeEnum.DRUG_ORDER.getType());
        recipeOrderDAO.updateNonNullFieldByPrimaryKey(recipeOrder);
        List<Integer> recipeIdList = recipeManager.setRecipeCancelState(recipeOrder);
        if (recipeOrder.getActualPrice() <= 0.0D) {
            recipeIdList.forEach(recipeId -> {
                stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_RETURN_DRUG);
            });
            orderManager.setOrderCancelState(recipeOrder);
            stateManager.updateOrderState(recipeOrder.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_RETURN_DRUG);
            return true;
        }
        RecipeOrderPayFlow recipeOtherOrderPayFlow = recipeOrderPayFlowManager.getByOrderIdAndType(recipeOrder.getOrderId(), PayFlowTypeEnum.RECIPE_AUDIT.getType());
        if (Objects.nonNull(recipeOtherOrderPayFlow)) {
            RefundResultDTO resultDTO = payClient.refund(recipeOrder.getOrderId(), PayBusTypeEnum.OTHER_BUS_TYPE.getName());
            if (Objects.isNull(resultDTO) || resultDTO.getStatus() != 0) {
                return false;
            }
        }
        RefundResultDTO refundResultDTO = payClient.refund(recipeOrder.getOrderId(), PayBusTypeEnum.RECIPE_BUS_TYPE.getName());
        if (Objects.isNull(refundResultDTO) || refundResultDTO.getStatus() != 0) {
            return false;
        }
        RecipeRefund recipeRefund = new RecipeRefund();
        recipeRefund.setTradeNo(recipeOrder.getTradeNo());
        recipeRefund.setPrice(recipeOrder.getActualPrice());
        recipeRefund.setNode(RecipeRefundRoleConstant.RECIPE_REFUND_ROLE_PATIENT);
        recipeRefund.setStatus(0);
        recipeRefund.setBusId(recipeIdList.get(0));
        recipeRefund.setApplyTime(new Date());
        recipeRefund.setCheckTime(new Date());
        recipeRefundDAO.saveRefund(recipeRefund);
        RecipeMsgService.batchSendMsg(recipeIdList.get(0), RecipeStatusConstant.RECIPE_REFUND_SUCC);
        return true;
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
