package recipe.dao.bean;

import lombok.Data;

import java.util.Date;
@Data
public class BillRecipeDetailBean {
    private Integer id;
    private Integer billType;
    private String outTradeNo;
    private Integer recipeId;
    private String mpiId;
    private String patientName;
    private String patientMobile;
    private Integer doctorId;
    private String doctorName;
    private String hisRecipeId;
    private Date recipeTime;
    private Integer organId;
    private Integer deptId;
    private Integer settleType;
    private Integer deliveryMethod;
    private Integer drugCompany;
    private String drugCompanyName;
    private Integer payFlag;
    private Double appointFee;
    private Double deliveryFee;
    private Double daiJianFee;
    private Double reviewFee;
    private Double otherFee;
    private Double drugFee;
    private Double dicountedFee;
    private Double totalFee;
    private Double medicarePay;
    private Double selfPay;

}
