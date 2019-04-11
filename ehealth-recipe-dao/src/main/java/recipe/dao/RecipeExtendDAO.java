package recipe.dao;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.springframework.util.ObjectUtils;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * 处方扩展表
 * Created by yuzq on 2019/3/1.
 */
@RpcSupportDAO
public abstract class RecipeExtendDAO extends HibernateSupportDelegateDAO<RecipeExtend> {

    public RecipeExtendDAO() {
        super();
        this.setEntityName(RecipeExtend.class.getName());
        this.setKeyField("recipeId");
    }

    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod
    public abstract RecipeExtend getByRecipeId(int recipeId);

    /**
     * 保存OR更新
     * @param recipeExtend
     */
    public void saveOrUpdateRecipeExtend(RecipeExtend recipeExtend) {
        if(null == recipeExtend.getRecipeId()){
            return;
        }
        if (ObjectUtils.isEmpty(this.getByRecipeId(recipeExtend.getRecipeId()))) {
            this.save(recipeExtend);
        } else {
            this.update(recipeExtend);
        }
    }


}
