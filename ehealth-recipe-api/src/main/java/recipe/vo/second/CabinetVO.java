package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 存储药柜传输对象
 */
@Data
public class CabinetVO implements Serializable {

    @ItemProperty(alias = "纳里平台机构Id号")
    private Integer organId;

    @ItemProperty(alias = "his处方号")
    private String recipeCode;

    @ItemProperty(alias = "到院取药-是否有效：true有效，false无效")
    private Boolean effectiveFlag;

    @ItemProperty(alias = "患者联系方式")
    private String mobile;

    @ItemProperty(alias = "取药地址")
    private String medicineAddress;

    @ItemProperty(alias = "药柜号")
    private String cabinetId;

    @ItemProperty(alias = "药柜地址")
    private String cabinetAddress;

    @ItemProperty(alias = "格子位置")
    private String latticeLocation;

    @ItemProperty(alias = "取药码")
    private String medicineCode;

    @ItemProperty(alias = "取药码过期时间")
    private String medicineCodeInvalidTime;

    @ItemProperty(alias = "取件角色：admin 管理员;patient 患者")
    private String role;
}
