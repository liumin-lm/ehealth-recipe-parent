//package recipe.audit.bean;
//
//import com.ngari.recipe.common.RecipeCommonResTO;
//
//import java.io.Serializable;
//import java.util.List;
//
///**
// * @author： 0184/yu_yun
// * @date： 2018/11/20
// * @description： 合理用药审查返回对象
// * @version： 1.0
// */
//public class AutoAuditResult extends RecipeCommonResTO implements Serializable {
//
//    private static final long serialVersionUID = 3728542412956781881L;
//
//    private String highestDrangeLevel;
//
//    /**
//     * 正常进行流程的风险等级
//     */
//    private String normalFlowLevel;
//    /**
//     * 需要填写用药理由的风险等级
//     */
//    private String medicineReasonLevel;
//    /**
//     * 需要修改处方的风险等级
//     */
//    private String updateRecipeLevel;
//
//
//    private List<PAWebMedicines> medicines;
//
//    private List<PAWebRecipeDanger> recipeDangers;
//
//    public List<PAWebRecipeDanger> getRecipeDangers() {
//        return recipeDangers;
//    }
//
//    public void setRecipeDangers(List<PAWebRecipeDanger> recipeDangers) {
//        this.recipeDangers = recipeDangers;
//    }
//
//    public List<PAWebMedicines> getMedicines() {
//        return medicines;
//    }
//
//    public void setMedicines(List<PAWebMedicines> medicines) {
//        this.medicines = medicines;
//    }
//
//    public String getHighestDrangeLevel() {
//        return highestDrangeLevel;
//    }
//
//    public void setHighestDrangeLevel(String highestDrangeLevel) {
//        this.highestDrangeLevel = highestDrangeLevel;
//    }
//
//    public String getNormalFlowLevel() {
//        return normalFlowLevel;
//    }
//
//    public void setNormalFlowLevel(String normalFlowLevel) {
//        this.normalFlowLevel = normalFlowLevel;
//    }
//
//    public String getMedicineReasonLevel() {
//        return medicineReasonLevel;
//    }
//
//    public void setMedicineReasonLevel(String medicineReasonLevel) {
//        this.medicineReasonLevel = medicineReasonLevel;
//    }
//
//    public String getUpdateRecipeLevel() {
//        return updateRecipeLevel;
//    }
//
//    public void setUpdateRecipeLevel(String updateRecipeLevel) {
//        this.updateRecipeLevel = updateRecipeLevel;
//    }
//}
