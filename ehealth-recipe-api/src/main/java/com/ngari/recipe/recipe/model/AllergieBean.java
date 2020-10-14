package com.ngari.recipe.recipe.model;


import java.io.Serializable;

public class AllergieBean implements Serializable{
    private static final long serialVersionUID = -8672280465582957714L;

    private String type; //类型 0=其他 1=药品大类 2=药品成份 5= HIS 药品代码

    private String name; //过敏源名称

    private String code; //过敏源代码

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
