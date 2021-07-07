package recipe.bean;

import com.ngari.patient.dto.PatientDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author fuzi
 */
@Setter
@Getter
public class RecipePdfDTO extends RecipeDTO implements Serializable {
    private static final long serialVersionUID = 4097986146206606609L;
    private PatientDTO patientBean;
}
