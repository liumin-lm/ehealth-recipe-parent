package recipe.vo.patient;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ReadyRecipeVO implements Serializable {
    private static final long serialVersionUID = -8064021326524394210L;

    private Integer recipeSource;
    private boolean haveRecipe;
}
