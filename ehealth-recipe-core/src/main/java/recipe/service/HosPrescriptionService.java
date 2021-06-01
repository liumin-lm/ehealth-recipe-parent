package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.clientconfig.service.IClientConfigService;
import com.ngari.base.clientconfig.to.ClientConfigBean;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.push.model.SmsInfoBean;
import com.ngari.base.push.service.ISmsPushService;
import com.ngari.base.qrinfo.model.BusQrInfoBean;
import com.ngari.base.qrinfo.model.QRInfoBean;
import com.ngari.base.qrinfo.service.INgariQrInfoService;
import com.ngari.patient.dto.ClientConfigDTO;
import com.ngari.patient.dto.OrganConfigDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.HospitalRecipe;
import com.ngari.recipe.hisprescription.model.*;
import com.ngari.recipe.hisprescription.service.IHosPrescriptionService;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.upload.service.IUrlResourceService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ClientConfigConstant;
import eh.base.constant.ErrorCode;
import eh.qrcode.constant.QRInfoConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.dao.HospitalRecipeDAO;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.medicationguide.service.MedicationGuideService;
import recipe.service.hospitalrecipe.PrescribeService;

import java.math.BigDecimal;
import java.util.*;

/**
 * 对接第三方医院服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/4/17.
 */
@RpcBean("hosPrescriptionService")
public class HosPrescriptionService implements IHosPrescriptionService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(HosPrescriptionService.class);

    @Autowired
    @Qualifier("remotePrescribeService")
    private PrescribeService prescribeService;

    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;

    /**
     * 接收第三方处方
     *
     * @param hospitalRecipeDTO 医院处方
     * @return 结果
     */
    @Override
    @RpcService
    public HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult<RecipeBean> result = prescribeService.createPrescription(hospitalRecipeDTO);
        Integer recipeId = null;
        if (HosRecipeResult.SUCCESS.equals(result.getCode())) {
            RecipeBean recipe = result.getData();
            recipeId = recipe.getRecipeId();
            HosRecipeResult orderResult = createBlankOrderForHos(recipe, hospitalRecipeDTO);
            if (HosRecipeResult.FAIL.equals(orderResult.getCode())) {
                result.setCode(HosRecipeResult.FAIL);
                result.setMsg(orderResult.getMsg());
            }
           /* //是否走外配模式 现根据distributionMode判断，1:支付宝外配 2:九州通外延
            if (RecipeBussConstant.WUCHANG_JZT.equals(hospitalRecipeDTO.getDistributionMode())){
                //没有库存就推送九州通
                drugsEnterpriseService.pushHosInteriorSupport(recipe.getRecipeId(),recipe.getClinicOrgan());
                //发送患者没库存消息
                RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_HOSSUPPORT_NOINVENTORY, ObjectCopyUtils.convert(recipe, Recipe.class));
                String memo = "医院保存没库存处方并推送九州通/发送无库存短信成功";
                //日志记录
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
            }*/
        }

        if (HosRecipeResult.DUPLICATION.equals(result.getCode())) {
            result.setCode(HosRecipeResult.SUCCESS);
        }
        RecipeBean backNew = new RecipeBean();
        backNew.setRecipeId(recipeId);
        result.setData(backNew);
        return result;
    }

    /**
     * 接收第三方流转处方（仅流转不在平台做任何功能）
     *
     * @param hospitalRecipeDTO 医院处方
     * @return 结果
     */
    @Override
    @RpcService
    public HosRecipeResult createTransferPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        if (hospitalRecipeDTO.getNoSaveRecipeFlag()) {
            ydRecipePushInfo(hospitalRecipeDTO);
        } else {
            HosRecipeResult<RecipeBean> result = prescribeService.createPrescription(hospitalRecipeDTO);
            Integer recipeId = null;
            if (HosRecipeResult.SUCCESS.equals(result.getCode())) {
                RecipeBean recipe = result.getData();
                recipeId = recipe.getRecipeId();
                //将流转处方推送给药企
                drugsEnterpriseService.pushHosInteriorSupport(recipe.getRecipeId(), recipe.getClinicOrgan());
                String memo = "医院流转处方推送药企成功";
                //日志记录
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
            }

            if (HosRecipeResult.DUPLICATION.equals(result.getCode())) {
                result.setCode(HosRecipeResult.SUCCESS);
            }
            RecipeBean backNew = new RecipeBean();
            backNew.setRecipeId(recipeId);
            result.setData(backNew);
            return result;
        }
        return null;
    }

    private HosRecipeResult ydRecipePushInfo(HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult<RecipeBean> result = new HosRecipeResult();
        LOG.info("PrescribeService-createPrescription hospitalRecipeDTO:{}.", JSONUtils.toString(hospitalRecipeDTO));
        //保存数据
        saveHospitalRecipe(hospitalRecipeDTO);
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        //说明不需要保存处方到平台可以直接传给药企
        //转换组织结构编码
        Integer clinicOrgan = null;
        OrganBean organ = null;
        try {
            organ = getOrganByOrganId(hospitalRecipeDTO.getOrganId());
            if (null != organ) {
                clinicOrgan = organ.getOrganId();
            }
        } catch (Exception e) {
            LOG.warn("createPrescription 查询机构异常，organId={}", hospitalRecipeDTO.getOrganId(), e);
        } finally {
            if (null == clinicOrgan) {
                LOG.warn("createPrescription 平台未匹配到该组织机构编码，organId={}", hospitalRecipeDTO.getOrganId());
                result.setMsg("平台未匹配到该组织机构编码");
                return result;
            }
        }
        //查询当前医院关联的药企
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(clinicOrgan, 1);
        if (CollectionUtils.isNotEmpty(drugsEnterprises)) {
            DrugsEnterprise drugsEnterprise = drugsEnterprises.get(0);
            DrugEnterpriseResult enterpriseResult = service.pushSingleRecipe(hospitalRecipeDTO, drugsEnterprise);
            if (enterpriseResult.getCode() == 1) {
                //表示推送药企成功,需要查询患者是否已经在平台注册
                PatientService patientService = BasicAPI.getService(PatientService.class);
                //需要根据机构和身份证查询当前机构所在的用户组是否存在该身份证的患者
                List<PatientDTO> patientDTOList = patientService.findByMobileAndIdCard("",hospitalRecipeDTO.getCertificate(), clinicOrgan);
                LOG.info("createPrescription patientDTOList :{}.", JSONUtils.toString(patientDTOList));
                if (CollectionUtils.isNotEmpty(patientDTOList)) {
                    //给该机构这个身份证的用户和绑定了这个就诊人的用户都推送微信消息
                    for (PatientDTO patient : patientDTOList) {
                        pushWechatTplForYd(hospitalRecipeDTO, organ, patient);
                    }
                } else {
                    //用户没有注册,需要给用户发送短信
                    if (StringUtils.isNotEmpty(hospitalRecipeDTO.getPatientTel())) {
                        pushDyInfoForYd(hospitalRecipeDTO);
                    } else {
                        LOG.info("PrescribeService-createPrescription 手机号为空无法发送短信, 姓名:{}, 身份证号:{}.", hospitalRecipeDTO.getPatientName(), hospitalRecipeDTO.getCertificate());
                    }
                }
            }
        }
        return result;
    }

    private void pushDyInfoForYd(HospitalRecipeDTO hospitalRecipeDTO) {
        SmsInfoBean smsInfo = new SmsInfoBean();
        smsInfo.setBusType("RecipeHisCreate");
        smsInfo.setSmsType("RecipeHisCreate");
        OrganBean organBean = getOrganByOrganId(hospitalRecipeDTO.getOrganId());
        smsInfo.setOrganId(organBean.getOrganId());
        smsInfo.setBusId(0);

        Map<String, Object> smsMap = Maps.newHashMap();
        smsMap.put("mobile", hospitalRecipeDTO.getPatientTel());

        smsInfo.setExtendValue(JSONUtils.toString(smsMap));
        ISmsPushService smsPushService = ApplicationUtils.getBaseService(ISmsPushService.class);
        smsPushService.pushMsgData2OnsExtendValue(smsInfo);
    }

    private void pushWechatTplForYd(HospitalRecipeDTO hospitalRecipeDTO, OrganBean organ, PatientDTO patientDTO) {
        String thirdUrl = getYdPushUrl(patientDTO, hospitalRecipeDTO.getRecipeCode(), hospitalRecipeDTO.getPatientId());
        //推送模板消息
        Map<String, Object> map = new HashMap<>();
        map.put("doctorName", hospitalRecipeDTO.getDoctorName());
        StringBuilder recipeType = new StringBuilder("西药");
        recipeType.append("\n");
        recipeType.append("开方时间：").append(hospitalRecipeDTO.getCreateDate()).append("\n");
        recipeType.append("开方医生：").append(hospitalRecipeDTO.getDoctorName());
        map.put("recipeType", recipeType);
        map.put("organId", organ.getOrganId());
        map.put("idCard", hospitalRecipeDTO.getCertificate());
        map.put("patientName", hospitalRecipeDTO.getPatientName());
        map.put("signTime", hospitalRecipeDTO.getCreateDate());
        map.put("appId", null);
        map.put("openId", null);
        map.put("url", thirdUrl);
        map.put("loginId", patientDTO.getLoginId());
        RecipeMsgService.sendRecipeThirdMsg(map);
    }

    public OrganBean getOrganByOrganId(String organId){
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        OrganBean organ = null;
        List<OrganBean> organList = organService.findByOrganizeCode(organId);
        if (CollectionUtils.isNotEmpty(organList)) {
            organ = organList.get(0);
        }

        return organ;
    }

    private String getYdPushUrl(PatientDTO patientDTO, String recipeNo, String patientId) {
        IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);
        List<PatientBean> patientBeans = iPatientService.findByMpiIdIn(Arrays.asList(patientDTO.getMpiId()));
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        return recipeService.getThirdUrlString(patientBeans, recipeNo, patientId);
    }

    private void saveHospitalRecipe(HospitalRecipeDTO hospitalRecipeDTO) {
        HospitalRecipeDAO hospitalRecipeDAO = DAOFactory.getDAO(HospitalRecipeDAO.class);
        HospitalRecipe hospitalRecipe = new HospitalRecipe();
        hospitalRecipe.setRecipeCode(hospitalRecipeDTO.getRecipeCode());
        hospitalRecipe.setClinicId(hospitalRecipeDTO.getClinicId());
        hospitalRecipe.setPatientId(hospitalRecipeDTO.getPatientId());
        hospitalRecipe.setCertificate(hospitalRecipeDTO.getCertificate());
        hospitalRecipe.setPatientName(hospitalRecipeDTO.getPatientName());
        hospitalRecipe.setPatientTel(hospitalRecipeDTO.getPatientTel());
        hospitalRecipe.setPatientNumber(hospitalRecipeDTO.getPatientNumber());
        hospitalRecipe.setRegisterId(hospitalRecipeDTO.getRegisterId());
        hospitalRecipe.setPatientSex(hospitalRecipeDTO.getPatientSex());
        hospitalRecipe.setClinicOrgan(hospitalRecipeDTO.getClinicOrgan());
        hospitalRecipe.setOrganId(hospitalRecipeDTO.getOrganId());
        hospitalRecipe.setDoctorNumber(hospitalRecipeDTO.getDoctorNumber());
        hospitalRecipe.setDoctorName(hospitalRecipeDTO.getDoctorName());
        hospitalRecipe.setCreateDate(hospitalRecipeDTO.getCreateDate());
        hospitalRecipeDAO.save(hospitalRecipe);
    }

    /**
     *  用药指导
     *  场景三-接收his推送的处方(不保存) 并推送用药指导模板消息
     * @param hosPatientRecipeDTO
     * @return
     */
    @Override
    @RpcService
    public HosRecipeResult sendMedicationGuideData(HosPatientRecipeDTO hosPatientRecipeDTO) {
        HosRecipeResult result = new HosRecipeResult();
        LOG.info("sendMedicationGuideData reqParam={}",JSONUtils.toString(hosPatientRecipeDTO));
        MedicationGuideService guideService = ApplicationUtils.getRecipeService(MedicationGuideService.class);
        //reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）
        hosPatientRecipeDTO.setReqType(RecipeBussConstant.REQ_TYPE_AUTO);
        //推送微信模板消息
        guideService.sendMedicationGuideMsg(null,null,hosPatientRecipeDTO);
        result.setCode(HosRecipeResult.SUCCESS);
        return result;
    }

    /**
     * 用药指导----前置机根据医院相关参数信息来获取二维码
     * @param req
     * @return
     */
    @Override
    @RpcService
    public HosRecipeResult getQrUrlForRecipeRemind(RecipeQrCodeReqDTO req) {
        LOG.info("getQrUrlForRecipeRemind reqParam={}",JSONUtils.toString(req));
        verifyParam(req);
        HosRecipeResult result = new HosRecipeResult();
        Map<String,String> qrUrl;
        try {
            //根据前置机传的appId获取指定的端
            ClientConfigDTO clientConfig = getClientConfig(req.getAppId(), req.getOrganId(), req.getClientType());
            if(clientConfig==null){
                result.setCode(HosRecipeResult.FAIL);
                result.setMsg("未找到公众号appId="+req.getAppId()+"，无法生成二维码");
                LOG.info("getQrUrlForRecipeRemind error msg={}","未找到公众号appId="+req.getAppId()+"，无法生成二维码");
                return result;
            }
            qrUrl = getQrUrl(clientConfig,req.getClientType(), req.getOrganId(), req.getQrInfo());
        } catch (Exception e) {
            LOG.error("getQrUrlForRecipeRemind error",e);
            result.setCode(HosRecipeResult.FAIL);
            result.setMsg("获取二维码异常");
            return result;
        }
        result.setCode(HosRecipeResult.SUCCESS);
        //二维码数据
        result.setData(qrUrl);
        return result;
    }

    private Map<String,String> getQrUrl(ClientConfigDTO clientConfig, String clientType, Integer organId, String qrcodeInfo) {
        INgariQrInfoService ngariQrInfoService = AppContextHolder.getBean("eh.ngariQrInfoService", INgariQrInfoService.class);
        String sceneStr=new StringBuffer().append(QRInfoConstant.QRTYPE_RECIPE_REMIND).append("_")
                .append(organId).append("_")
                .append(qrcodeInfo)
                .toString();
        BusQrInfoBean bean = new BusQrInfoBean();
        bean.setExpireSeconds(null);
        bean.setSceneStr(sceneStr);
        bean.setOrganId(organId);
        bean.setQrType(QRInfoConstant.QRTYPE_RECIPE_REMIND);
        bean.setCreateWay(clientType);
        //生成业务相关临时二维码[临时]* 二维码失效则重新生成* 二维码内容自定义
        QRInfoBean qrInfo = ngariQrInfoService.createBriefQRInfoForBusCustom(bean, clientConfig);
        String fileid=qrInfo==null?null:qrInfo.getQrCode();
        LOG.info("RecipeRemind getQrUrl qrInfo={}",JSONUtils.toString(qrInfo));
        if(fileid==null){
            return null;
        }
        //拼装二维码
        IUrlResourceService urlResourceService =
                AppDomainContext.getBean("eh.urlResourceService", IUrlResourceService.class);
        String uploadUrl = urlResourceService.getUrlByParam("imgUrl");
        //既返回二维码图片地址 又返回原始二维码url
        Map<String,String> result = new HashMap<>(2);
        result.put("qrImgUrl",new StringBuffer(uploadUrl).append(fileid).toString());
        result.put("qrUrl",qrInfo.getQrUrl());
        return result;
    }

    private ClientConfigDTO getClientConfig(String appId, Integer organId, String clientType) {
        IClientConfigService ccService = BaseAPI.getService(IClientConfigService.class);
        OrganConfigService organConfigService=BasicAPI.getService(OrganConfigService.class);
        IConfigurationCenterUtilsService configurationCenterUtils = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        //根据前置机传的appId获取指定的端
        ClientConfigBean clientConfigDTO = ccService.getByAppKey(appId);
        //前置机未指定公众号，则取运营平台默认的医生二维码指定的支付宝，或者微信
        if(clientConfigDTO==null){
            //医生默认微信二维码
            if(ClientConfigConstant.APP_CLIENTTYPE_WX.equals(clientType)){
                OrganConfigDTO organConfig = organConfigService.getOrganConifigById(organId);
                Integer wxConfigId =organConfig==null?null:organConfig.getWxConfigId();
                clientConfigDTO =ccService.getByTypeAndClientId(clientType,wxConfigId);
            }else if(ClientConfigConstant.APP_CLIENTTYPE_ALILIFE.equals(clientType)){
                //医生默认支付宝二维码
                String qrCodeAppId  = (String) configurationCenterUtils.getConfiguration(organId,"Qraliapp");
                clientConfigDTO =ccService.getByAppKey(qrCodeAppId);
            }
        }
        return ObjectCopyUtils.convert(clientConfigDTO,ClientConfigDTO.class);
    }

    private void verifyParam(RecipeQrCodeReqDTO req) {
        if(StringUtils.isEmpty(req.getQrInfo())
                ||StringUtils.isEmpty(req.getClientType())
                ||req.getOrganId()==null){
            throw new DAOException(ErrorCode.SERVICE_ERROR,"qrcodeInfo,clientType,organId 参数不完整");
        }
    }

    @RpcService
    public HosRecipeResult updateRecipeStatus(HospitalStatusUpdateDTO request) {
        HosRecipeResult result = prescribeService.updateRecipeStatus(request, null);
        return result;
    }

    public HosRecipeResult createBlankOrderForHos(RecipeBean recipe, HospitalRecipeDTO hospitalRecipeDTO) {
        HosRecipeResult result = new HosRecipeResult();
        result.setCode(HosRecipeResult.FAIL);

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        //创建订单
        //待煎费或者膏方制作费，存在该值说明需要待煎
        String decoctionFeeStr = hospitalRecipeDTO.getDecoctionFee();
        boolean decoctionFlag = StringUtils.isNotEmpty(decoctionFeeStr)
                && RecipeBussConstant.RECIPETYPE_TCM.equals(recipe.getRecipeType()) ? true : false;
        boolean gfFeeFlag = StringUtils.isNotEmpty(decoctionFeeStr)
                && RecipeBussConstant.RECIPETYPE_HP.equals(recipe.getRecipeType()) ? true : false;
        Map<String, String> orderMap = Maps.newHashMap();
        orderMap.put("operMpiId", recipe.getMpiid());
        //PayWayEnum.UNKNOW
        orderMap.put("payway", "-1");
        orderMap.put("payMode", null != recipe.getPayMode() ? recipe.getPayMode().toString() : "0");
        orderMap.put("decoctionFlag", decoctionFlag ? "1" : "0");
        orderMap.put("gfFeeFlag", gfFeeFlag ? "1" : "0");
        orderMap.put("calculateFee", "0");
        OrderCreateResult orderCreateResult = orderService.createOrder(
                Collections.singletonList(recipe.getRecipeId()), orderMap, 1);
        if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
            try {
                //更新订单数据
                Map<String, Object> orderAttr = Maps.newHashMap();
                orderAttr.put("status", OrderStatusConstant.READY_PAY);
                orderAttr.put("effective", 0);
                orderAttr.put("payFlag", recipe.getPayFlag());
                //接收患者手机信息
                orderAttr.put("recMobile", hospitalRecipeDTO.getPatientTel());
                //服务费为0
                orderAttr.put("registerFee", BigDecimal.ZERO);
                orderAttr.put("recipeFee", recipe.getTotalMoney());
                orderAttr.put("expressFee", StringUtils.isEmpty(hospitalRecipeDTO.getExpressFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getExpressFee()));
                orderAttr.put("decoctionFee", StringUtils.isEmpty(decoctionFeeStr) ?
                        BigDecimal.ZERO : new BigDecimal(decoctionFeeStr));
                orderAttr.put("couponFee", StringUtils.isEmpty(hospitalRecipeDTO.getCouponFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getCouponFee()));
                orderAttr.put("totalFee", new BigDecimal(hospitalRecipeDTO.getOrderTotalFee()));
                orderAttr.put("actualPrice", new BigDecimal(hospitalRecipeDTO.getActualFee()).doubleValue());

                RecipeResultBean resultBean = orderService.updateOrderInfo(
                        orderCreateResult.getOrderCode(), orderAttr, null);
                if (RecipeResultBean.SUCCESS.equals(resultBean.getCode())) {
                    LOG.info("createPrescription 订单更新成功 orderCode={}", orderCreateResult.getOrderCode());
                    result.setCode(HosRecipeResult.SUCCESS);
                } else {
                    LOG.warn("createPrescription 订单更新失败. recipeCode={}, orderCode={}",
                            hospitalRecipeDTO.getRecipeCode(), orderCreateResult.getOrderCode());
                    updateOrderError(recipe.getRecipeId(), hospitalRecipeDTO.getRecipeCode(), result);
                }
            } catch (Exception e) {
                LOG.warn("createPrescription 订单更新异常. recipeCode={}, orderCode={}",
                        hospitalRecipeDTO.getRecipeCode(), orderCreateResult.getOrderCode(), e);
                updateOrderError(recipe.getRecipeId(), hospitalRecipeDTO.getRecipeCode(), result);
            }
        } else {
            LOG.warn("createPrescription 创建订单失败. recipeCode={}, result={}",
                    hospitalRecipeDTO.getRecipeCode(), JSONUtils.toString(orderCreateResult));
            //删除处方
            recipeService.delRecipeForce(recipe.getRecipeId());
            result.setMsg("处方[" + hospitalRecipeDTO.getRecipeCode() + "]订单创建失败, 原因：" + orderCreateResult.getMsg());
        }

        return result;
    }

    private void updateOrderError(Integer recipeId, String orderCode, HosRecipeResult result) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        //删除订单

        //删除处方
        recipeService.delRecipeForce(recipeId);
        result.setMsg("处方[" + orderCode + "]订单更新失败");
    }

}
