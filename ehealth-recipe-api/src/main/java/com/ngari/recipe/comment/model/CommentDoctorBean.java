package com.ngari.recipe.comment.model;

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

    private String doctName;

    private String doctTitleId;

    private String doctorJobNumber;
}
