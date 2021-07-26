package com.ngari.recipe.dto;

import lombok.*;

import java.io.Serializable;

/**
 * 用于处方开方
 *
 * @author fuzi
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoSignRecipeDTO implements Serializable {
    private static final long serialVersionUID = -3209609337037005713L;
    private Boolean signResult;
    private Boolean errorFlag;
    private String canContinueFlag;
    private String msg;
    private Integer recipeId;
    private Integer checkFlag;
}
