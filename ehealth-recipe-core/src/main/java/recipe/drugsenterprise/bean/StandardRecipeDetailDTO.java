package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 更新处方详情标准数据
 * @version： 1.0
 */
public class StandardRecipeDetailDTO implements Serializable {

    private static final long serialVersionUID = -2437877243507991721L;

    @Verify(desc = "药企对照药品编码")
    private String drugCode;

    @Verify(desc = "药品单价", isMoney = true)
    private String salePrice;

    @Verify(desc = "药品总价", isMoney = true)
    private String drugCost;

    @Verify(isNotNull = false, desc = "药品批号")
    private String drugBatch;

    @Verify(isNotNull = false, desc = "药品效期", isDate = true)
    private String validDate;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(String salePrice) {
        this.salePrice = salePrice;
    }

    public String getDrugCost() {
        return drugCost;
    }

    public void setDrugCost(String drugCost) {
        this.drugCost = drugCost;
    }

    public String getDrugBatch() {
        return drugBatch;
    }

    public void setDrugBatch(String drugBatch) {
        this.drugBatch = drugBatch;
    }

    public String getValidDate() {
        return validDate;
    }

    public void setValidDate(String validDate) {
        this.validDate = validDate;
    }
}
