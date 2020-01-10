package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\1\3 0003 14:07
 */
public class YsqDrugResponse implements Serializable{

    private static final long serialVersionUID = 6943838153364712445L;
    private String storecode;
    private String storename;
    private String yygoods;
    private String storegoods;
    private String drugname;
    private String druggname;
    private String drugspec;
    private String drugproducer;
    private String drugprc;
    private String inventorynum;

    public String getStorecode() {
        return storecode;
    }

    public void setStorecode(String storecode) {
        this.storecode = storecode;
    }

    public String getStorename() {
        return storename;
    }

    public void setStorename(String storename) {
        this.storename = storename;
    }

    public String getYygoods() {
        return yygoods;
    }

    public void setYygoods(String yygoods) {
        this.yygoods = yygoods;
    }

    public String getStoregoods() {
        return storegoods;
    }

    public void setStoregoods(String storegoods) {
        this.storegoods = storegoods;
    }

    public String getDrugname() {
        return drugname;
    }

    public void setDrugname(String drugname) {
        this.drugname = drugname;
    }

    public String getDruggname() {
        return druggname;
    }

    public void setDruggname(String druggname) {
        this.druggname = druggname;
    }

    public String getDrugspec() {
        return drugspec;
    }

    public void setDrugspec(String drugspec) {
        this.drugspec = drugspec;
    }

    public String getDrugproducer() {
        return drugproducer;
    }

    public void setDrugproducer(String drugproducer) {
        this.drugproducer = drugproducer;
    }

    public String getDrugprc() {
        return drugprc;
    }

    public void setDrugprc(String drugprc) {
        this.drugprc = drugprc;
    }

    public String getInventorynum() {
        return inventorynum;
    }

    public void setInventorynum(String inventorynum) {
        this.inventorynum = inventorynum;
    }
}
