package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class HdSendDrug implements Serializable {
    private static final long serialVersionUID = -1525446192275289528L;

    private Map<String, List> drugList;

    public Map<String, List> getDrugList() {
        return drugList;
    }

    public void setDrugList(Map<String, List> drugList) {
        this.drugList = drugList;
    }
}
