package com.ngari.recipe.entity;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by  on 2017/10/23.
 * @author jiangtingfeng
 */
@Schema
@Entity
@Table(name = "cdr_priortydrugbinddoctor")
@Access(AccessType.PROPERTY)
public class PriortyDrugBindDoctor
{
    @ItemProperty(alias="主键")
    private int id;

    @ItemProperty(alias="重点药品ID")
    private Integer drugId;

    @ItemProperty(alias="能开该重点药品的医生列表")
    private Integer doctorId;

    @ItemProperty(alias="创建时间")
    private Date createTime;

    @Id
    @Column(name = "id")
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @Column(name = "drugId")
    public Integer getDrugId()
    {
        return drugId;
    }

    public void setDrugId(Integer drugId)
    {
        this.drugId = drugId;
    }

    @Column(name = "createTime")
    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Column(name = "doctorId")
    public Integer getDoctorId()
    {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId)
    {
        this.doctorId = doctorId;
    }
}
