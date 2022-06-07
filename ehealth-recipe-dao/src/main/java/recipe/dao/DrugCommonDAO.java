package recipe.dao;

import com.ngari.recipe.entity.DrugCommon;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author jiangtingfeng
 * date:2017/5/22.
 */
@RpcSupportDAO
public abstract class DrugCommonDAO extends HibernateSupportDelegateDAO<DrugCommon> {

    public DrugCommonDAO() {
        super();
        this.setEntityName(DrugCommon.class.getName());
        this.setKeyField("id");
    }

    /**
     * 获取常用列表
     * @param organId 机构id
     * @param doctorId 医生id
     * @param drugTypes 药品类型
     * @return 常用药列表
     */
    @DAOMethod(sql = "from DrugCommon where organId=:organId and doctorId=:doctorId and  drugType in (:drugTypes) order by sort desc")
    public abstract List<DrugCommon> findByOrganIdAndDoctorIdAndDrugCode(@DAOParam("organId") Integer organId,
                                                                         @DAOParam("doctorId") Integer doctorId,
                                                                         @DAOParam("drugType") List<Integer> drugTypes,
                                                                         @DAOParam(pageStart = true) int start,
                                                                         @DAOParam(pageLimit = true) int limit);



}