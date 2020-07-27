package com.ngari.recipe.recipe.model;

import com.ngari.his.recipe.mode.ChronicDiseaseListResTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * created by shiyuping on 2020/7/24
 */
@Data
@NoArgsConstructor
public class RankShiftList extends ChronicDiseaseListResTO implements Serializable {
    private String chronicDiseaseFlagText;
    private List<RankShiftList> rankShiftList;
}
