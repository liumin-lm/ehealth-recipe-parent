//package recipe.thread;
//
//import com.ngari.patient.dto.PatientDTO;
//import ctd.util.AppContextHolder;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import recipe.service.OfflineToOnlineService;
//
//import java.util.concurrent.Callable;
//
///**
// * Created by Erek on 2020/3/26.
// */
//public class QueryHisRecipeCallable implements Callable<String> {
//    private static final Log LOGGER = LogFactory.getLog(QueryHisRecipeCallable.class);
//    private Integer organId;
//    private String mpiId;
//    private Integer timeQuantum;
//    private Integer flag;
//    private PatientDTO patientDTO;
//    public QueryHisRecipeCallable(Integer organId,String mpiId,Integer timeQuantum,Integer flag,PatientDTO patientDTO) {
//        this.organId = organId;
//        this.mpiId = mpiId;
//        this.timeQuantum = timeQuantum;
//        this.flag = flag;
//        this.patientDTO = patientDTO;
//    }
//
//    @Override
//    public String call() {
//        OfflineToOnlineService offlineToOnlineService = AppContextHolder.getBean("eh.hisRecipeService", OfflineToOnlineService.class);
//        try {
//            offlineToOnlineService.queryHisRecipeInfo(organId, patientDTO, timeQuantum, flag);
//        } catch (Exception e) {
//            LOGGER.error("查询his线下处方数据 error ", e);
//        }
//        return null;
//    }
//}
//
