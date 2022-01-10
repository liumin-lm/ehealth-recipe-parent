package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 机构药企关联关系Vo
 * @author： whf
 * @date： 2022-01-10 9:55
 */
@Getter
@Setter
public class OrganEnterpriseRelationVo implements Serializable {

    /**
     * 机构id
     */
    private Integer organId;

    /**
     * 药企id
     */
    private Integer drugsEnterpriseId;

    /**
     * 支持的购药方式 见 RecipeSupportGiveModeEnum
     */
    private List<Integer> giveModeTypes;
}
