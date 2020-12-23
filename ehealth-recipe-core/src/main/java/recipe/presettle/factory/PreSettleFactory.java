package recipe.presettle.factory;

import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import recipe.presettle.IRecipePreSettleService;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.settle.IRecipeSettleService;

import java.util.Arrays;

/**
 * created by shiyuping on 2020/12/4
 * @author shiyuping
 */
public class PreSettleFactory {

    public static IRecipePreSettleService getPreSettleService(Integer organId,Integer orderType){
        //根据运营平台机构配置判断是否走预结算
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String[] configOrderType = (String[])configService.getConfiguration(organId, "preSettleOrderType");
        if (Arrays.asList(configOrderType).contains(String.valueOf(orderType))){
            return RecipeOrderTypeEnum.getPreSettleService(orderType);
        }
        return null;

    }

    public static IRecipeSettleService getSettleService(Integer organId, Integer orderType){
        //根据运营平台机构配置判断是否走结算
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String[] configOrderType = (String[])configService.getConfiguration(organId, "recipeSettleOrderType");
        if (Arrays.asList(configOrderType).contains(String.valueOf(orderType))){
            return RecipeOrderTypeEnum.getSettleService(orderType);
        }
        return null;

    }
}
