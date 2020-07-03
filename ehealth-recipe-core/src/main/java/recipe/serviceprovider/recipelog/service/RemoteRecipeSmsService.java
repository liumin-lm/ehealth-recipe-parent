package recipe.serviceprovider.recipelog.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipemsg.model.RecipeMsgEnum;
import com.ngari.recipe.recipemsg.service.IRecipeSmsService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.service.RecipeMsgService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RpcBean("remoteRecipeSmsService")
public class RemoteRecipeSmsService implements IRecipeSmsService {
    @RpcService
    @Override
    public void sendRecipeMsg(RecipeMsgEnum em, RecipeBean... recipeList) {
      List<Recipe> recipes= Arrays.stream(recipeList).map(recipeBean -> {
            Recipe recipe=  ObjectCopyUtils.convert(recipeBean, Recipe.class);
            return recipe;
        }).collect(Collectors.toList());
        Recipe[] array = recipes.stream().toArray( Recipe[]::new);
        RecipeMsgService.sendRecipeMsg(em, array);

    }
}
