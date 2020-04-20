package recipe.dao;

import com.ngari.recipe.entity.ChronicDisease;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * Created by Erek on 2020/4/20.
 */
@RpcSupportDAO
public abstract class ChronicDiseaseDAO extends HibernateSupportDelegateDAO<ChronicDisease> {
    public ChronicDiseaseDAO() {
        super();
        this.setEntityName(ChronicDisease.class.getName());
        this.setKeyField("chronicDiseaseId");
    }

    @DAOMethod(sql = "from ChronicDisease where organId =:organId and chronicDiseaseFlag=:chronicDiseaseFlag and status = 1")
    public abstract List<ChronicDisease> findChronicDiseasesByOrganId(@DAOParam("organId") Integer organId,
                                                                 @DAOParam("chronicDiseaseFlag") Integer chronicDiseaseFlag);
}
