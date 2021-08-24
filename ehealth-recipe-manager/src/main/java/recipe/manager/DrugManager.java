package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugClient;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 药品数据处理类
 *
 * @author fuzi
 */
@Service
public class DrugManager extends BaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugManager.class);
    @Autowired
    private DrugClient drugClient;
    @Autowired
    private DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private DrugEntrustDAO drugEntrustDAO;
    @Autowired
    private DrugListDAO drugListDAO;

    /**
     * todo 分层不合理 静态不合理 方法使用不合理 需要修改 （尹盛）
     * 后台处理药品显示名---卡片消息/处方笺/处方列表页第一个药名/电子病历详情
     *
     * @param recipedetail
     * @param drugType
     * @return
     */
    public static String dealWithRecipeDrugName(Recipedetail recipedetail, Integer drugType, Integer organId) {
        LOGGER.info("DrugManager dealwithRecipeDrugName recipedetail:{},drugType:{},organId:{}", JSONUtils.toString(recipedetail), drugType, organId);
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            StringBuilder stringBuilder = new StringBuilder();
            //所有页面中药药品显示统一“药品名称”和“剂量单位”以空格间隔
            stringBuilder.append(recipedetail.getDrugName());
            if (StringUtils.isNotEmpty(recipedetail.getMemo())) {
                stringBuilder.append("(").append(recipedetail.getMemo()).append(")").append(StringUtils.SPACE);
            }
            if (StringUtils.isNotEmpty(recipedetail.getUseDoseStr())) {
                stringBuilder.append(recipedetail.getUseDoseStr());
            } else {
                stringBuilder.append(recipedetail.getUseDose());
            }
            stringBuilder.append(recipedetail.getUseDoseUnit());
            return stringBuilder.toString();
        }
        if (StringUtils.isEmpty(recipedetail.getDrugDisplaySplicedName())) {
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(organId, recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
            LOGGER.info("DrugClient dealwithRecipeDrugName organDrugLists:{}", JSONUtils.toString(organDrugLists));
            return dealWithRecipeDetailName(organDrugLists, recipedetail);
        }
        LOGGER.info("DrugManager dealwithRecipeDrugName res:{}", recipedetail.getDrugDisplaySplicedName());
        return recipedetail.getDrugDisplaySplicedName();
    }

    public static String dealWithRecipeDetailName(List<OrganDrugList> organDrugLists, Recipedetail recipedetail) {
        LOGGER.info("DrugClient dealwithRecipedetailName organDrugLists:{},recipedetail:{}", JSONUtils.toString(organDrugLists), JSONUtils.toString(recipedetail));
        StringBuilder stringBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(organDrugLists)) {
            //机构药品名称、剂型、药品规格、单位
            stringBuilder.append(organDrugLists.get(0).getDrugName());
            if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                stringBuilder.append(organDrugLists.get(0).getDrugForm());
            }
        } else {
            LOGGER.info("DrugClient res:{}", stringBuilder.toString());
            stringBuilder.append(recipedetail.getDrugName());
        }
        //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
        stringBuilder.append(StringUtils.SPACE);
        stringBuilder.append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit());
        LOGGER.info("DrugClient res:{}", stringBuilder.toString());
        return stringBuilder.toString();
    }

    /**
     * 获取制法code 为key的Map
     *
     * @param organId 机构id
     * @return code = key对象
     */
    public Map<String, DrugMakingMethod> drugMakingMethodCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugMakingMethod> drugMakingMethodList = drugMakingMethodDao.findByOrganId(organId);
        logger.info("DrugClient drugMakingMethodList organId = {}, drugMakingMethodList:{}", organId, JSON.toJSONString(drugMakingMethodList));
        return Optional.ofNullable(drugMakingMethodList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugMakingMethod::getMethodCode, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取煎法code 为key的Map
     *
     * @param organId 机构id
     * @return code = key对象
     */
    public Map<String, DecoctionWay> decoctionWayCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DecoctionWay> decoctionWayList = drugDecoctionWayDao.findByOrganId(organId);
        logger.info("DrugClient decoctionWayCodeMap organId = {} ,decoctionWayList:{}", organId, JSON.toJSONString(decoctionWayList));
        return Optional.ofNullable(decoctionWayList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DecoctionWay::getDecoctionCode, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取嘱托（特殊煎法）code 为key的Map
     *
     * @param organId 机构id
     * @return 机构name = key对象
     */
    public Map<String, DrugEntrust> drugEntrustNameMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        if (CollectionUtils.isEmpty(drugEntrusts)) {
            drugEntrusts = drugEntrustDAO.findByOrganId(0);
        }
        logger.info("DrugClient drugEntrustNameMap organId = {} ,drugEntrusts={}", organId, JSON.toJSONString(drugEntrusts));
        return Optional.ofNullable(drugEntrusts).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugEntrust::getDrugEntrustName, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构 药物使用频率
     *
     * @param organId        机构id
     * @param organUsingRate 机构药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String organUsingRate) {
        return drugClient.usingRate(organId, organUsingRate);
    }

    /**
     * 获取机构 药物使用途径
     *
     * @param organId          机构id
     * @param organUsePathways 机构药物使用途径代码
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String organUsePathways) {
        return drugClient.usePathways(organId, organUsePathways);
    }

    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 id = key对象
     */
    public Map<Integer, UsingRate> usingRateMap(Integer organId) {
        return drugClient.usingRateMap(organId);
    }


    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 机构code = key对象
     */
    public Map<String, UsingRate> usingRateMapCode(Integer organId) {
        return drugClient.usingRateMapCode(organId);
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 id = key对象
     */
    public Map<Integer, UsePathways> usePathwaysMap(Integer organId) {
        return drugClient.usePathwaysMap(organId);
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 机构code = key对象
     */
    public Map<String, UsePathways> usePathwaysCodeMap(Integer organId) {
        return drugClient.usePathwaysCodeMap(organId);
    }

    /**
     * 患者端搜索药品
     *
     * @param saleName 搜索关键字
     * @param organId  机构id
     * @param drugType 类型
     * @param start    起始
     * @param limit    条数
     * @return
     */
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(String saleName, String organId, List<String> drugType, int start, int limit) {
        logger.info("DrugManager findDrugWithEsByPatient saleName : {} organId:{} drugType:{} start:{}  limit:{}", saleName, organId, JSON.toJSONString(drugType), start, limit);
        // 搜索药品信息
        List<PatientDrugWithEsDTO> drugWithEsByPatient = drugClient.findDrugWithEsByPatient(saleName, organId, drugType, start, limit);
        if (CollectionUtils.isEmpty(drugWithEsByPatient)) {
            return null;
        }
        // 拼接 药品图片
        Set<Integer> drugIds = drugWithEsByPatient.stream().map(PatientDrugWithEsDTO::getDrugId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(drugIds)) {
            List<DrugList> byDrugIds = drugListDAO.findByDrugIds(drugIds);
            if(CollectionUtils.isNotEmpty(byDrugIds)){
                Map<Integer, List<DrugList>> collect = byDrugIds.stream().collect(Collectors.groupingBy(DrugList::getDrugId));
                drugWithEsByPatient.forEach(patientDrugWithEsDTO -> {
                    patientDrugWithEsDTO.setDrugPic(collect.get(patientDrugWithEsDTO.getDrugId()).get(0).getDrugPic());
                });
            }

        }
        logger.info("DrugManager findDrugWithEsByPatient res drugWithEsByPatient:{}", JSON.toJSONString(drugWithEsByPatient));
        return drugWithEsByPatient;
    }
}
