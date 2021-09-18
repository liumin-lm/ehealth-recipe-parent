package recipe.dao;

import com.ngari.recipe.entity.RecipeOrderPayFlow;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @description： 支付流水dao 邵逸夫模式专用
 * @author： whf
 * @date： 2021-09-17 18:48
 */
@RpcSupportDAO
public abstract class RecipeOrderPayFlowDao extends HibernateSupportDelegateDAO<RecipeOrderPayFlow> {

    private static final Log LOGGER = LogFactory.getLog(RecipeOrderPayFlowDao.class);

    public RecipeOrderPayFlowDao() {
        super();
        this.setEntityName(RecipeOrderPayFlow.class.getName());
        this.setKeyField("id");
    }


    /**
     * 根据orderId 查询订单支付流水(已支付)
     *
     * @param orderId
     */
    @DAOMethod(sql = "FROM RecipeOrderPayFlow WHERE orderId =: orderId AND payFlag = '1'")
    public abstract void deleteByDrugsEnterpriseId(@DAOParam("orderId") Integer orderId);
}
