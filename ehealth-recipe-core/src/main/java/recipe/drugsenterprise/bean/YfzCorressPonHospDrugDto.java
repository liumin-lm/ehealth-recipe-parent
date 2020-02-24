package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 对接上海六院易复诊同步药品中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */
@Schema
public class YfzCorressPonHospDrugDto implements Serializable {
    /**
     * access_token
     */
    private String access_token;
    /**
     * 药品列表
     */
    private List<YfzHospDrugDto> hospDrugList;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public List<YfzHospDrugDto> getHospDrugList() {
        return hospDrugList;
    }

    public void setHospDrugList(List<YfzHospDrugDto> hospDrugList) {
        this.hospDrugList = hospDrugList;
    }
}
