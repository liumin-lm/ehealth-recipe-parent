package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.common.dto.CheckRequestCommonOrderItemDTO;
import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.bean.QueryResult;
import ctd.util.JSONUtils;
import eh.entity.bus.pay.BusTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DoctorClient;
import recipe.client.OrganClient;
import recipe.client.PatientClient;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.enumerate.type.NeedSendTypeEnum;
import recipe.factory.status.givemodefactory.GiveModeProxy;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;
import recipe.util.ObjectCopyUtils;
import recipe.vo.ResultBean;
import recipe.vo.base.BaseRecipeDetailVO;
import recipe.vo.second.enterpriseOrder.*;

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
    @Autowired
    private RecipeDAO recipeDAO;
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
    private RecipeParameterDao parameterDao;

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
            receiverInfo.setStreet(street);
            receiverInfo.setAddress(recipeOrder.getAddress4());
            receiverInfo.setProvinceCode(StringUtils.isNotEmpty(recipeOrder.getAddress1()) ? recipeOrder.getAddress1() + "0000" : "");
            receiverInfo.setCityCode(StringUtils.isNotEmpty(recipeOrder.getAddress2()) ? recipeOrder.getAddress2() + "00" : "");
            downRecipeOrderVO.setReceiverInfo(receiverInfo);
            //设置处方信息
            recipeList.forEach(recipe -> {
                DownRecipeVO downRecipeVO = new DownRecipeVO();
                List<BaseRecipeDetailVO> baseRecipeDetailVOList = new ArrayList<>();
                ObjectCopyUtils.copyProperties(downRecipeVO, recipe);
                downRecipeVO.setOrganId(recipe.getClinicOrgan());
                if (null != recipe.getCheckOrgan()) {
                    downRecipeVO.setCheckerName(recipe.getCheckerText());
                    com.ngari.recipe.dto.OrganDTO organDTO = organClient.organDTO(recipe.getCheckOrgan());
                    downRecipeVO.setCheckOrganName(organDTO.getName());
                }
                //设置签名图片的url
                String fileImgUrl = parameterDao.getByName("fileImgUrl");
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    downRecipeVO.setSignFileUrl(fileImgUrl + recipe.getChemistSignFile());
                } else {
                    downRecipeVO.setSignFileUrl(fileImgUrl + recipe.getSignFile());
                }
                //处方患者信息
                PatientDTO patient = patientClient.getPatientBeanByMpiId(recipe.getMpiid());
                logger.info("ThirdEnterpriseCallService.downLoadRecipes patient:{} .", JSONUtils.toString(patient));
                downRecipeVO.setBirthday(patient.getBirthday());
                downRecipeVO.setSexCode(patient.getPatientSex());
                try {
                    downRecipeVO.setSexName(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));
                } catch (ControllerException e) {
                    e.printStackTrace();
                }

                RecipeExtend recipeExtend = recipeExtendMap.get(recipe.getRecipeId());
                ObjectCopyUtils.copyProperties(downRecipeVO, recipeExtend);
                List<Recipedetail> recipeDetailListFromMap = recipeDetailListMap.get(recipe.getRecipeId());
                recipeDetailListFromMap.forEach(recipeDetail -> {
                    BaseRecipeDetailVO baseRecipeDetailVO = new BaseRecipeDetailVO();
                    ObjectCopyUtils.copyProperties(baseRecipeDetailVO, recipeDetail);
                    SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
                    if (null != saleDrugList) {
                        baseRecipeDetailVO.setSaleDrugCode(saleDrugList.getSaleDrugCode());
                    }
                    baseRecipeDetailVOList.add(baseRecipeDetailVO);
                });
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

}
