package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.DecoctionWay;
import com.ngari.recipe.entity.DrugMakingMethod;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import es.api.DrugSearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 药品数据 交互处理类
 *
 * @author fuzi
 */
@Service
public class DrugClient extends BaseClient {
    @Autowired
    private IUsingRateService usingRateService;
    @Autowired
    private IUsePathwaysService usePathwaysService;
    @Autowired
    private DrugSearchService drugSearchService;


    /**
     * 患者端搜索药品
     *
     * @param saleName 搜索关键字
     * @param organId  机构id
     * @param drugType 药品类型
     * @param start    起始条数
     * @param limit    条数
     * @return
     */
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(String saleName, String organId, List<String> drugType, int start, int limit) {
        logger.info("DrugClient findDrugWithEsByPatient saleName : {} organId:{} drugType:{} start:{}  limit:{}", saleName, organId, JSON.toJSONString(drugType), start, limit);
        if (Objects.isNull(organId)) {
            return null;
        }
        List<String> drugStrings = drugSearchService.searchOrganDrugForPatient(saleName, organId, drugType, start, limit);
        logger.info("findDrugWithEsByPatient drugStrings={}", JSONArray.toJSONString(drugStrings));
        if (CollectionUtils.isEmpty(drugStrings)) {
            return null;
        }
        List<PatientDrugWithEsDTO> patientDrugWithEsDTOS = drugStrings.stream().map(drugString -> {
            PatientDrugWithEsDTO patientDrugWithEsDTO = JSONArray.parseObject(drugString, PatientDrugWithEsDTO.class);
            return patientDrugWithEsDTO;
        }).collect(Collectors.toList());

        return patientDrugWithEsDTOS;
    }

    /**
     * 获取机构 药物使用频率
     *
     * @param organId        机构id
     * @param organUsingRate 机构药物使用频率代码
     * @return
     */
    public UsingRateDTO usingRate(Integer organId, String organUsingRate) {
        if (null == organId || StringUtils.isEmpty(organUsingRate)) {
            return null;
        }
        try {
            UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(organId, organUsingRate);
            if (null == usingRateDTO) {
                return null;
            }
            return usingRateDTO;
        } catch (Exception e) {
            logger.warn("DrugClient usingRate usingRateDTO error", e);
            return null;
        }
    }

    /**
     * 获取机构 药物使用途径
     *
     * @param organId          机构id
     * @param organUsePathways 机构药物使用途径代码
     * @return
     */
    public UsePathwaysDTO usePathways(Integer organId, String organUsePathways) {
        if (null == organId || StringUtils.isEmpty(organUsePathways)) {
            return null;
        }
        try {
            UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(organId, organUsePathways);
            if (null == usePathwaysDTO) {
                return null;
            }
            return usePathwaysDTO;
        } catch (Exception e) {
            logger.warn("DrugClient usePathways usePathwaysDTO error", e);
            return null;
        }
    }

    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 id = key对象
     */
    public Map<Integer, UsingRate> usingRateMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>(1);
        }
        List<UsingRate> usingRates = usingRateService.findAllusingRateByOrganId(organId);
        logger.info("DrugClient usingRateMap organId = {} usingRates:{}", organId, JSON.toJSONString(usingRates));
        return Optional.ofNullable(usingRates).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsingRate::getId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药频率
     *
     * @param organId 机构id
     * @return 用药频率 机构code = key对象
     */
    public Map<String, UsingRate> usingRateMapCode(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<UsingRate> usingRates = usingRateService.findAllusingRateByOrganId(organId);
        logger.info("DrugClient usingRateMapCode organId = {} usingRates:{}", organId, JSON.toJSONString(usingRates));
        return Optional.ofNullable(usingRates).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsingRate::getUsingRateKey, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 id = key对象
     */
    public Map<Integer, UsePathways> usePathwaysMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>(1);
        }
        List<UsePathways> usePathways = usePathwaysService.findAllUsePathwaysByOrganId(organId);
        logger.info("DrugClient usePathwaysMap organId = {} usePathways:{}", organId, JSON.toJSONString(usePathways));
        return Optional.ofNullable(usePathways).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsePathways::getId, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取机构的用药途径
     *
     * @param organId 机构id
     * @return 用药途径 机构code = key对象
     */
    public Map<String, UsePathways> usePathwaysCodeMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<UsePathways> usePathways = usePathwaysService.findAllUsePathwaysByOrganId(organId);
        logger.info("DrugClient usePathwaysCodeMap organId = {} usePathways:{}", organId, JSON.toJSONString(usePathways));
        return Optional.ofNullable(usePathways).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(UsePathways::getPathwaysKey, a -> a, (k1, k2) -> k1));
    }


    /**
     * 获取煎法 codeMap对象
     *
     * @param decoctionCode key
     * @param codeMap       map字典
     * @return
     */
    public static DecoctionWay validateDecoction(String decoctionCode, Map<String, DecoctionWay> codeMap) {
        DecoctionWay decoctionWay = new DecoctionWay();
        if (StringUtils.isEmpty(decoctionCode)) {
            return decoctionWay;
        }
        if (null == codeMap) {
            return decoctionWay;
        }
        DecoctionWay code = codeMap.get(decoctionCode);
        if (null == code) {
            return decoctionWay;
        }
        return code;
    }

    /**
     * 获取制法 codeMap对象
     *
     * @param makeMethod key
     * @param codeMap    map字典
     * @return
     */
    public static DrugMakingMethod validateMakeMethod(String makeMethod, Map<String, DrugMakingMethod> codeMap) {
        DrugMakingMethod drugMakingMethod = new DrugMakingMethod();
        if (StringUtils.isEmpty(makeMethod)) {
            return drugMakingMethod;
        }
        if (null == codeMap) {
            return drugMakingMethod;
        }
        DrugMakingMethod code = codeMap.get(makeMethod);
        if (null == code) {
            return drugMakingMethod;
        }
        return code;
    }


}
