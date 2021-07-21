package recipe.dao;

import com.ngari.recipe.common.RecipePatientRefundVO;
import com.ngari.recipe.entity.RecipeRefund;
import ctd.dictionary.DictionaryController;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @DAOMethod(sql = "from RecipeRefund where busId = :recipeId order by applyTime desc, node desc")
    public abstract List<RecipeRefund> findRefundListByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据id获取
     *
     * @param recipeId
     * @return
     */
    @DAOMethod(sql = "from RecipeRefund where busId = :recipeId and node = 9 order by applyTime desc, node desc")
    public abstract List<RecipeRefund> findRefundListByRecipeIdAndNode(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = "from RecipeRefund where busId in (:recipeId) and node = 9 order by applyTime desc, node desc", limit = 0)
    public abstract List<RecipeRefund> findRefundListByRecipeIdsAndNode(@DAOParam("recipeId") List<Integer> recipeId);


    /**
     * 根据处方和node状态获取退费的一单信息
     *
     * @return
     */
    @DAOMethod(sql = "from RecipeRefund where busId = :busId and node = :node ", limit = 1)
    public abstract List<RecipeRefund> findRecipeRefundByRecipeIdAndNode(@DAOParam("busId") Integer busId, @DAOParam("node") Integer node);

    public List<RecipePatientRefundVO> findDoctorPatientRefundListByRefundType(Integer doctorId, Integer refundType, int start, int limit) {
        HibernateStatelessResultAction<List<RecipePatientRefundVO>> action = new AbstractHibernateStatelessResultAction<List<RecipePatientRefundVO>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sqlNew = new StringBuilder(" SELECT " +
                        "  r.busId, " +
                        "  r.patientName, " +
                        "  r.Mpiid, " +
                        "  r.doctorId, " +
                        "  r.Price, " +
                        "  GROUP_CONCAT( IF ( node =- 1, r.Reason, NULL ) ) AS '申请理由', " +
                        "  GROUP_CONCAT( IF ( node = 0, r.Reason, NULL ) ) AS '审核理由', " +
                        "  max( r.STATUS ), " +
                        "  min( r.checkTime ) " +
                        " FROM " +
                        "  `cdr_recipe_refund` r " +
                        " WHERE " +
                        "  doctorId = :doctorId " +
                        "  AND node in (-1, 0) " +
                        " GROUP BY " +
                        "  r.BusId " );
                if(null != refundType){
                    switch(refundType){
                        case 0:
                            break;
                        case 1:
                        case 2:
                        case 3:
                            sqlNew.append(" having max( r.STATUS ) = :refundStatus ");
                            break;
                        default:
                            LOGGER.warn("当前查询处方退费列表信息，没有传状态，不做筛选");
                            break;
                    }

                }
                sqlNew.append(" order by r.checkTime desc ");

                Query query = ss.createSQLQuery(sqlNew.toString());
                if(null != doctorId){
                    query.setParameter("doctorId", doctorId);
                }
                if(null != refundType && ! refundType.equals(new Integer(0))){

                    query.setParameter("refundStatus", refundType -1);
                }
                query.setFirstResult(start);
                query.setMaxResults(limit);

                List<Object[]> result = query.list();
                List<RecipePatientRefundVO> results = new ArrayList<RecipePatientRefundVO>();
                for(Object[] b : result){
                    RecipePatientRefundVO recipePatientRefundVO = new RecipePatientRefundVO();
                    recipePatientRefundVO.setBusId(getIntValue(b[0]));
                    recipePatientRefundVO.setDoctorId(getIntValue(b[3]));
                    recipePatientRefundVO.setDoctorNoPassReason(getStringValue(b[6]));
                    recipePatientRefundVO.setPatientMpiid(getStringValue(b[2]));
                    recipePatientRefundVO.setPatientName(getStringValue(b[1]));
                    recipePatientRefundVO.setRefundDate(getDateValue(b[8]));
                    recipePatientRefundVO.setRefundPrice(getDoubleValue(b[4]));
                    recipePatientRefundVO.setRefundReason(getStringValue(b[5]));
                    recipePatientRefundVO.setRefundStatus(getIntValue(b[7]));
                    recipePatientRefundVO.setRefundStatusMsg(null != b[7] ? DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(b[7]) : null);
                    results.add(recipePatientRefundVO);
                }



                setResult(results);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    private Date getDateValue(Object o) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
        return null != o ? format.parse(o.toString()) : null;
    }

    private String getStringValue(Object o) {
        return null != o ? o.toString() : null;
    }

    private Integer getIntValue(Object o) {
        return null != o ? Integer.parseInt(o.toString()) : null;
    }

    private Double getDoubleValue(Object o) {
        return null != o ? Double.parseDouble(o.toString()) : null;
    }

    public RecipePatientRefundVO getDoctorPatientRefundByRecipeId(Integer busId) {
        HibernateStatelessResultAction<RecipePatientRefundVO> action = new AbstractHibernateStatelessResultAction<RecipePatientRefundVO>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder sqlNew = new StringBuilder(" SELECT " +
                        "  r.busId busId, " +
                        "  r.patientName patientName, " +
                        "  r.Mpiid patientMpiid, " +
                        "  r.doctorId doctorId, " +
                        "  r.Price refundPrice, " +
                        "  GROUP_CONCAT( IF ( node =- 1, r.Reason, NULL ) ) AS  refundReason, " +
                        "  GROUP_CONCAT( IF ( node = 0, r.Reason, NULL ) ) AS doctorNoPassReason, " +
                        "  max( r.STATUS ) refundStatus, " +
                        "  min( r.checkTime ) refundDate " +
                        " FROM " +
                        "  `cdr_recipe_refund` r " +
                        " WHERE " +
                        "  node in (-1, 0) " +
                        " and  " +
                        "  r.BusId  = :busId " );
                Query query = ss.createSQLQuery(sqlNew.toString())
                        .addScalar("refundPrice", StandardBasicTypes.DOUBLE)
                        .addScalar("doctorId", StandardBasicTypes.INTEGER)
                        .addScalar("refundStatus", StandardBasicTypes.INTEGER)
                        .addScalar("refundReason", StandardBasicTypes.STRING)
                        .addScalar("refundDate", TimestampType.INSTANCE)
                        .addScalar("patientName", StandardBasicTypes.STRING)
                        .addScalar("busId", StandardBasicTypes.INTEGER)
                        .addScalar("patientMpiid", StandardBasicTypes.STRING)
                        .addScalar("doctorNoPassReason", StandardBasicTypes.STRING)
                        .setResultTransformer(new AliasToBeanResultTransformer(RecipePatientRefundVO.class));
                if(null != busId){
                    query.setParameter("busId", busId);
                }
                RecipePatientRefundVO result = (RecipePatientRefundVO) query.uniqueResult();
                result.setRefundStatusMsg(null != result.getRefundStatus() ? DictionaryController.instance().get("eh.cdr.dictionary.RecipeRefundCheckStatus").getText(result.getRefundStatus()) : null);
                setResult(result);
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
