package com.ngari.recipe.recipe.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2020/7/24
 */
@Data
@NoArgsConstructor
public class RankShiftList implements Serializable {
    private String code;
    private String name;
    private List<RankShiftList> rankShiftList;

    public RankShiftList(String name) {
        this.name = name;
    }

}
