package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;

/**
 * 同步电子票号
 */
public class SyncEinvoiceNumberDTO implements Serializable {

    private static final long serialVersionUID = 7582949428033413379L;

    @ItemProperty(alias="组织机构编码")
    private String organId;//

    @ItemProperty(alias="机构名称")
    private String organName;//

    @ItemProperty(alias="HIS结算单据号")
    private String invoiceNo;//

    @ItemProperty(alias = "电子票号")
    private String einvoiceNumber;//

    @ItemProperty(alias = "开票日期")
    private String issueDate;//

    @ItemProperty(alias = "票据类型 1线上复诊2线上处方")
    private String einvoiceType;//

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getEinvoiceNumber() {
        return einvoiceNumber;
    }

    public void setEinvoiceNumber(String einvoiceNumber) {
        this.einvoiceNumber = einvoiceNumber;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getEinvoiceType() {
        return einvoiceType;
    }

    public void setEinvoiceType(String einvoiceType) {
        this.einvoiceType = einvoiceType;
    }
}
