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

    /**
     * 根据机构id，病种类型获取病种数据，暂时去掉机构过滤后期有其他机构需要加上机构条件
     * @param chronicDiseaseFlag
     * @return
     */
    @DAOMethod(sql = "from ChronicDisease where  chronicDiseaseFlag=:chronicDiseaseFlag and status = 1")
    public abstract List<ChronicDisease> findChronicDiseasesByOrganId(@DAOParam("chronicDiseaseFlag") String chronicDiseaseFlag);
}
