package recipe.atop.open;

import com.ngari.recipe.entity.Dispensatory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import recipe.api.open.IDrugAtopService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IDrugBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.DrugBookVo;

import javax.annotation.Resource;

/**
 * @description： 药品 open atop
 * @author： whf
 * @date： 2021-08-26 9:45
 */
@RpcBean("drugOpenAtop")
public class DrugOpenAtop extends BaseAtop implements IDrugAtopService {

    @Resource
    private IDrugBusinessService drugBusinessService;

    @Override
    public DrugBookVo getDrugBook(Integer organId, String organDrugCode) {
        logger.info("DrugOpenAtop getDrugBook organId={} organDrugCode={}", organId, organDrugCode);
        validateAtop(organId, organDrugCode);
        try {
            Dispensatory dispensatory = drugBusinessService.getDrugBook(organId,organDrugCode);
            DrugBookVo drugBookVo = ObjectCopyUtils.convert(dispensatory, DrugBookVo.class);
            logger.info("DrugOpenAtop getDrugBook result = {}", drugBookVo);
            return drugBookVo;
        } catch (DAOException e1) {
            logger.error("DrugOpenAtop getDrugBook error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugOpenAtop getDrugBook error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
