package recipe.drugsenterprise.bean;
/**
 * @Description: 对接英克推送处方中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

@Schema
public class YkRecipeListInfoDto implements Serializable {

    /**
     * 药品详情
     */
    private List<YkRecipeInfoDto> data;

    public List<YkRecipeInfoDto> getData() {
        return data;
    }

    public void setData(List<YkRecipeInfoDto> data) {
        this.data = data;
    }
}
