package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
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
import com.ngari.patient.dto.AddressDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.AddressService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.InvoiceInfoResTO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
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
import ctd.util.JSONUtils;
import easypay.entity.vo.param.bus.SelfPreSettleQueryReq;
import eh.entity.bus.pay.BusTypeEnum;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.RecipePayModeSupportBean;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.*;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.status.PayModeEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.enumerate.type.NeedSendTypeEnum;
import recipe.factory.status.givemodefactory.GiveModeProxy;
import recipe.hisservice.RecipeToHisService;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.manager.RecipeManager;
import recipe.presettle.IRecipePreSettleService;
import recipe.presettle.factory.PreSettleFactory;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.third.IFileDownloadService;
import recipe.util.*;
import recipe.util.LocalStringUtil;
import recipe.util.ObjectCopyUtils;
import recipe.vo.ResultBean;
import recipe.vo.base.BaseRecipeDetailVO;
import recipe.vo.greenroom.InvoiceRecordVO;
import recipe.vo.second.CabinetVO;
import recipe.vo.second.enterpriseOrder.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方订单处理实现类 （新增）
 *
 * @author fuzi
 */
@Service
public class RecipeOrderBusinessService implements IRecipeOrderBusinessService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
                        baseRecipeDetailVO.setSaleDrugCode(saleDrugList.getSaleDrugCode());
                    }
                    if (null != recipeDetail.getActualSalePrice()) {
                        recipeFee = recipeFee.add(recipeDetail.getActualSalePrice().multiply(new BigDecimal(recipeDetail.getUseTotalDose())).setScale(4, BigDecimal.ROUND_HALF_UP)).setScale(2, BigDecimal.ROUND_HALF_UP);
                        baseRecipeDetailVO.setSalePrice(recipeDetail.getActualSalePrice());
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
        order.setRecipeIdList(JSONUtils.toString(Arrays.asList(recipe.getRecipeId())));
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

        //患者手机号
        if(effectiveFlag && !StringUtils.isEmpty(recipe.getMpiid())){
            com.ngari.patient.dto.PatientDTO patientDTO=patientClient.getPatientDTOByMpiId(recipe.getMpiid());
            cabinetVO.setMobile(patientDTO==null?"":patientDTO.getMobile());
        }
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

            List<String> recipeCodeS = recipeList.stream().map(Recipe::getRecipeCode).collect(Collectors.toList());
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
                    String totalAmount = hisResult.getData().getZje();
                    String accountBalance = hisResult.getData().getZhye();
                    String rechargeAmount = hisResult.getData().getZhzf();
                    //his收据号
                    String hisSettlementNo = hisResult.getData().getSjh();
                    selfPreSettleQueryReq.setOrganId(recipe.getClinicOrgan());
                    selfPreSettleQueryReq.setOutTradeNo(recipeOrder.getOutTradeNo());
                    //获取就诊卡号--一般来说处方里已经保存了复诊里的就诊卡号了取不到再从复诊里取
                    selfPreSettleQueryReq.setMrn(getMrnForRecipe(recipe));
                    selfPreSettleQueryReq.setClinicNo(String.valueOf(recipe.getClinicId()));
                    selfPreSettleQueryReq.setHisSettlementNo(hisSettlementNo);
                    selfPreSettleQueryReq.setTotalAmount(new BigDecimal(totalAmount));
                    selfPreSettleQueryReq.setAccountBalance(new BigDecimal(accountBalance));
                    selfPreSettleQueryReq.setRechargeAmount(new BigDecimal(rechargeAmount));
                    selfPreSettleQueryReq.setRecipeNos(join);
                    selfPreSettleQueryReq.setIsInHosPay(supportRecipeRechargeFlag);
                }

            }
        } catch (Exception e) {
            logger.error("selfPreSettleQueryInfo busId={} error", busId, e);
        }
        return selfPreSettleQueryReq;
    }

    @Override
    public ThirdOrderPreSettleRes thirdOrderPreSettle(ThirdOrderPreSettleReq thirdOrderPreSettleReq) {
        ThirdOrderPreSettleRes thirdOrderPreSettleRes = new ThirdOrderPreSettleRes();
        checkParams(thirdOrderPreSettleReq);
        setUrtToContext(thirdOrderPreSettleReq.getAppkey(), thirdOrderPreSettleReq.getTid());
        String mpiId = getOwnMpiId();
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
        RecipeExtend extend = recipeExtendDAO.getByRecipeId(recipes.get(0).getRecipeId());
        if (extend == null) {
            logger.info("ThirdOrderPreSettle extend is null");
            throw new DAOException(609,"补充信息不存在");
        }
        if (!RecipeBussConstant.PAYMODE_ONLINE.equals(recipeOrder.getPayMode())) {
            logger.info("ThirdOrderPreSettle no support. recipeId={}", JSONUtils.toString(recipes.get(0).getRecipeId()));
            throw new DAOException(609,"不是线上支付订单");
        }
        Integer depId = recipeOrder.getEnterpriseId();
        Integer orderType = recipeOrder.getOrderType() == null ? 0 : recipeOrder.getOrderType();
        String insuredArea = extend.getInsuredArea();
        Map<String, Object> param = com.google.common.collect.Maps.newHashMap();
        param.put("depId", depId);
        param.put("insuredArea", insuredArea);
        param.put("recipeNoS", JSONUtils.toString(recipeNoS));
        param.put("payMode", recipeOrder.getPayMode());
        param.put("recipeIds", recipes.get(0).getRecipeId());
        //获取对应预结算服务
        IRecipePreSettleService preSettleService = PreSettleFactory.getPreSettleService(recipes.get(0).getClinicOrgan(),orderType);
        if (preSettleService != null){
            Map<String, Object> map = preSettleService.recipePreSettle(recipes.get(0).getRecipeId(), param);
            thirdOrderPreSettleRes.setPreSettleTotalAmount(map.get("preSettleTotalAmount").toString());
            thirdOrderPreSettleRes.setCashAmount(map.get("cashAmount").toString());
            thirdOrderPreSettleRes.setFundAmount(map.get("hisSettlementNo").toString());
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
     * @param organId
     * @param recipeCode
     * @return
     */
    @Override
    public Integer getImperfectFlag(Integer organId, String recipeCode) {
        logger.info("getImperfectFlag organId={},recipeCode={}",organId,recipeCode);
        RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getByOrganIdAndRecipeCode(organId, recipeCode);
        if(recipeBeforeOrder != null){
            return recipeBeforeOrder.getIsReady();
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
        return orderManager.getShoppingCartDetail(mpiId);
    }

    @Override
    public void saveRecipeBeforeOrderInfo(ShoppingCartReqVO shoppingCartReqVO) {
        logger.info("saveRecipeBeforeOrderInfo shoppingCartReqVO={}",JSONUtils.toString(shoppingCartReqVO));
        orderManager.saveRecipeBeforeOrderInfo(Objects.requireNonNull(ObjectCopyUtils.convert(shoppingCartReqVO, ShoppingCartReqDTO.class)));
    }

    @Override
    public String improvePreOrderInfo(PreOrderInfoReqVO preOrderInfoReqVO) {
        logger.info("improvePreOrderInfo preOrderInfoReqVO={}",JSONUtils.toString(preOrderInfoReqVO));
        if(preOrderInfoReqVO.getRecipeId() != null){
            Recipe recipe = recipeDAO.getByRecipeId(preOrderInfoReqVO.getRecipeId());
            if(recipe != null){
                if(new Integer(1).equals(recipe.getReviewType()) && new Integer(8).equals(recipe.getStatus())){
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "处方待药师审核");
                }
                else {
                    RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(preOrderInfoReqVO.getRecipeId());
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
        return "success";
    }

    @Override
    public Boolean getPreOrderFlag(Integer recipeId) {
        RecipeBeforeOrder recipeBeforeOrder = recipeBeforeOrderDAO.getRecipeBeforeOrderByRecipeId(recipeId);
        if(Objects.nonNull(recipeBeforeOrder)){
            return true;
        }
        return false;
    }
}
