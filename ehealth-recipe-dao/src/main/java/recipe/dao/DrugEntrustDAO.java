package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.DrugEntrust;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.model.PharmacyTcmDTO;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;

import java.util.List;
import java.util.Map;

/**
 * 药品嘱托dao
 *
 * @author renfuhao
 */
@RpcSupportDAO
public abstract class DrugEntrustDAO extends HibernateSupportDelegateDAO<DrugEntrust> {
    private static Logger logger = Logger.getLogger(DrugEntrustDAO.class);

    public DrugEntrustDAO() {
        super();
        this.setEntityName(DrugEntrust.class.getName());
        this.setKeyField("drugEntrustId");
    }

    /**
     * 通过 drugEntrustName获取平台默认的嘱托
     * @param drugEntrustName
     * @return
     */
    @DAOMethod(sql = "from DrugEntrust where organId=0 and drugEntrustName=:drugEntrustName")
    public abstract DrugEntrust getDrugEntrustInfoByName(@DAOParam("drugEntrustName") String drugEntrustName);


    /**
     * 通过嘱托Id查找嘱托
     * @param drugEntrustId
     * @return
     */
    @DAOMethod(sql = "select drugEntrustName from DrugEntrust where drugEntrustId=:drugEntrustId")
    public abstract String getDrugEntrustById(@DAOParam("drugEntrustId") Integer drugEntrustId);

    /**
     * 通过orgsnId和 药房编码获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugEntrust where organId=:organId and drugEntrustCode=:drugEntrustCode")
    public abstract DrugEntrust getByOrganIdAndDrugEntrustCode(@DAOParam("organId") Integer organId, @DAOParam("drugEntrustCode") String drugEntrustCode);

    /**
     * pharmacyName 查找相应药房ID
     *
     * @param drugEntrustName
     * @return
     */
    @DAOMethod(sql = "select drugEntrustId from DrugEntrust where drugEntrustName=:drugEntrustName")
    public abstract Integer getIdByDrugEntrustName(@DAOParam("drugEntrustName") String drugEntrustName);

    /**
     * pharmacyName 查找相应药房ID
     *
     * @param drugEntrustName
     * @return
     */
    @DAOMethod(sql = "select drugEntrustId from DrugEntrust where drugEntrustName=:drugEntrustName and organId=:organId")
    public abstract Integer getIdByDrugEntrustNameAndOrganId(@DAOParam("drugEntrustName") String drugEntrustName,@DAOParam("organId") Integer organId);


    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugEntrust where organId=:organId order by sort ASC " ,limit =0)
    public abstract List<DrugEntrust> findByOrganId(@DAOParam("organId") Integer organId);

    /**
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugEntrust where organId=:organId and drugEntrustName=:drugEntrustName order by sort ASC " ,limit =0)
    public abstract DrugEntrust getByOrganIdAndName(@DAOParam("organId") Integer organId,@DAOParam("drugEntrustName") String drugEntrustName);

    /**
     * 通过orgsnId获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = " select  drugEntrustId from DrugEntrust where organId=:organId order by sort ASC " ,limit =0)
    public abstract List<Integer> findDrugEntrustByOrganId(@DAOParam("organId") Integer organId);


    /**
     * 通过orgsnId和 药房名称获取
     *
     * @param organId
     * @return
     */
    @DAOMethod(sql = "from DrugEntrust where organId=:organId and drugEntrustName=:drugEntrustName")
    public abstract DrugEntrust getByOrganIdAndDrugEntrustName(@DAOParam("organId") Integer organId,@DAOParam("drugEntrustName") String drugEntrustName);


    /**
     * 通过orgsnId 和症候名称  模糊查询
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    public QueryResult<DrugEntrustDTO> queryTempByTimeAndName(Integer organId , String input, Integer start, Integer limit){
        HibernateStatelessResultAction<QueryResult<DrugEntrustDTO>> action = new AbstractHibernateStatelessResultAction<QueryResult<DrugEntrustDTO>>(){

            @Override
            public void execute(StatelessSession ss) throws Exception {
                if (null == organId) {
                    throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
                }
                Map<String,Object> param = Maps.newHashMap();
                StringBuffer sql = new StringBuffer(" from DrugEntrust d where d.organId =:organId ");
                param.put("organId",organId);
                if (!ObjectUtils.isEmpty(input)){
                    sql.append(" and d.drugEntrustName like:name ");
                    param.put("name","%"+input+"%");
                }
                sql.append(" order by d.drugEntrustId,d.sort ASC");
                Query countQuery = ss.createQuery("select count(*) "+sql.toString());
                countQuery.setProperties(param);
                Long total = (Long) countQuery.uniqueResult();
                Query query = ss.createQuery("select d "+sql.toString());
                query.setProperties(param);
                query.setFirstResult(start);
                query.setMaxResults(limit);
                List<DrugEntrust> temps = query.list();
                List<DrugEntrustDTO> convert = ObjectCopyUtils.convert(temps, DrugEntrustDTO.class);
                setResult(new QueryResult<>(total, query.getFirstResult(), query.getMaxResults(), convert));
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "select count(*) from DrugEntrust where organId=:organId")
    public abstract Long getCountOfOrgan(@DAOParam("organId") Integer organId);


}
