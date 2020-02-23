package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @Description: 对接上海六院易复诊开处方接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzTADrugStoreDto implements Serializable {
    /**
     * 支付方式，默认值线上支付
     */
    private String id;
    /**
     * 收货人姓名
     */
    private String name;
    /**
     * 收货地址
     */
    private String address;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
