package com.ngari.recipe.recipe.model;

import com.ngari.his.recipe.mode.Consult;
import com.ngari.patient.dto.PatientDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author zgy
 * @date 2022/1/10 17:19
 */
@Setter
@Getter
public class WriteDrugRecipeDTO implements Serializable {

    private static final long serialVersionUID = -1182841922613719429L;

    private PatientDTO patient;
    private PatientDTO requestPatient;
    private Consult consult;
    private Integer type;
    private WriteDrugRecipeBean writeDrugRecipeBean;
}
