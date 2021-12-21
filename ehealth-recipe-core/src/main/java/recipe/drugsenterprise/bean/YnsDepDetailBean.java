package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/7/3.
 * 对接药企 无需脱敏
 */
@Schema
public class YnsDepDetailBean {

    /**
     * 药店名称
     */
    private String pharmacyName;
    /**
     * 药店编码
     */
    private String pharmacyCode;
    /**
     * 药店地址
     */
    private String address;
    //药店坐标
    private YnsPosition position;
    //距离
    private String distance;

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public YnsPosition getPosition() {
        return position;
    }

    public void setPosition(YnsPosition position) {
        this.position = position;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }
}
