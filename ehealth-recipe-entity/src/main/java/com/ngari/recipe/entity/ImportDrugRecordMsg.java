package com.ngari.recipe.entity;


import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 导入药品记录提示
 *
 * @author liumin 20220616
 */
@Entity
@Schema
@Table(name = "base_importdrug_record_msg")
@Access(AccessType.PROPERTY)
@NoArgsConstructor
@DynamicUpdate
@DynamicInsert
public class ImportDrugRecordMsg implements java.io.Serializable {
    public static final long serialVersionUID = -3983203173007645688L;

    @ItemProperty(alias = "ID")
    private Integer id;

    @ItemProperty(alias = "药品记录ID")
    private Integer importDrugRecordId;

    @ItemProperty(alias = "错误定位")
    private String errLocaction;

    @ItemProperty(alias = "错误提示")
    private String errMsg;

    @ItemProperty(alias = "创建时间")
    private Date createDate;

    @ItemProperty(alias = "最后修改时间")
    private Date lastModifyDate;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "import_drug_record_id")
    public Integer getImportDrugRecordId() {
        return importDrugRecordId;
    }

    public void setImportDrugRecordId(Integer importDrugRecordId) {
        this.importDrugRecordId = importDrugRecordId;
    }

    @Column(name = "err_locaction")
    public String getErrLocaction() {
        return errLocaction;
    }

    public void setErrLocaction(String errLocaction) {
        this.errLocaction = errLocaction;
    }

    @Column(name = "err_msg")
    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    @Column(name = "create_date")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "last_modify_date")
    public Date getLastModifyDate() {
        return lastModifyDate;
    }

    public void setLastModifyDate(Date lastModifyDate) {
        this.lastModifyDate = lastModifyDate;
    }
}