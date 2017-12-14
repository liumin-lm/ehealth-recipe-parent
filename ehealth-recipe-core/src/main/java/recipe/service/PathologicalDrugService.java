package recipe.service;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.PathologicalDrug;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import jersey.repackaged.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.PathologicalDrugDAO;

import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/5/26.
 */
@RpcBean("pathologicalDrugService")
public class PathologicalDrugService {
    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PathologicalDrugService.class);

    @RpcService
    public List<DrugList> findPathologicalDrugList(PathologicalDrug pDrug, int start, int limit) {
        if (null == pDrug || null == pDrug.getPathologicalType()) {
            return Lists.newArrayList();
        }
        PathologicalDrugDAO pathologicalDrugDAO = DAOFactory.getDAO(PathologicalDrugDAO.class);
        return pathologicalDrugDAO.findPathologicalDrugList(pDrug.getPathologicalType(), start, limit);
    }

}
