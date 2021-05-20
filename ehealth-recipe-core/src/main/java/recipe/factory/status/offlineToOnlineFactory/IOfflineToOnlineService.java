package recipe.factory.status.offlineToOnlineFactory;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import com.ngari.recipe.vo.FindHisRecipeDetailVO;
import com.ngari.recipe.vo.FindHisRecipeListVO;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import ctd.util.annotation.RpcService;
import recipe.bean.RecipeGiveModeButtonRes;

import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/5/18 上午11:42
 * @Description 线下转线上接口类
 */
public interface IOfflineToOnlineService {

    /**
     * 获取线下处方列表
     *
     * @param request
     * @return
     */
    @RpcService
    public List<HisRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request);

    /**
     * 获取线下处方详情
     *
     * @param request
     * @return
     */
    Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request);

    /**
     * 获取实现类 类型
     *
     * @return
     */
    Integer getPayMode();

    /**
     * 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     *
     * @param request
     * @return
     */
    List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request);
}
