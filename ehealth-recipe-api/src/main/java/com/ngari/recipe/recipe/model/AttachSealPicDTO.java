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
    /**
     * 医生签名图片
     */
    private String doctorSignImg;
    private String doctorSignImgToken;
    /**
     * 审方药师签名图片
     */
    private String checkerSignImg;
    private String checkerSignImgToken;
    /**
     * 核发药师签名图片
     */
    private String giveUserSignImg;
    private String giveUserSignImgToken;

}
