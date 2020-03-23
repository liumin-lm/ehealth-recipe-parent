package recipe.regulation;

import com.ngari.his.regulation.entity.RegulationChargeDetailReq;
import com.ngari.his.regulation.entity.RegulationChargeDetailReqTo;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.regulation.service.IRegulationRecipeQueryService;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeOrderDAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
@RpcBean("remoteRecipeQueryService")
public class RegulationRecipeQueryService implements IRegulationRecipeQueryService{
    private static final Logger logger = LoggerFactory.getLogger(RegulationRecipeQueryService.class);
    @Autowired
    RecipeOrderDAO recipeOrderDAO;

    @Override
    public List<RegulationChargeDetailReqTo> queryRegulationChargeDetailList(QueryRegulationUnitReq queryRegulationUnit){
        List<RegulationChargeDetailReqTo> result = new ArrayList<>();
                List<Integer> ngariOrganIds = queryRegulationUnit.getNgariOrganIds();
                Date startTime = queryRegulationUnit.getStartTime();
                Date endTime = queryRegulationUnit.getEndTime();
        result = recipeOrderDAO.queryRegulationChargeDetailList(ngariOrganIds,startTime,endTime);
        logger.info("queryRegulationChargeDetailList result " + JSONUtils.toString(result));
        return result;
    }


}
