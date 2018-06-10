package recipe.serviceprovider.his.service;

import com.ngari.base.BaseAPI;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.visit.mode.*;
import com.ngari.his.visit.service.IVisitService;
import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.his.service.IRecipeToHisService;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.BusTypeEnum;
import recipe.util.DateConversion;

import java.util.Map;

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
        IVisitService hisService = AppDomainContext.getBean("his.iVisitService", IVisitService.class);
        Map<String, Object> map = request.getConditions();
        VistPatientRequestTO hisRequest = new VistPatientRequestTO();
        hisRequest.setOrganId(Integer.valueOf(map.get("organId").toString()));
        hisRequest.setCertificate(map.get("certificate").toString());
        hisRequest.setCertificateType(Integer.valueOf(map.get("certificateType").toString()));
        hisRequest.setMpi(map.get("mpi").toString());
        hisRequest.setPatientName(map.get("patientName").toString());
        hisRequest.setMobile(map.get("mobile").toString());
        hisRequest.setJobNumber(map.get("jobNumber").toString());
        LOGGER.info("canVisit request={}", JSONUtils.toString(hisRequest));
        HisResponseTO hisResponse = hisService.canVisit(hisRequest);
        LOGGER.info("canVisit response={}", JSONUtils.toString(hisResponse));
        RecipeCommonResTO response = new RecipeCommonResTO();
        if("200".equals(hisResponse.getMsgCode())) {
            response.setCode(RecipeCommonResTO.SUCCESS);
        }else{
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg(response.getMsg());
        }
        return response;
    }

    @RpcService
    @Override
    public RecipeCommonResTO visitRegist(RecipeCommonReqTO request) {
        IVisitService hisService = AppDomainContext.getBean("his.iVisitService", IVisitService.class);
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
        LOGGER.info("visitRegist request={}", JSONUtils.toString(hisRequest));
        HisResponseTO<VisitRegistResponseTO> hisResponse = hisService.visitRegist(hisRequest);
        LOGGER.info("visitRegist response={}", JSONUtils.toString(hisResponse));
        RecipeCommonResTO response = new RecipeCommonResTO();
        if("200".equals(hisResponse.getMsgCode())) {
            response.setCode(RecipeCommonResTO.SUCCESS);
            VisitRegistResponseTO resDate = hisResponse.getData();
            IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
            HosrelationBean hosrelationBean = new HosrelationBean();
            hosrelationBean.setBusId(Integer.valueOf(map.get("consultId").toString()));
            hosrelationBean.setOrganId(Integer.valueOf(map.get("organId").toString()));
            hosrelationBean.setBusType(BusTypeEnum.CONSULT.getId());
            hosrelationBean.setRequestUrt(Integer.valueOf(map.get("urt").toString()));
            hosrelationBean.setRegisterId(resDate.getRegisterId());
            hosrelationBean.setClinicNo(resDate.getClinicNo());
            hosrelationBean.setPatId(resDate.getPatId());
            hosrelationService.save(hosrelationBean);
        }else{
            response.setCode(RecipeCommonResTO.FAIL);
            response.setMsg(response.getMsg());
        }
        return response;
    }

    @RpcService
    @Override
    public boolean queryVisitStatus(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());
        if(null != hosrelationBean){
            IVisitService hisService = AppDomainContext.getBean("his.iVisitService", IVisitService.class);
            QueryVisitsRequestTO hisRequest = new QueryVisitsRequestTO();
            hisRequest.setRegisterId(hosrelationBean.getRegisterId());
            hisRequest.setOrganId(hosrelationBean.getOrganId());
            LOGGER.info("queryVisitStatus request={}", JSONUtils.toString(hisRequest));
            HisResponseTO<QueryVisitsResponseTO> hisResponse = hisService.queryVisitStatus(hisRequest);
            LOGGER.info("queryVisitStatus response={}", JSONUtils.toString(hisResponse));
            RecipeCommonResTO response = new RecipeCommonResTO();
            if("200".equals(hisResponse.getMsgCode())) {
                response.setCode(RecipeCommonResTO.SUCCESS);
                QueryVisitsResponseTO resDate = hisResponse.getData();
                //HIS如果已接诊，则返回 true，否则返回false


                //如果his未接诊，则取消挂号
                CancelVisitRequestTO cancelRequest = new CancelVisitRequestTO();
                cancelRequest.setOrganId(hosrelationBean.getOrganId());
                cancelRequest.setRegisterId(hosrelationBean.getRegisterId());
                cancelRequest.setPatId(hosrelationBean.getPatId());
                cancelRequest.setCancelReason("系统取消");
                hisService.cancelVisit(cancelRequest);
            }else{
                response.setCode(RecipeCommonResTO.FAIL);
                response.setMsg(response.getMsg());
            }
        }else{
            LOGGER.warn("queryVisitStatus hosrelationBean is null. consultId={}", consultId);
        }
        return false;
    }

}
