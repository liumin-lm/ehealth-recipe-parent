package com.ngari.recipe.comment.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description
 * @Author yzl
 * @Date 2023-02-22
 */
@Data
public class CommentPatientBean implements Serializable {
    private static final long serialVersionUID = 2435793659857228426L;

    @ItemProperty(alias = "就诊人电子健康卡")
    private String cardId;

    @ItemProperty(alias = "就诊人证件类型")
    private Integer certType;

    @ItemProperty(alias = "就诊人证件号")
    private String certNo;

    @ItemProperty(alias = "姓名")
    private String patientName;

    @ItemProperty(alias = "年龄")
    private String patientAge;

    @ItemProperty(alias = "性别")
    private Integer patientSex;

    @ItemProperty(alias = "就诊人电话")
    private String patientMobile;

    @ItemProperty(alias = "监护人证件类型")
    private String guardianCertType;

    @ItemProperty(alias = "监护人证件号码")
    private String guardianCertNo;

    @ItemProperty(alias = "监护人手机号")
    private String guardianMobile;
}
