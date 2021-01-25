package com.ngari.recipe.pay.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * created by shiyuping on 2021/1/22
 */
@Data
public class WnExtBusCdrRecipeDTO implements Serializable {
    private static final long serialVersionUID = 6724199234598624208L;
    private String action;
    private String hzxm;
    private String patid;
    private String zje;
    private String ybdm;
    private String ghxh;
    private String cfxhhj;
    private String isynzh;
    private String iszfjs;
    private String ybrc;
    private Map<String, String> psxx;
    private String kzxx;
    private String cflysm;
    private String ksdm;
}
