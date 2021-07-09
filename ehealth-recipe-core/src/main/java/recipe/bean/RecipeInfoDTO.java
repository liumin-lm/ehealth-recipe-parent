package recipe.bean;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author fuzi
 */
@Setter
@Getter
public class RecipeInfoDTO extends RecipeDTO implements Serializable {
    private static final long serialVersionUID = 4097986146206606609L;
    /**
     * 患者信息
     */
    private PatientDTO patientBean;
    /**
     * 签名信息
     */
    private ApothecaryVO apothecary;

}
