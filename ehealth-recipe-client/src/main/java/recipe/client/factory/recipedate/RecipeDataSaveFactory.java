package recipe.client.factory.recipedate;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import recipe.util.ValidateUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;


/**
 * 处方保存数据 工厂处理类
 * @author fuzi
 */
@Service
public abstract class RecipeDataSaveFactory implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<Integer,RecipeDataSaveFactory> map = new TreeMap<>();

    /**
     * 设置处方默认数据 责任链
     *
     *  //根据默认值-设置处方默认数据
     *  defaultValueClientUtil.setRecipe(recipe);
     *  //设置机构-处方默认数据
     *   organClient.setRecipe(recipe);
     *   //设置医生-处方默认数据
     *   doctorClient.setRecipe(recipe);
     *   //设置患者-处方默认数据
     *   patientClient.setRecipe(recipe);
     *   //根据配置项-设置处方默认数据
     *   configurationClient.setRecipe(recipe);
     *   //设置复诊-处方默认数据
     *   revisitClient.setRecipe(recipe);
     *   //设置咨询-处方默认数据
     *   consultClient.setRecipe(recipe);
     *   //设置科室-处方默认数据
     *   departClient.setRecipe(recipe);
     * @param recipe 处方头对象
     *
     */
    public void setRecipeList(Recipe recipe) {
        map.forEach((k,v)->v.setRecipe(recipe));
    }
    /**
     * 设置处方扩展默认数据 责任链
     * //根据默认值-设置处方默认数据
     * defaultValueClientUtil.setRecipeExt(recipe, extend);
     * //根据患者-设置处方默认数据
     * patientClient.setRecipeExt(recipe, extend);
     * //根据配置项-设置处方默认数据
     * configurationClient.setRecipeExt(recipe, extend);
     * //设置电子病例-处方默认数据
     * docIndexClient.setRecipeExt(recipe, extend);
     * //设置复诊-处方默认数据
     * revisitClient.setRecipeExt(recipe, extend);
     * //设置咨询-处方默认数据
     * consultClient.setRecipeExt(recipe, extend);
     * @param recipe 处方头对象
     * @param extend 处方扩展对象
     */
    public void setRecipeExtList(Recipe recipe, RecipeExtend extend) {
        map.forEach((k,v)->v.setRecipeExt(recipe,extend));
    }

    protected Integer getSort() {
        return null;
    }

    /**
     * 设置处方默认数据 基类空实现 具体赋值子类处理
     *
     * @param recipe 处方头对象
     */
    protected void setRecipe(Recipe recipe) {
    }

    /**
     * 设置处方默认数据 基类空实现 具体赋值子类处理
     *
     * @param recipe 处方扩展对象
     */
    protected void setRecipeExt(Recipe recipe, RecipeExtend extend) {
    }

    /**
     * 添加工厂实现类
     *
     * @param applicationContext spring上下文
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanNamesForType(RecipeDataSaveFactory.class);
        logger.info("RecipeDataSaveFactory添加授权服务工厂类，beanNames = {}", Arrays.toString(beanNames));
        for (String beanName : beanNames) {
            RecipeDataSaveFactory giveModeService = applicationContext.getBean(beanName, RecipeDataSaveFactory.class);
            if (!ValidateUtil.integerIsEmpty(giveModeService.getSort())) {
                map.put(giveModeService.getSort(),giveModeService);
            }
        }
        logger.info("RecipeDataSaveFactory添加授权服务工厂类，giveModeMap = {}", JSON.toJSONString(map));
    }
}
