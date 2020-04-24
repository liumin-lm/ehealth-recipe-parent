package recipe.audit.bean;

import java.io.Serializable;

public class PAWebRecipe implements Serializable{
    private static final long serialVersionUID = -714567583743283789L;

    private String Type;
    private String Lvl;
    private String Summary;
    private String HisCodeA;
    private String GroupA;
    private String NameA;
    private String HisCodeB;
    private String GroupB;
    private String NameB;
    private Integer IssueID;
    private String Others;
    private String PresID;

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getLvl() {
        return Lvl;
    }

    public void setLvl(String lvl) {
        Lvl = lvl;
    }

    public String getSummary() {
        return Summary;
    }

    public void setSummary(String summary) {
        Summary = summary;
    }

    public String getHisCodeA() {
        return HisCodeA;
    }

    public void setHisCodeA(String hisCodeA) {
        HisCodeA = hisCodeA;
    }

    public String getGroupA() {
        return GroupA;
    }

    public void setGroupA(String groupA) {
        GroupA = groupA;
    }

    public String getNameA() {
        return NameA;
    }

    public void setNameA(String nameA) {
        NameA = nameA;
    }

    public String getHisCodeB() {
        return HisCodeB;
    }

    public void setHisCodeB(String hisCodeB) {
        HisCodeB = hisCodeB;
    }

    public String getGroupB() {
        return GroupB;
    }

    public void setGroupB(String groupB) {
        GroupB = groupB;
    }

    public String getNameB() {
        return NameB;
    }

    public void setNameB(String nameB) {
        NameB = nameB;
    }

    public Integer getIssueID() {
        return IssueID;
    }

    public void setIssueID(Integer issueID) {
        IssueID = issueID;
    }

    public String getOthers() {
        return Others;
    }

    public void setOthers(String others) {
        Others = others;
    }

    public String getPresID() {
        return PresID;
    }

    public void setPresID(String presID) {
        PresID = presID;
    }
}
