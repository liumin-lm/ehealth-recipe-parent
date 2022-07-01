package com.ngari.recipe.entity;


import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 导入药品记录
 *
 * @author yinfeng 2020-05-21
 */
@Entity
@Schema
@Table(name = "base_importdrug_record")
@Access(AccessType.PROPERTY)
public class ImportDrugRecord implements java.io.Serializable {
    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "药品记录ID")
    private Integer recordId;

    @ItemProperty(alias = "导入文件名称")
    private String fileName;

    @ItemProperty(alias = "新增药品数")
    private Integer addNum;

    @ItemProperty(alias = "更新药品数")
    private Integer updateNum;

    @ItemProperty(alias = "失败药品数")
    private Integer failNum;

    @ItemProperty(alias = "导入人员")
    private String importOperator;

    @ItemProperty(alias = "错误提示")
    private String errMsg;

    @ItemProperty(alias = "创建时间")
    private Date createDt;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModify;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "导入状态 1、导入成功——指写入平台成功 2、正在导入——还没存在oss里，或者已存在oss里，未但导入至平台 3、导入失败，点击查看原因——导入失败（不管哪里失败都展示失败）=第三种，点击查看原因，出现报错内容弹窗，如右图所示")
    private Integer status;

    @FileToken
    private String fileId;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "RecordId", unique = true, nullable = false)
    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    @Column(name = "FileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Column(name = "AddNum")
    public Integer getAddNum() {
        return addNum;
    }

    public void setAddNum(Integer addNum) {
        this.addNum = addNum;
    }

    @Column(name = "UpdateNum")
    public Integer getUpdateNum() {
        return updateNum;
    }

    public void setUpdateNum(Integer updateNum) {
        this.updateNum = updateNum;
    }

    @Column(name = "FailNum")
    public Integer getFailNum() {
        return failNum;
    }

    public void setFailNum(Integer failNum) {
        this.failNum = failNum;
    }

    @Column(name = "ImportOperator")
    public String getImportOperator() {
        return importOperator;
    }

    public void setImportOperator(String importOperator) {
        this.importOperator = importOperator;
    }

    @Column(name = "errMsg")
    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    @Column(name = "CreateDt")
    public Date getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Date createDt) {
        this.createDt = createDt;
    }

    @Column(name = "LastModify")
    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    @Column(name = "OrganId")
    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }
}