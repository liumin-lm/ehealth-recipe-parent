package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 卡号
 *
 * @author 刘敏
 * @date
 */
@Schema
@Data
public class CardBean implements Serializable {

    private static final long serialVersionUID = -8882418262625511814L;
    private String cardType;
    @Desensitizations(type = DesensitizationsType.HEALTHCARD)
    private String cardNo;
    private String cardTypeName;

}
