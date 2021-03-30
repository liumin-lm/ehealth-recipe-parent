package com.ngari.recipe.recipe.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * oss图片文件对象
 *
 * @author fuzi
 */
@Setter
@Getter
public class AttachSealPicDTO implements Serializable {
    private static final long serialVersionUID = -9171130710516904526L;

    private String doctorSignImg;
    private String doctorSignImgToken;
    private String checkerSignImg;
    private String checkerSignImgToken;
}
