package recipe.dao;

import com.ngari.recipe.entity.SaleDrugListSyncField;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.comment.ExtendDao;

import java.util.List;

/**
 * @author 刘敏
 */
@RpcSupportDAO
public abstract class SaleDrugListSyncFieldDAO extends HibernateSupportDelegateDAO<SaleDrugListSyncField>
        implements DBDictionaryItemLoader<SaleDrugListSyncField>, ExtendDao<SaleDrugListSyncField> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaleDrugListSyncFieldDAO.class);
    private static final Integer ALL_DRUG_FLAG = 9;

    public SaleDrugListSyncFieldDAO() {
        super();
        this.setEntityName(SaleDrugListSyncField.class.getName());
        this.setKeyField("id");
    }

    @Override
    public boolean updateNonNullFieldByPrimaryKey(SaleDrugListSyncField saleDrugListSyncField) {
        return updateNonNullFieldByPrimaryKey(saleDrugListSyncField, "id");
    }

    /**
     * 根据药企id获取药企同步数据表数据
     *
     * @param drugsenterpriseId
     * @return
     */
    @DAOMethod(sql = "from SaleDrugListSyncField where drugsenterpriseId=:drugsenterpriseId")
    public abstract List<SaleDrugListSyncField> findByDrugsenterpriseId(@DAOParam("drugsenterpriseId") int drugsenterpriseId);

    @DAOMethod(sql = "from SaleDrugListSyncField where drugsenterpriseId=:drugsenterpriseId and  type=:type")
    public abstract List<SaleDrugListSyncField>  findByDrugsenterpriseIdAndType(@DAOParam("drugsenterpriseId") int drugsenterpriseId,@DAOParam("type") String type);

    @DAOMethod(sql = "from SaleDrugListSyncField where drugsenterpriseId=:drugsenterpriseId and fieldCode=:fieldCode and type=:type")
    public abstract SaleDrugListSyncField getByDrugsenterpriseIdAndFieldCodeAndType(@DAOParam("drugsenterpriseId") int drugsenterpriseId,@DAOParam("fieldCode") String fieldCode,@DAOParam("type") String type);


}
