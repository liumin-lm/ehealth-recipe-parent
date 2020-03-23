package recipe.thread;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import ctd.util.AppContextHolder;
import recipe.ApplicationUtils;
import recipe.service.HisRecipeService;
import recipe.service.RecipeHisService;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * created by shiyuping on 2019/7/22
 */
public class QueryHisRecipeCallable implements Callable<String> {

    private Integer organId;
    private String mpiId;
    private Integer timeQuantum;
    private Integer flag;
    public QueryHisRecipeCallable(Integer organId,String mpiId,Integer timeQuantum,Integer flag) {
        this.organId = organId;
        this.mpiId = mpiId;
        this.timeQuantum = timeQuantum;
        this.flag = flag;
    }

    @Override
    public String call() throws Exception {
        HisRecipeService hisRecipeService = AppContextHolder.getBean("eh.hisRecipeService",HisRecipeService.class);
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = hisRecipeService.queryHisRecipeInfo(organId,mpiId,timeQuantum,flag);
        if(null != responseTO){
            if(null != responseTO.getData()){
                hisRecipeService.saveHisRecipeInfo(responseTO);
            }
        }
        return null;
    }
}
