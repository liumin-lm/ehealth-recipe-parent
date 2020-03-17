package recipe.service;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.recipe.mode.NoticeHisRecipeInfoReq;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.ApplicationUtils;
import recipe.dao.RecipeExtendDAO;
import recipe.hisservice.RecipeToHisMqService;

/**
 * @author yinsheng
 * @date 2020\3\17 0017 11:51
 */
@RpcBean("hisCheckRecipeService")
public class HisCheckRecipeService {

    public void sendCheckRecipeInfo(Recipe recipe){
        IConfigurationCenterUtilsService configurationCenterUtilsService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer isOpenHisCheckRecipeFlag =  (Integer)configurationCenterUtilsService.getConfiguration(recipe.getClinicOrgan(), "isOpenHisCheckRecipeFlag");
        if (isOpenHisCheckRecipeFlag == 2) {
            OrganService organService = BasicAPI.getService(OrganService.class);
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            //表示为HIS审方
            RecipeToHisMqService recipeToHisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
            NoticeHisRecipeInfoReq notice = new NoticeHisRecipeInfoReq();
            notice.setOrganId(recipe.getClinicOrgan());
            OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
            notice.setOrganizeCode(organDTO.getOrganizeCode());
            notice.setRecipeID(recipe.getRecipeCode());
            notice.setPlatRecipeID(recipe.getRecipeId()+"");
            notice.setRegisterId(recipeExtend.getRegisterID());
            notice.setRecipeStatus("5");
            recipeToHisMqService.recipeStatusToHis(notice);
        }
    }
}
