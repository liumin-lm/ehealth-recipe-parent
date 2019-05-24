package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:46
 */
@Schema
public class ChooseDrugParamDTO implements Serializable{

    private static final long serialVersionUID = -2647150552347475921L;

    @Verify(isNotNull = true, desc = "药品通⽤名", maxLength = 64)
    private String drugCommonName;

    @Verify(isNotNull = true, desc = "药品规格", maxLength = 64)
    private String spec;

    @Verify(isNotNull = false, desc = "商品名", maxLength = 64)
    private String durgName;

    @Verify(isNotNull = false, desc = "商品Ids，以,分隔", maxLength = 128)
    private String itemIds;

    @Verify(isNotNull = false, desc = "spuId", maxLength = 20)
    private String spuId;

    public String getDrugCommonName() {
        return drugCommonName;
    }

    public void setDrugCommonName(String drugCommonName) {
        this.drugCommonName = drugCommonName;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getDurgName() {
        return durgName;
    }

    public void setDurgName(String durgName) {
        this.durgName = durgName;
    }

    public String getItemIds() {
        return itemIds;
    }

    public void setItemIds(String itemIds) {
        this.itemIds = itemIds;
    }

    public String getSpuId() {
        return spuId;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
    }
}
