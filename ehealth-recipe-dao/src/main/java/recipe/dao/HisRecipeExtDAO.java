package recipe.dao;

import com.ngari.recipe.entity.HisRecipeExt;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:25
 */
@RpcSupportDAO
public class HisRecipeExtDAO extends HibernateSupportDelegateDAO<HisRecipeExt> {

    public HisRecipeExtDAO() {
        super();
        this.setEntityName(HisRecipeExt.class.getName());
        this.setKeyField("hisRecipeExtID");
    }
}
