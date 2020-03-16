package recipe.regulation;

import com.ngari.recipe.regulation.model.QueryRegulationUnitDTO;
import com.ngari.recipe.regulation.model.RegulationRecipeInfoDTO;
import com.ngari.recipe.regulation.service.IRegulationRecipeQueryService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * 监管平台查询处方数据
 * 2020/03/16
 */
@RpcBean
public class RegulationRecipeQueryService implements IRegulationRecipeQueryService{


    @Override
    @RpcService
    public List<RegulationRecipeInfoDTO> queryRecipeInfo(QueryRegulationUnitDTO queryRegulationUnit) {
        return null;
    }
}
