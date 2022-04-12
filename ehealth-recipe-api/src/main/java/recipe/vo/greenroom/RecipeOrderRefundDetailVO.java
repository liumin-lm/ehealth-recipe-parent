package recipe.vo.greenroom;

import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class RecipeOrderRefundDetailVO implements Serializable {
    private static final long serialVersionUID = 4052678532528362339L;

    private RecipeOrderBean recipeOrderBean;

    private List<RecipeBean> recipeBeanList;

    private DrugsEnterpriseBean drugsEnterpriseBean;

    private PatientDTO patientDTO;

}
