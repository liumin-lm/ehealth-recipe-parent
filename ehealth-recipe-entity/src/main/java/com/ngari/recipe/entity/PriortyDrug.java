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
@Table(name = "cdr_priortydrug")
@Access(AccessType.PROPERTY)
public class PriortyDrug
{
    @ItemProperty(alias="主键")
    private int id;

    @ItemProperty(alias="重点药品ID")
    private Integer drugId;

    @ItemProperty(alias="能开该重点药品的医生列表")
    private Integer sort;

    @ItemProperty(alias="药品图片id，前端可依据图片id查询文件服务器")
    private Integer drugPicId;

    @ItemProperty(alias="创建时间")
    private Date createTime;

    @ItemProperty(alias="创建时间")
    private Date lastModify;

    @Column(name = "drugPicId")
    public Integer getDrugPicId() {
        return drugPicId;
    }

    public void setDrugPicId(Integer drugPicId) {
        this.drugPicId = drugPicId;
    }

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

    @Column(name = "sort")
    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }
    @Column(name = "lastModify")
    public Date getLastModify()
    {
        return lastModify;
    }

    public void setLastModify(Date lastModify)
    {
        this.lastModify = lastModify;
    }
}
