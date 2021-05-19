package recipe.atop;

import com.ngari.patient.service.PatientService;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.constant.ErrorCode;
import recipe.factory.status.constant.OfflineToOnlineEnum;
import recipe.factory.status.offlineToOnlineFactory.IOfflineToOnlineService;
import recipe.factory.status.offlineToOnlineFactory.OfflineToOnlineFactory;
import recipe.service.RecipeHisService;

import javax.validation.Valid;
import java.util.List;


/**
 * @Author liumin
 * @Date 2021/05/18 上午11:42
 * @Description 线下转线上服务入口类
 */
@RpcBean("offlineToOnlineAtop")
@Validated
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
     * @param request
     * @return
     * @Description 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     * @Author liumin
     */
    @RpcService
    @Validated
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(@Valid SettleForOfflineToOnlineVO request) {
        logger.info("{} request:{}", Thread.currentThread().getStackTrace()[1].getMethodName(), JSONUtils.toString(request));
        if (request == null
                || CollectionUtils.isEmpty(request.getRecipeCode())
                || StringUtils.isEmpty(request.getOrganId())
                || StringUtils.isEmpty(request.getBusType())
                || StringUtils.isEmpty(request.getMpiId())
        ) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        OfflineToOnlineFactory offlineToOnlineFactory = new OfflineToOnlineFactory();
        IOfflineToOnlineService offlineToOnlineService = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType());
        List<RecipeGiveModeButtonRes> response = offlineToOnlineService.settleForOfflineToOnline(request);
        logger.info("{} response:{}", Thread.currentThread().getStackTrace()[1].getMethodName(), JSONUtils.toString(response));
        return response;
    }

}
