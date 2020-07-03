package recipe.service.recipereportforms;

import com.ngari.patient.service.OrganService;
import com.ngari.recipe.recipereportform.model.RecipeMonthAccountCheckResponse;
import com.ngari.recipe.recipereportform.model.RecipeReportFormsRequest;
import com.ngari.recipe.recipereportform.model.RecivedDispatchedBalanceResponse;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeReportFormsService.class);

    @RpcService
    public void recipeReportFormsService() {

    }

    private List<Integer> getQueryOrganIdList(RecipeReportFormsRequest request) {
        List<Integer> organIdList = new ArrayList<>();
        if (null == request.getOrganId() && CollectionUtils.isEmpty(request.getOrganIdList())) {
            UserRoleToken urt = UserRoleToken.getCurrent();
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
        List<Integer> organIdList = getQueryOrganIdList(request);
        try {
            List<RecivedDispatchedBalanceResponse> drugReceivedDispatchedBalanceList = recipeOrderDAO.
                    findDrugReceivedDispatchedBalanceList(organIdList, request.getStartTime(), request.getEndTime(), request.getStart(), request.getLimit());
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
        Args.notNull(request.getMonth(), "startTime");
        Args.notNull(request.getYear(), "endTime");
        Args.notNull(request.getStart(), "start");
        Args.notNull(request.getLimit(), "limit");
        List<Integer> organIdList = getQueryOrganIdList(request);
        try{
            List<RecipeMonthAccountCheckResponse> responses = recipeOrderDAO.findRecipeMonthAccountCheckList(organIdList,request.getYear(),request.getMonth(),request.getStart(),request.getLimit());
            resultMap.put("data",responses);
        }catch (Exception e){
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
    public Map<String, Object> recipeAccountCheckDetailList() {
        return null;
    }

    /**
     * 药企处方月度汇总列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> enterpriseRecipeMonthSummaryList() {
        return null;
    }

    /**
     * 药企处方明细列表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> enterpriseRecipeDetailList() {
        return null;
    }

    /**
     * 处方his对账表
     *
     * @return
     */
    @RpcService
    public Map<String, Object> recipeHisAccountCheckList() {
        return null;
    }

}
