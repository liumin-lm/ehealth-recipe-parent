package recipe.dao;

import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.persistence.support.impl.dictionary.DBDictionaryItemLoader;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.util.DateConversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 *
 * @author yinfeng 2020-05-21
 */
@RpcSupportDAO
public abstract class ImportDrugRecordDAO extends HibernateSupportDelegateDAO<ImportDrugRecord>
        implements DBDictionaryItemLoader<ImportDrugRecord> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDrugRecordDAO.class);


    public ImportDrugRecordDAO() {
        super();
        this.setEntityName(ImportDrugRecord.class.getName());
        this.setKeyField("recordId");
    }

    @DAOMethod(sql = "from ImportDrugRecord where organId =:organId",limit=0)
    public abstract List<ImportDrugRecord> findImportDrugRecordByOrganId(@DAOParam("organId") Integer organId);



}
