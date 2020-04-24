package recipe.audit.bean;

import com.ngari.recipe.common.RecipeCommonResTO;

import java.io.Serializable;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/20
 * @description： 合理用药审查返回对象
 * @version： 1.0
 */
public class AutoAuditResult extends RecipeCommonResTO implements Serializable {

    private static final long serialVersionUID = 3728542412956781881L;

    private String highestDrangeLevel;

    private List<PAWebMedicines> medicines;

    private List<PAWebRecipeDanger> recipeDangers;

    public List<PAWebRecipeDanger> getRecipeDangers() {
        return recipeDangers;
    }

    public void setRecipeDangers(List<PAWebRecipeDanger> recipeDangers) {
        this.recipeDangers = recipeDangers;
    }

    public List<PAWebMedicines> getMedicines() {
        return medicines;
    }

    public void setMedicines(List<PAWebMedicines> medicines) {
        this.medicines = medicines;
    }

    public String getHighestDrangeLevel() {
        return highestDrangeLevel;
    }

    public void setHighestDrangeLevel(String highestDrangeLevel) {
        this.highestDrangeLevel = highestDrangeLevel;
    }
}
