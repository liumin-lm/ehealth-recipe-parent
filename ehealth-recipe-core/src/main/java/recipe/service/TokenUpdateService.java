package recipe.service;


import recipe.ApplicationUtils;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/8/24.
 */
public class TokenUpdateService {

    /**
     * tomcat启动时调用
     */
    public void updateTokenAfterInit() {
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);
        service.updateDrugsEnterpriseToken();
    }
}
