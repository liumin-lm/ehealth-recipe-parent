package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 对接上海六院易复诊查询处方库存接口中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzCheckPrescriptionDrugStockDto implements Serializable {
    /**
     * access_token
     */
    private String access_token;
    /**
     * 药品列表
     */
    private List<YfzMesDrugDetailDto> mesDrugDetailList;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public List<YfzMesDrugDetailDto> getMesDrugDetailList() {
        return mesDrugDetailList;
    }

    public void setMesDrugDetailList(List<YfzMesDrugDetailDto> mesDrugDetailList) {
        this.mesDrugDetailList = mesDrugDetailList;
    }
}
