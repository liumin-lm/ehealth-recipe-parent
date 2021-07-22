package recipe.business.offlinetoonline.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import ctd.persistence.exception.DAOException;
import ngari.openapi.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.business.BaseService;
import recipe.business.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.business.offlinetoonline.OfflineToOnlineFactory;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IOfflineToOnlineService;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.manager.HisRecipeManager;
import recipe.service.CommonRecipeService;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.List;

/**
 * @Author liumin
 * @Date 2021/7/20 下午4:58
 * @Description
 */
@Service
public class OfflineToOnlineService extends BaseService implements IOfflineToOnlineService {

    private static final Logger logger = LoggerFactory.getLogger(CommonRecipeService.class);

    @Autowired
    HisRecipeManager hisRecipeManager;

    @Autowired
    OfflineToOnlineFactory offlineToOnlineFactory;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
        try {
            // 1、公共参数获取
            PatientDTO patientDTO = obtainPatientInfo(request);
            // 2、获取his数据
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.getOfflineToOnlineType(request.getStatus()), null);
            // 3、待处理、进行中、已处理线下处方列表服务差异化实现
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            List<MergeRecipeVO> hisRecipeVos = offlineToOnlineStrategy.findHisRecipeList(hisRecipeInfos, patientDTO, request);
            return hisRecipeVos;
        } catch (DAOException e) {
            logger.error("OfflineToOnlineAtop findHisRecipeList error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineAtop findHisRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineAtop findHisRecipeDetail request:{}", JSONUtils.toString(request));
        try {
            request = obtainFindHisRecipeDetailParam(request);
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            return offlineToOnlineStrategy.findHisRecipeDetail(request);
        } catch (DAOException e) {
            logger.error("OfflineToOnlineAtop findHisRecipeDetail error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineAtop findHisRecipeDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        logger.info("offlineToOnlineService settleForOfflineToOnline request:{}", JSONUtils.toString(request));
        IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
        List<RecipeGiveModeButtonRes> result = offlineToOnlineStrategy.settleForOfflineToOnline(request);
        logger.info("offlineToOnlineService settleForOfflineToOnline res:{}", JSONUtils.toString(result));
        return result;
    }

    @Override
    public String getHandlerMode() {
        return null;
    }

    /**
     * 获取患者信息
     *
     * @param request
     * @return
     */
    private PatientDTO obtainPatientInfo(FindHisRecipeListVO request) {
        logger.info("offlineToOnlineService obtainPatientInfo request:{}", JSONUtils.toString(request));
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        patientDTO.setCardId(StringUtils.isNotEmpty(request.getCardId()) ? request.getCardId() : "");
        logger.info("offlineToOnlineService obtainPatientInfo req patientDTO:{}", JSONUtils.toString(patientDTO));
        return patientDTO;
    }

    /**
     * 如果status没传，需根据参数判断，获取状态
     *
     * @param request
     * @return
     */
    private FindHisRecipeDetailReqVO obtainFindHisRecipeDetailParam(FindHisRecipeDetailReqVO request) {
        logger.info("offlineToOnlineService obtainFindHisRecipeDetailParam request:{}", JSONUtils.toString(request));
        //获取对应的status
        if (StringUtils.isEmpty(request.getStatus())) {
            String status = hisRecipeManager.attachHisRecipeStatus(request.getMpiId(), request.getOrganId(), request.getRecipeCode());
            request.setMpiId(status);
        }
        logger.info("offlineToOnlineService obtainFindHisRecipeDetailParam req:{}", JSONUtils.toString(request));
        return request;
    }

}
