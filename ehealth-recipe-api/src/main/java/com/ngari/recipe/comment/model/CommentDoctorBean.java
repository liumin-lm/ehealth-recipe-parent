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
public class CommentDoctorBean implements Serializable {
    private static final long serialVersionUID = 4797761150503386137L;

    @ItemProperty(alias = "平台医生Id")
    private String doctorId;

    @ItemProperty(alias = "医生姓名")
    private String doctorName;

    @ItemProperty(alias = "医生职称Id")
    private String doctorTitleId;

    @ItemProperty(alias = "医生职称")
    private String doctorTitle;

    @ItemProperty(alias = "医生姓名")
    private String doctorJobNumber;

    @ItemProperty(alias = "医生电话")
    private String doctorMobile;
}
