package recipe.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2019/3/5
 * @description： 购药请求对象
 * @version： 1.0
 */
public class PurchaseRequest implements Serializable {

    private static final long serialVersionUID = 4014344377789940685L;

    @Verify(desc = "处方ID", isInt = true)
    private Integer recipeId;

    /**
     * RecipeBussConstant中 GIVEMODE部分取值
     */
    @Verify(desc = "购药方式", isInt = true)
    private Integer type;

    @Verify(desc = "应用ID", isNotNull = false)
    private String appId;

    @Verify(desc = "药企ID", isNotNull = false)
    private Integer depId;

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Integer getDepId() {
        return depId;
    }

    public void setDepId(Integer depId) {
        this.depId = depId;
    }
}
