package recipe.service;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.service.IDrugEntrustService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.drugTool.validate.RecipeDetailValidateTool;
import recipe.service.client.IConfigurationClient;
import recipe.util.MapValueUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处方明细
 *
 * @author fuzi
 */
@Service
public class RecipeDetailService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeDetailValidateTool recipeDetailValidateTool;
    @Autowired
    private IDrugEntrustService drugEntrustService;

    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param organId       机构id
     * @param recipeDetails 处方明细
     * @return
     */
    public List<RecipeDetailBean> validateDrug(Integer organId, Integer recipeType, List<RecipeDetailBean> recipeDetails) {
        //处方药物使用天数时间
        String[] recipeDay = configurationClient.recipeDay(organId, recipeType);
        //药房信息
        List<PharmacyTcm> pharmacyList = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("RecipeDetailService validateDrug pharmacyList= {}", JSON.toJSONString(pharmacyList));
        Map<String, PharmacyTcm> pharmacyCodeMap = Optional.ofNullable(pharmacyList).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyCode, a -> a, (k1, k2) -> k1));
        //查询机构药品
        List<String> organDrugCodeList = recipeDetails.stream().map(RecipeDetailBean::getOrganDrugCode).distinct().collect(Collectors.toList());
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(organId, organDrugCodeList);
        logger.info("RecipeDetailService validateDrug organDrugList= {}", JSON.toJSONString(organDrugList));
        Map<String, List<OrganDrugList>> organDrugGroup = organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
        //药品名拼接配置
        Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, recipeType));
        //获取嘱托
        List<DrugEntrustDTO> drugEntrusts = drugEntrustService.querDrugEntrustByOrganId(organId);
        //校验数据判断状态
        recipeDetails.forEach(a -> {
            //校验机构药品
            OrganDrugList organDrug = recipeDetailValidateTool.validateOrganDrug(a, organDrugGroup);
            if (null == organDrug || RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE.equals(a.getValidateStatus())) {
                return;
            }
            //校验药品药房是否变动
            if (recipeDetailValidateTool.pharmacyVariation(a.getPharmacyId(), a.getPharmacyCode(), organDrug.getPharmacy(), pharmacyCodeMap)) {
                a.setValidateStatus(RecipeDetailValidateTool.VALIDATE_STATUS_FAILURE);
                logger.info("RecipeDetailService validateDrug pharmacy OrganDrugCode ：= {}", a.getOrganDrugCode());
                return;
            }
            //校验数据是否完善
            recipeDetailValidateTool.validateDrug(a, recipeDay, organDrug, recipeType, drugEntrusts);
            /**返回前端必须字段*/
            a.setStatus(organDrug.getStatus());
            a.setDrugId(organDrug.getDrugId());
            if (CollectionUtils.isEmpty(a.getUseDoseAndUnitRelation())) {
                List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = new LinkedList<>();
                if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
                }
                if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
                }
                a.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
            }
            //续方也会走这里但是 续方要用药品名实时配置
            a.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(a, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
        });
        return recipeDetails;
    }
}
