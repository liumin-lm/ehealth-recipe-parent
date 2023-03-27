package recipe.core.api.patient;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.HisRecipeDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.offlinetoonline.model.*;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import recipe.vo.patient.PatientRecipeListReqVO;
import recipe.vo.patient.PatientRecipeListResVo;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.Date;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/5/18 上午11:42
 * @Description 线下转线上接口类
 */
public interface IOfflineRecipeBusinessService {


    /**
     * 获取线下处方列表
     *
     * @param findHisRecipeListVO
     */
    List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO findHisRecipeListVO);

    /**
     * 获取线下处方详情
     *
     * @param request
     * @return
     */
    FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request);

    /**
     * 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     *
     * @param request
     * @return
     */
    List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request);


    /**
     * 获取卡类型
     *
     * @param organId
     * @return
     */
    List<String> getCardType(Integer organId);


    /**
     * 获取线下处方详情
     *
     * @param mpiId       患者ID
     * @param clinicOrgan 机构ID
     * @param recipeCode  处方号码
     * @date 2021/8/06
     */
    OffLineRecipeDetailVO getHisRecipeDetail(String mpiId, Integer clinicOrgan, String recipeCode,String createDate);


    /**
     * 推送处方信息到his
     *
     * @param recipeId 处方id
     * @param pushType 推送类型: 1：提交处方，2:撤销处方
     * @param sysType  处方端类型 1 医生端 2患者端
     * @return RecipeInfoDTO 处方信息
     */
    RecipeInfoDTO pushRecipe(Integer recipeId, Integer pushType, Integer sysType, Integer expressFeePayType,
                             Double expressFee, String giveModeKey, Integer pushDest);

    void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request);

    /**
     * 撤销线下处方
     *
     * @param organId
     * @param recipeCode
     * @return
     */
    HisResponseTO abolishOffLineRecipe(Integer organId, List<String> recipeCode);

    /**
     * 根据患者id获取下线处方列表
     *
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    List<RecipeInfoTO> patientOfflineRecipe(Integer organId, String patientId, String patientName, Date startTime, Date endTime);

    /**
     * 根据处方code获取线下处方详情
     *
     * @param createDate 处方时间
     * @param organId    机构id
     * @param recipeCode 处方code
     */
    HisRecipeDTO getOffLineRecipeDetailsV1(Integer organId, String recipeCode, String createDate);

    /**
     * 查询处方列表（线上+线下）
     *
     * @param patientRecipeListReq
     * @return
     */
    List<List<PatientRecipeListResVo>> patientRecipeList(PatientRecipeListReqVO patientRecipeListReq);

    /**
     * his处方 预校验
     *
     * @param recipeDTO
     */
    DoSignRecipeDTO hisRecipeCheck(RecipeDTO recipeDTO);

    /**
     * 线下转线上
     * @param request
     * @return
     */
    OfflineToOnlineResVO offlineToOnline(OfflineToOnlineReqVO request);

    /**
     * 批量线下转线上
     * @param request
     * @return
     */
    List<OfflineToOnlineResVO> batchOfflineToOnline(BatchOfflineToOnlineReqVO request);

    /**
     * 获取本人是否已经转到过线上
     * @param req
     * @return
     */
    Integer obtainExistFlagOwn(checkForOrderBeforeReqVo req);
}
