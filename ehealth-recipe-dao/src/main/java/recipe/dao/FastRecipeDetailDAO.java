package recipe.dao;

import com.ngari.recipe.entity.FastRecipeDetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@RpcSupportDAO
public abstract class FastRecipeDetailDAO extends HibernateSupportDelegateDAO<FastRecipeDetail> {

    public FastRecipeDetailDAO() {
        super();
        this.setEntityName(FastRecipeDetail.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "FROM FastRecipeDetail WHERE fastRecipeId = :fastRecipeId AND status = 1")
    public abstract List<FastRecipeDetail> findFastRecipeDetailsByFastRecipeId(@DAOParam("fastRecipeId") Integer fastRecipeId);

    @DAOMethod(sql = "UPDATE FastRecipeDetail SET status = :status  WHERE id = :id")
    public abstract void updateStatusById(@DAOParam("id") Integer id,
                                          @DAOParam("status") int status);

    @DAOMethod(sql = "UPDATE FastRecipeDetail SET type = :type WHERE fastRecipeId = :fastRecipeId AND status = 1")
    public abstract void updateTypeByFastRecipeId(@DAOParam("fastRecipeId") Integer fastRecipeId,
                                                  @DAOParam("type") int type);
}
