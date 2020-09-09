package com.ngari.recipe.drugsenterprise.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author fuzi
 */
@Setter
@Getter
public class RecipeLabelVO implements Serializable {

    private String name;
    private String englishName;
    private Object value;
}
