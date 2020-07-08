package recipe.service.recipereportforms;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.recipereportform.model.*;
import ctd.account.UserRoleToken;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeOrderDAO;
import recipe.util.DateConversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * created by shiyuping on 2020/7/1
 * 处方报表服务
 */
@RpcBean("recipeReportFormsService")
public class RecipeReportFormsService {
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private OrganService organService;
    @Autowired
    private PatientService patientService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeReportFormsService.class);


    @RpcService
    public void recipeReportFormsService() {

    }

    private List<Integer> getQueryOrganIdList(RecipeReportFormsRequest request) {
        List<Integer> organIdList = new ArrayList<>();
        if (null == request.getOrganId() && CollectionUtils.isEmpty(request.getOrganIdList())) {
            UserRoleToken urt = UserRoleToken.getCurrent();
            LOGGER.info("opbase get urt = {}", JSONUtils.toString(urt));
            String manageUnit = urt.getManageUnit();
            List<Integer> organIds = new ArrayList<>();
            if (!"eh".equals(manageUnit)) {
                organIds = organService.findOrganIdsByManageUnit(manageUnit + "%");
            }
            organIdList = organIds;
        } else if (null != request.getOrganId()) {
            organIdList = Arrays.asList(request.getOrganId());
        } else if (null == request.getOrganId() && CollectionUtils.isNotEmpty(request.getOrganIdList())) {
            organIdList = request.getOrganIdList();
        }
        return organIdList;
    }

    /**
     * 互联网药品收发存报表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> drugReceivedDispatchedBalanceList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getStartTime(), "startTime");
        Args.notNull(request.getEndTime(), "endTime");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        //将结束时间+1计算
        request.setEndTime(DateConversion.getDateAftXDays(request.getEndTime(), 1));
        List<Integer> organIdList = getQueryOrganIdList(request);
        try {
            List<RecivedDispatchedBalanceResponse> drugReceivedDispatchedBalanceList = recipeOrderDAO.
                    findDrugReceivedDispatchedBalanceList(organIdList, request.getStartTime(), request.getEndTime(), request.getStart(), request.getLimit());
            if (CollectionUtils.isNotEmpty(drugReceivedDispatchedBalanceList)) {
                resultMap.put("total", drugReceivedDispatchedBalanceList.get(0).getTotal());
            } else {
                resultMap.put("total", 0);
            }
            resultMap.put("data", drugReceivedDispatchedBalanceList);
        } catch (Exception e) {
            LOGGER.error("drugReceivedDispatchedBalanceList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

    /**
     * 互联网月度对账汇总列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> recipeMonthAccountCheckList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getMonth(), "month");
        Args.notNull(request.getYear(), "year");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        List<Integer> organIdList = getQueryOrganIdList(request);
        try {
            List<RecipeMonthAccountCheckResponse> responses = recipeOrderDAO.findRecipeMonthAccountCheckList(organIdList, request.getYear(), request.getMonth(), request.getStart(), request.getLimit());
            if (CollectionUtils.isNotEmpty(responses)) {
                resultMap.put("total", responses.get(0).getTotal());
            } else {
                resultMap.put("total", 0);
            }
            resultMap.put("data", responses);
        } catch (Exception e) {
            LOGGER.error("recipeMonthAccountCheckList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

    /**
     * 处方对账明细列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> recipeAccountCheckDetailList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getStartTime(), "startTime");
        Args.notNull(request.getEndTime(), "endTime");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        //将结束时间+1计算
        request.setEndTime(DateConversion.getDateAftXDays(request.getEndTime(), 1));
        List<Integer> organIdList = getQueryOrganIdList(request);
        try {
            request.setOrganIdList(organIdList);
            List<RecipeAccountCheckDetailResponse> responses = recipeOrderDAO.findRecipeAccountCheckDetailList(request);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientDTO;
            for(RecipeAccountCheckDetailResponse recipeAccountCheckDetailResponse : responses){
                if(null != recipeAccountCheckDetailResponse.getMpiId()){
                    patientDTO = patientService.getPatientBeanByMpiId(recipeAccountCheckDetailResponse.getMpiId());
                    if(null != patientDTO){
                        recipeAccountCheckDetailResponse.setPatientName(patientDTO.getPatientName() + "\n" + patientDTO.getMobile());
                    }else{
                        LOGGER.error("recipeHisAccountCheckList 当前患者{}不存在", recipeAccountCheckDetailResponse.getMpiId());
                    }
                }

            }
            if (CollectionUtils.isNotEmpty(responses)) {
                resultMap.put("total", responses.get(0).getTotal());
            } else {
                resultMap.put("total", 0);
            }
            resultMap.put("data", responses);
        } catch (Exception e) {
            LOGGER.error("recipeAccountCheckDetailList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

    /**
     * 药企处方月度汇总列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> enterpriseRecipeMonthSummaryList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getMonth(), "month");
        Args.notNull(request.getYear(), "year");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        List<Integer> organIdList = getQueryOrganIdList(request);
        try {
            request.setOrganIdList(organIdList);
            List<EnterpriseRecipeMonthSummaryResponse> responses = recipeOrderDAO.findEnterpriseRecipeMonthSummaryList(request);
            if (CollectionUtils.isNotEmpty(responses)) {
                resultMap.put("total", responses.get(0).getTotal());
            } else {
                resultMap.put("total", 0);
            }
            resultMap.put("data", responses);
        } catch (Exception e) {
            LOGGER.error("enterpriseRecipeMonthSummaryList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

    /**
     * 药企处方明细列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> enterpriseRecipeDetailList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getStartTime(), "startTime");
        Args.notNull(request.getEndTime(), "endTime");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        //将结束时间+1计算
        request.setEndTime(DateConversion.getDateAftXDays(request.getEndTime(), 1));
        List<Integer> organIdList = getQueryOrganIdList(request);
        request.setOrganIdList(organIdList);
        try {
            List<EnterpriseRecipeDetailResponse> responses = recipeOrderDAO.findEnterpriseRecipeDetailList(request);
            if (CollectionUtils.isNotEmpty(responses)) {
                responses.forEach(item -> {
                    PatientDTO patient = patientService.getPatientByMpiId(item.getMpiId());
                    item.setPatientName(patient.getPatientName());
                });
                resultMap.put("total", responses.get(0).getTotal());
            } else {
                resultMap.put("total", 0);
            }
            resultMap.put("data", responses);
        } catch (Exception e) {
            LOGGER.error("enterpriseRecipeDetailList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

    /**
     * 处方his对账表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> recipeHisAccountCheckList(RecipeReportFormsRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        Args.notNull(request.getStartTime(), "startTime");
        Args.notNull(request.getEndTime(), "endTime");
        //将结束时间+1计算
        request.setEndTime(DateConversion.getDateAftXDays(request.getEndTime(), 1));
        List<Integer> organIdList = getQueryOrganIdList(request);
        request.setOrganIdList(organIdList);
        try {
            List<RecipeHisAccountCheckResponse> responses = recipeOrderDAO.findRecipeHisAccountCheckList(request);
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientDTO;
            for(RecipeHisAccountCheckResponse recipeHisAccountCheckResponse : responses){
                if(null != recipeHisAccountCheckResponse.getMpiId()){
                    patientDTO = patientService.getPatientByMpiId(recipeHisAccountCheckResponse.getMpiId());
                    if(null != patientDTO){
                        recipeHisAccountCheckResponse.setPatientName(patientDTO.getPatientName() + "\n" + patientDTO.getMobile());
                    }else{
                        LOGGER.error("recipeHisAccountCheckList 当前患者{}不存在", recipeHisAccountCheckResponse.getMpiId());
                    }
                }

            }
            resultMap.put("data", responses);
        } catch (Exception e) {
            LOGGER.error("recipeHisAccountCheckList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        return resultMap;
    }

}
