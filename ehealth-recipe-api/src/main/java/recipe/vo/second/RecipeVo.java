package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description： 端用处方信息
 * @author： whf
 * @date： 2021-11-08 17:22
 */
@Getter
@Setter
@ToString
public class RecipeVo implements Serializable {

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "开方机构名称")
    private String organName;

    @ItemProperty(alias = "开方科室")
    private String depart;

    @ItemProperty(alias = "开方医生（医生Id）")
    private String doctor;

    @ItemProperty(alias = "机构疾病名称")
    private String organDiseaseName;
}
