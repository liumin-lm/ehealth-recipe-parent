package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @Description: 对接上海六院易复诊查询处方库存接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzMesDrugDetailDto implements Serializable {
    /**
     * 医院药品ID
     */
    private String drugId;
    /**
     * 规格
     */
    private String spec;
    /**
     * 用法用量
     */
    private String form;
    /**
     * 价格
     */
    private String amount;
    /**
     * 药品售卖价格
     */
    private String hospDrugPrice;
    /**
     * 药品备注
     */
    private String drugMark;

    public String getDrugId() {
        return drugId;
    }

    public void setDrugId(String drugId) {
        this.drugId = drugId;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getHospDrugPrice() {
        return hospDrugPrice;
    }

    public void setHospDrugPrice(String hospDrugPrice) {
        this.hospDrugPrice = hospDrugPrice;
    }

    public String getDrugMark() {
        return drugMark;
    }

    public void setDrugMark(String drugMark) {
        this.drugMark = drugMark;
    }
}
