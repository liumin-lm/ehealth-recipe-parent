package recipe.service;

import com.ngari.base.searchcontent.model.SearchContentBean;
import com.ngari.base.searchcontent.service.ISearchContentService;
import com.ngari.recipe.entity.DrugList;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.PyConverter;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import recipe.dao.DrugListDAO;
import recipe.util.ApplicationUtils;
import recipe.util.RecipeUtil;

import java.util.*;

/**
 * Created by zhongzx on 2016/4/28 0028.
 * 药品服务
 */
@RpcBean("drugListService")
public class DrugListService {

    private static Logger logger = Logger.getLogger(DrugListService.class);

    /**
     * 获取热门类目
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    @RpcService
    public List<DictionaryItem> findHotDrugList(int organId, int drugType) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> list = drugListDAO.findDrugClassByDrugList(organId, drugType, "", 0, 6);
        List<DictionaryItem> all = drugListDAO.getDrugClass(null, 0);
        List<DictionaryItem> hotItem = new ArrayList<>();

        for (DrugList drugList : list) {
            for (DictionaryItem item : all) {
                if (item.getKey().equals(drugList.getDrugClass())) {
                    hotItem.add(item);
                    break;
                }
            }
        }
        return hotItem;
    }

    /**
     * 把热门类目和一级类目服务包装成一个服务
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @param parentKey
     * @return
     */
    @RpcService
    public HashMap<String, Object> findHotAndFirstDrugList(int organId, int drugType, String parentKey) {
        //热门类目
        List<DictionaryItem> hotList = findHotDrugList(organId, drugType);

        //一级类目
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DictionaryItem> firstList = drugListDAO.findChildByDrugClass(organId, drugType, parentKey);
        HashMap<String, Object> map = new HashMap<>();
        map.put("hot", hotList);
        map.put("first", firstList);
        return map;
    }

    /**
     * 添加药品
     *
     * @param d
     * @return
     * @author zhongzx
     */
    @RpcService
    public DrugList addDrugList(DrugList d) {
        logger.info("新增药品服务[addDrugList]:" + JSONUtils.toString(d));
        if (null == d) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugList is null");
        }
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        //根据saleName 判断改药品是否已添加
        if (StringUtils.isEmpty(d.getDrugName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName is needed");
        }
        if (StringUtils.isEmpty(d.getSaleName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleName is needed");
        }
        if (null == d.getPrice1()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price1 is needed");
        }
        if (null == d.getPrice2()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price2 is needed");
        }
        if (null == d.getDrugType()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugType is needed");
        }
        if (StringUtils.isEmpty(d.getDrugClass())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugClass is needed");
        }
        if (null == d.getStatus()) {
            d.setStatus(1);
        }
        d.setCreateDt(new Date());
        d.setLastModify(new Date());
        d.setAllPyCode(PyConverter.getPinYinWithoutTone(d.getSaleName()));
        d.setPyCode(PyConverter.getFirstLetter(d.getSaleName()));
        return dao.save(d);
    }

    /**
     * 给没有拼音全码的加上拼音全码
     */
    @RpcService
    public void setAllPyCode() {
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> list = dao.findAll();
        for (DrugList d : list) {
            if (StringUtils.isEmpty(d.getAllPyCode())) {
                d.setAllPyCode(PyConverter.getPinYinWithoutTone(d.getSaleName()));
                dao.update(d);
            }
        }
    }

    //给所有药物加上拼音简码
    @RpcService
    public void setPyCode() {
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> list = dao.findAll();
        for (DrugList d : list) {
            if (StringUtils.isEmpty(d.getPyCode())) {
                d.setPyCode(PyConverter.getFirstLetter(d.getSaleName()));
                dao.update(d);
            }
        }
    }

    /**
     * 更新药品信息
     *
     * @param drugList
     * @return
     * @author zhongzx
     */
    @RpcService
    public DrugList updateDrugList(DrugList drugList) {
        logger.info("修改药品服务[updateDrugList]:" + JSONUtils.toString(drugList));
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        if (null == drugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        DrugList target = dao.getById(drugList.getDrugId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "Can't found drugList");
        } else {
            drugList.setLastModify(new Date());
            BeanUtils.map(drugList, target);
            target = dao.update(target);
        }
        return target;
    }

    /**
     * 运营平台 药品查询服务
     *
     * @param drugClass 药品分类
     * @param status    药品状态
     * @param keyword   查询关键字:药品名称 or 生产厂家 or 商品名称 or 批准文号 or drugId
     * @param start     分页起始位置
     * @param limit     每页限制条数
     * @return QueryResult<DrugList>
     * @author houxr
     */
    @RpcService
    public QueryResult<DrugList> queryDrugListsByDrugNameAndStartAndLimit(final String drugClass, final String keyword,
                                                                          final Integer status,
                                                                          final int start, final int limit) {
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        return dao.queryDrugListsByDrugNameAndStartAndLimit(drugClass, keyword, status, start, limit);
    }

    /**
     * 患者端 获取对应机构的西药 或者 中药的药品有效全目录（现在目录有二级）
     *
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<Map<String, Object>> queryDrugCatalog() {
        List<Map<String, Object>> returnList = new ArrayList<>();
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        //先获得一级有效类目
        List<DictionaryItem> firstList = drugListDAO.findChildByDrugClass(null, null, "");

        //再获取二级有效目录
        for (DictionaryItem first : firstList) {
            List<DictionaryItem> childList = drugListDAO.findChildByDrugClass(null, null, first.getKey());
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("key", first.getKey());
            map.put("text", first.getText());
            map.put("leaf", first.isLeaf());
            map.put("index", first.getIndex());
            map.put("mcode", first.getMCode());
            map.put("child", childList);
            returnList.add(map);
        }
        return returnList;
    }


    /**
     * 医生端 获取有效药品目录
     *
     * @param organId  机构平台编号
     * @param drugType 药品类型 1-西药 2-中成药
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<Map<String, Object>> queryDrugCatalogForDoctor(int organId, int drugType) {
        List<Map<String, Object>> returnList = new ArrayList<>();
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        //先获得一级有效类目
        List<DictionaryItem> firstList = drugListDAO.findChildByDrugClass(organId, drugType, "");

        //再获取二级有效目录
        for (DictionaryItem first : firstList) {
            List<DictionaryItem> childList = drugListDAO.findChildByDrugClass(organId, drugType, first.getKey());
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("key", first.getKey());
            map.put("text", first.getText());
            map.put("leaf", first.isLeaf());
            map.put("index", first.getIndex());
            map.put("mcode", first.getMCode());
            map.put("child", childList);
            returnList.add(map);
        }
        return returnList;
    }

    /**
     * 患者端 推荐药品列表
     *
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<DrugList> recommendDrugList(int start, int limit) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugList = drugListDAO.queryDrugList(start, limit);
        return drugList;
    }

    /**
     * 患者端 查询某个二级药品目录下的药品列表
     *
     * @param drugClass 药品二级目录
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<DrugList> queryDrugsInDrugClass(String drugClass, int start, int limit) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        // TODO: 2017/3/13 0013  因为梅州药品的原因 患者端 写死查询邵逸夫的药品
        List<DrugList> drugList = drugListDAO.findDrugListsByOrganOrDrugClass(1, null, drugClass, start, limit);
        return drugList;
    }

    /**
     * 患者端 药品搜索服务 药品名 商品名 拼音 别名
     *
     * @param drugName 搜索的文字或者拼音
     * @param start
     * @param limit
     * @return
     * @author zhongzx
     */
    @RpcService
    public List<DrugList> searchDrugByNameOrPyCode(String drugName, String mpiId, int start, int limit) {
        saveSearchContendForDrug(drugName, mpiId);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        //todo 因为 梅州药品的原因 患者端 写死查询邵逸夫的药品
        return drugListDAO.searchDrugListWithES(1, null, drugName, start, limit);
    }

    /**
     * PC端 药品搜索
     *
     * @param organId
     * @param drugType
     * @param drugName
     * @param start
     * @return
     */
    @RpcService
    public List<DrugList> searchDrugByNameOrPyCodeForPC(
            final int organId, final int drugType, final String drugName, final int start, final int limit) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        return drugListDAO.searchDrugListWithES(organId, drugType, drugName, start, limit);
    }

