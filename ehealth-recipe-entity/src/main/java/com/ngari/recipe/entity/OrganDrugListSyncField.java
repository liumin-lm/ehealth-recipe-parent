package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import org.springframework.util.ObjectUtils;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 机构药品目录同步字段
 *
 *
 */
@Entity
@Schema
@Table(name = "organ_drug_list_sync_field")
@Access(AccessType.PROPERTY)
public class OrganDrugListSyncField implements java.io.Serializable {
    private static final long serialVersionUID = -7090271704460035622L;

    @ItemProperty(alias = "id")
    private Integer id;

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "<dic>\n" +
            "\t<item key=\"1\" text=\"新增药品\"/>\n" +
            "\t<item key=\"2\" text=\"更新药品\"/>\n" +
            "\t<item key=\"3\" text=\"删除药品\"/>\n" +
            "</dic>")
    private String type;

    @ItemProperty(alias = "字段名称")
    private String fieldName;

    @ItemProperty(alias = "字段编码")
    private String fieldCode;

    @ItemProperty(alias = "是否同步 同步勾选 0否 1是")
    private String isSync;

    @ItemProperty(alias = "是否允许编辑 0不允许 1允许")
    private String isAllowEdit;

    @ItemProperty(alias = "创建时间")
    private Date createTime;

    @ItemProperty(alias = "更新时间")
    private Date updateTime;

    @Column(name = "is_allow_edit")
    public String getIsAllowEdit() {
        return isAllowEdit;
    }

    public void setIsAllowEdit(String isAllowEdit) {
        this.isAllowEdit = isAllowEdit;
    }

    @Column(name = "create_time", length = 19)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "update_time", length = 19)
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "organ_id")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "field_name")
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Column(name = "field_code")
    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    @Column(name = "is_sync")
    public String getIsSync() {
        return isSync;
    }

    public void setIsSync(String isSync) {
        this.isSync = isSync;
    }

    public void init(OrganDrugListSyncField obj) {
        if (ObjectUtils.isEmpty(obj.getCreateTime())){
            obj.setCreateTime(new Date());
        }
        if (ObjectUtils.isEmpty(obj.getIsSync())){
            obj.setIsSync("1");
        }
        obj.setUpdateTime(new Date());
    }

}