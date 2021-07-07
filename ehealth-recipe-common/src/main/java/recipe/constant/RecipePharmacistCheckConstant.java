package recipe.constant;


/**
* @Description: RecipePharmacistCheckConstant 类（或接口）是 药师审核状态
* @Author: JRK
* @Date: 2019/11/11
 * 0:待审核 1:通过 2:不通过 3:二次签名 4:失效
*/
public class RecipePharmacistCheckConstant {
    /**
     * 待审核
     */
    public static final int Already_Check = 0;
    /**
     * 通过
     */
    public static final int Check_Pass = 1;
    /**
     * 不通过
     */
    public static final int Check_No_Pass = 2;
    /**
     * 二次签名
     */
    public static final int Second_Sign = 0;
    /**
     * 失效
     */
    public static final int Check_Failure = 0;
    /**
     * 已取消
     */
    public static final int Cancel = 7;

}
