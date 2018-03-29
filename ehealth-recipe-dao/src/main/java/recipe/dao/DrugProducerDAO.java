package recipe.dao;

import com.ngari.recipe.entity.DrugProducer;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author zhongzx/0004
 * @date 2016/7/4
 * 药品产地 dao
 */
@RpcSupportDAO
public abstract class DrugProducerDAO extends HibernateSupportDelegateDAO<DrugProducer>
        implements DBDictionaryItemLoader<DrugProducer> {

    public DrugProducerDAO() {
        super();
        this.setEntityName(DrugProducer.class.getName());
        this.setKeyField("id");
    }


    /**
     * zhongzx
     * 根据机构和产地名字 查找产地
     *
     * @param name
     * @param organ
     * @return
     */
    @DAOMethod
    public abstract List<DrugProducer> findByNameAndOrgan(String name, Integer organ);

}

