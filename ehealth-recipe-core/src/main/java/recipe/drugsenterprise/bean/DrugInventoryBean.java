package recipe.drugsenterprise.bean;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\1\3 0003 11:52
 */
public class DrugInventoryBean {

    private String Appkey;
    private String Appsecret;
    private List<InventoryDrug> DRUGS;

    public String getAppkey() {
        return Appkey;
    }

    public void setAppkey(String appkey) {
        Appkey = appkey;
    }

    public String getAppsecret() {
        return Appsecret;
    }

    public void setAppsecret(String appsecret) {
        Appsecret = appsecret;
    }

    public List<InventoryDrug> getDRUGS() {
        return DRUGS;
    }

    public void setDRUGS(List<InventoryDrug> DRUGS) {
        this.DRUGS = DRUGS;
    }
}
