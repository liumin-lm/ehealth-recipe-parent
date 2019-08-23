package recipe.drugsenterprise.bean;


import java.io.Serializable;
/**
* @Description: HdDrugResponseData 类（或接口）是华东药品响应数据
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdDrugResponseData implements Serializable {

    private static final long serialVersionUID = -3311804678678507007L;
    /**
     * 药品编码（处方药平台编码)
     */
    private String drugCode;
    /**
     * 库存数量（批次汇总）
     */
    private String invQty;
    /**
     * 参考价格
     */
    private String price;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getInvQty() {
        return invQty;
    }

    public void setInvQty(String invQty) {
        this.invQty = invQty;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}