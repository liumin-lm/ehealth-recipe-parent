package com.ngari.recipe.recipe.model;

import com.ngari.patient.dto.UsePathwaysDTO;
import com.ngari.patient.dto.UsingRateDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-04
 */
@Data
public class RateAndPathwaysVO implements Serializable {
    private static final long serialVersionUID = -7257665728902493423L;

    private List<UsingRateDTO> usingRate;

    private List<UsePathwaysDTO> usePathway;

}
