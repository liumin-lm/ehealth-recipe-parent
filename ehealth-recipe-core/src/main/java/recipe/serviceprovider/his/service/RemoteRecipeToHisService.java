package recipe.serviceprovider.his.service;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.BaseAPI;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.visit.mode.*;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.his.service.IRecipeToHisService;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.BusTypeEnum;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 处方相关对接HIS服务
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2017/9/12.
 */
@RpcBean("remoteRecipeToHisService")
public class RemoteRecipeToHisService implements IRecipeToHisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRecipeToHisService.class);

    @RpcService
    @Override
    public RecipeCommonResTO canVisit(RecipeCommonReqTO request) {
        IVisitService hisService = AppDomainContext.getBean("his.visitService", IVisitService.class);
        Map<String, Object> map = request.getConditions();
        VistPatientRequestTO hisRequest = new VistPatientRequestTO();
        hisRequest.setOrganId(Integer.valueOf(map.get("organId").toString()));
        hisRequest.setCertificate(map.get("certificate").toString());
        hisRequest.setCertificateType(Integer.valueOf(map.get("certificateType").toString()));
        hisRequest.setMpi(map.get("mpi").toString());
        hisRequest.setPatientName(map.get("patientName").toString());
        hisRequest.setMobile(map.get("mobile").toString());
        hisRequest.setJobNumber(map.get("jobNumber").toString());
        hisRequest.setCardType(MapValueUtil.getString(map, "cardType"));
        hisRequest.setCardID(MapValueUtil.getString(map, "cardID"));
        LOGGER.info("canVisit request={}", JSONUtils.toString(hisRequest));
        HisResponseTO hisResponse = null;
        try {
            hisResponse = hisService.canVisit(hisRequest);
        } catch (Exception e) {
            LOGGER.error("canVisit his error. request={}", JSONUtils.toString(hisRequest), e);
        }
        LOGGER.info("canVisit response={}", JSONUtils.toString(hisResponse));
        RecipeCommonResTO response = new RecipeCommonResTO();
        if(null == hisResponse){
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg("HIS返回数据有误");
        }else {
            if ("200".equals(hisResponse.getMsgCode())) {
                response.setCode(RecipeCommonResTO.SUCCESS);
            } else {
                response.setCode(RecipeCommonResTO.FAIL);
                //抱歉，因您1月内在医院没有就诊记录，不能发起在线复诊。
                response.setMsg(hisResponse.getMsg());
            }
        }
        return response;
    }

    @RpcService
    @Override
    public RecipeCommonResTO visitRegist(RecipeCommonReqTO request) {
        LOGGER.info("visitRegist请求入参：{}", JSONUtils.toString(request));
        IVisitService hisService = AppDomainContext.getBean("his.visitService", IVisitService.class);
        Map<String, Object> map = request.getConditions();
        VisitRegistRequestTO hisRequest = new VisitRegistRequestTO();
        hisRequest.setOrganId(Integer.valueOf(map.get("organId").toString()));
        hisRequest.setCertificate(map.get("certificate").toString());
        hisRequest.setCertificateType(Integer.valueOf(map.get("certificateType").toString()));
        hisRequest.setMpi(map.get("mpi").toString());
        hisRequest.setPatientName(map.get("patientName").toString());
        hisRequest.setMobile(map.get("mobile").toString());
        hisRequest.setJobNumber(map.get("jobNumber").toString());
        hisRequest.setWorkDate(DateConversion.parseDate(map.get("workDate").toString(), DateConversion.YYYY_MM_DD));
        hisRequest.setUrt(Integer.valueOf(map.get("urt").toString()));
        hisRequest.setCardType(MapValueUtil.getString(map, "cardType"));
        hisRequest.setCardID(MapValueUtil.getString(map, "cardID"));
        //平台复诊id
        hisRequest.setPlatRegisterId(String.valueOf(MapValueUtil.getInteger(map,"consultId")));
        //科室代码
        hisRequest.setDeptCode(MapValueUtil.getString(map,"deptCode"));
        hisRequest.setRegType(2);
        Integer consultId = MapValueUtil.getInteger(map, "consultId");
        if(null != consultId){
            IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
            RevisitBean revisitBean = iRevisitService.getById(consultId);
            if (null == revisitBean) {
                LOGGER.error("visitRegist当前咨询id对应的咨询不存在{}", consultId);
            }else{
                if (null != revisitBean.getPreSettletotalAmount()) {
                    hisRequest.setTotalAmount(new BigDecimal(revisitBean.getPreSettletotalAmount()).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                if (null != revisitBean.getFundAmount()) {
                    hisRequest.setFundAmount(new BigDecimal(revisitBean.getFundAmount()).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                if (null != revisitBean.getCashAmount()) {
                    hisRequest.setCashAmount(new BigDecimal(revisitBean.getCashAmount()).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO exDTO = iRevisitExService.getByConsultId(consultId);
                if(null != exDTO){
                    hisRequest.setMedicalPayFlag(null == exDTO.getMedicalFlag() ? 0 : exDTO.getMedicalFlag());
                }else{
                    LOGGER.error("visitRegist当前咨询id对应的咨询补充信息不存在{}", consultId);
                }
            }
        }else{
            LOGGER.error("当前咨询id传参为空");
        }

        Double consultPrice = MapValueUtil.getDouble(map, "consultPrice");
        if (consultPrice !=null){
            //复诊金额
            hisRequest.setRegPrice(new BigDecimal(consultPrice).setScale(2,BigDecimal.ROUND_HALF_UP));
        }
        //支付状态
        hisRequest.setPayFlag(MapValueUtil.getInteger(map,"payFlag"));
        hisRequest.setOrganName(MapValueUtil.getString(map,"organName"));
        // 复诊就诊类型
        hisRequest.setRegType(MapValueUtil.getInteger(map,"regType"));
        hisRequest.setFundAmount(MapValueUtil.getBigDecimal(map,"fundAmount"));
        hisRequest.setCashAmount(MapValueUtil.getBigDecimal(map,"cashAmount"));
        hisRequest.setMedicalPayFlag(MapValueUtil.getInteger(map,"medicalPayFlag"));

        //支付流水号
        hisRequest.setTradeNo(MapValueUtil.getString(map,"tradeNo"));
        //商户订单号
        hisRequest.setOutTradeNo(MapValueUtil.getString(map,"outTradeNo"));
        LOGGER.info("visitRegist request={}", JSONUtils.toString(hisRequest));
        HisResponseTO<VisitRegistResponseTO> hisResponse = null;
        try {
            hisResponse = hisService.visitRegist(hisRequest);
        } catch (Exception e) {
            LOGGER.error("visitRegist his error. request={}", JSONUtils.toString(hisRequest), e);
        }
        LOGGER.info("visitRegist response={}", JSONUtils.toString(hisResponse));
        RecipeCommonResTO response = new RecipeCommonResTO();
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        Date date = DateTime.now().toDate();
        HosrelationBean hosrelationBean = new HosrelationBean();
        hosrelationBean.setBusId(Integer.valueOf(map.get("consultId").toString()));
        hosrelationBean.setOrganId(Integer.valueOf(map.get("organId").toString()));
        hosrelationBean.setBusType(100);
        hosrelationBean.setRequestUrt(Integer.valueOf(map.get("urt").toString()));
        hosrelationBean.setPatientName(MapValueUtil.getString(map, "patientName"));
        hosrelationBean.setCardType(MapValueUtil.getString(map, "cardType"));
        hosrelationBean.setCardId(MapValueUtil.getString(map, "cardID"));
        hosrelationBean.setCreateTime(date);
        hosrelationBean.setLastModify(date);
        if(null == hisResponse){
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg("HIS返回数据有误");
            hosrelationBean.setStatus(0);
            hosrelationBean.setRegisterId("-1");
            hosrelationBean.setMemo("由于系统原因，请稍后再试，咨询已自动取消");
        }else {
            if ("200".equals(hisResponse.getMsgCode())) {
                response.setCode(RecipeCommonResTO.SUCCESS);
                VisitRegistResponseTO resDate = hisResponse.getData();
                //更新复诊挂号序号
                if (StringUtils.isNotEmpty(resDate.getRegisterId())){
                    IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                    RevisitExDTO consultExDTO = exService.getByConsultId(hosrelationBean.getBusId());
                    if (consultExDTO!=null && StringUtils.isEmpty(consultExDTO.getRegisterNo())) {
                        exService.updateConsultExInfoByConsultId(hosrelationBean.getBusId(), ImmutableMap.of("registerNo", resDate.getRegisterId()));
                    }
                }
                hosrelationBean.setRegisterId(resDate.getRegisterId());
                hosrelationBean.setClinicNo(resDate.getClinicNo());
                hosrelationBean.setPatId(resDate.getPatId());
                hosrelationBean.setExtendsParam(resDate.getExtendsParam());
                hosrelationBean.setStatus(1);
                //date 20200323
                //添加设置HIS结算单据号
                hosrelationBean.setInvoiceNo(resDate.getHisSettlementNo());
            } else {
                response.setCode(RecipeCommonResTO.FAIL);
                response.setMsg(hisResponse.getMsg());
                hosrelationBean.setStatus(0);
                hosrelationBean.setRegisterId("-1");
                hosrelationBean.setMemo(hisResponse.getMsg());
            }

        }
        LOGGER.info("visitRegist save hosrelationBean={}", JSONUtils.toString(hosrelationBean));
        hosrelationService.save(hosrelationBean);
        return response;
    }

    @RpcService
    @Override
    public RecipeCommonResTO visitRegistSuccess(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());
        RecipeCommonResTO response = new RecipeCommonResTO();
        response.setCode(RecipeCommonResTO.FAIL);
        if(null != hosrelationBean){
            if(1 == hosrelationBean.getStatus()) {
                response.setCode(RecipeCommonResTO.SUCCESS);
            }
            response.setMsg(hosrelationBean.getMemo());
        }else{
            //-1 会让前端重复查询
            response.setCode(-1);
//            response.setMsg("由于系统原因，请稍后再试，咨询已自动取消");
        }
        LOGGER.info("visitRegistSuccess consultId={}, response={}", consultId, JSONUtils.toString(response));
        return response;
    }

    @RpcService
    @Override
    public RecipeCommonResTO queryVisitStatus(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());
        RecipeCommonResTO response = new RecipeCommonResTO();
        if(null != hosrelationBean){
            IVisitService hisService = AppDomainContext.getBean("his.visitService", IVisitService.class);
            QueryVisitsRequestTO hisRequest = new QueryVisitsRequestTO();
            hisRequest.setRegisterId(hosrelationBean.getRegisterId());
            hisRequest.setOrganId(hosrelationBean.getOrganId());
            IRevisitExService iRevisitExService = AppDomainContext.getBean("revisit.revisitExService", IRevisitExService.class);
            RevisitExDTO revisitExDTO = iRevisitExService.getByConsultId(consultId);
            if (Objects.nonNull(revisitExDTO)) {
                hisRequest.setPatId(revisitExDTO.getPatId());
            }
            LOGGER.info("queryVisitStatus request={}", JSONUtils.toString(hisRequest));
            HisResponseTO<QueryVisitsResponseTO> hisResponse = null;
            try {
                hisResponse = hisService.queryVisitStatus(hisRequest);
            } catch (Exception e) {
                LOGGER.error("queryVisitStatus his error. request={}", JSONUtils.toString(hisRequest), e);
            }
            LOGGER.info("queryVisitStatus response={}", JSONUtils.toString(hisResponse));
            if(null == hisResponse){
                response.setCode(RecipeCommonResTO.FAIL);
                response.setMsg("HIS返回数据有误");
                hosrelationService.cancelSuccess(hosrelationBean.getBusId(), hosrelationBean.getBusType(), 0);
            }else {
                if ("200".equals(hisResponse.getMsgCode())) {
                    QueryVisitsResponseTO resDate = hisResponse.getData();
                    if (resDate.getRegisterId().equals(hosrelationBean.getRegisterId())) {
                        //HIS就诊状态： 1 已接诊 2 已取消 0未接诊
                        if ("1".equals(resDate.getStatus())) {
                            LOGGER.info("queryVisitStatus consultId={} 已接诊", consultId);
                            response.setCode(RecipeCommonResTO.SUCCESS);
                            return response;
                        } else {
                            response.setCode(RecipeCommonResTO.FAIL);
                            //在consult项目 cancelVisitRecordTask 方法会进行咨询取消退款操作 CancelConsult，故在此会导致重复取消
//                            cancelVisitImpl(hosrelationBean);
                            hosrelationService.cancelSuccess(hosrelationBean.getBusId(), hosrelationBean.getBusType(), 0);
                            return response;
                        }
                    }
                } else {
                    response.setCode(-1);
                    response.setMsg("系统返回失败," + JSONUtils.toString(hisResponse));
                    hosrelationService.cancelSuccess(hosrelationBean.getBusId(), hosrelationBean.getBusType(), 0);
                }
            }
        }else{
            LOGGER.warn("queryVisitStatus hosrelationBean is null. consultId={}", consultId);
            response.setCode(-1);
            response.setMsg("查询不到业务记录,consultId="+consultId);
        }
        return response;
    }

    @RpcService
    @Override
    public RecipeCommonResTO cancelVisit(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());
        RecipeCommonResTO response = new RecipeCommonResTO();
        response.setCode(RecipeCommonResTO.FAIL);
        if(null != hosrelationBean) {
            //无效的挂号记录则直接返回成功
            if(0 == hosrelationBean.getStatus()){
                response.setCode(RecipeCommonResTO.SUCCESS);
                return response;
            }
            boolean flag = cancelVisitImpl(hosrelationBean);
            if (flag) {
                response.setCode(RecipeCommonResTO.SUCCESS);
            }
        }
        return response;
    }

    @RpcService
    public boolean cancelVisitImpl(HosrelationBean hosrelationBean){
        IVisitService hisService = AppDomainContext.getBean("his.visitService", IVisitService.class);
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);

        //如果his未接诊，则取消挂号
        CancelVisitRequestTO cancelRequest = new CancelVisitRequestTO();
        cancelRequest.setOrganId(hosrelationBean.getOrganId());
        cancelRequest.setRegisterId(hosrelationBean.getRegisterId());
        cancelRequest.setPatId(hosrelationBean.getPatId());
        cancelRequest.setPatientName(hosrelationBean.getPatientName());
        cancelRequest.setCardType(hosrelationBean.getCardType());
        cancelRequest.setCardID(hosrelationBean.getCardId());
        cancelRequest.setExtendsParam(hosrelationBean.getExtendsParam());
        cancelRequest.setCancelReason("系统取消");
        LOGGER.info("cancelVisit request={}", JSONUtils.toString(cancelRequest));
        HisResponseTO cancelResponse = null;
        try {
            cancelResponse = hisService.cancelVisit(cancelRequest);
        } catch (Exception e) {
            LOGGER.error("cancelVisit his error. request={}", JSONUtils.toString(cancelRequest));
        }
        LOGGER.info("cancelVisit response={}", JSONUtils.toString(cancelResponse));
        if(null == cancelResponse){
            LOGGER.warn("HIS返回为NULL, consultId={}", hosrelationBean.getBusId());
        }else {
            //取消成功记录
            if ("200".equals(cancelResponse.getMsgCode())) {
                hosrelationService.cancelSuccess(hosrelationBean.getBusId(), hosrelationBean.getBusType(), 1);
                LOGGER.info("cancelVisit consultId={} 取消成功", hosrelationBean.getBusId());
                return true;
            } else {
                LOGGER.warn("cancelVisit consultId={} 取消失败, msg={}", hosrelationBean.getBusId(), cancelResponse.getMsg());
            }
        }
        hosrelationService.cancelSuccess(hosrelationBean.getBusId(), hosrelationBean.getBusType(), 0);
        return false;
    }

    @RpcService
    @Override
    public void cancelVisitForFail() {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        List<HosrelationBean> list = hosrelationService.findBatchCancelVisit();
        LOGGER.info("cancelVisitForFail size={}", list.size());
        for(HosrelationBean hosrelationBean : list){
            cancelVisitImpl(hosrelationBean);
        }
    }
}
