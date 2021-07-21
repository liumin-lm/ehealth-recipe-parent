package recipe.api.open;

import ctd.util.annotation.RpcService;

/**
 * 处方提供的服务接口
 *
 * @Date: 2021/7/19
 * @Author: zhaoh
 */
public interface IRecipeAtopService {
    /**
     * @Description: 查询是否存在药师未审核状态的处方
     * @Param: bussSource 处方来源
     * @Param: clinicID 复诊ID
     * @return: True存在 False不存在
     * @Date: 2021/7/19
     */
    @RpcService
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicID);
}
