package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.DoSignRecipeDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.OperationClient;
import recipe.constant.ErrorCode;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrganDrugListManager;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.ListValueUtil;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.EnterpriseStockVO;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
@Service
public class DrugEnterpriseBusinessService extends BaseService implements IDrugEnterpriseBusinessService {
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private OperationClient operationClient;
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;


    @Override
    public List<DrugEnterpriseStockVO> stockList(Integer organId, List<Recipedetail> recipeDetails) {
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(organId, recipeDetails);
        //药企库存
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheckAll(organId, recipeDetails);
        //处理库存数据结构 逆转为 药品-药企
        List<EnterpriseStockVO> enterpriseStockList = this.getEnterpriseStockVO(organStock, enterpriseStock);
        Map<Integer, List<EnterpriseStockVO>> enterpriseStockGroup = enterpriseStockList.stream().collect(Collectors.groupingBy(EnterpriseStockVO::getDrugId));
        //处方签于例外支付
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        String supportDownloadButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
        String supportMedicalPaymentButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
        //组织 药品 对应的 药企列表库存
        List<DrugEnterpriseStockVO> drugEnterpriseStockList = new LinkedList<>();
        recipeDetails.forEach(a -> {
            DrugEnterpriseStockVO drugEnterpriseStock = new DrugEnterpriseStockVO();
            drugEnterpriseStock.setDrugId(a.getDrugId());
            //默认无库存
            boolean stock = false;
            if (StringUtils.isNotEmpty(supportDownloadButton) || StringUtils.isNotEmpty(supportMedicalPaymentButton)) {
                stock = true;
            }
            List<EnterpriseStockVO> list = enterpriseStockGroup.get(a.getDrugId());
            if (CollectionUtils.isNotEmpty(list)) {
                drugEnterpriseStock.setEnterpriseStockList(list);
                boolean stockEnterprise = list.stream().anyMatch(EnterpriseStockVO::getStock);
                if (stock || stockEnterprise) {
                    stock = true;
                }
            }
            drugEnterpriseStock.setAllStock(stock);
            drugEnterpriseStockList.add(drugEnterpriseStock);
        });
        return drugEnterpriseStockList;
    }

    @Override
    public List<EnterpriseStock> stockList(Recipe recipe, List<Recipedetail> recipeDetails) {
        //药企库存
        List<EnterpriseStock> result = this.enterpriseStockCheckAll(recipe, recipeDetails);
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        if (null != organStock) {
            result.add(organStock);
        }
        return result;
    }

