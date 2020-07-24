package com.ngari.recipe.recipe.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class AllergieBean implements Serializable{
    private static final long serialVersionUID = -8672280465582957714L;

    private String type; //类型 0=其他 1=药品大类 2=药品成份 5= HIS 药品代码

    private String name; //过敏源名称

    private String code; //过敏源代码
}
