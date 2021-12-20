package com.ngari.recipe.drugsenterprise.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;


@Schema
@Data
public class DrugsEnterpriseRes extends DrugsEnterpriseBean implements Serializable {

    private static final long serialVersionUID = 3811188626885371263L;

    @JsonIgnore
    @ItemProperty(alias = "药企在平台的账户")
    private String account;

    @JsonIgnore
    @ItemProperty(alias = "密码")
    private String password;

    @JsonIgnore
    @ItemProperty(alias = "药企联系电话")
    private String tel;

}
