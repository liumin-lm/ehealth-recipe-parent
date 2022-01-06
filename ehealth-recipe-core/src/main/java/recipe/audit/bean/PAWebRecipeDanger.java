package recipe.audit.bean;

import java.io.Serializable;

public class PAWebRecipeDanger implements Serializable {
    private static final long serialVersionUID = -5081142743263685360L;
    private String dangerType; //问题类型
    private String dangerLevel; //风险等级
    private String dangerDesc; //问题说明
    private String dangerDrug; //问题药品
    private String detailUrl; //详细信息

    public PAWebRecipeDanger() {
    }

//    public PAWebRecipeDanger(PAWebRecipe paWebRecipe,String detailId){
//        this.dangerType = paWebRecipe.getType();
//        this.dangerLevel = paWebRecipe.getLvl();
//        this.dangerDesc = paWebRecipe.getSummary();
//        this.dangerDrug = paWebRecipe.getNameA();
//        this.detailUrl = "http://103.38.233.27:880"+ "/Region?DetailID=" + detailId;
//    }

    public String getDangerType() {
        return dangerType;
    }

    public void setDangerType(String dangerType) {
        this.dangerType = dangerType;
    }

    public String getDangerLevel() {
        return dangerLevel;
    }

    public void setDangerLevel(String dangerLevel) {
        this.dangerLevel = dangerLevel;
    }

    public String getDangerDesc() {
        return dangerDesc;
    }

    public void setDangerDesc(String dangerDesc) {
        this.dangerDesc = dangerDesc;
    }

    public String getDangerDrug() {
        return dangerDrug;
    }

    public void setDangerDrug(String dangerDrug) {
        this.dangerDrug = dangerDrug;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }
}
