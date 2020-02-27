package recipe.drugsenterprise.bean;

/**
 * @Description: 对接英克推送处方中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;

@Schema
public class YkDrugDto implements Serializable {
    /**
     * 货品id
     */
    private int goods_id;
    /**
     *货品数量
     */
    private int goods_num;
    /**
     *处方细单号
     */
    private int prescriptiondtlid;
    /**
     *接口处方类型
     */
    private int prescriptiontype;
    /**
     *处方来源平台
     */
    private int sourceplatform;
    /**
     *单价
     */
    private String unitprice;
    /**
     *金额
     */
    private String money;

    public int getGoods_id() {
        return goods_id;
    }

    public void setGoods_id(int goods_id) {
        this.goods_id = goods_id;
    }

    public int getGoods_num() {
        return goods_num;
    }

    public void setGoods_num(int goods_num) {
        this.goods_num = goods_num;
    }

    public int getPrescriptiondtlid() {
        return prescriptiondtlid;
    }

    public void setPrescriptiondtlid(int prescriptiondtlid) {
        this.prescriptiondtlid = prescriptiondtlid;
    }

    public int getPrescriptiontype() {
        return prescriptiontype;
    }

    public void setPrescriptiontype(int prescriptiontype) {
        this.prescriptiontype = prescriptiontype;
    }

    public int getSourceplatform() {
        return sourceplatform;
    }

    public void setSourceplatform(int sourceplatform) {
        this.sourceplatform = sourceplatform;
    }

    public String getUnitprice() {
        return unitprice;
    }

    public void setUnitprice(String unitprice) {
        this.unitprice = unitprice;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }
}
