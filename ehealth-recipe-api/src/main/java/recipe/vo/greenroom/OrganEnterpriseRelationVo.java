package recipe.vo.greenroom;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.PageVO;

import java.io.Serializable;
import java.util.List;

/**
 * @description： 机构药企关联关系Vo
 * @author： whf
 * @date： 2022-01-10 9:55
 */
@Getter
@Setter
public class OrganEnterpriseRelationVo extends PageVO implements Serializable {
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

    private List<Integer> recipeTypes;

    private List<Integer> decoctionIds;

    /**
     * 1 流转药企，2自建药企
     */
    private Integer type;
    /**
     * 药企名称
     */
    private String name;
    /**
     * 创建类型：1：非自建  0：自建
     */
    private Integer createType;
    /**
     * 药企列表
     */
    private List<DrugsEnterpriseBean> drugsEnterpriseList;

    /**
     * 药品剂型 药企配置后，对应处方单及药品属性，将不支持配送
     */
    private List<String> enterpriseDrugForm;

    /**
     * 支持煎法的类型  药企配置后，将不支持配送 0 非代煎 1 代煎
     */
    private List<Integer> supportDecoctionType;

    /**
     * 支持自取煎法的类型  药企配置后，将不支持自取 0 非代煎 1 代煎
     */
    private List<Integer> supportSelfDecoctionState;
    /**
     * 不可流转医保类型 0不支持自费处方 1不支持医保处方
     */
    private List<Integer> cannotMedicalFlag;
}
