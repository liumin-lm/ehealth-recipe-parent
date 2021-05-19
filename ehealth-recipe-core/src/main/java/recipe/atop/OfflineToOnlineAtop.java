package recipe.atop;

import com.ngari.patient.service.PatientService;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.service.RecipeHisService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Author liumin
 * @Date 2021/05/18 上午11:42
 * @Description 线下转线上服务入口类
 */
@RpcBean("offlineToOnlineAtop")
public class OfflineToOnlineAtop extends BaseAtop {

    @Autowired
    RecipeHisService recipeHisService;

    @Autowired
    @Qualifier("basic.patientService")
    PatientService patientService;

    /**
     * 获取线下处方列表
     * @param request
     * @return
     */
//    @RpcService
//    public List<HisRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
//        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(request.getMpiId());
//        if (null == patientDTO) {
//            throw new DAOException(609, "患者信息不存在");
//        }
//        // 1获取his数据
//        HisResponseTO<List<QueryHisRecipResTO>>  hisRecipeInfos=recipeHisService.queryHisRecipeInfo(request.getOrganId(),patientDTO,request.getTimeQuantum(),Integer.parseInt(request.getStatus()),request.getCardId(),null);
//        OfflineToOnlineFactory offlineToOnlineFactory=new OfflineToOnlineFactory();
//        IOfflineToOnlineService offlineToOnlineService = offlineToOnlineFactory.getFactoryService(Integer.parseInt(request.getStatus()));
//        //待缴费、已缴费线下处方列表服务差异化实现
//        List<HisRecipeVO> hisRecipeVOS=offlineToOnlineService.findHisRecipeList(hisRecipeInfos,patientDTO,request);
//        return hisRecipeVOS;
//    }

    /**
     * 获取线下处方详情
     * @param request
     * @return
     */
//    @RpcService
//    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
//        OfflineToOnlineFactory offlineToOnlineFactory=new OfflineToOnlineFactory();
//        IOfflineToOnlineService offlineToOnlineService=null;
//        if(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType().equals(request.getStatus())){
//            //已缴费
//            offlineToOnlineService = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType());
//        }else{
//            //待缴费
//            offlineToOnlineService = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType());
//        }
//        return offlineToOnlineService.findHisRecipeDetail(request);
//    }

    /**
     *
     * @param request
     * @return
     */
    @RpcService
    public Map<String,Object> settleForOfflineToOnline(SettleForOfflineToOnlineVO request){
        Map<String,Object> map=new HashMap<>();
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonResList=new ArrayList<RecipeGiveModeButtonRes>();
        RecipeGiveModeButtonRes recipeGiveModeButtonRes=new RecipeGiveModeButtonRes();
        recipeGiveModeButtonRes.setJumpType("1");
        recipeGiveModeButtonResList.add(recipeGiveModeButtonRes);
        map.put("recipeGiveModeButtonRes",recipeGiveModeButtonResList);
        return map;
    }

}
