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
    private String longitude;

    //纬度
    private String latitude;

    public Integer getRange() {
        return range;
    }

    public void setRange(Integer range) {
        this.range = range;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
}
