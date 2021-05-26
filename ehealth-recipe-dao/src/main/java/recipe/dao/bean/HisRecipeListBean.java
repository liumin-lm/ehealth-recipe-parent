package recipe.dao.bean;

import com.ngari.recipe.recipe.model.HisRecipeDetailVO;
import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @description：
 * @author： whf
 * @date： 2021-05-19 16:31
 */
@Schema
@Data
public class HisRecipeListBean implements Serializable {

    private static final long serialVersionUID = -3694270598793030159L;
    /**
     * 处方序号
     */
    private Integer hisRecipeID;
    /**
     * 挂号序号
     */
    private String registeredId;
    /**
     * 用户mpi id
     */
    private String mpiId;
    /**
     * his处方单号
     */
    private String recipeCode;
    /**
     * 开方机构
     */
    private  Integer clinicOrgan;
    /**
     * 开方科室
     */
    private String departCode;
    /**
     * 科室名称
     */
    private String departName;
    /**
     * 开方时间
     */
    private Date createDate;
    /**
     * 开方医生工号
     */
    private String doctorCode;
    /**
     * 开方医生姓名
     */
    private String doctorName;
    /**
     * 病种代码
     */
    private String chronicDiseaseCode;
    /**
     * 病种名称
     */
    private String chronicDiseaseName;
    /**
     * 患者姓名
     */
    private String patientName;
    /**
     * 诊断
     */
    private String memo;

    /**
     * 1西药  2中成药 3 草药
     */
    private Integer recipeType;
    /**
     * 是否缓存在平台
     */
    private Integer isCachePlatform;

    /**
     * 诊断名称
     */
    private String diseaseName;

    /********************************以下是从处方表获取的信息*******************************************/
    /**
     * 处方单号
     */
    private Integer recipeId;
    /**
     * 订单编号
     */
    private String orderCode;
    /**
     * 处方状态
     */
    @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
    private Integer status;
    private String statusText;


    /**
     * 处方来源
     */
    private Integer fromFlag;

    /**
     * 药品详情
     */
    private List<HisRecipeDetailVO> recipeDetail;

    private Integer jumpPageType;
    @ItemProperty(alias = "订单状态")
    private Integer orderStatus;
    @ItemProperty(alias = "订单状态描述")
    private String orderStatusText;
    @ItemProperty(alias = "诊断名称")
    private String organDiseaseName;

    public HisRecipeListBean(String diseaseName,Integer hisRecipeID,String registeredId, String mpiId, String recipeCode, Integer clinicOrgan, String departCode, String departName, Date createDate, String doctorCode, String doctorName, String chronicDiseaseCode, String chronicDiseaseName, String patientName, String memo,Integer recipeType,Integer fromFlag, Integer recipeId, String orderCode, Integer status) {
        this.diseaseName = diseaseName;
        this.hisRecipeID = hisRecipeID;
        this.registeredId = registeredId;
        this.mpiId = mpiId;
        this.recipeCode = recipeCode;
        this.clinicOrgan = clinicOrgan;
        this.departCode = departCode;
        this.departName = departName;
        this.createDate = createDate;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
        this.chronicDiseaseCode = chronicDiseaseCode;
        this.chronicDiseaseName = chronicDiseaseName;
        this.patientName = patientName;
        this.recipeType = recipeType;
        this.fromFlag = fromFlag;
        this.memo = memo;
        this.recipeId = recipeId;
        this.orderCode = orderCode;
        this.status = status;
    }
    public HisRecipeListBean(String diseaseName,Integer hisRecipeID,String registeredId, String mpiId, String recipeCode, Integer clinicOrgan, String departCode, String departName, Date createDate, String doctorCode, String doctorName, String chronicDiseaseCode, String chronicDiseaseName, String patientName, String memo, Integer recipeId, String orderCode, Integer status) {
        this.diseaseName = diseaseName;
        this.hisRecipeID = hisRecipeID;
        this.registeredId = registeredId;
        this.mpiId = mpiId;
        this.recipeCode = recipeCode;
        this.clinicOrgan = clinicOrgan;
        this.departCode = departCode;
        this.departName = departName;
        this.createDate = createDate;
        this.doctorCode = doctorCode;
        this.doctorName = doctorName;
        this.chronicDiseaseCode = chronicDiseaseCode;
        this.chronicDiseaseName = chronicDiseaseName;
        this.patientName = patientName;
        this.memo = memo;
        this.recipeId = recipeId;
        this.orderCode = orderCode;
        this.status = status;
    }

    public HisRecipeListBean() {
    }

}
