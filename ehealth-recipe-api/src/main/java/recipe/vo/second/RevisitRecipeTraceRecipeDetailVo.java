//package recipe.vo.second;
//
//import ctd.schema.annotation.Dictionary;
//import ctd.schema.annotation.ItemProperty;
//import lombok.Data;
//
//import java.io.Serializable;
//import java.math.BigDecimal;
//
///**
// * @author liumin
// */
//@Data
//public class RevisitRecipeTraceRecipeDetailVo implements Serializable {
//
//    private static final long serialVersionUID = -8882418262625511818L;
//
//    @ItemProperty(alias = "药物名称")
//    private String drugName;
//
//    @ItemProperty(alias = "药品商品名")
//    private String saleName;
//
//    @ItemProperty(alias = "药物使用途径代码")
//    @Dictionary(id = "eh.cdr.dictionary.UsePathways")
//    private String usePathways;
//
//    @ItemProperty(alias = "药物使用次剂量")
//    private Double useDose;
//
//    @ItemProperty(alias = "药物使用规格单位")
//    private String useDoseUnit;
//
//    @ItemProperty(alias = "药物规格")
//    private String drugSpec;
//
//    @ItemProperty(alias = "药物单位")
//    private String drugUnit;
//
//    @ItemProperty(alias = "生产厂家")
//    private String producer;
//
//    @ItemProperty(alias = "药物使用总数量")
//    private Double useTotalDose;
//
//    @ItemProperty(alias = "药物单位")
//    private String drugUnit;
//
//    @ItemProperty(alias = "销售价格")
//    private BigDecimal salePrice;
//
//}
