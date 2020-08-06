package recipe.dao;

import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.entity.DecoctionWay;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @company: ngarihealth
 * @author: gaomw
 * @date:2020/8/5.
 */
@RpcSupportDAO
public abstract class DrugDecoctionWayDao extends HibernateSupportDelegateDAO<DecoctionWay> {
    public static final Logger log = LoggerFactory.getLogger(DecoctionWay.class);
    public DrugDecoctionWayDao() {
        super();
        this.setEntityName(DecoctionWay.class.getName());
        this.setKeyField("methodId");
    }

    @DAOMethod(sql = "from DecoctionWay where organId =:organId ", limit = 0)
    public abstract List<DecoctionWayBean> findAllDecoctionWayByOrganId(@DAOParam("organId")Integer organId);
}
