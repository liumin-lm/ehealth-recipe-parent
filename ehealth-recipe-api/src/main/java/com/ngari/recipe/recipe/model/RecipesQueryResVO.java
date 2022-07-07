package com.ngari.recipe.recipe.model;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 处方查询出参
 *
 * @author fuzi
 */
@Getter
@Setter
public class RecipesQueryResVO extends RecipeBean implements Serializable {
    private static final long serialVersionUID = 1006821231283734183L;

    private Long detailCount;

    private String bussSourceText;

    private String drugsEnterprise;

    private Date payDate;

    private String giveModeText;

    private RecipeExtendBean recipeExtend;

    private RecipeOrderBean recipeOrder;

    private PatientDTO patient;


}