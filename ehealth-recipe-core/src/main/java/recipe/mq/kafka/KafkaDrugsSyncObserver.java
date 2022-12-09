package recipe.mq.kafka;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import es.api.DrugSearchService;
import es.vo.DoctorDrugDetailVO;
import es.vo.PatientDrugDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import recipe.client.DrugClient;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.manager.DrugManager;
import recipe.service.RecipeHisService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.LocalStringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 同步业务数据
 * 判断消息中的表，更新到es
 *
 * @author 0421
 */
public class KafkaDrugsSyncObserver implements Observer<String> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaDrugsSyncObserver.class);
    private DrugManager drugManager = AppContextHolder.getBean("drugManager", DrugManager.class);

    private static final String DRUG_LIST = "base_druglist";

    private static final String ORGAN_DRUG_LIST = "base_organdruglist";

    private static final String TYPE_UPDATE = "UPDATE";
    private static final String TYPE_INSERT = "INSERT";
    private static final String TYPE_DELETE = "DELETE";
    private static final List<String> TYPE_LIST = Arrays.asList(TYPE_INSERT, TYPE_UPDATE, TYPE_DELETE);

    @Override
    public void onMessage(String message) {
        try {
            //message示例 {"data":[{"DrugId":"1","DrugName":"阿莫西林胶囊","SaleName":"阿莫灵 阿莫西林胶囊","DrugSpec":"0.25g*24粒","Pack":"24","Unit":"盒","DrugType":"1","DrugClass":"0101","UseDose":"0.250","UseDoseUnit":"g","UsingRate":"tid","UsePathways":"po","Producer":"香港澳美制药厂","Instructions":null,"DrugPic":null,"Price1":"9.39000","Price2":"9.39000","Status":"1","SourceOrgan":null,"Indications":"\\急性病3天量\\慢性病14天量（需有慢性诊断）\\","PyCode":"","CreateDt":"2016-07-01 00:00:00","LastModify":"2022-03-17 09:55:27","AllPyCode":"","ApprovalNumber":"","licenseNumber":"","standardCode":"","drugForm":"","packingMaterials":"","baseDrug":null,"drugCode":"1","usingRateId":"4","usePathwaysId":null,"isRegulation":"0","isPrescriptions":"0","isStandardDrug":"0"}],"database":"eh_recipe_devtest","es":1647482127000,"id":10366,"isDdl":false,"mysqlType":{"DrugId":"int(11)","DrugName":"varchar(100)","SaleName":"varchar(100)","DrugSpec":"varchar(100)","Pack":"smallint(5) unsigned","Unit":"varchar(6)","DrugType":"int(11)","DrugClass":"varchar(20)","UseDose":"decimal(10,3)","UseDoseUnit":"varchar(6)","UsingRate":"varchar(10)","UsePathways":"varchar(10)","Producer":"varchar(255)","Instructions":"int(11)","DrugPic":"varchar(50)","Price1":"decimal(11,5)","Price2":"decimal(11,5)","Status":"int(11)","SourceOrgan":"int(11)","Indications":"varchar(255)","PyCode":"varchar(100)","CreateDt":"timestamp","LastModify":"timestamp","AllPyCode":"varchar(255)","ApprovalNumber":"varchar(100)","licenseNumber":"varchar(30)","standardCode":"varchar(30)","drugForm":"varchar(20)","packingMaterials":"varchar(50)","baseDrug":"tinyint(1) unsigned","drugCode":"varchar(30)","usingRateId":"varchar(20)","usePathwaysId":"varchar(20)","isRegulation":"tinyint(2)","isPrescriptions":"tinyint(2)","isStandardDrug":"tinyint(2)"},"old":[{"DrugClass":"01010","LastModify":"2022-03-17 09:55:20"}],"pkNames":["DrugId"],"sql":"","sqlType":{"DrugId":4,"DrugName":12,"SaleName":12,"DrugSpec":12,"Pack":5,"Unit":12,"DrugType":4,"DrugClass":12,"UseDose":3,"UseDoseUnit":12,"UsingRate":12,"UsePathways":12,"Producer":12,"Instructions":4,"DrugPic":12,"Price1":3,"Price2":3,"Status":4,"SourceOrgan":4,"Indications":12,"PyCode":12,"CreateDt":93,"LastModify":93,"AllPyCode":12,"ApprovalNumber":12,"licenseNumber":12,"standardCode":12,"drugForm":12,"packingMaterials":12,"baseDrug":-6,"drugCode":12,"usingRateId":12,"usePathwaysId":12,"isRegulation":-6,"isPrescriptions":-6,"isStandardDrug":-6},"table":"base_druglist","ts":1647482127297,"type":"UPDATE"}
            JSONObject value = JSONObject.parseObject(message);
            String type = value.getString("type");
            if (!TYPE_LIST.contains(type)) {
                return;
            }
            int deleteFlag = TYPE_DELETE.equals(type) ? 1 : 0;
            String table = value.getString("table");
            LOG.info("table:" + table);
            JSONArray array = value.getJSONArray("data");
            LOG.info("data:" + JSONArray.toJSONString(array));

            if (DRUG_LIST.equals(table)) {
                LOG.info("KafkaDrugsSyncObserver  message={}", JSONUtils.toString(message));
                List<DrugList> drugLists = JSONObject.parseArray(JSONObject.toJSONString(array), DrugList.class);
                // 武昌药品数据变更要同步his
                List<DrugList> finalDrugList = drugLists;
                try {
                    if (drugLists.get(0).getSourceOrgan() == 1001780) {
                        RecipeBusiThreadPool.submit(() -> {
                            RecipeHisService recipeHisService = RecipeAPI.getService(RecipeHisService.class);
                            recipeHisService.syncDrugListToHis(finalDrugList);
                            return null;
                        });
                    }
                } catch (Exception e) {
                    LOG.error("武昌药品数据变更要同步his error", e);
                }

                // 先更新 drugList
                drugManager.updateDrugListToEs(drugLists, deleteFlag);
                // 再处理OrganDrugList信息
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                // 数据量存在不确定性
                List<Integer> drugIds = drugLists.stream().map(DrugList::getDrugId).collect(Collectors.toList());
                List<OrganDrugList> organDrugLists = organDrugListDAO.findOrganDrugByDrugIds(drugIds);
                LOG.info("KafkaDrugsSyncObserver " + DRUG_LIST + " modify organDrugList size={}", organDrugLists.size());
                drugManager.updateOrganDrugListToEs(organDrugLists, deleteFlag, drugLists);
            } else if (ORGAN_DRUG_LIST.equals(table)) {
                LOG.info("KafkaDrugsSyncObserver  message={}", JSONUtils.toString(message));
                List<OrganDrugList> organDrugLists = JSONObject.parseArray(JSONObject.toJSONString(array), OrganDrugList.class);
                drugManager.updateOrganDrugListToEs(organDrugLists, deleteFlag, null);
            }

        } catch (Exception e) {
            LOG.error("KafkaDrugsSyncObserver save failed.", e);
        }
    }

}
