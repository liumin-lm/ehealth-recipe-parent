package recipe.business;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.entity.HisRecipe;
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
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.factory.offlinetoonline.OfflineToOnlineFactory;
import recipe.manager.HisRecipeManager;
import recipe.service.CommonRecipeService;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/7/20 下午4:58
 * @Description
 */
@Service
public class OfflineRecipeBusinessService extends BaseService implements IOfflineRecipeBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecipeService.class);

    @Autowired
    private HisRecipeManager hisRecipeManager;

    @Autowired
    private OfflineToOnlineFactory offlineToOnlineFactory;

    @Autowired
    private IConfigurationCenterUtilsService configurationCenterUtilsService;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(FindHisRecipeListVO request) {
        LOGGER.info("OfflineToOnlineService findHisRecipeList request:{}", JSONUtils.toString(request));
        try {
            // 1、公共参数获取
            PatientDTO patientDTO = obtainPatientInfo(request);
            // 2、获取his数据
            HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.getOfflineToOnlineType(request.getStatus()), null);
            // 3、待处理、进行中、已处理线下处方列表服务差异化实现
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            List<MergeRecipeVO> res = offlineToOnlineStrategy.findHisRecipeList(hisRecipeInfos, patientDTO, request);
            LOGGER.info("OfflineToOnlineService findHisRecipeList res:{}", JSONUtils.toString(res));
            return res;
        } catch (DAOException e) {
            logger.error("OfflineToOnlineService findHisRecipeList error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineService findHisRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineService findHisRecipeDetail request:{}", JSONUtils.toString(request));
        try {
            request = obtainFindHisRecipeDetailParam(request);
            IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(request.getStatus());
            return offlineToOnlineStrategy.findHisRecipeDetail(request);
        } catch (DAOException e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("OfflineToOnlineService findHisRecipeDetail error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        logger.info("OfflineToOnlineService settleForOfflineToOnline request:{}", JSONUtils.toString(request));
        IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
        List<RecipeGiveModeButtonRes> result = offlineToOnlineStrategy.settleForOfflineToOnline(request);
        logger.info("OfflineToOnlineService settleForOfflineToOnline res:{}", JSONUtils.toString(result));
        return result;
    }

    @Override
    public String getHandlerMode() {
        return null;
    }

    @Override
    public List<String> getCardType(Integer organId) {
        //卡类型 1 表示身份证  2 表示就诊卡  3 表示就诊卡
        //根据运营平台配置  如果配置了就诊卡 医保卡（根据卡类型进行查询）； 如果都不配（默认使用身份证查询）
        String[] cardTypes = (String[]) configurationCenterUtilsService.getConfiguration(organId, "getCardTypeForHis");
        List<String> cardList = new ArrayList<>();
        if (cardTypes == null || cardTypes.length == 0) {
            cardList.add("1");
            return cardList;
        }
        return Arrays.asList(cardTypes);
    }

    /**
     * 获取患者信息
     *
     * @param request
     * @return
     */
    private PatientDTO obtainPatientInfo(FindHisRecipeListVO request) {
        logger.info("OfflineToOnlineService obtainPatientInfo request:{}", JSONUtils.toString(request));
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        patientDTO.setCardId(StringUtils.isNotEmpty(request.getCardId()) ? request.getCardId() : patientDTO.getCardId());
        logger.info("OfflineToOnlineService obtainPatientInfo req patientDTO:{}", JSONUtils.toString(patientDTO));
        return patientDTO;
    }

    /**
     * 如果前端status没传【卡片消息详情获取】，需根据参数判断，获取状态
     *
     * @param request
     * @return
     */
    private FindHisRecipeDetailReqVO obtainFindHisRecipeDetailParam(FindHisRecipeDetailReqVO request) {
        logger.info("OfflineToOnlineService obtainFindHisRecipeDetailParam request:{}", JSONUtils.toString(request));
        //获取对应的status
        if (StringUtils.isEmpty(request.getStatus())) {
            String status = hisRecipeManager.attachHisRecipeStatus(request.getMpiId(), request.getOrganId(), request.getRecipeCode());
            request.setStatus(status);
        }
        //获取对应的hisRecipeId
        if (request.getHisRecipeId() == null) {
            //如果为已处理，需要获取hisRecipeId,再根据hisRecipeId获取详情（）
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName().equals(request.getStatus())) {
                HisRecipe hisRecipe = hisRecipeManager.obatainHisRecipeByOrganIdAndMpiIdAndRecipeCode(request.getOrganId(), request.getMpiId(), request.getRecipeCode());
                if (hisRecipe != null) {
                    request.setHisRecipeId(hisRecipe.getHisRecipeID());
                }
            }
        }
        logger.info("OfflineToOnlineService obtainFindHisRecipeDetailParam req:{}", JSONUtils.toString(request));
        return request;
    }

}
