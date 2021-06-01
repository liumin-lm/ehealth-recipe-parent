package recipe.drugsenterprise.bean;

/**
 * @author yinsheng
 * @date 2020\1\3 0003 11:53
 */
public class InventoryDrug {

    private String DRUGCODE;
    private String DRUGNAME;
    private String DRUGGNAME;

    public String getDRUGCODE() {
        return DRUGCODE;
    }

    public void setDRUGCODE(String DRUGCODE) {
        this.DRUGCODE = DRUGCODE;
    }

    public String getDRUGNAME() {
        return DRUGNAME;
    }

    public void setDRUGNAME(String DRUGNAME) {
        this.DRUGNAME = DRUGNAME;
    }

    public String getDRUGGNAME() {
        return DRUGGNAME;
    }

    public void setDRUGGNAME(String DRUGGNAME) {
        this.DRUGGNAME = DRUGGNAME;
    }
}
