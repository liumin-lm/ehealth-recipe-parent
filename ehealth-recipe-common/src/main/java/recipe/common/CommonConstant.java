package recipe.common;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/3/8
 */
public class CommonConstant {

    public static final String SUCCESS = "000";

    public static final String FAIL = "001";

    public static final Integer requestSuccessCode = 200;

    /**
     * 推送类型: 1：提交处方，2:撤销处方
     */
    public static final Integer RECIPE_PUSH_TYPE = 1;
    public static final Integer RECIPE_CANCEL_TYPE = 2;

    /**
     * 处方推送类型类型 1 医生端 2患者端 3。。。
     */
    public static final Integer RECIPE_DOCTOR_TYPE = 1;
    public static final Integer RECIPE_PATIENT_TYPE = 2;
}
