package recipe.dao;

import com.ngari.recipe.entity.HisRecipeExt;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:25
 */
@RpcSupportDAO
public abstract class HisRecipeExtDAO extends HibernateSupportDelegateDAO<HisRecipeExt> {

    public HisRecipeExtDAO() {
        super();
        this.setEntityName(HisRecipeExt.class.getName());
        this.setKeyField("hisRecipeExtID");
    }

    @DAOMethod(sql = "from HisRecipeExt where hisRecipeId=:hisRecipeId")
    public abstract List<HisRecipeExt> findByHisRecipeId(@DAOParam("hisRecipeId") int hisRecipeId);

    /**
     * 根据处方id批量删除
     *
     * @param hisRecipeIds
     */
    @DAOMethod(sql = "delete from HisRecipeExt where hisRecipeId in (:hisRecipeIds)")
    public abstract void deleteByHisRecipeIds(@DAOParam("hisRecipeIds") List<Integer> hisRecipeIds);
}
