package com.ngari.recipe.hisprescription.model;

/**
 * @author yinsheng
 * @date 2019\12\25 0025 15:34
 */
public class DetaillistDTO {
    /**
     *  "recipedtlno": "rx00101",
     "drugcode": "YP1128",
     "quantity": "10",
     "memo": ""
     */

    private String recipedtlno;
    private String drugcode;
    private String quantity;
    private String memo;

    public String getRecipedtlno() {
        return recipedtlno;
    }

    public void setRecipedtlno(String recipedtlno) {
        this.recipedtlno = recipedtlno;
    }

    public String getDrugcode() {
        return drugcode;
    }

    public void setDrugcode(String drugcode) {
        this.drugcode = drugcode;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
