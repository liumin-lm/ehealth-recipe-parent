package recipe.vo.doctor;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description： 药房 vo
 * @author： whf
 * @date： 2021-11-23 20:42
 */
@Setter
@Getter
@ToString
public class PharmacyTcmVO implements Serializable {
    @ItemProperty(alias = "药房ID")
    private  Integer pharmacyId;

    @ItemProperty(alias = "药房编码")
    private String pharmacyCode;

    @ItemProperty(alias = "药房名称")
    private String pharmacyName;

    @ItemProperty(alias = "药房分类")
    private String category;

    @ItemProperty(alias = "是否默认")
    private Boolean whDefault;

    @ItemProperty(alias = "药房排序")
    private Integer sort;

    @ItemProperty(alias = "机构ID")
    private Integer organId;

    @ItemProperty(alias = "药房类型")
    @Deprecated
    private String type;

    @ItemProperty(alias = "药房类型")
    private String pharmacyCategray;
}
