//package recipe.offlinetoonline.service;
//
//import com.ngari.common.mode.HisResponseTO;
//import com.ngari.his.recipe.mode.QueryHisRecipResTO;
//import com.ngari.patient.dto.PatientDTO;
//import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
//import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
//import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
//import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
//import com.ngari.recipe.recipe.model.MergeRecipeVO;
//import ctd.util.annotation.RpcService;
//import recipe.vo.patient.RecipeGiveModeButtonRes;
//
//import java.util.List;
//
///**
// * @Author liumin
// * @Date 2021/5/18 上午11:42
// * @Description 线下转线上接口类
// */
//public interface IOfflineToOnlineStrategy {
//
//
//    /**
//     * 获取线下处方列表
//     * @param hisRecipeInfos
//     * @param patientDTO
//     * @param request
//     * @return
//     */
//    @RpcService
//    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request);
//
//    /**
//     * 获取线下处方详情
//     * @param request
//     * @return
//     */
//    FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request);
//
//    /**
//     * 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
//     *
//     * @param request
//     * @return
//     */
//    List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request);
//
//    /**
//     * 获取实现类 类型
//     *
//     * @return
//     */
//    String getHandlerMode();
//}
