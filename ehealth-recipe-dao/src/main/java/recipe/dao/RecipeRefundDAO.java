package recipe.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
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
        this.setEntityName(Recipe.class.getName());
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
    @DAOMethod(sql = "from RecipeRefund where recipeId = :recipeId order by modifyDate desc")
    public abstract List<RecipeRefund> findRefundListByRecipeId(@DAOParam("recipeId") Integer recipeId);

    /**
     * 根据订单编号获取处方id集合
     *
     * @return
     */
    @DAOMethod(sql = "select count(*) from RecipeRefund")
    public abstract Long getCountByAll();

}
