package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 字典信息对象
 *
 * @author yins
 */
@Getter
@Setter
public class RecipeDictionaryVO implements Serializable {
    private static final long serialVersionUID = -3504678068471963119L;

    @ItemProperty(alias = "字典id")
    private Integer id;
    @ItemProperty(alias = "机构id")
    private Integer organId;
    @ItemProperty(alias = "字典类型 1 超量原因")
    private Integer dictionaryType;
    @ItemProperty(alias = "字典编码")
    private String dictionaryCode;
    @ItemProperty(alias = "字典名称")
    private String dictionaryName;
    @ItemProperty(alias = "项目价格")
    private BigDecimal itemPrice;
    @ItemProperty(alias = "字典拼音")
    private String dictionaryPingying;
    @ItemProperty(alias = "1 表示删除，0 表示未删除")
    private Integer isDelete;
    @ItemProperty(alias = "字典排序")
    private Integer dictionarySort;
    @ItemProperty(alias = "创建时间")
    private Date gmtCreate;

}
