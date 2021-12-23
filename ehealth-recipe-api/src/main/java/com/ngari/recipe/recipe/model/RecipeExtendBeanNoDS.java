package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 处方扩展信息
 */
@Schema
@Data
public class RecipeExtendBeanNoDS implements Serializable {

    @ItemProperty(alias = "监管人证件号")
    private String guardianCertificate;
    @ItemProperty(alias = "监管人手机号")
    private String guardianMobile;
    @ItemProperty(alias = "HIS处方关联的卡号")
    private String cardNo;
}
