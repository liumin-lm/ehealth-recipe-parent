package recipe.regulation;

import com.ngari.recipe.regulation.model.QueryRegulationUnitDTO;
import com.ngari.recipe.regulation.model.RegulationDrugDeliveryDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeDetailDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeInfoDTO;
import com.ngari.recipe.regulation.service.IRegulationRecipeQueryService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
@RpcBean("remoteRecipeQueryService")
public class RegulationRecipeQueryService implements IRegulationRecipeQueryService{


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

}
