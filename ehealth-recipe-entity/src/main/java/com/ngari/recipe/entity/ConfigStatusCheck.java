package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author fuzi
 */
@Schema
@Entity
@Table(name = "cdr_config_status_check")
@Access(AccessType.PROPERTY)
public class ConfigStatusCheck implements Serializable {

    private static final long serialVersionUID = 3157281576674463160L;
    @ItemProperty(alias = "主键")
    private Integer id;
    @ItemProperty(alias = "位置：1订单状态（配送到家），2订单状态（到院取药），3处方状态...")
    private Integer location;
    @ItemProperty(alias = "位置备注")
    private String locationRemark;
    @ItemProperty(alias = "源状态")
    private Integer source;
    @ItemProperty(alias = "源名称")
    private String sourceName;
    @ItemProperty(alias = "目标状态")
    private Integer target;
    @ItemProperty(alias = "目标名称")
    private String targetName;
    @ItemProperty(alias = "备注")
    private String remark;


    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "location")
    public Integer getLocation() {
        return location;
    }

    public void setLocation(Integer location) {
        this.location = location;
    }

    @Column(name = "location_remark")
    public String getLocationRemark() {
        return locationRemark;
    }

    public void setLocationRemark(String locationRemark) {
        this.locationRemark = locationRemark;
    }

    @Column(name = "source")
    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    @Column(name = "source_name")
    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @Column(name = "target")
    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    @Column(name = "target_name")
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    @Column(name = "remark")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
