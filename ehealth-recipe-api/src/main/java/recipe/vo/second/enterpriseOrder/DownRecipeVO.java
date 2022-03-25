package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.base.BaseRecipeDetailVO;
import recipe.vo.base.BaseRecipeVO;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 第三方下载处方信息
 * @author： yins
 * @date： 2021-12-08 15:50
 */
@Getter
@Setter
public class DownRecipeVO extends BaseRecipeVO implements Serializable {
    private static final long serialVersionUID = -1550787664443643742L;

    @ItemProperty(alias = "开方机构")
    private Integer organId;

    @ItemProperty(alias = "签名文件的URL")
    private String signFileUrl;

    @ItemProperty(alias = "审方机构名称")
    private String checkOrganName;

    @ItemProperty(alias = "审核药师姓名")
    private String checkerName;

    @ItemProperty(alias = "处方详情")
    private List<BaseRecipeDetailVO> recipeDetailList;
}
