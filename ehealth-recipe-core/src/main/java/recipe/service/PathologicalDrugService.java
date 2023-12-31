package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.PathologicalDrug;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.PathologicalDrugDAO;

import java.util.List;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/5/26.
 */
@RpcBean("pathologicalDrugService")
public class PathologicalDrugService {
    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PathologicalDrugService.class);

    @RpcService
    public List<DrugListBean> findPathologicalDrugList(PathologicalDrug pDrug, int start, int limit) {
        if (null == pDrug || null == pDrug.getPathologicalType()) {
            return Lists.newArrayList();
        }
        PathologicalDrugDAO pathologicalDrugDAO = DAOFactory.getDAO(PathologicalDrugDAO.class);
        List<DrugList> list = pathologicalDrugDAO.findPathologicalDrugList(pDrug.getPathologicalType(), start, limit);
        return ObjectCopyUtils.convert(list, DrugListBean.class);
    }

}
