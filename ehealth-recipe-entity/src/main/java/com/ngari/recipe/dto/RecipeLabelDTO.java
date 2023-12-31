package com.ngari.recipe.dto;

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
public class RecipeLabelDTO implements Serializable {

    private String name;
    private String englishName;
    private Object value;

    public RecipeLabelDTO(String name, String englishName, Object value) {
        this.name = name;
        this.englishName = englishName;
        this.value = value;
    }
}
