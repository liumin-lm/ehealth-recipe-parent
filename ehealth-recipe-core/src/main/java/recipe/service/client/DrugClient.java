package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.recipe.entity.DecoctionWay;
import com.ngari.recipe.entity.DrugMakingMethod;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.service.IDrugEntrustService;
import eh.entity.base.UsePathways;
import eh.entity.base.UsingRate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.dao.DrugDecoctionWayDao;
import recipe.dao.DrugMakingMethodDao;

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
    private DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    private DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private IDrugEntrustService drugEntrustService;

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

    /**
     * 获取嘱托（特殊煎法）code 为key的Map
     *
     * @param organId 机构id
     * @return 机构name = key对象
     */
    public Map<String, DrugEntrustDTO> drugEntrustNameMap(Integer organId) {
        if (null == organId) {
            return new HashMap<>();
        }
        List<DrugEntrustDTO> drugEntrusts = drugEntrustService.querDrugEntrustByOrganId(organId);
        logger.info("DrugClient drugEntrustNameMap organId = {} ,drugEntrusts={}", organId, JSON.toJSONString(drugEntrusts));
        return Optional.ofNullable(drugEntrusts).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(DrugEntrustDTO::getDrugEntrustName, a -> a, (k1, k2) -> k1));
    }


}