    @Override
    public Map<String, Object> enterpriseStock(Integer recipeId) {
        Recipe recipe = recipeDAO.get(recipeId);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        //药企库存
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheckAll(recipe, recipeDetails);
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);

        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeId, null);
        //机构配置购药方式
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(giveModeButtonBeans)) {
            enterpriseManager.doSignRecipe(doSignRecipe, null, "抱歉，机构未配置购药方式，无法开处方");
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        //未配置药企 医院无库存
        if (CollectionUtils.isEmpty(enterpriseStock) && null != organStock && !organStock.getStock()) {
            enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品门诊药房库存不足，请更换其他药品后再试");
        }
        //未配置医院 药企无库存
        if (CollectionUtils.isNotEmpty(enterpriseStock) && null == organStock) {
            boolean stock = enterpriseStock.stream().anyMatch(EnterpriseStock::getStock);
            if (!stock) {
                List<List<String>> groupList = new LinkedList<>();
                enterpriseStock.forEach(a -> groupList.add(a.getDrugName()));
                List<String> enterpriseDrugName = ListValueUtil.minIntersection(groupList);
                enterpriseManager.doSignRecipe(doSignRecipe, enterpriseDrugName, "药品库存不足，请更换其他药品后再试");
            }
        }
        //校验医院和药企
        if (CollectionUtils.isNotEmpty(enterpriseStock) && null != organStock) {
            boolean stockEnterprise = enterpriseStock.stream().anyMatch(EnterpriseStock::getStock);
            //医院有库存 药企无库存
            if (!stockEnterprise && organStock.getStock()) {
                List<List<String>> groupList = new LinkedList<>();
                enterpriseStock.forEach(a -> groupList.add(a.getDrugName()));
                List<String> enterpriseDrugName = ListValueUtil.minIntersection(groupList);
                doSignRecipe.setCanContinueFlag("2");
                enterpriseManager.doSignRecipe(doSignRecipe, enterpriseDrugName, "药品配送药企库存不足，该处方仅支持到院取药，无法药企配送，是否继续？");
            }
            //医院无库存 药企有库存
            if (stockEnterprise && !organStock.getStock()) {
                doSignRecipe.setCanContinueFlag("1");
                enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品医院库存不足，该处方仅支持药企配送，无法到院取药，是否继续？");
            }
            //医院无库存 药企无库存
            if (!stockEnterprise && !organStock.getStock()) {
                enterpriseManager.doSignRecipe(doSignRecipe, organStock.getDrugName(), "药品库存不足，请更换其他药品后再试");
            }
        }
        //保存药品购药方式
        saveGiveMode(recipe, organStock, enterpriseStock);
        return MapValueUtil.beanToMap(doSignRecipe);
    }

    @Override
    public List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails, Integer enterpriseId) {
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getOrganGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        if (!ValidateUtil.integerIsEmpty(enterpriseId)) {
            enterpriseStockList = enterpriseStockList.stream().filter(a -> a.getDrugsEnterpriseId().equals(enterpriseId)).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        return this.enterpriseStockCheck(organId, recipeDetails, enterpriseStockList);
    }

    /**
     * 校验 药品库存 全部药企的库存数量
     *
     * @param organId       机构id
     * @param recipeDetails 药品信息 drugId，code
     * @return 药品信息 一定存在于出参
     */
    private List<EnterpriseStock> enterpriseStockCheckAll(Integer organId, List<Recipedetail> recipeDetails) {
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getOrganGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        return this.enterpriseStockCheck(organId, recipeDetails, enterpriseStockList);
    }


    /**
     * 校验 药品库存 指定药企的库存数量
     *
     * @param organId             机构id
     * @param recipeDetails       药品信息 drugId，code
     * @param enterpriseStockList 指定某药企List
     * @return 药品信息 一定存在于出参
     */
    private List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails, List<EnterpriseStock> enterpriseStockList) {
        //校验药企库存
        List<FutureTask<EnterpriseStock>> futureTasks = new LinkedList<>();
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            enterpriseStock.setStock(false);
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
                continue;
            }
            //根据药企配置查询 库存
            Recipe recipe = new Recipe();
            recipe.setClinicOrgan(organId);
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStock(enterpriseStock, recipe, recipeDetails));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks);
    }

    /**
     * 校验 药品库存在 遍历药企 同一个药企下的库存数量
     *
     * @param recipe        处方机构id
     * @param recipeDetails 药品信息 drugId，code
     * @return 药品信息非必须
     */
    private List<EnterpriseStock> enterpriseStockCheckAll(Recipe recipe, List<Recipedetail> recipeDetails) {
        Integer organId = recipe.getClinicOrgan();
        //获取机构配置按钮
        List<GiveModeButtonDTO> giveModeButtonBeans = buttonManager.getOrganGiveModeMap(organId);
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockList(organId, giveModeButtonBeans);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        //每个药企对应的 不满足的药品列表
        List<Integer> enterpriseIds = enterpriseStockList.stream().map(EnterpriseStock::getDrugsEnterpriseId).collect(Collectors.toList());
        Map<Integer, List<String>> enterpriseDrugNameGroup = enterpriseManager.checkEnterpriseDrugName(enterpriseIds, recipeDetails);
        //校验药企库存
        List<FutureTask<EnterpriseStock>> futureTasks = new LinkedList<>();
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            enterpriseStock.setStock(false);
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                continue;
            }
            //验证能否药品配送以及能否开具到一张处方单上
            List<String> drugNames = enterpriseDrugNameGroup.get(enterpriseStock.getDrugsEnterpriseId());
            if (CollectionUtils.isNotEmpty(drugNames)) {
                enterpriseStock.setDrugName(drugNames);
                continue;
            }
            //根据药企配置查询 库存
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStock(enterpriseStock, recipe, recipeDetails));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks);
    }

    /**
     * 根据药企配置查询 库存
     *
     * @param enterpriseStock 药企购药配置-库存对象
     * @param recipe          处方信息
     * @param recipeDetails   处方明细
     */
    private EnterpriseStock enterpriseStock(EnterpriseStock enterpriseStock, Recipe recipe, List<Recipedetail> recipeDetails) {
        DrugsEnterprise drugsEnterprise = enterpriseStock.getDrugsEnterprise();
        Integer checkInventoryFlag = drugsEnterprise.getCheckInventoryFlag();
        if (null == checkInventoryFlag) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, drugsEnterprise.getName() + "checkInventoryFlag is null");
        }
        if (0 == drugsEnterprise.getCheckInventoryFlag()) {
            enterpriseStock.setStock(true);
            enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, true));
            logger.info("DrugEnterpriseBusinessService enterpriseStock 0 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return enterpriseStock;
        }
        //医院自建药企-查询医院库存
        if (3 == drugsEnterprise.getCheckInventoryFlag()) {
            DrugStockAmountDTO organStock = organDrugListManager.scanDrugStockByRecipeId(recipe, recipeDetails);
            enterpriseStock.setStock(organStock.isResult());
            enterpriseStock.setDrugName(organStock.getNotDrugNames());
            enterpriseStock.setDrugInfoList(organStock.getDrugInfoList());
            logger.info("DrugEnterpriseBusinessService enterpriseStock 3 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return enterpriseStock;
        }
        //通过前置机调用
        if (1 == drugsEnterprise.getOperationType()) {
            DrugStockAmountDTO code = enterpriseManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
            enterpriseStock.setStock(code.isResult());
            enterpriseStock.setDrugInfoList(code.getDrugInfoList());
            logger.info("DrugEnterpriseBusinessService enterpriseStock 1 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return enterpriseStock;
        }
        //通过平台调用药企
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        DrugStockAmountDTO result = drugEnterpriseService.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
        enterpriseStock.setStock(result.isResult());
        enterpriseStock.setDrugInfoList(result.getDrugInfoList());
        logger.info("DrugEnterpriseBusinessService enterpriseStock else enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
        return enterpriseStock;
    }


    /***
     * 组装药企药品一对一库存关系
     * @param organStock 医院库存
     * @param enterpriseStock 药企库存
     * @return
     */
    private List<EnterpriseStockVO> getEnterpriseStockVO(EnterpriseStock organStock, List<EnterpriseStock> enterpriseStock) {
        logger.info("DrugEnterpriseBusinessService getEnterpriseStockVO organStock = {} enterpriseStock={}", JSON.toJSONString(organStock), JSON.toJSONString(enterpriseStock));
        List<EnterpriseStockVO> enterpriseStockList = new LinkedList<>();
        enterpriseStock.forEach(a -> {
            if (CollectionUtils.isEmpty(a.getDrugInfoList())) {
                return;
            }
            a.getDrugInfoList().forEach(b -> {
                EnterpriseStockVO enterpriseStockVO = new EnterpriseStockVO();
                enterpriseStockVO.setAppointEnterpriseType(a.getAppointEnterpriseType());
                enterpriseStockVO.setDeliveryCode(a.getDeliveryCode());
                enterpriseStockVO.setDeliveryName(a.getDeliveryName());
                enterpriseStockVO.setStock(b.getStock());
                if (StringUtils.isNotEmpty(b.getStockAmountChin())) {
                    enterpriseStockVO.setStockAmountChin(b.getStockAmountChin());
                } else {
                    enterpriseStockVO.setStockAmountChin(String.valueOf(b.getStockAmount()));
                }
                enterpriseStockVO.setDrugId(b.getDrugId());
                enterpriseStockList.add(enterpriseStockVO);
            });
        });

        organStock.getDrugInfoList().forEach(a -> {
            EnterpriseStockVO organStockList = new EnterpriseStockVO();
            organStockList.setAppointEnterpriseType(organStock.getAppointEnterpriseType());
            organStockList.setDeliveryCode(organStock.getDeliveryCode());
            organStockList.setDeliveryName(organStock.getDeliveryName());
            organStockList.setDrugId(a.getDrugId());
            if (!a.getStock()) {
                organStockList.setStock(false);
            } else {
                organStockList.setStock(a.getStock());
                if (StringUtils.isNotEmpty(a.getStockAmountChin())) {
                    organStockList.setStockAmountChin(a.getStockAmountChin());
                } else {
                    organStockList.setStockAmountChin(String.valueOf(a.getStockAmount()));
                }
            }
            enterpriseStockList.add(organStockList);
        });
        logger.info("DrugEnterpriseBusinessService getEnterpriseStockVO enterpriseStockList={}", JSON.toJSONString(enterpriseStockList));
        return enterpriseStockList;
    }


    /**
     * * 异步保存处方购药方式
     *
     * @param recipe          处方信息
     * @param organStock      机构库存
     * @param enterpriseStock 药企库存
     */
    private void saveGiveMode(Recipe recipe, EnterpriseStock organStock, List<EnterpriseStock> enterpriseStock) {
        RecipeBusiThreadPool.execute(() -> {
            logger.info("DrugEnterpriseBusinessService saveGiveMode start recipe={}", JSON.toJSONString(recipe));
            List<GiveModeButtonDTO> giveModeButton = new LinkedList<>();
            if (null != organStock && organStock.getStock()) {
                giveModeButton.addAll(organStock.getGiveModeButton());
            }
            enterpriseStock.stream().filter(EnterpriseStock::getStock).forEach(a -> giveModeButton.addAll(a.getGiveModeButton()));
            List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(recipe.getClinicOrgan());
            String supportDownloadButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            if (StringUtils.isNotEmpty(supportDownloadButton)) {
                GiveModeButtonDTO supportDownload = new GiveModeButtonDTO();
                supportDownload.setType(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType());
                giveModeButton.add(supportDownload);
            }
            String supportMedicalPaymentButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
            if (StringUtils.isNotEmpty(supportMedicalPaymentButton)) {
                GiveModeButtonDTO supportMedicalPayment = new GiveModeButtonDTO();
                supportMedicalPayment.setType(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getType());
                giveModeButton.add(supportMedicalPayment);
            }
            if (CollectionUtils.isEmpty(giveModeButton)) {
                return;
            }
            Set<Integer> recipeGiveMode = giveModeButton.stream().map(GiveModeButtonDTO::getType).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
                String join = StringUtils.join(recipeGiveMode, ",");
                Recipe recipeUpdate = new Recipe();
                recipeUpdate.setRecipeId(recipe.getRecipeId());
                recipeUpdate.setRecipeSupportGiveMode(join);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
            }
            logger.info("DrugEnterpriseBusinessService saveGiveMode 异步保存处方购药方式 {},{}", recipe.getRecipeId(), JSON.toJSONString(recipeGiveMode));
        });
    }

}
