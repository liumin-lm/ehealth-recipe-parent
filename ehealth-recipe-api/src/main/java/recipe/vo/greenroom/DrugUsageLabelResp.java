package recipe.vo.greenroom;

import com.ngari.recipe.recipe.ChineseMedicineMsgVO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Description
 * @Author yzl
 * @Date 2022-06-02
 */
@Data
@Schema
public class DrugUsageLabelResp implements Serializable {
    private static final long serialVersionUID = 3737516056719140481L;

    @ItemProperty(alias = "药企名称")
    private String enterpriseName;

    private String patientName;

    private String organName;

    private String patientSex;

    private String patientAge;

    @ItemProperty(alias = "发药时间")
    private Date dispensingTime;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药 3 中药 4膏方")
    private Integer recipeType;

    List<RecipeDetailBean> drugUsageLabelList;

    ChineseMedicineMsgVO chineseMedicineMsg;
}
