package recipe.thread;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import ctd.util.AppContextHolder;
import recipe.service.HisRecipeService;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Erek on 2020/3/26.
 */
public class QueryHisRecipeCallable implements Callable<String> {

    private Integer organId;
    private String mpiId;
    private Integer timeQuantum;
    private Integer flag;
    private PatientDTO patientDTO;
    public QueryHisRecipeCallable(Integer organId,String mpiId,Integer timeQuantum,Integer flag,PatientDTO patientDTO) {
        this.organId = organId;
        this.mpiId = mpiId;
        this.timeQuantum = timeQuantum;
        this.flag = flag;
        this.patientDTO = patientDTO;
    }

    @Override
    public String call() throws Exception {
        HisRecipeService hisRecipeService = AppContextHolder.getBean("eh.hisRecipeService",HisRecipeService.class);
        HisResponseTO<List<QueryHisRecipResTO>> responseTO = hisRecipeService.queryHisRecipeInfo(organId,patientDTO,timeQuantum,flag);
        if(null != responseTO){
            if(null != responseTO.getData()){
                hisRecipeService.saveHisRecipeInfo(responseTO,patientDTO,flag);
            }
        }
        return null;
    }
}

