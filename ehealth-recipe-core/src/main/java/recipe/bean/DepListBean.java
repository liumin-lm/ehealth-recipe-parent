package recipe.bean;





import com.ngari.recipe.entity.Recipedetail;

import java.util.List;

/**
 * 多供应商选择返回数据
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
public class DepListBean {

    /**
     * true表示单个供应商
     */
    private boolean sigle;

    private String recipeGetModeTip;

    private List<DepDetailBean> list;

    private List<Recipedetail> details;


    public boolean isSigle() {
        return sigle;
    }

    public void setSigle(boolean sigle) {
        this.sigle = sigle;
    }

    public String getRecipeGetModeTip() {
        return recipeGetModeTip;
    }

    public void setRecipeGetModeTip(String recipeGetModeTip) {
        this.recipeGetModeTip = recipeGetModeTip;
    }

    public List<DepDetailBean> getList() {
        return list;
    }

    public void setList(List<DepDetailBean> list) {
        this.list = list;
    }

    public List<Recipedetail> getDetails() {
        return details;
    }

    public void setDetails(List<Recipedetail> details) {
        this.details = details;
    }
}

