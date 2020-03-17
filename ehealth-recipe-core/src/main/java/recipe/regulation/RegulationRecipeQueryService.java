package recipe.regulation;

import com.ngari.his.regulation.entity.RegulationChargeDetailReq;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.regulation.model.QueryRegulationUnitDTO;
import com.ngari.recipe.regulation.model.RegulationDrugDeliveryDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeDetailDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeInfoDTO;
import com.ngari.recipe.regulation.service.IRegulationRecipeQueryService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.RecipeOrderDAO;

import java.util.Date;
import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
@RpcBean("remoteRecipeQueryService")
public class RegulationRecipeQueryService implements IRegulationRecipeQueryService{

    @Autowired
    RecipeOrderDAO recipeOrderDAO;

    @Override
    @RpcService
    public List<RegulationRecipeInfoDTO> queryRecipeInfos(QueryRegulationUnitDTO queryRegulationUnit) {
        return null;
    }

    @Override
    public List<RegulationRecipeDetailDTO> queryRecipeDetails(QueryRegulationUnitDTO queryRegulationUnit) {
        return null;
    }

    @Override
    public List<RegulationDrugDeliveryDTO> queryDrugDeliveries(QueryRegulationUnitDTO queryRegulationUnit) {
        return null;
    }

    @Override
    public List<RegulationChargeDetailReq> queryRegulationChargeDetailList(QueryRegulationUnitReq queryRegulationUnit){
                List<Integer> ngariOrganIds = queryRegulationUnit.getNgariOrganIds();
                Date startTime = queryRegulationUnit.getStartTime();
                Date endTime = queryRegulationUnit.getEndTime();
        return recipeOrderDAO.queryRegulationChargeDetailList(ngariOrganIds,startTime,endTime);

    }


}
