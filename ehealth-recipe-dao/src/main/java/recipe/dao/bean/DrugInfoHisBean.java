package recipe.dao.bean;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/3/21 0021.
 * 处方邵逸夫药品信息查询 请求体
 */
public class DrugInfoHisBean implements Serializable{


    private static final long serialVersionUID = -1043098753732609326L;
    //药品医院代码
    private String drcode;

    //药品名称
    private String drname;

    //药品规格
    private String drmodel;

    //药品包装
    private String pack;

    //药品包装单位
    private String packUnit;

    //药品产地代码
    private String manfcode;

    //是否有效标志
    private String useflag;

    //机构平台代码
    private Integer organId;

    //药房Id
    private String pharmacy;

    public DrugInfoHisBean(){

    }

    public DrugInfoHisBean(String drcode, int pack, String packUnit){
        this.drcode = drcode;
        this.pack = String.valueOf(pack);
        this.packUnit = packUnit;
    }

    public DrugInfoHisBean(String drcode, int pack, String packUnit, String manfcode){
        this.drcode = drcode;
        this.pack = String.valueOf(pack);
        this.packUnit = packUnit;
        this.manfcode = manfcode;
    }

    public DrugInfoHisBean(String drcode, int pack, String packUnit, String manfcode,String pharmacy){
        this.drcode = drcode;
        this.pack = String.valueOf(pack);
        this.packUnit = packUnit;
        this.manfcode = manfcode;
        this.pharmacy = pharmacy;
    }

    public DrugInfoHisBean(String drcode){
        this.drcode = drcode;
    }


    public String getDrcode() {
        return drcode;
    }

    public void setDrcode(String drcode) {
        this.drcode = drcode;
    }

    public String getDrname() {
        return drname;
    }

    public void setDrname(String drname) {
        this.drname = drname;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public String getDrmodel() {
        return drmodel;
    }

    public void setDrmodel(String drmodel) {
        this.drmodel = drmodel;
    }

    public String getPackUnit() {
        return packUnit;
    }

    public void setPackUnit(String packUnit) {
        this.packUnit = packUnit;
    }

    public String getManfcode() {
        return manfcode;
    }

    public void setManfcode(String manfcode) {
        this.manfcode = manfcode;
    }

    public String getUseflag() {
        return useflag;
    }

    public void setUseflag(String useflag) {
        this.useflag = useflag;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(String pharmacy) {
        this.pharmacy = pharmacy;
    }
}
