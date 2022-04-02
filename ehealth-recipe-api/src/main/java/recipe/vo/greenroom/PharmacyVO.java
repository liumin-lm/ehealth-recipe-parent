package recipe.vo.greenroom;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

@Data
public class PharmacyVO {
    @ItemProperty(alias = "关联的药企id")
    private Integer drugsenterpriseId;
    @ItemProperty(alias = "药店电话号")
    private String pharmacyPhone;
    @ItemProperty(alias = "药店地址")
    private String pharmacyAddress;
    @ItemProperty(alias = "药店经度")
    private String pharmacyLongitude;
    @ItemProperty(alias = "药店纬度")
    private String pharmacyLatitude;
}
