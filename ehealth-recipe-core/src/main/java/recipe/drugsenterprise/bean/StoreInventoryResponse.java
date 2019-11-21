package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yinsheng
 * @date 2019\11\15 0015 16:05
 */
public class StoreInventoryResponse implements Serializable{
    private static final long serialVersionUID = -1600683451817280554L;

    private Long busiid;
    private Long wareid;
    private String businm;
    private String hwarecode;
    private String warename;
    private String waregeneralname;
    private String factory;
    private String warespec;
    private String minunit;
    private double price;
    private double storeqty;
    private double minprice;
    private double minstoreqty;
    private Date itime;
    private Date utime;

    public Long getBusiid() {
        return busiid;
    }

    public void setBusiid(Long busiid) {
        this.busiid = busiid;
    }

    public Long getWareid() {
        return wareid;
    }

    public void setWareid(Long wareid) {
        this.wareid = wareid;
    }

    public String getBusinm() {
        return businm;
    }

    public void setBusinm(String businm) {
        this.businm = businm;
    }

    public String getHwarecode() {
        return hwarecode;
    }

    public void setHwarecode(String hwarecode) {
        this.hwarecode = hwarecode;
    }

    public String getWarename() {
        return warename;
    }

    public void setWarename(String warename) {
        this.warename = warename;
    }

    public String getWaregeneralname() {
        return waregeneralname;
    }

    public void setWaregeneralname(String waregeneralname) {
        this.waregeneralname = waregeneralname;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public String getWarespec() {
        return warespec;
    }

    public void setWarespec(String warespec) {
        this.warespec = warespec;
    }

    public String getMinunit() {
        return minunit;
    }

    public void setMinunit(String minunit) {
        this.minunit = minunit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getStoreqty() {
        return storeqty;
    }

    public void setStoreqty(double storeqty) {
        this.storeqty = storeqty;
    }

    public double getMinprice() {
        return minprice;
    }

    public void setMinprice(double minprice) {
        this.minprice = minprice;
    }

    public double getMinstoreqty() {
        return minstoreqty;
    }

    public void setMinstoreqty(double minstoreqty) {
        this.minstoreqty = minstoreqty;
    }

    public Date getItime() {
        return itime;
    }

    public void setItime(Date itime) {
        this.itime = itime;
    }

    public Date getUtime() {
        return utime;
    }

    public void setUtime(Date utime) {
        this.utime = utime;
    }
}
