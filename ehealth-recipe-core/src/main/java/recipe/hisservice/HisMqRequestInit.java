package recipe.hisservice;

import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.util.LocalStringUtil;

import java.util.Date;

/**
 * @author： 0184/yu_yun
 * @date： 2018/12/3
 * @description： MQ HIS消息处理
 * @version： 1.0
 */
public class HisMqRequestInit {
    private static final Logger logger = LoggerFactory.getLogger(HisMqRequestInit.class);

    /**
     *
     * @param recipeBean
     * @param status 参考 HisBussConstant
     * @return
     */
    public static NoticeHisRecipeInfoReq initRecipeStatusToHisReq(RecipeBean recipeBean, String status) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        NoticeHisRecipeInfoReq notice = new NoticeHisRecipeInfoReq();
        notice.setClinicID(LocalStringUtil.toString(recipeBean.getClinicId()));
        notice.setOrganId(recipeBean.getClinicOrgan());
        notice.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipeBean.getClinicOrgan()));
        notice.setRecipeID(recipeBean.getRecipeCode());
        notice.setPlatRecipeID(String.valueOf(recipeBean.getRecipeId()));
        notice.setRecipeStatus(status);
        notice.setRemark(recipeBean.getRecipeMemo());
        try {
            EmploymentService iEmploymentService = ApplicationUtils.getBasicService(EmploymentService.class);
            String jobNumber = iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipeBean.getDoctor(), recipeBean.getClinicOrgan(), recipeBean.getDepart());
            notice.setDoctorNumber(jobNumber);
        } catch (Exception e) {
            logger.warn("initRecipeStatusToHisReq jobNumber error", e);
        }
        return notice;
    }

    public static NoticeHisRecipeInfoReq initRecipeStatusToHisReq(Recipe recipe, String status) {
        return initRecipeStatusToHisReq(ObjectCopyUtils.convert(recipe,RecipeBean.class),status);
    }

    public static NoticeHisRecipeInfoReq initRecipeStatusToHisReq(Recipe recipe, String status,String channel) {
        NoticeHisRecipeInfoReq notice = initRecipeStatusToHisReq(recipe,status);
        //核销渠道
        notice.setVerificationChannel(channel);
        //核销方式 1物流配送 2药店取药 3未知
        notice.setVerificationType(getVerificationType(recipe));
        //核销时间
        notice.setVerificationTime(new Date());
        return notice;
    }

    private static String getVerificationType(Recipe recipe) {
        switch (recipe.getGiveMode()){
            case 3:
                return "2";
            case 1:
                return "1";
            default:
                return "3";

        }
    }
}
