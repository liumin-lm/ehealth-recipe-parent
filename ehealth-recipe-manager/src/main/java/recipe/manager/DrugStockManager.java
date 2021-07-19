package recipe.manager;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.platform.recipe.mode.RecipeResultBean;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.IConfigurationClient;
import recipe.dao.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @description： 库存检查
 * @author： whf
 * @date： 2021-07-19 9:48
 */
@Service
public class DrugStockManager extends BaseManager {

    @Resource
    private RecipeDAO recipeDAO;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Resource
    private IConfigurationClient configurationClient;

    @Resource
    private IHisConfigService iHisConfigService;

    @Resource
    private DrugStockClient drugStockClient;

    @Resource
    private OrganDrugListDAO drugDao;

    @Resource
    private PharmacyTcmDAO pharmacyTcmDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;


    /**
     * 校验医院库存
     *
     * @param recipeId
     * @return
     */
    public RecipeResultBean scanDrugStockByRecipeId(Integer recipeId) {
        logger.info("scanHisDrugStockByRecipeId req recipeId={}", recipeId);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (Integer.valueOf(1).equals(recipe.getTakeMedicine())) {
            //外带药处方则不进行校验
            return RecipeResultBean.getSuccess();
        }
        return scanDrugStock(recipe, recipeDetailDAO.findByRecipeId(recipeId));
    }


    /**
     * 医院库存查询
     *
     * @param recipe
     * @param detailList
     * @return
     */
    public RecipeResultBean scanDrugStock(Recipe recipe, List<Recipedetail> detailList) {
        logger.info("scanDrugStock 入参 recipe={},recipedetail={}", JSONObject.toJSONString(recipe), JSONObject.toJSONString(detailList));
        RecipeResultBean result = RecipeResultBean.getSuccess();

        if (Objects.isNull(recipe)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("没有该处方");
            return result;
        }

        // 判断是否需要对接HIS
        if (configurationClient.skipHis(recipe)) {
            return result;
        }

        if (CollectionUtils.isEmpty(detailList)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("处方没有详情");
            return result;
        }

        // 判断his 是否存在
        if (iHisConfigService.isHisEnable(recipe.getClinicOrgan())) {
            List<Integer> emptyOrganCode = new ArrayList<>();
            for (Recipedetail detail : detailList) {
                if (StringUtils.isEmpty(detail.getOrganDrugCode())) {
                    emptyOrganCode.add(detail.getDrugId());
                }
            }
            if (CollectionUtils.isNotEmpty(emptyOrganCode)) {
                logger.error("scanDrugStock 医院配置药品存在编号为空的数据. drugIdList={}", JSONUtils.toString(emptyOrganCode));
                result.setCode(RecipeResultBean.FAIL);
                result.setError("医院配置药品存在编号为空的数据");
                return result;
            }

            DrugInfoRequestTO request = scanDrugStock(detailList, recipe.getClinicOrgan());
            // 请求his
            DrugInfoResponseTO response = drugStockClient.scanDrugStock(request);
            if (null == response) {
                //his未配置该服务则还是可以通过
                result.setError("HIS返回为NULL");
            } else {
                if (!Integer.valueOf(0).equals(response.getMsgCode())) {
                    String organCodeStr = response.getMsg();
                    List<String> nameList = new ArrayList<>();
                    if (StringUtils.isNotEmpty(organCodeStr)) {
                        List<String> organCodes = Arrays.asList(organCodeStr.split(","));
                        nameList = organDrugListDAO.findNameByOrganIdAndDrugCodes(recipe.getClinicOrgan(), organCodes);
                    }
                    String showMsg = "由于" + Joiner.on(",").join(nameList) + "门诊药房库存不足，该处方仅支持配送，无法到院取药，是否继续？";
                    result.setCode(RecipeResultBean.FAIL);
                    result.setError(showMsg);
                    result.setExtendValue("1");
                    result.setObject(nameList);
                    logger.error("scanDrugStock 存在无库存药品. response={} ", JSONUtils.toString(response));
                }
            }
        } else {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("医院HIS未启用。");
            logger.error("scanDrugStock 医院HIS未启用[organId:" + recipe.getClinicOrgan() + ",recipeId:" + recipe.getRecipeId() + "]");
        }
        logger.info("scanDrugStock 结果={}", JSONObject.toJSONString(result));
        return result;
    }


    /**
     * 拼装 his 库存请求参数
     *
     * @param detailList
     * @param organId
     * @return
     */
    private DrugInfoRequestTO scanDrugStock(List<Recipedetail> detailList, int organId) {
        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        List<Integer> drugIdList = detailList.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());

        List<OrganDrugList> organDrugList = drugDao.findByOrganIdAndDrugIds(organId, drugIdList);
        Map<String, List<OrganDrugList>> drugIdAndProduce =
                organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));

        List<DrugInfoTO> data = new ArrayList<>(detailList.size());
        detailList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(a.getPack().toString());
            drugInfo.setPackUnit(a.getDrugUnit());
            drugInfo.setUseTotalDose(a.getUseTotalDose());
            List<OrganDrugList> organDrugs = drugIdAndProduce.get(a.getOrganDrugCode());
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                Map<Integer, String> producerCodeMap = organDrugs.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, OrganDrugList::getProducerCode));
                String producerCode = producerCodeMap.get(a.getDrugId());
                if (StringUtils.isNotEmpty(producerCode)) {
                    drugInfo.setManfcode(producerCode);
                }
            }
            //药房
            if (a.getPharmacyId() != null) {
                PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(a.getPharmacyId());
                if (pharmacyTcm != null) {
                    drugInfo.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                    drugInfo.setPharmacy(pharmacyTcm.getPharmacyName());
                }
            }
            data.add(drugInfo);
        });
        request.setData(data);

        return request;
    }


}
