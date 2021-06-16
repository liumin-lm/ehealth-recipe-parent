package recipe.offlinetoonline.atop;

import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import recipe.atop.BaseAtop;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.constant.ErrorCode;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.offlinetoonline.factory.OfflineToOnlineFactory;

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
    OfflineToOnlineFactory offlineToOnlineFactory;

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
            IOfflineToOnlineService offlineToOnlineService = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType());
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