    @RpcService
    public void saveSearchContendForDrug(String drugName, String mpiId) {
        /**
         * 保存患者搜索记录(药品，医生)
         */
        if (!StringUtils.isEmpty(drugName) && !StringUtils.isEmpty(mpiId)) {
            ISearchContentService iSearchContentService = ApplicationUtils.getBaseService(ISearchContentService.class);
            SearchContentBean content = new SearchContentBean();
            content.setMpiId(mpiId);
            content.setContent(drugName);
            content.setBussType(2);
            iSearchContentService.addSearchContent(content, 0);
        }
    }

    /**
     * 更新药品首字母拼音字段--更换成商品名首字母拼音
     *
     * @return
     * @author zhongzx
     */
    @RpcService
    public boolean updatePyCode() {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findAll();
        Map<String, Object> changeAttr = new HashMap<>();
        for (DrugList drugList : drugLists) {
            String pyCode = PyConverter.getFirstLetter(drugList.getSaleName());
            changeAttr.put("pyCode", pyCode);
            drugListDAO.updateDrugListInfoById(drugList.getDrugId(), changeAttr);
        }
        return true;
    }

    /**
     * 三级目录改成二级目录，更改 三级目录药品 到对应的二级目录
     *
     * @return
     * @author zhongzx
     */
    @RpcService
    public boolean updateDrugCatalog() {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> dList = drugListDAO.findAll();
        Map<String, Object> changeAttr = new HashMap<>();
        for (DrugList drugList : dList) {
            String drugClass = drugList.getDrugClass();
            if (6 == drugClass.length()) {
                drugClass = drugClass.substring(0, 4);
                changeAttr.put("drugClass", drugClass);
                drugListDAO.updateDrugListInfoById(drugList.getDrugId(), changeAttr);
            }
        }
        return true;
    }

    /**
     * PC端搜索药品，药品参数为空时展示该机构的药品列表
     *
     * @param organId
     * @param drugType
     * @param drugClass
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<DrugList> findAllInDrugClassByOrganForPC(int organId, int drugType,
                                                         String drugClass, int start, int limit) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> dList = drugListDAO.findDrugListsByOrganOrDrugClass(organId, drugType, drugClass, start,
                limit);
        // 添加医院价格
        if (!dList.isEmpty()) {
            RecipeUtil.getHospitalPrice(dList);
        }
        return dList;
    }
}
