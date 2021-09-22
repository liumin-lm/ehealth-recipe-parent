package recipe.dao;

import com.ngari.recipe.entity.RecipeOrderPayFlow;
import com.ngari.recipe.entity.RecipeTherapy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * @description： 支付流水dao 邵逸夫模式专用
 * @author： whf
 * @date： 2021-09-17 18:48
 */
@RpcSupportDAO
public abstract class RecipeOrderPayFlowDao extends HibernateSupportDelegateDAO<RecipeOrderPayFlow> implements ExtendDao<RecipeOrderPayFlow> {

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
    @DAOMethod(sql = "FROM RecipeOrderPayFlow WHERE orderId =:orderId AND payFlag = '1'")
    public abstract List<RecipeOrderPayFlow> findByOrderId(@DAOParam("orderId") Integer orderId);

    /**
     * 根据orderId和payFlowType查询订单支付流水
     * @param orderId     orderId
     * @param payFlowType payFlowType
     * @return RecipeOrderPayFlow
     */
    @DAOMethod(sql = "FROM RecipeOrderPayFlow WHERE orderId =: orderId AND payFlowType =: payFlowType")
    public abstract RecipeOrderPayFlow getByOrderIdAndType(@DAOParam("orderId") Integer orderId,
                                                           @DAOParam("payFlowType") Integer payFlowType);

    /**
     * 根据商户订单号获取支付流水
     * @param outTradeNo 商户订单号
     * @return RecipeOrderPayFlow
     */
    @DAOMethod(sql = "FROM RecipeOrderPayFlow WHERE outTradeNo =: outTradeNo")
    public abstract RecipeOrderPayFlow getByOutTradeNo(@DAOParam("outTradeNo") String outTradeNo);

    @Override
    public boolean updateNonNullFieldByPrimaryKey(RecipeOrderPayFlow recipeOrderPayFlow) {
        return updateNonNullFieldByPrimaryKey(recipeOrderPayFlow, SQL_KEY_ID);
    }
}
