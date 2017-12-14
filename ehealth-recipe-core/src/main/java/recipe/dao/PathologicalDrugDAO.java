package recipe.dao;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.PathologicalDrug;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2017/5/26.
 */
@RpcSupportDAO
public abstract class PathologicalDrugDAO extends HibernateSupportDelegateDAO<PathologicalDrug> {

    /**
     * logger
     */
//    private static final Logger logger = LoggerFactory.getLogger(PathologicalDrugDAO.class);
    public PathologicalDrugDAO() {
        super();
        this.setEntityName(PathologicalDrug.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "select d from PathologicalDrug p, DrugList d where p.drugId=d.drugId and p.pathologicalType=:pathologicalType " +
            "and d.status=1 order by p.sort desc ")
    public abstract List<DrugList> findPathologicalDrugList(@DAOParam("pathologicalType") int type, @DAOParam(pageStart = true) int start,
                                                            @DAOParam(pageLimit = true) int limit);
}
