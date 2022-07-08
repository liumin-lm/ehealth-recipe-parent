package recipe.dao;

import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.ImportDrugRecordMsg;
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
 * company: ngarihealth
 *
 * @author yinfeng 2020-05-21
 */
@RpcSupportDAO
public abstract class ImportDrugRecordMsgDAO extends HibernateSupportDelegateDAO<ImportDrugRecordMsg>
        implements ExtendDao<ImportDrugRecordMsg> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDrugRecordMsgDAO.class);


    public ImportDrugRecordMsgDAO() {
        super();
        this.setEntityName(ImportDrugRecordMsg.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "from ImportDrugRecordMsg where importDrugRecordId =:importdrugRecordId",limit=0)
    public abstract List<ImportDrugRecordMsg> findImportDrugRecordMsgByImportdrugRecordId(@DAOParam("importdrugRecordId") Integer importdrugRecordId);



}
