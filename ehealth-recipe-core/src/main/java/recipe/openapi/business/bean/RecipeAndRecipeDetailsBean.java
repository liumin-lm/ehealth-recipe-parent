package recipe.openapi.business.bean;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\9\18 0018 15:24
 */
@Data
public class RecipeAndRecipeDetailsBean implements Serializable{
    private static final long serialVersionUID = -1431814682363852501L;

    private Integer recipeId;

    private String patientName;

    private String photo;

    private String patientSex;

    private String organDiseaseName;

    private Date signDate;

    private Double totalMoney;

    private Integer status;

    private Integer organId;

    private String statusText;

    private Integer statusCode;

    private String recipeSurplusHours;

    private Integer recipeType;

    private String logisticsCompany;

    private String trackingNumber;

    private Integer orderId;
    @ItemProperty(alias = "有效天数")
    private Integer valueDays;
    private List<ThirdRecipeDetailBean> recipeDetailBeans;
}
