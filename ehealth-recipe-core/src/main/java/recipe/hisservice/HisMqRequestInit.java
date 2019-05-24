package recipe.hisservice;

import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import recipe.util.LocalStringUtil;

/**
 * @author： 0184/yu_yun
 * @date： 2018/12/3
 * @description： MQ HIS消息处理
 * @version： 1.0
 */
public class HisMqRequestInit {

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
        //TODO 增加冗余字段
        notice.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipeBean.getClinicOrgan()));
        notice.setRecipeID(recipeBean.getRecipeCode());
        notice.setRecipeStatus(status);
        notice.setRemark(recipeBean.getRecipeMemo());
        return notice;
    }

    public static NoticeHisRecipeInfoReq initRecipeStatusToHisReq(Recipe recipe, String status) {
        OrganService organService = BasicAPI.getService(OrganService.class);

        NoticeHisRecipeInfoReq notice = new NoticeHisRecipeInfoReq();
        notice.setClinicID(LocalStringUtil.toString(recipe.getClinicId()));
        notice.setOrganId(recipe.getClinicOrgan());
        notice.setOrganizeCode(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        notice.setRecipeID(recipe.getRecipeCode());
        notice.setRecipeStatus(status);
        notice.setRemark(recipe.getRecipeMemo());
        return notice;
    }
}
