package com.ngari.recipe.entity;

import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.io.Serializable;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * 医生默认数据
 * @author fuzi
 */
@Entity
@Schema
@Table(name = "cdr_doctor_default")
@Access(AccessType.PROPERTY)
public class DoctorDefault implements Serializable {

    private static final long serialVersionUID = -746651295596084377L;

    private Integer id;

    private Integer organId;

    private Integer doctorId;
    /**
     * 类别 ：0:默认，1:药房，2药企
     */
    private Integer category;
    /**
     * 0 默认
     * 类型：
     * category =1 时type： 1西药 2中成药 3中药 4膏方
     * category =2 时type： 1药企 2机构
     */
    private Integer type;
    /**
     * 关联数据主键，如：药房id，药企id
     */
    private Integer idKey;


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

    @Column(name = "doctor_id")
    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Column(name = "id_key")
    public Integer getIdKey() {
        return idKey;
    }

    public void setIdKey(Integer idKey) {
        this.idKey = idKey;
    }
}
