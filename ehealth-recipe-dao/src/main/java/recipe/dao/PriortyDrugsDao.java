package recipe.dao;

import com.ngari.recipe.entity.PriortyDrug;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * 重点药品表
 * @author jiangtingfeng
 */
@RpcSupportDAO
public abstract class PriortyDrugsDao extends HibernateSupportDelegateDAO<PriortyDrug>{

    public PriortyDrugsDao() {
        super();
        this.setEntityName(PriortyDrug.class.getName());
        this.setKeyField("id");
    }

    /**
     * 获取药品
     * @return
     */
    @DAOMethod(sql = " from PriortyDrug order by sort desc")
    public abstract List<PriortyDrug> findPriortyDrugs();
}



