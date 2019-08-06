package recipe.drugsenterprise.bean;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
* @Description: 对接英特处方下详门店下药品的库存信息中间对象
* @Author: JRK
* @Date: 2019/7/10
*/
@Schema
public class YtStockResponse implements Serializable{
    private static final long serialVersionUID = -1053592285452361394L;

    /**
     * 门店机构编码
     */
    private String orgCode;
    /**
     * 总店机构编码
     */
    private String parentOrgCode;
    /**
     * 商品编码
     */
    private String code;
    /**
     * 价格
     */
    private Double price;
    /**
     * 库存数量
     */
    private Double stock;

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getParentOrgCode() {
        return parentOrgCode;
    }

    public void setParentOrgCode(String parentOrgCode) {
        this.parentOrgCode = parentOrgCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }
}