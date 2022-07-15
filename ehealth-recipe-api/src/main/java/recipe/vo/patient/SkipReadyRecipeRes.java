package recipe.vo.patient;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class SkipReadyRecipeRes implements Serializable {
    private static final long serialVersionUID = -5490500468124422358L;

    /**
     * 是否存在待处理处方
     */
    private Boolean haveRecipe;
    /**
     * 处方来源 1 线上处方 2 线下处方
     */
    private Integer recipeSource;
}
