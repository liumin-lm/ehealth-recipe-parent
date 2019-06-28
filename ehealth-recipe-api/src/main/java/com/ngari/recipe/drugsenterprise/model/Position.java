package com.ngari.recipe.drugsenterprise.model;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\6\10 0010 10:22
 */
public class Position implements Serializable{
    private static final long serialVersionUID = 3200925007210364579L;

    //范围
    private Integer range;

    //经度
    private Double longitude;

    //纬度
    private Double latitude;

    public Integer getRange() {
        return range;
    }

    public void setRange(Integer range) {
        this.range = range;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
}
