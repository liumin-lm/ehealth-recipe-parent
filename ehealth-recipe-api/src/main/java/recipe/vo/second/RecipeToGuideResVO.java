package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 就医引导处方出参
 * @author： whf
 * @date： 2022-11-17 9:48
 */
@Getter
@Setter
@ToString
public class RecipeToGuideResVO implements Serializable {
    private static final long serialVersionUID = -8551482281042853L;

    @ItemProperty(alias = "处方序号")
    private Integer recipeId;

    @ItemProperty(alias = "就诊序号(对应来源的业务id)  复诊id")
    private Integer clinicId;

    @ItemProperty(alias = "处方类型 1 西药 2 中成药 3 中药 4膏方")
    @Dictionary(id = "eh.cdr.dictionary.RecipeType")
    private Integer recipeType;

    @ItemProperty(alias = "是否隐方")
    private Boolean isHiddenRecipeDetail;

    @ItemProperty(alias = "药店或者站点地址")
    private String drugStoreAddr;

    @ItemProperty(alias = "取药窗口")
    private String pharmNo;

    @ItemProperty(alias = "处方父状态：0：默认 ， 1：待提交，2：待审核，3：待够药，4：待发药，5：配送中，6：待取药，7：已完成，8：已删除 ，9：已作废")
    private Integer processState;

    @ItemProperty(alias = "药品信息")
    private List<RecipeDetailToGuideResVO> recipeDetailToGuideResVOList;
}
