package com.ngari.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排列组合对象
 * @author fuzi
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermutationDTO {
    private String key;
    private List<Integer> value;
}
