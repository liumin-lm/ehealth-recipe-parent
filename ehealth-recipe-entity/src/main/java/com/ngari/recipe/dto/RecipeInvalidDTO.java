package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Created by liuxiaofeng on 2021/2/20 0020.
 */
@Data
public class RecipeInvalidDTO implements Serializable {

    private static final long serialVersionUID = -5087587614407023997L;
    private String invalidType;
    private Date invalidDate;
}