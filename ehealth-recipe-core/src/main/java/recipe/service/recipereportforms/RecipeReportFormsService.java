package recipe.service.recipereportforms;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.recipereportform.model.*;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.DateConversion;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by shiyuping on 2020/7/1
 * 处方报表服务
 */
@RpcBean("recipeReportFormsService")
public class RecipeReportFormsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeReportFormsService.class);
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private OrganService organService;
    @Autowired
    private PatientService patientService;
    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;


    @RpcService
    public void recipeReportFormsService() {

    }

    private List<Integer> getQueryOrganIdList(RecipeReportFormsRequest request) {
        List<Integer> organIdList = new ArrayList<>();
        if (null != request.getOrganId()) {
            organIdList = Arrays.asList(request.getOrganId());
            return organIdList;
        }
        if (StringUtils.isNotEmpty(request.getManageUnit())) {
            if (!"eh".equals(request.getManageUnit())) {
                organIdList = organService.findOrganIdsByManageUnit(request.getManageUnit() + "%");
            }
            return organIdList;
        }
        if (null == request.getOrganId() && CollectionUtils.isEmpty(request.getOrganIdList()) && StringUtils.isEmpty(request.getManageUnit())) {
            UserRoleToken urt = UserRoleToken.getCurrent();
            String manageUnit = urt.getManageUnit();
            if (!"eh".equals(manageUnit)) {
                organIdList = organService.findOrganIdsByManageUnit(manageUnit + "%");
            }
            return organIdList;
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
                    findDrugReceivedDispatchedBalanceList(request.getManageUnit(), organIdList, request.getStartTime(), request.getEndTime(), request.getStart(), request.getLimit());
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
        request.setOrganIdList(organIdList);
        try {
            List<RecipeAccountCheckDetailResponse> responses = recipeOrderDAO.findRecipeAccountCheckDetailList(request);
            LOGGER.info("recipeAccountCheckDetailList request = {} responses={}", JSONUtils.toString(request), JSONUtils.toString(responses));
            if (CollectionUtils.isEmpty(responses)) {
                resultMap.put("total", 0);
                resultMap.put("data", responses);
                return resultMap;
            }
            Set<String> mpiIds = new HashSet<>();
            Set<Integer> enterpriseIds = new HashSet<>();
            responses.forEach(a -> {
                mpiIds.add(a.getMpiId());
                enterpriseIds.add(a.getEnterpriseId());
            });

            Map<String, PatientDTO> patientMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(mpiIds)) {
                List<PatientDTO> patientList = patientService.findByMpiIdIn(new LinkedList<>(mpiIds));
                patientMap.putAll(patientList.stream().collect(Collectors.toMap(PatientDTO::getMpiId, a -> a, (k1, k2) -> k1)));
            }
            Map<Integer, DrugsEnterprise> drugsEnterpriseMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(enterpriseIds)) {
                List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(new LinkedList<>(enterpriseIds));
                drugsEnterpriseMap.putAll(drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId, a -> a, (k1, k2) -> k1)));
            }
            responses.forEach(a -> {
                PatientDTO patientDTO = patientMap.get(a.getMpiId());
                if (null != patientDTO) {
                    a.setPatientName(patientDTO.getPatientName() + "\n" + patientDTO.getMobile());
                } else {
                    LOGGER.warn("recipeAccountCheckDetailList mpiId is null :{}", a.getMpiId());
                }

                DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(a.getEnterpriseId());
                if (null != drugsEnterprise) {
                    a.setEnterpriseName(drugsEnterprise.getName());
                } else {
                    LOGGER.warn("recipeAccountCheckDetailList enterpriseId is null {}", a.getEnterpriseId());
                }
            });
            if (CollectionUtils.isEmpty(responses)) {
                resultMap.put("total", 0);
                resultMap.put("data", responses);
            } else {
                responses.stream().forEach(a -> {
                    if (0 == a.getRefundFlag()) {
                        a.setRefundMessage("未退费");
                    }
                    if (1 == a.getRefundFlag()) {
                        a.setRefundMessage("已退费");
                    }
                });
                resultMap.put("total", responses.get(0).getTotal());
                resultMap.put("data", responses);
            }

        } catch (Exception e) {
            LOGGER.error("recipeAccountCheckDetailList error,request = {}", JSONUtils.toString(request), e);
            resultMap.put("data", Collections.emptyList());
        }
        LOGGER.info("recipeAccountCheckDetailList resultMap = {}", JSONUtils.toString(resultMap));
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
            LOGGER.info("enterpriseRecipeMonthSummaryList request = {} responses={}", JSONUtils.toString(request), JSONUtils.toString(responses));
            if (CollectionUtils.isEmpty(responses)) {
                resultMap.put("total", 0);
                resultMap.put("data", responses);
                return resultMap;
            }
            List<Integer> enterpriseIds = responses.stream().map(EnterpriseRecipeMonthSummaryResponse::getEnterpriseId).distinct().collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(enterpriseIds)) {
                List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(enterpriseIds);
                Map<Integer, DrugsEnterprise> drugsEnterpriseMap = drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId, a -> a, (k1, k2) -> k1));
                responses.forEach(a -> {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(a.getEnterpriseId());
                    if (null != drugsEnterprise) {
                        a.setEnterpriseName(drugsEnterprise.getName());
                    } else {
                        LOGGER.warn("recipeAccountCheckDetailList enterpriseId is null {}", a.getEnterpriseId());
                    }
                });
            }
            resultMap.put("total", responses.get(0).getTotal());
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
            LOGGER.info("enterpriseRecipeDetailList request = {} responses={}", JSONUtils.toString(request), JSONUtils.toString(responses));
            if (CollectionUtils.isEmpty(responses)) {
                resultMap.put("total", 0);
                resultMap.put("data", responses);
                return resultMap;
            }
            Set<String> mpiIds = new HashSet<>();
            Set<Integer> enterpriseIds = new HashSet<>();
            responses.forEach(a -> {
                mpiIds.add(a.getMpiId());
                enterpriseIds.add(a.getEnterpriseId());
            });

            Map<String, PatientDTO> patientMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(mpiIds)) {
                List<PatientDTO> patientList = patientService.findByMpiIdIn(new LinkedList<>(mpiIds));
                patientMap.putAll(patientList.stream().collect(Collectors.toMap(PatientDTO::getMpiId, a -> a, (k1, k2) -> k1)));
            }
            Map<Integer, DrugsEnterprise> drugsEnterpriseMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(enterpriseIds)) {
                List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(new LinkedList<>(enterpriseIds));
                drugsEnterpriseMap.putAll(drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId, a -> a, (k1, k2) -> k1)));
            }
            responses.forEach(a -> {
                PatientDTO patientDTO = patientMap.get(a.getMpiId());
                if (null != patientDTO) {
                    a.setPatientName(patientDTO.getPatientName());
                } else {
                    LOGGER.warn("recipeAccountCheckDetailList mpiId is null :{}", a.getMpiId());
                }

                DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(a.getEnterpriseId());
                if (null != drugsEnterprise) {
                    a.setEnterpriseName(drugsEnterprise.getName());
                } else {
                    LOGGER.warn("recipeAccountCheckDetailList enterpriseId is null {}", a.getEnterpriseId());
                }

                try {
                    a.setGiveModeText(DictionaryController.instance().get("eh.cdr.dictionary.GiveMode").getText(a.getGiveMode()));
                    a.setPayeeCodeText(DictionaryController.instance().get("eh.cdr.dictionary.PayeeCode").getText(a.getPayeeCode()));
                } catch (ControllerException e) {
                    e.printStackTrace();
                }

            });
            resultMap.put("data", responses);
            resultMap.put("total", responses.get(0).getTotal());
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
                    } else {
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
