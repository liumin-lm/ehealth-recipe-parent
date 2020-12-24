package com.ngari.recipe.drug.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * @author Zhou Wenfei
 * date 2020/11/18 19:30
 */
@Builder
public class DrugInventoryInfo implements Serializable {
    private static final long serialVersionUID = 7452550864993181555L;

    @ItemProperty(alias = "库存类型 his-医院库存")
    private String type;

    @ItemProperty(alias = "药房库存列表")
    private List<DrugPharmacyInventoryInfo> pharmacyInventories;

    @ItemProperty(alias = "远程查询状态0 - 查询成功 1-查询失败")
    private String remoteQueryStatus;

    public DrugInventoryInfo() {

    }
    public DrugInventoryInfo(String type, List<DrugPharmacyInventoryInfo> pharmacyInventories, String remoteQueryStatus) {
        this.type = type;
        this.pharmacyInventories = pharmacyInventories;
        this.remoteQueryStatus = remoteQueryStatus;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<DrugPharmacyInventoryInfo> getPharmacyInventories() {
        return pharmacyInventories;
    }

    public void setPharmacyInventories(List<DrugPharmacyInventoryInfo> pharmacyInventories) {
        this.pharmacyInventories = pharmacyInventories;
    }

    public String getRemoteQueryStatus() {
        return remoteQueryStatus;
    }

    public void setRemoteQueryStatus(String remoteQueryStatus) {
        this.remoteQueryStatus = remoteQueryStatus;
    }
}
