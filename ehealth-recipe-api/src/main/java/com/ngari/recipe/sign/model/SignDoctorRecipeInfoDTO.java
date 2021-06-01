package com.ngari.recipe.sign.model;

import ctd.schema.annotation.FileToken;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema
public class SignDoctorRecipeInfoDTO implements Serializable{
    private static final long serialVersionUID = 4059321523544138843L;
    private Integer id;

    /** 处方订单号*/
    private Integer recipeId;

    /** 医生序列号*/
    private  String caSerCodeDoc;

    /** 药师序列号*/
    private String caSerCodePha;

    /** 医生签名时间戳*/
    private String signCaDateDoc;

    /**医生签名值*/
    private String signCodeDoc;

    /**医生签名文件*/
    private String signFileDoc;

    /**医生签名时间*/
    private Date signDate;

    /** 药师审方时间戳*/
    private String signCaDatePha;

    /**药师审方签名值*/
    private String signCodePha;

    /**药师签名文件*/
    private String signFilePha;

    /**药师审方时间*/
    private Date checkDatePha;

    /**医生手签图片*/
    private String sealDataDoc;

    /**药师手签图片*/
    private String sealDataPha;

    /**医生签名摘要-CA签名证书*/
    private String signRemarkDoc;

    /**药师签名摘要-CA签名证书*/
    private String signRemarkPha;

    /**签名原文*/
    private String signBefText;

    private Date createDate;

    private Date lastmodify;

    /** 签名类型*/
    private String type;

    /**业务类型 1：处方 2：病历*/
    private Integer serverType;

    /**医生手签图片 (oss文件id)  */
    @FileToken
    private String signPictureDoc;

    /**药师手签图片 */
    @FileToken
    private String signPicturePha;

    private String uniqueId;

    private Integer signDoctor;

}
