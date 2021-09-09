package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IOrganBusinessService;

import java.util.List;
import java.util.Set;

/**
 * 机构相关服务
 */
@RpcBean(value = "organAtop")
public class OrganPatientAtop extends BaseAtop {

    @Autowired
    private IOrganBusinessService organBusinessService;

    @RpcService
    public List<Integer> getOrganForWeb() {
        try {
            List<Integer> result = organBusinessService.getOrganForWeb();
            logger.info("OrganAtop getOrganForWeb result:{}.", result);
            return result;
        } catch (DAOException e1) {
            logger.error("OrganAtop getOrganForWeb error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OrganAtop getOrganForWeb error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 获取机构购药方式配置
     * @param organId organId
     * @return 购药方式列表
     */
    @RpcService
    public List<GiveModeButtonBean> getOrganGiveMode(Integer organId){
        logger.info("OrganAtop getOrganGiveModeConfig organId:{}.", organId);
        try {
            List<GiveModeButtonBean> result = organBusinessService.getOrganGiveModeConfig(organId);
            logger.info("OrganAtop getOrganGiveModeConfig result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.error("OrganAtop getOrganGiveModeConfig error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("OrganAtop getOrganGiveModeConfig error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
