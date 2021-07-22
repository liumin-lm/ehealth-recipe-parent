//package recipe.offlinetoonline.vo;
//
//import com.ngari.recipe.recipe.model.HisRecipeVO;
//import lombok.Data;
//
//import java.io.Serializable;
//import java.util.List;
//
///**
// * @author liumin
// * @date 2021\6\30
// */
//@Data
//public class FindHisRecipeListResVO  implements Serializable{
//    private static final long serialVersionUID = 4183151633663146015L;
//
//    /**
//     * 分组字段
//     */
//    private String groupField;
//
//    private List<HisRecipeVO> recipe;
//
//    /**
//     * 是否合并处方标识
//     */
//    private Boolean mergeRecipeFlag;
//
//    /**
//     * 第一个处方单id 排序用
//     */
//    private Integer firstRecipeId;
//
//    /**
//     * 合并支付的机构配置
//     * e.registerId支持同一个挂号序号下的处方合并支付
//     * e.registerId,e.chronicDiseaseName 支持同一个挂号序号且同一个病种的处方合并支付
//     */
//    private String mergeRecipeWay;
//
//}
