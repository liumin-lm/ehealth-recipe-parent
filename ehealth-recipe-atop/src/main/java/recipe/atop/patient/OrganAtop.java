package recipe.atop.patient;

import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IOrganBusinessService;

import java.util.List;

/**
 * 机构相关服务
 */
@RpcBean(value = "organAtop")
public class OrganAtop extends BaseAtop{

    @Autowired
    private IOrganBusinessService organBusinessService;

    @RpcService
    public List<Integer> getOrganForWeb(){
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
}
