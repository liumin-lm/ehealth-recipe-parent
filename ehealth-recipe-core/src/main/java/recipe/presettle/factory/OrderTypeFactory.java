package recipe.presettle.factory;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import recipe.presettle.condition.IOrderTypeConditionHandler;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * created by shiyuping on 2020/11/27
 * 根据不同的条件生成不同的订单类型
 */
@Component
public class OrderTypeFactory implements ApplicationContextAware {
    private final static List<IOrderTypeConditionHandler> list = Lists.newArrayList();

    public static Integer getRecipeOrderType(OrderTypeCreateConditionRequest request){
        //默认普通自费
        Integer recipeOrderType = RecipeOrderTypeEnum.COMMON_SELF.getType();
        for (IOrderTypeConditionHandler conditionHandler : list) {
            //按优先级判断条件如果返回null就执行下一个条件判断
            if (conditionHandler.getOrderType(request) !=null){
                recipeOrderType = conditionHandler.getOrderType(request);
                break;
            }
        }
        return recipeOrderType;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, IOrderTypeConditionHandler> beans = applicationContext.getBeansOfType(IOrderTypeConditionHandler.class);
        beans.forEach((key,value)->list.add(value));
        //根据优先级排序--升序
        list.sort(Comparator.comparingInt(IOrderTypeConditionHandler::getPriorityLevel));
    }
}
