package recipe.regulation;

import com.ngari.his.regulation.entity.RegulationChargeDetailReqTo;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.dto.RegulationChargeDetailDTO;
import com.ngari.recipe.regulation.service.IRegulationRecipeQueryService;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.PayConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.ObjectCopyUtils;

import java.util.Date;
import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
@RpcBean("remoteRecipeQueryService")
public class RegulationRecipeQueryService implements IRegulationRecipeQueryService {
    private static final Logger logger = LoggerFactory.getLogger(RegulationRecipeQueryService.class);
    @Autowired
    RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;

    @Override
    public List<RegulationChargeDetailReqTo> queryRegulationChargeDetailList(QueryRegulationUnitReq queryRegulationUnit) {
        List<Integer> ngariOrganIds = queryRegulationUnit.getNgariOrganIds();
        Date startTime = queryRegulationUnit.getStartTime();
        Date endTime = queryRegulationUnit.getEndTime();
        List<RegulationChargeDetailDTO> result = recipeOrderDAO.queryRegulationChargeDetailList(ngariOrganIds, startTime, endTime);
        logger.info("queryRegulationChargeDetailList result " + JSONUtils.toString(result));
        List<RegulationChargeDetailReqTo> list = ObjectCopyUtils.convert(result, RegulationChargeDetailReqTo.class);
        return list;
    }


    @Override
    public Integer findUnfinishedRecipe(Integer organId) {
        logger.info("findUnfinishedRecipe organId={}", organId);
        Long size = recipeDAO.getUnfinishedRecipe(organId, PayConstant.PAY_FLAG_NOT_PAY);
        return size.intValue();
    }
}
