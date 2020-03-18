package com.ngari.recipe.regulation.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class QueryRegulationUnitDTO implements Serializable{
    private static final long serialVersionUID = -5005144351071569121L;

    private List<Integer> ngariOrganIds;
    private Date startTime;
    private Date endTime;

    public List<Integer> getNgariOrganIds() {
        return ngariOrganIds;
    }

    public void setNgariOrganIds(List<Integer> ngariOrganIds) {
        this.ngariOrganIds = ngariOrganIds;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
