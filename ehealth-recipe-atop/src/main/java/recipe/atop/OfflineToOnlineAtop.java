package recipe.atop;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import recipe.constant.ErrorCode;
import recipe.core.api.IOfflineToOnlineService;
import recipe.vo.patient.RecipeGiveModeButtonRes;

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
    private IOfflineToOnlineService offlineToOnlineFactory;

    /**
     * todo 需要确认 是否正确 （刘敏）
     *
     * @param request
     * @return
     * @Description 线下处方点够药、缴费点结算 1、线下转线上 2、获取购药按钮
     * @Author liumin
     */
    @RpcService
    @Validated
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(@Valid SettleForOfflineToOnlineVO request) {
        logger.info("OfflineToOnlineAtop settleForOfflineToOnline request={}", JSON.toJSONString(request));
        validateAtop(request, request.getRecipeCode(), request.getOrganId(), request.getBusType(), request.getMpiId());
        try {
            List<RecipeGiveModeButtonRes> result = offlineToOnlineFactory.settleForOfflineToOnline(request);
            logger.info("OfflineToOnlineAtop settleForOfflineToOnline result = {}", JSON.toJSONString(result));
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
