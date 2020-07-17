package recipe.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.common.RecipePatientRefundVO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipesQueryVO;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import ctd.util.converter.ConversionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import org.hibernate.type.LongType;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.*;
import recipe.dao.bean.PatientRecipeBean;
import recipe.dao.bean.RecipeRollingInfo;
import recipe.util.DateConversion;
import recipe.util.SqlOperInfo;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 处方退费DAO
 *
 * @author gaomw
 */
@RpcSupportDAO
public abstract class RecipeRefundDAO extends HibernateSupportDelegateDAO<RecipeRefund> {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    public RecipeRefundDAO() {
        super();
        this.setEntityName(RecipeRefund.class.getName());
        this.setKeyField("id");
    }

    public void saveRefund(RecipeRefund recipeRefund) {
        LOGGER.info("处方退费记录表保存：" + JSONUtils.toString(recipeRefund));
        super.save(recipeRefund);
    }
    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "from RecipeRefund where busId = :recipeId order by node desc, checkTime desc")
    public abstract List<RecipeRefund> findRefundListByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据订单编号获取处方id集合
     *
     * @return
     */
    @DAOMethod(sql = "select count(*) from RecipeRefund")
    public abstract Long getCountByAll();

    /**
     * 根据处方和node状态获取退费的一单信息
     *
     * @return
     */
    @DAOMethod(sql = "from RecipeRefund where busId = :busId and node = :node ")
    public abstract RecipeRefund getRecipeRefundByRecipeIdAndNode(@DAOParam("busId") Integer busId, @DAOParam("node") Integer node);

    public List<Integer> findDoctorPatientRefundListByRefundType(Integer doctorId, Integer refundType, int start, int limit) {
        HibernateStatelessResultAction<List<Integer>> action = new AbstractHibernateStatelessResultAction<List<Integer>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
//                StringBuilder sql = new StringBuilder("SELECT cr.BusId ,crd.Doctor,group_concat((case cr.node when 0  then cr.reason else '' end) separator '') ,crd.MPIID, crd.patientName, min(cr.checktime) ,cro.ActualPrice,group_concat((case cr.node when -1  then cr.reason else '' end) separator ''),max(cr.node),sum(case cr.node when 0  then cr.status else 0 end) checkStatus FROM `cdr_recipe_refund` cr  INNER JOIN `cdr_recipe` crd on cr.BusId = crd.RecipeID  " +
//                        " INNER JOIN `cdr_recipeorder` cro on crd.orderCode = cro.orderCode where crd.Doctor = :doctorId" +
//                        " GROUP BY cr.BusId ");
                StringBuilder sql = new StringBuilder("SELECT cr.BusId FROM `cdr_recipe_refund` cr ");
                //0：全部
                //1：待审核
                //2：审核通过
                //3：审核不通过
                if(null != refundType){
                    switch(refundType){
                        case 0:
                            sql.append("where cr.doctorId = :doctorId group by cr.BusId ");
                            break;
                        case 1:
                            sql.append("where cr.id in ( select MAX(id) from cdr_recipe_refund  r where r.doctorId = :doctorId GROUP BY BusId ) and node=-1 ");
                            break;
                        case 2:
                            sql.append("where cr.doctorId = :doctorId and cr.node = 0 and cr.status = 1 group by cr.BusId ");
                            break;
                        case 3:
                            sql.append("where cr.doctorId = :doctorId and cr.node = 0 and cr.status = 2 group by cr.BusId ");
                            break;
                        default:
                            LOGGER.warn("当前查询处方退费列表信息，没有传状态，不做筛选");
                            sql.append("group by cr.BusId ");
                            break;
                    }

                }
                sql.append(" order by cr.checkTime desc ");

                Query query = ss.createSQLQuery(sql.toString());
                if(null != doctorId){
                    query.setParameter("doctorId", doctorId);
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);



                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    /**
     * 根据id和关联的node获取列表 排序
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "from RecipeRefund where busId = :recipeId and node in :nodes order by status desc")
    public abstract List<RecipeRefund> findRefundListByRecipeIdAndNodes(@DAOParam("recipeId") Integer recipeId, @DAOParam("nodes") List<Integer> nodes);



}
