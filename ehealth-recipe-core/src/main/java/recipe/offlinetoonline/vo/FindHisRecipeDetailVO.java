package recipe.offlinetoonline.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上获取详情参数
 */
@Data
public class FindHisRecipeDetailVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

     //机构
    private Integer organId;

     //mpiId
    private String mpiId;

    //处方cdr_his_recipe表的hisRecipeId
    private Integer hisRecipeId;

    //处方号
    private String recipeCode;

    //
    private String cardId;

    //缴费状态 1 未处理 2已处理
    private Integer status;



}


