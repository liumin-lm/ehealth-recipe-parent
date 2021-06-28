package recipe.offlinetoonline.atop;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import recipe.atop.BaseAtop;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.constant.ErrorCode;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.factory.OfflineToOnlineFactory;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.offlinetoonline.service.third.RecipeHisService;
import recipe.offlinetoonline.vo.FindHisRecipeDetailVO;
import recipe.offlinetoonline.vo.FindHisRecipeListVO;
import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;


/**
 * @Author liumin
 * @Date 2021/05/18 上午11:42
 * @Description 线下转线上服务入口类
 */
@RpcBean("offlineToOnlineAtop")
@Validated
public class OfflineToOnlineAtop extends BaseAtop {

    @Autowired
    OfflineToOnlineFactory offlineToOnlineFactory;

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
    @RpcService
    public List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
        logger.info("OfflineToOnlineAtop findHisRecipeList request:{}", ctd.util.JSONUtils.toString(request));
        if (null == request
                || request.getOrganId() == null
                || StringUtils.isEmpty(request.getMpiId())
                || StringUtils.isEmpty(request.getStatus())) {
            throw new DAOException(eh.base.constant.ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try{
            PatientDTO patientDTO = patientService.getPatientBeanByMpiId(request.getMpiId());
            if (null == patientDTO) {
                throw new DAOException(609, "患者信息不存在");
            }
            patientDTO.setCardId(StringUtils.isNotEmpty(request.getCardId()) ? request.getCardId() : "");

            // 1、获取his数据
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos=recipeHisService.queryData(request.getOrganId(),patientDTO,request.getTimeQuantum(),Integer.parseInt(request.getStatus()),null);
            IOfflineToOnlineService offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            // 2、待缴费、进行中、已缴费线下处方列表服务差异化实现
            List<MergeRecipeVO> hisRecipeVoS=offlineToOnlineStrategy.findHisRecipeList(hisRecipeInfos,patientDTO,request);
            return hisRecipeVoS;
        } catch (DAOException e) {
            logger.error("OfflineToOnlineAtop findHisRecipeList error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineAtop findHisRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取线下处方详情
     * @param request
     * @return
     */
    @RpcService
    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
        logger.info("OfflineToOnlineAtop findHisRecipeDetail request:{}", ctd.util.JSONUtils.toString(request));
        if (null == request
                || request.getOrganId() == null
                || StringUtils.isEmpty(request.getMpiId())
                || StringUtils.isEmpty(request.getStatus())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        try {
            IOfflineToOnlineService offlineToOnlineService = offlineToOnlineFactory.getFactoryService(request.getStatus());
            return offlineToOnlineService.findHisRecipeDetail(request);
        } catch (DAOException e) {
            logger.error("OfflineToOnlineAtop findHisRecipeDetail error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineAtop findHisRecipeDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * @param request
     * @return
     * @Description 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     * @Author liumin
     */
    @RpcService
    @Validated
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(@Valid SettleForOfflineToOnlineVO request) {
        logger.info("OfflineToOnlineAtop settleForOfflineToOnline request={}", JSONUtils.toString(request));
        if (request == null
                || CollectionUtils.isEmpty(request.getRecipeCode())
                || StringUtils.isEmpty(request.getOrganId())
                || StringUtils.isEmpty(request.getBusType())
                || StringUtils.isEmpty(request.getMpiId())
        ) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            IOfflineToOnlineService offlineToOnlineService = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
            List<RecipeGiveModeButtonRes> result = offlineToOnlineService.settleForOfflineToOnline(request);
            logger.info("OfflineToOnlineAtop settleForOfflineToOnline result = {}", JSONUtils.toString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("OfflineToOnlineAtop settleForOfflineToOnline error", e1);
            throw new DAOException(e1.getCode(), e1.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineAtop settleForOfflineToOnline error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }

    }

}
