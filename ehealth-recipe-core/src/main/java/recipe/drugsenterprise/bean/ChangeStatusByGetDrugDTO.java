package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import lombok.Data;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import recipe.constant.GetDrugChangeStatusSceneConstant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
* @Description: ChangeStatusByGetDrugDTO 类（或接口）是获取药品时药企修改状态的对象
* @Author: JRK
* @Date: 2019/8/21 
*/
public class ChangeStatusByGetDrugDTO implements Serializable {

    private static final long serialVersionUID = -6607434357722673932L;

    /**
     * 处方的id
     */
    private Integer recipeId;

    /**
     * 设置状态的场景
     */
    private Integer installScene;

    /**
     * 失败的原因
     */
    private String failureReason;

    /**
     * 修改的状态
     */
    private Integer changeStatus;

    /**
     * @method  checkRation
     * @description 检测数据的安全性
     * @date: 2019/8/26
     * @author: JRK
     * @param result 检查结果
     * @return void
     */
    public void checkRation(StandardResultDTO result){
        if(null == this.getRecipeId()){
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("传入的recipeId不能为空!");
            return;
        }
        if(null == this.getInstallScene()){
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("设置场景不能为空!");
            return;
        }
        if(null == this.getChangeStatus()){
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("修改的状态不能为空!");
            return;
        }
        if(GetDrugChangeStatusSceneConstant.Fail_Change_Recipe == this.getInstallScene() && null == this.getFailureReason()){
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("失败原因不能为空!");
            return;
        }

    }

    @Override
    public String toString() {
        return "ChangeStatusByGetDrugDTO{" +
                "recipeId=" + recipeId +
                ", installScene=" + installScene +
                ", failureReason='" + failureReason + '\'' +
                ", changeStatus=" + changeStatus +
                '}';
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getInstallScene() {
        return installScene;
    }

    public void setInstallScene(Integer installScene) {
        this.installScene = installScene;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getChangeStatus() {
        return changeStatus;
    }

    public void setChangeStatus(Integer changeStatus) {
        this.changeStatus = changeStatus;
    }
}
