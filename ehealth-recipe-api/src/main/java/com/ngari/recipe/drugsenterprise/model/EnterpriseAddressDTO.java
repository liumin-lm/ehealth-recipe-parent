package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

/**
 * 药企配送地址
 *
 * @company: Ngarihealth
 * @author: zhongzixuan
 * @date:2016/6/8.
 */

@Schema
public class EnterpriseAddressDTO implements java.io.Serializable {

    private static final long serialVersionUID = -5216911762475290638L;

    @ItemProperty(alias = "药企地址序号")
    private Integer id;

    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "药企配送地址")
    private String address;

    @ItemProperty(alias = "配送地址状态")
    private Integer status;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Integer enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
