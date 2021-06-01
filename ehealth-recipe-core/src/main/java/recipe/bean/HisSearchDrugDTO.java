package recipe.bean;

import com.ngari.his.recipe.mode.QueryDrugResTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liuxiaofeng on 2020/12/21.
 */
@Data
public class HisSearchDrugDTO implements Serializable{
    private static final long serialVersionUID = -1009157727453363942L;
    private Integer nextPage;
    private QueryDrugResTO hisDrug;
    private List<SearchDrugDetailDTO> searchList;
}
