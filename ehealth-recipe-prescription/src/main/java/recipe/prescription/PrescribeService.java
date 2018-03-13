package recipe.prescription;

import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalRecipeDTO;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/1/31
 * @description： 开方服务
 * @version： 1.0
 */
@RpcBean("prescribeService")
public class PrescribeService {

    /** logger */
    private static final Logger LOG = LoggerFactory.getLogger(PrescribeService.class);

    /**
     * 新增标识
     */
    private static final int ADD_FLAG = 1;

    /**
     * 撤销标识
     */
    private static final int CANCEL_FLAG = 2;

    /**
     * 更新标识
     */
    private static final int UPDATE_FLAG = 3;

    /**
     * 创建处方
     * @param recipeInfo
     * @return
     */
    @RpcService
    public HosRecipeResult createPrescription(String recipeInfo){
        if(StringUtils.isEmpty(recipeInfo)){
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "传入参数为空");
        }

        List<HospitalRecipeDTO> hospitalRecipeDTO = null;
        try {
            hospitalRecipeDTO = JSONUtils.parse(recipeInfo, List.class);
        } catch (Exception e) {
            LOG.error("createPrescription parse error. param={}", recipeInfo, e);
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "解析出错");
        }

        if(null != hospitalRecipeDTO){



        }else{
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "未知错误-处方对象为空");
        }

        return ResponseUtils.getSuccessResponse(HosRecipeResult.class);
    }

    /**
     * 校验医院处方信息
     *
     * @param obj 医院处方
     * @return 结果
     */
    private HosRecipeResult validateHospitalRecipe(List<HospitalRecipeDTO> recipeList, int flag) {
        HosRecipeResult result = ResponseUtils.getFailResponse(HosRecipeResult.class, null);
        if (ADD_FLAG == flag) {
            //新增
            HospitalRecipeDTO hospitalRecipe;
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < recipeList.size(); i++) {
                hospitalRecipe = recipeList.get(i);
                prefix = prefix.append("处方["+i+"]:");

                if (StringUtils.isEmpty(hospitalRecipe.getRecipeType())) {
                    result.setMsg(prefix + "处方类型为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getRecipeCode())) {
                    result.setMsg(prefix + "处方编号为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getClinicOrgan())) {
                    result.setMsg(prefix + "开方机构为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getDoctorNumber())) {
                    result.setMsg(prefix + "开方医生工号为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getCertificate())) {
                    result.setMsg(prefix + "患者身份证信息为空");
                    return result;
                }

                if (CollectionUtils.isEmpty(hospitalRecipe.getDrugList())) {
                    result.setMsg(prefix + "处方详情数据为空");
                    return result;
                }
            }
        }

        result.setCode(CommonConstant.SUCCESS);
        return result;
    }

}
