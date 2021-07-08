package com.ngari.recipe.drugsenterprise.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 处方签生成字段对象
 *
 * @author fuzi
 */
@Setter
@Getter
public class RecipeLabelVO implements Serializable {

    private String name;
    private String englishName;
    private Object value;

    public RecipeLabelVO(String name, String englishName, Object value) {
        this.name = name;
        this.englishName = englishName;
        this.value = value;
    }
}
