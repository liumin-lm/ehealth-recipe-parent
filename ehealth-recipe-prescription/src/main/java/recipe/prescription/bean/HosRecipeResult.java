package recipe.prescription.bean;

import ctd.util.JSONUtils;
import recipe.common.ResponseUtils;
import recipe.common.response.CommonResponse;

import java.io.Serializable;

/**
 * 对接医院HIS结果对象
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * date:2017/4/18.
 */
public class HosRecipeResult extends CommonResponse implements Serializable {

    public HosRecipeResult() {

    }

    public static void main(String[] args) {
        HosRecipeResult result = ResponseUtils.getSuccessResponse(HosRecipeResult.class);
        System.out.println(result.toString());
        HosRecipeResult failResponse = ResponseUtils.getFailResponse(HosRecipeResult.class, null);
        System.out.println(failResponse.toString());
    }
}
