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
public class CommentBean implements Serializable {
    private static final long serialVersionUID = 2479380746145351244L;

    @ItemProperty(alias = "平台点评记录Id")
    private Integer commentId;

    @ItemProperty(alias = "点评结果编码：0:不通过，1：通过")
    private Integer commentResultCode;

    @ItemProperty(alias = "点评结果")
    private String commentResult;

    @ItemProperty(alias = "点评备注")
    private String commentRemark;

    @ItemProperty(alias = "点评人姓名")
    private String commentUserName;

    @ItemProperty(alias = "点评人类型")
    private String commentUserType;

    @ItemProperty(alias = "点评时间")
    private String commentTime;
}
