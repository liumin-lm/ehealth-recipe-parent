package recipe.dao;

import com.ngari.recipe.entity.HisRecipeDetail;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:26
 */
@RpcSupportDAO
public abstract class HisRecipeDetailDAO extends HibernateSupportDelegateDAO<HisRecipeDetail> {

    public HisRecipeDetailDAO() {
        super();
        this.setEntityName(HisRecipeDetail.class.getName());
        this.setKeyField("hisrecipedetailID");
    }

    @DAOMethod(sql = "from HisRecipeDetail where hisRecipeId=:hisRecipeId and status=1")
    public abstract List<HisRecipeDetail> findByHisRecipeId(@DAOParam("hisRecipeId") int hisRecipeId);

    /**
     * 批量查询
     *
     * @param hisRecipeIds his处方id
     * @return
     */
    @DAOMethod(sql = "from HisRecipeDetail where hisRecipeId in (:hisRecipeIds) and status=1")
    public abstract List<HisRecipeDetail> findByHisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);

    /**
     * 根据处方id批量删除
     *
     * @param hisRecipeIds
     */
    @DAOMethod(sql = "delete from HisRecipeDetail where hisRecipeId in (:hisRecipeIds)")
    public abstract void deleteByHisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);
}
