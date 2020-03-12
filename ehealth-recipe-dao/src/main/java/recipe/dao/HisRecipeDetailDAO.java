package recipe.dao;

import com.ngari.recipe.entity.HisRecipeDetail;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:26
 */
@RpcSupportDAO
public class HisRecipeDetailDAO extends HibernateSupportDelegateDAO<HisRecipeDetail> {

    public HisRecipeDetailDAO() {
        super();
        this.setEntityName(HisRecipeDetail.class.getName());
        this.setKeyField("hisrecipedetailID");
    }
}
