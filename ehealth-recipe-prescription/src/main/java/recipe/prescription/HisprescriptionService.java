package recipe.prescription;

import com.ngari.patient.service.BaseService;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.entity.Hisprescription;
import com.ngari.recipe.entity.HisprescriptionDetail;
import com.ngari.recipe.hisprescription.model.HisprescriptionDetailTO;
import com.ngari.recipe.hisprescription.model.HisprescriptionTO;
import com.ngari.recipe.hisprescription.service.IHisprescriptionService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.HisprescriptionDAO;
import recipe.dao.HisprescriptionDetailDAO;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description： 接收医院处方, 与cdr_recipe不同表
 * @version： 1.0
 */

@RpcBean("remoteHisprescriptionService")
public class HisprescriptionService extends BaseService<HisprescriptionTO> implements IHisprescriptionService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HisprescriptionService.class);

    @RpcService
    @Override
    public HisprescriptionTO get(Object id) {
        HisprescriptionDAO hisprescriptionDAO = DAOFactory.getDAO(HisprescriptionDAO.class);
        Hisprescription recipe = hisprescriptionDAO.get(id);
        return getBean(recipe, HisprescriptionTO.class);
    }

    @RpcService
    @Override
    public RecipeCommonResTO createPrescription(HisprescriptionTO hisprescription) {
        HisprescriptionDAO hisprescriptionDAO = DAOFactory.getDAO(HisprescriptionDAO.class);
        RecipeCommonResTO response = new RecipeCommonResTO();
        response.setCode(RecipeCommonResTO.FAIL);
        if (null != hisprescription) {
            try {
                Hisprescription dbHisprescription = hisprescriptionDAO.save(getBean(hisprescription, Hisprescription.class));
                if (CollectionUtils.isNotEmpty(hisprescription.getRecipeDetail())) {
                    HisprescriptionDetailDAO detailDAO = DAOFactory.getDAO(HisprescriptionDetailDAO.class);
                    HisprescriptionDetail hisprescriptionDetail;
                    for (HisprescriptionDetailTO detailTO : hisprescription.getRecipeDetail()) {
                        hisprescriptionDetail = getBean(detailTO, HisprescriptionDetail.class);
                        hisprescriptionDetail.setRecipeId(dbHisprescription.getRecipeId());
                        detailDAO.save(hisprescriptionDetail);
                    }
                }

                response.setCode(RecipeCommonResTO.SUCCESS);
            } catch (DAOException e) {
                response.setMsg("存储失败");
                LOGGER.error("createPrescription error. hisprescription={}", JSONUtils.toString(hisprescription), e);
            }
        } else {
            response.setMsg("对象为空");
        }
        return response;
    }

}
