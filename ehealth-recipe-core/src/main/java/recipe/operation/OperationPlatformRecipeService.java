package recipe.operation;

import com.ngari.base.organ.model.OrganBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.client.RecipeAuditClient;
import recipe.dao.RecipeDAO;

import java.util.List;
import java.util.Map;

/**
 * 服务在用 新方法不再此类新增
 *
 * @author wzc
 * @date 2020-10-27 14:27
 * @desc 运营平台处方服务
 */
@RpcBean("operationPlatformRecipeService")
@Deprecated
public class OperationPlatformRecipeService {
    @Autowired
    private RecipeAuditClient recipeAuditClient;

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckByIdEncrypt(String recipeId, Integer doctorId) {
        return recipeAuditClient.findRecipeAndDetailsAndCheckByIdEncrypt(recipeId,doctorId);
    }

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId, Integer checkerId) {
        return recipeAuditClient.findRecipeAndDetailsAndCheckById(recipeId,checkerId);
    }


    /**
     * chuwei
     * 前端页面调用该接口查询是否存在待审核的处方单
     *
     * @param organ 审核机构
     * @return
     */
    @RpcService
    @LogRecord
    public boolean existUncheckedRecipe(int organ) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        return rDao.checkIsExistUncheckedRecipe(organ);
    }


    /**
     * 获取药师能审核的机构
     *
     * @param doctorId 药师ID
     * @return
     */
    @RpcService
    @LogRecord
    public List<OrganBean> findCheckOrganList(Integer doctorId) {
        return recipeAuditClient.findCheckOrganList(doctorId);
    }


    /**
     * 获取抢单状态和自动解锁时间
     *
     * @param map
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> getGrabOrderStatusAndLimitTime(Map<String, Object> map) {
        return recipeAuditClient.getGrabOrderStatusAndLimitTime(map);
    }
}
