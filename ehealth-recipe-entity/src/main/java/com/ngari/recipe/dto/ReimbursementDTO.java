package com.ngari.recipe.dto;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 报销清单转换对象
 * @author zgy
 * @date 2022/6/8 18:20
 */
@Data
public class ReimbursementDTO implements Serializable {
    private static final long serialVersionUID = 7085804215104400293L;

    private String invoiceNumber;

    private Recipe recipe;

    private PatientDTO patientDTO;

    private RecipeOrder recipeOrder;

    private List<Recipedetail> recipeDetailList;
}
