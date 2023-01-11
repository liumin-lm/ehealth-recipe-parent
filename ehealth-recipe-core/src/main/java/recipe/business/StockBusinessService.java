package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DrugStockClient;
import recipe.client.OperationClient;
import recipe.constant.ErrorCode;
import recipe.core.api.IStockBusinessService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.type.FastRecipeFlagEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.manager.EnterpriseManager;
import recipe.manager.RecipeManager;
import recipe.util.MapValueUtil;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * 库存处理实现类
 *
 * @author fuzi
 */
@Service
public class StockBusinessService extends BaseService implements IStockBusinessService {
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OperationClient operationClient;
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Override
    public List<DrugEnterpriseStockVO> stockList(DrugQueryVO drugQueryVO, List<Recipedetail> recipeDetails, Integer stockCheckType) {
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(drugQueryVO.getOrganId(), drugQueryVO.getEnterpriseId(), drugQueryVO.getAppointEnterpriseType(), recipeDetails);
        //药企库存
        List<EnterpriseStock> enterpriseList = buttonManager.enterpriseStockCheck(drugQueryVO.getOrganId(), drugQueryVO.getRecipeType(), drugQueryVO.getDecoctionId(), recipeDetails);
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheck(drugQueryVO.getOrganId(), enterpriseList, drugQueryVO.getEnterpriseId(), recipeDetails, stockCheckType);
        //处理库存数据结构 逆转为 药品-药企
        List<EnterpriseStockVO> enterpriseStockList = this.getEnterpriseStockVO(organStock, enterpriseStock);
        Map<Integer, List<EnterpriseStockVO>> enterpriseStockGroup = enterpriseStockList.stream().collect(Collectors.groupingBy(EnterpriseStockVO::getDrugId));
        //组织 药品 对应的 药企列表库存
        List<DrugEnterpriseStockVO> drugEnterpriseStockList = new LinkedList<>();
        recipeDetails.forEach(a -> {
            DrugEnterpriseStockVO drugEnterpriseStock = new DrugEnterpriseStockVO();
            drugEnterpriseStock.setDrugId(a.getDrugId());
            boolean stockEnterprise = false;
            List<EnterpriseStockVO> list = enterpriseStockGroup.get(a.getDrugId());
            if (CollectionUtils.isNotEmpty(list)) {
                drugEnterpriseStock.setEnterpriseStockList(list);
                stockEnterprise = list.stream().anyMatch(EnterpriseStockVO::getStock);
            }
            drugEnterpriseStock.setAllStock(stockEnterprise);
            drugEnterpriseStockList.add(drugEnterpriseStock);
        });
        return drugEnterpriseStockList;
    }

    @Override
    public List<EnterpriseStock> stockList(RecipeDTO recipeDTO) {
        return this.drugRecipeStockV1(recipeDTO, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
    }

    @Override
    public Map<String, Object> enterpriseStockMap(Integer recipeId, Integer stockCheckType) {
        RecipeDTO recipeDTO = recipeManager.getRecipe(recipeId);
        return MapValueUtil.beanToMap(this.enterpriseStock(recipeDTO, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType()));
    }

    @Override
    public DoSignRecipeDTO validateRecipeGiveMode(RecipeDTO recipe) {
        return this.enterpriseStock(recipe, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
    }

    @Override
    public EnterpriseStock enterpriseStockCheck(Recipe recipe, List<Recipedetail> recipeDetails, Integer enterpriseId, Integer stockCheckType) {
        String decoctionId = null;
        if (null != recipe.getRecipeId()) {
            RecipeExtend recipeExtend = recipeManager.recipeExtend(recipe.getRecipeId());
            decoctionId = recipeExtend.getDecoctionId();
        }
        List<EnterpriseStock> enterpriseStockList = this.enterpriseStockCheckAll(recipe, decoctionId, recipeDetails, enterpriseId, stockCheckType);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return null;
        }
        List<EnterpriseStock> list = enterpriseStockList.stream().filter(EnterpriseStock::getStock).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public EnterpriseStock enterpriseStockCheckV1(RecipeDTO recipeDTO, Integer id, Integer type, Integer stockCheckType) {
        Recipe recipe = recipeDTO.getRecipe();
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        //药企库存
        if (2 == type) {
            List<EnterpriseStock> enterpriseStockList = this.enterpriseStockCheckAll(recipe, recipeExtend.getDecoctionId(), recipeDetails, id, stockCheckType);
            if (CollectionUtils.isEmpty(enterpriseStockList)) {
                return null;
            }
            List<EnterpriseStock> list = enterpriseStockList.stream().filter(EnterpriseStock::getStock).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
            return list.get(0);
        }
        //医院库存
        return organDrugListManager.organStock(recipe, recipeDetails);
    }

    @Override
    public List<DrugForGiveModeListVO> drugForGiveModeV1(RecipeDTO recipeDTO, Integer stockCheckType) {
        Recipe recipe = recipeDTO.getRecipe();
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        //药企库存
        List<EnterpriseStock> enterpriseList = buttonManager.enterpriseStockCheck(recipe.getClinicOrgan(), recipe.getRecipeType(), recipeExtend.getDecoctionId(), recipeDetails);
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheck(recipe.getClinicOrgan(), enterpriseList, null, recipeDetails, stockCheckType);
        Map<Integer, EnterpriseStock> enterpriseStockMap = enterpriseStock.stream().collect(Collectors.toMap(EnterpriseStock::getDrugsEnterpriseId, a -> a, (k1, k2) -> k1));
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        if (null != organStock) {
            enterpriseList.add(organStock);
        }
        List<DrugForGiveModeListVO> giveModeList = new ArrayList<>();
        enterpriseList.forEach(a -> {
            EnterpriseStock stock = enterpriseStockMap.get(a.getDrugsEnterpriseId());
            if (null == stock) {
                stock = a;
            }
            List<GiveModeButtonDTO> giveModeButton = stock.getGiveModeButton();
            if (CollectionUtils.isEmpty(giveModeButton)) {
                return;
            }
            for (GiveModeButtonDTO button : giveModeButton) {
                DrugForGiveModeListVO giveMode = new DrugForGiveModeListVO();
                giveMode.setSupportKey(button.getShowButtonKey());
                EnterpriseStockVO enterpriseStockVO = ObjectCopyUtils.convert(stock, EnterpriseStockVO.class);
                List<DrugStockVO> drugStockList = ObjectCopyUtils.convert(stock.getDrugInfoList(), DrugStockVO.class);
                enterpriseStockVO.setDrugStockList(drugStockList);
                giveMode.setEnterpriseStock(enterpriseStockVO);
                giveModeList.add(giveMode);
            }
        });
        logger.info("StockBusinessService drugForGiveModeV1 giveModeList={}", JSONArray.toJSONString(giveModeList));
        return giveModeList;
    }

    @Override
    public List<EnterpriseStock> drugRecipeStock(RecipeDTO recipeDTO, Integer stockCheckType) {
        return this.drugRecipeStockV1(recipeDTO, stockCheckType);
    }


    @Override
    public Boolean getOrderStockFlag(List<Integer> recipeIds, Integer enterpriseId, String giveModeKey) {
        Recipe recipe = recipeDAO.get(recipeIds.get(0));
        if (Objects.isNull(recipe)) {
            throw new DAOException("处方不存在");
        }
        Integer giveMode = RecipeSupportGiveModeEnum.getGiveMode(giveModeKey);
        if (giveMode == 0) {
            return true;
        }
        Boolean fastRecipeUsePlatStock = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "fastRecipeUsePlatStock", false);
        if (FastRecipeFlagEnum.FAST_RECIPE_FLAG_QUICK.getType().equals(recipe.getFastRecipeFlag()) && fastRecipeUsePlatStock) {
            recipeIds.forEach(recipeId -> {
                if (!recipeManager.fastRecipeStock(recipeId)) {
                    throw new DAOException("药品已售罄");
                }
            });
            return true;
        }
        recipe.setGiveMode(giveMode);
        return this.getStockFlag(recipeIds, recipe, enterpriseId);
    }

    
    @Override
    public List<EnterpriseStock> drugsStock(RecipeDTO recipeDTO, Integer stockCheckType) {
        Recipe recipe = recipeDTO.getRecipe();
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        //药企库存
        List<EnterpriseStock> enterpriseList = buttonManager.enterpriseStockCheck(recipe.getClinicOrgan(), recipe.getRecipeType(), recipeExtend.getDecoctionId(), recipeDetails);
        List<EnterpriseStock> result = this.enterpriseStockCheck(recipe.getClinicOrgan(), enterpriseList, null, recipeDetails, stockCheckType);
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        if (null != organStock) {
            result.add(organStock);
        }
        logger.info("StockBusinessService drugStock result={}", JSON.toJSONString(result));
        return result;
    }


    /**
     * 保存购药按钮
     *
     * @param recipe
     */
    public void setSupportGiveMode(Recipe recipe) {
        logger.info("StockBusinessService setSupportGiveMode recipe={}", JSONArray.toJSONString(recipe));
        RecipeDTO recipeDTO = recipeManager.getRecipe(recipe.getRecipeId());
        List<EnterpriseStock> enterpriseStockList = this.drugRecipeStockV1(recipeDTO, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
        saveGiveMode(recipe, enterpriseStockList, recipeDTO.getRecipeDetails());
    }

    /**
     * 预结算库存校验
     *
     * @param recipeIds
     * @param recipe
     * @param enterpriseId
     * @return
     */
    public Boolean getStockFlag(List<Integer> recipeIds, Recipe recipe, Integer enterpriseId) {
        logger.info("StockBusinessService getStockFlag recipeIds={} recipe={}", JSONArray.toJSONString(recipeIds), JSONArray.toJSONString(recipe));
        if (CollectionUtils.isEmpty(recipeIds)) {
            return false;
        }
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeIdList(recipeIds);
        Boolean drugToHosByEnterprise = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "drugToHosByEnterprise", false);

        // 到院取药 开启药企配偶就校验药企库存
        if(drugToHosByEnterprise && GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType().equals(recipe.getGiveMode())){
            recipe.setGiveMode(GiveModeEnum.GIVE_MODE_PHARMACY_DRUG.getType());
        }
        Boolean stockFlag = false;
        switch (GiveModeEnum.getGiveModeEnum(recipe.getGiveMode())) {
            case GIVE_MODE_HOME_DELIVERY:
                // 配送到家
            case GIVE_MODE_PHARMACY_DRUG:
                // 药店取药
                // 根据药企查询库存
                EnterpriseStock enterpriseStock = this.enterpriseStockCheck(recipe, recipeDetails, enterpriseId, StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                logger.info("StockBusinessService getStockFlag enterpriseStock={}", JSONArray.toJSONString(enterpriseStock));
                if (Objects.nonNull(enterpriseStock)) {
                    stockFlag = enterpriseStock.getStock();
                }
                break;

            case GIVE_MODE_HOSPITAL_DRUG:
                // 到院取药
                // 医院库存
                EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
                logger.info("StockBusinessService getStockFlag organStock={}", JSONArray.toJSONString(organStock));
                if (Objects.nonNull(organStock)) {
                    stockFlag = organStock.getStock();
                }
                break;

            case GIVE_MODE_DOWNLOAD_RECIPE:
            default:
                // 下载处方笺或其他购药方式默认有库存
                stockFlag = true;
                break;
        }
        logger.info("StockBusinessService getStockFlag stockFlag={}", stockFlag);
        return stockFlag;
    }

    /**
     * 校验 药品库存 指定药企的库存数量
     *
     * @param organId        机构id
     * @param organId        药企或机构id
     * @param recipeDetails  药品信息 drugId，code
     * @param enterpriseList 药企信息
     * @return 药品信息 一定存在于出参
     */
    private List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<EnterpriseStock> enterpriseList, Integer id, List<Recipedetail> recipeDetails, Integer stockCheckType) {
        if (!ValidateUtil.integerIsEmpty(id)) {
            enterpriseList = enterpriseList.stream().filter(a -> a.getDrugsEnterpriseId().equals(id)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(enterpriseList)) {
                return enterpriseList;
            }
        }
        //校验药企库存
        List<FutureTask<EnterpriseStock>> futureTasks = new LinkedList<>();
        //根据药企配置查询 库存
        for (EnterpriseStock enterpriseStock : enterpriseList) {
            Recipe recipe = new Recipe();
            recipe.setClinicOrgan(organId);
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStockFutureTask(enterpriseStock, recipe, recipeDetails, null, stockCheckType));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks, null);
    }

    /**
     * 校验 药品库存在 遍历药企 同一个药企下的库存数量
     * 验证能否药品配送以及能否开具到一张处方单上
     *
     * @param recipe        处方机构id
     * @param recipeDetails 药品信息 drugId，code
     * @return 药品信息非必须
     */
    private List<EnterpriseStock> enterpriseStockCheckAll(Recipe recipe, String decoctionId, List<Recipedetail> recipeDetails, Integer enterpriseId, Integer stockCheckType) {
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockCheck(recipe.getClinicOrgan(), recipe.getRecipeType(), decoctionId,recipeDetails);
        if (CollectionUtils.isEmpty(enterpriseStockList)) {
            return enterpriseStockList;
        }
        if (!ValidateUtil.integerIsEmpty(enterpriseId)) {
            enterpriseStockList = enterpriseStockList.stream().filter(a -> a.getDrugsEnterpriseId().equals(enterpriseId)).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(enterpriseStockList)) {
                return enterpriseStockList;
            }
        }
        //每个药企对应的 不满足的药品列表
        List<Integer> enterpriseIds = enterpriseStockList.stream().map(EnterpriseStock::getDrugsEnterpriseId).collect(Collectors.toList());
        Map<Integer, List<Integer>> enterpriseDrugIdGroup = enterpriseManager.enterpriseDrugIdGroup(enterpriseIds, recipeDetails);
        //校验药企库存
        List<FutureTask<EnterpriseStock>> futureTasks = new LinkedList<>();
        //根据药企配置查询 库存
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStockFutureTask(enterpriseStock, recipe, recipeDetails, enterpriseDrugIdGroup, stockCheckType));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks, null);
    }

    /**
     * 验 药品库存 指定药企的库存数量
     *
     * @param enterpriseStock       药企
     * @param recipe                机构id
     * @param recipeDetails         药品数据
     * @param enterpriseDrugIdGroup 验证能否药品配送以及能否开具到一张处方单上
     * @return
     */
    private EnterpriseStock enterpriseStockFutureTask(EnterpriseStock enterpriseStock, Recipe recipe,
                                                      List<Recipedetail> recipeDetails, Map<Integer, List<Integer>> enterpriseDrugIdGroup, Integer stockCheckType) {
        enterpriseStock.setStock(false);
        try {
            //药企无对应的购药按钮则 无需查询库存-返回无库存
            if (CollectionUtils.isEmpty(enterpriseStock.getGiveModeButton())) {
                enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
                return enterpriseStock;
            }
            //验证能否药品配送以及能否开具到一张处方单上
            if (null != enterpriseDrugIdGroup) {
                List<Integer> drugIds = enterpriseDrugIdGroup.get(enterpriseStock.getDrugsEnterpriseId());
                if (CollectionUtils.isNotEmpty(drugIds)) {
                    List<String> drugNames = recipeDetails.stream().filter(a -> drugIds.contains(a.getDrugId())).map(Recipedetail::getDrugName).distinct().collect(Collectors.toList());
                    enterpriseStock.setDrugName(drugNames);
                    enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
                    return enterpriseStock;
                }
            }
            enterpriseStock(enterpriseStock, recipe, recipeDetails, stockCheckType);
            return enterpriseStock;
        } catch (Exception e) {
            logger.error("StockBusinessService enterpriseStockFutureTask  e", e);
            logger.error("StockBusinessService enterpriseStockFutureTask enterpriseStock = {},recipe = {}, recipeDetails ={} , enterpriseDrugIdGroup = {} , e"
                    , JSON.toJSONString(enterpriseStock), JSON.toJSONString(recipe), JSON.toJSONString(recipeDetails), JSON.toJSONString(enterpriseDrugIdGroup), e);
            enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
            return enterpriseStock;
        }
    }


    /**
     * 根据药企配置查询 库存
     *
     * @param enterpriseStock 药企购药配置-库存对象
     * @param recipe          处方信息
     * @param recipeDetails   处方明细
     * @param stockCheckType 库存校验类型 1 医生端校验 2 患者端校验 3 运营平台
     */
    private void enterpriseStock(EnterpriseStock enterpriseStock, Recipe recipe, List<Recipedetail> recipeDetails, Integer stockCheckType) {
        DrugsEnterprise drugsEnterprise = enterpriseStock.getDrugsEnterprise();
        Integer checkInventoryWay = drugsEnterprise.getCheckInventoryWay();
        if (null == checkInventoryWay) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, drugsEnterprise.getName() + "checkInventoryWay is null");
        }
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        Map<Integer, SaleDrugList> saleDrugMap = enterpriseManager.saleDrugListEffectivity(drugsEnterprise.getId(), drugIds);
        List<Recipedetail> recipeDetailList = recipeDetails.stream().filter(a -> null != saleDrugMap.get(a.getDrugId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            enterpriseStock.setStock(false);
            enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
            logger.info("DrugEnterpriseBusinessService enterpriseStock recipeDetailList is null");
            return;
        }
        boolean checkSkipStock = false;
        if (StockCheckSourceTypeEnum.DOCTOR_STOCK.getType().equals(drugsEnterprise.getCheckInventoryType()) && StockCheckSourceTypeEnum.PATIENT_STOCK.getType().equals(stockCheckType)) {
            checkSkipStock = true;
        }
        if (StockCheckSourceTypeEnum.PATIENT_STOCK.getType().equals(drugsEnterprise.getCheckInventoryType()) && StockCheckSourceTypeEnum.DOCTOR_STOCK.getType().equals(stockCheckType)) {
            checkSkipStock = true;
        }
        if (StockCheckSourceTypeEnum.GREENROOM_STOCK.getType().equals(stockCheckType)) {
            checkSkipStock = false;
        }
        if (0 == drugsEnterprise.getCheckInventoryType() || checkSkipStock) {
            //机构药企 药品列表对不上
            enterpriseStock.setStock(true);
            List<DrugInfoDTO> list = DrugStockClient.getDrugInfoDTO(recipeDetailList, true);
            this.notRecipeDetailList(enterpriseStock, list, recipeDetails, saleDrugMap);
            enterpriseStock.setDrugInfoList(list);
            logger.info("DrugEnterpriseBusinessService enterpriseStock 0 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //医院自建药企-查询医院库存
        if (2 == drugsEnterprise.getCheckInventoryWay()) {
            DrugStockAmountDTO result = organDrugListManager.scanDrugStockByRecipeId(recipe, recipeDetailList);
            enterpriseStock.setDrugName(result.getNotDrugNames());
            enterpriseStock.setStock(result.isResult());
            List<DrugInfoDTO> list = result.getDrugInfoList();
            this.notRecipeDetailList(enterpriseStock, list, recipeDetails, saleDrugMap);
            enterpriseStock.setDrugInfoList(list);
            logger.info("DrugEnterpriseBusinessService enterpriseStock 3 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //通过前置机调用
        if (1 == drugsEnterprise.getOperationType()) {
            DrugStockAmountDTO result = enterpriseManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetailList);
            enterpriseStock.setStock(result.isResult());
            List<DrugInfoDTO> list = result.getDrugInfoList();
            this.notRecipeDetailList(enterpriseStock, list, recipeDetails, saleDrugMap);
            enterpriseStock.setDrugInfoList(list);
            logger.info("DrugEnterpriseBusinessService enterpriseStock 1 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //通过平台调用药企
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        DrugStockAmountDTO result = drugEnterpriseService.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetailList);
        enterpriseStock.setStock(result.isResult());
        List<DrugInfoDTO> list = result.getDrugInfoList();
        this.notRecipeDetailList(enterpriseStock, list, recipeDetails, saleDrugMap);
        enterpriseStock.setDrugInfoList(list);
        logger.info("DrugEnterpriseBusinessService enterpriseStock else enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
    }

    /**
     * 药企中不存在的药品 设置为无库存
     *
     * @param enterpriseStock
     * @param list
     * @param recipeDetails
     * @param saleDrugMap
     */
    private void notRecipeDetailList(EnterpriseStock enterpriseStock, List<DrugInfoDTO> list,
                                     List<Recipedetail> recipeDetails, Map<Integer, SaleDrugList> saleDrugMap) {
        List<Recipedetail> notRecipeDetailList = recipeDetails.stream().filter(a -> null == saleDrugMap.get(a.getDrugId())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notRecipeDetailList)) {
            List<DrugInfoDTO> notList = DrugStockClient.getDrugInfoDTO(notRecipeDetailList, false);
            list.addAll(notList);
            enterpriseStock.setStock(false);
            logger.info("DrugEnterpriseBusinessService notRecipeDetailList = {}", JSON.toJSONString(notRecipeDetailList));
        }
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
        if (null != organStock) {
            organStock.setDeliveryName("医院库存");
            enterpriseStock.add(0, organStock);
        }
        enterpriseStock.forEach(a -> {
            if (CollectionUtils.isEmpty(a.getGiveModeButton())) {
                return;
            }
            if (CollectionUtils.isEmpty(a.getDrugInfoList())) {
                return;
            }
            a.getDrugInfoList().forEach(b -> {
                EnterpriseStockVO enterpriseStockVO = new EnterpriseStockVO();
                enterpriseStockVO.setAppointEnterpriseType(a.getAppointEnterpriseType());
                enterpriseStockVO.setDeliveryCode(a.getDeliveryCode());
                enterpriseStockVO.setDeliveryName(a.getDeliveryName());
                enterpriseStockVO.setDrugId(b.getDrugId());
                enterpriseStockVO.setStock(b.getStock());
                if (StringUtils.isNotEmpty(b.getStockAmountChin())) {
                    enterpriseStockVO.setStockAmountChin(b.getStockAmountChin());
                } else {
                    enterpriseStockVO.setStockAmountChin(String.valueOf(b.getStockAmount()));
                }
                enterpriseStockList.add(enterpriseStockVO);
            });
        });
        logger.info("DrugEnterpriseBusinessService getEnterpriseStockVO organStock={}", JSON.toJSONString(enterpriseStockList));
        return enterpriseStockList;
    }

    /**
     * 查询药品库存存在的够药方式
     *
     * @param recipeDTO
     * @return
     */
    private DoSignRecipeDTO enterpriseStock(RecipeDTO recipeDTO, Integer stockCheckType) {
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeDTO.getRecipe().getRecipeId(), null, null, null);
        List<EnterpriseStock> enterpriseStockList = this.drugRecipeStockV1(recipeDTO, stockCheckType);
        Recipe recipe = recipeDTO.getRecipe();
        //保存药品购药方式
        List<GiveModeButtonDTO> supportGiveModeList = this.saveGiveMode(recipe, enterpriseStockList, recipeDTO.getRecipeDetails());
        Set<Integer> recipeGiveMode = supportGiveModeList.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getType).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
            doSignRecipe.setRecipeSupportGiveMode(StringUtils.join(recipeGiveMode, ","));
        }
        //获取弹框文本
        this.getSupportGiveModeNameText(recipe.getClinicOrgan(), doSignRecipe, supportGiveModeList);
        return doSignRecipe;
    }

    /**
     * 查询药品库存 同一个药企下的库存数量
     *
     * @param recipeDTO
     * @return
     */
    private List<EnterpriseStock> drugRecipeStockV1(RecipeDTO recipeDTO, Integer stockCheckType) {
        Recipe recipe = recipeDTO.getRecipe();
        List<Recipedetail> recipeDetails = recipeDTO.getRecipeDetails();
        RecipeExtend recipeExtend = recipeDTO.getRecipeExtend();
        //药企库存
        List<EnterpriseStock> result = this.enterpriseStockCheckAll(recipe, recipeExtend.getDecoctionId(), recipeDetails, null, stockCheckType);
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        if (null != organStock) {
            result.add(organStock);
        }
        logger.info("StockBusinessService drugRecipeStockV1 result={}", JSON.toJSONString(result));
        return result;
    }


    /**
     * * 异步保存处方购药方式
     *
     * @param recipe              处方信息
     * @param enterpriseStockList 库存信息
     * @param recipeDetails       药品信息
     * @return 支持的购药方式文案
     */
    private List<GiveModeButtonDTO> saveGiveMode(Recipe recipe, List<EnterpriseStock> enterpriseStockList, List<Recipedetail> recipeDetails) {
        List<EnterpriseStock> enterpriseStock = enterpriseStockList.stream().filter(a -> CollectionUtils.isNotEmpty(a.getGiveModeButton())).collect(Collectors.toList());
        logger.info("StockBusinessService saveGiveMode recipeId:{}, enterpriseStock={}", recipe.getRecipeId(), JSON.toJSONString(enterpriseStock));
        enterpriseStock = drugsEnterprisePriority(recipe, enterpriseStock);
        List<GiveModeButtonDTO> giveModeButton = new LinkedList<>();
        enterpriseStock.stream().filter(EnterpriseStock::getStock).forEach(a -> giveModeButton.addAll(a.getGiveModeButton()));
        String supportDownloadButton = organDrugListManager.organStockDownload(recipe.getClinicOrgan(), recipeDetails);
        if (StringUtils.isNotEmpty(supportDownloadButton)) {
            GiveModeButtonDTO supportDownload = new GiveModeButtonDTO();
            supportDownload.setType(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType());
            supportDownload.setShowButtonName(supportDownloadButton);
            supportDownload.setShowButtonKey(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            giveModeButton.add(supportDownload);
        }
        if (CollectionUtils.isEmpty(giveModeButton)) {
            return giveModeButton;
        }
        Set<Integer> recipeGiveMode = giveModeButton.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getType).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(recipeGiveMode) && !ValidateUtil.integerIsEmpty(recipe.getRecipeId())) {
            String join = StringUtils.join(recipeGiveMode, ",");
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipe.getRecipeId());
            recipeUpdate.setRecipeSupportGiveMode(join);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        }
        logger.info("StockBusinessService saveGiveMode saveGiveMode ,购药按钮 {},{},{}", recipe.getRecipeId(), JSON.toJSONString(recipeGiveMode), JSON.toJSONString(giveModeButton));
        return giveModeButton;
    }

    /**
     * 对药企优先级进行处理
     * @param recipe
     * @param enterpriseStock
     * @return
     */
    private List<EnterpriseStock> drugsEnterprisePriority(Recipe recipe, List<EnterpriseStock> enterpriseStock) {
        logger.info("StockBusinessService drugsEnterprisePriority recipe:{},enterpriseStock:{}", recipe.getRecipeId(), JSON.toJSONString(enterpriseStock));
        //对药企优先级进行处理
        try {
            List<DrugsEnterprise> subDepList = enterpriseStock.stream().filter(EnterpriseStock::getStock).map(EnterpriseStock::getDrugsEnterprise).filter(drugsEnterprise -> Objects.nonNull(drugsEnterprise)).collect(Collectors.toList());
            logger.info("StockBusinessService drugsEnterprisePriority drugsEnterpriseList:{}", JSON.toJSONString(subDepList));
            if (CollectionUtils.isNotEmpty(subDepList)) {
                List<DrugsEnterprise> drugsEnterpriseList = enterpriseManager.enterprisePriorityLevel(recipe.getClinicOrgan(), subDepList);
                List<Integer> drugsEnterpriseIds = drugsEnterpriseList.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
                enterpriseStock = enterpriseStock.stream().filter(a -> null == a.getDrugsEnterprise() || drugsEnterpriseIds.contains(a.getDrugsEnterpriseId())).collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("StockBusinessService drugsEnterprisePriority error", e);
        }
        logger.info("StockBusinessService drugsEnterprisePriority recipe:{},result enterpriseStock:{}", recipe.getRecipeId(), JSON.toJSONString(enterpriseStock));
        return enterpriseStock;
    }

    /**
     * 获取弹框文本
     *
     * @param organId
     * @param doSignRecipe        返回提示信息
     * @param supportGiveModeList 有库存的够药按钮
     */
    private void getSupportGiveModeNameText(Integer organId, DoSignRecipeDTO doSignRecipe, List<GiveModeButtonDTO> supportGiveModeList) {
        logger.info("StockBusinessService getSupportGiveModeNameText recipe:{}, supportGiveModeList:{}", organId, JSON.toJSONString(supportGiveModeList));
        String recipeShowTipOption = configurationClient.getValueCatchReturnArr(organId, "recipeShowTipOption", "0");
        Set<String> supportGiveModeNameSet = supportGiveModeList.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getShowButtonName).collect(Collectors.toSet());
        doSignRecipe.setRecipeSupportGiveModeText(supportGiveModeNameSet);
        if ("0".equals(recipeShowTipOption)) {
            //任何情况下都需要弹框提示
            showSupportGiveMode(doSignRecipe, supportGiveModeNameSet);
        } else if ("1".equals(recipeShowTipOption)) {
            //存在不满足库存的购药方式时告知
            //机构配置购药方式
            List<GiveModeButtonDTO> organGiveModeList = operationClient.getOrganGiveModeMap(organId);
            Set<String> giveModeKeys = organGiveModeList.stream().map(GiveModeButtonDTO::getShowButtonKey).collect(Collectors.toSet());
            if (giveModeKeys.size() != supportGiveModeNameSet.size()) {
                showSupportGiveMode(doSignRecipe, supportGiveModeNameSet);
            }
        }
    }

    private void showSupportGiveMode(DoSignRecipeDTO doSignRecipe, Set<String> supportGiveModeNameSet) {
        if (CollectionUtils.isEmpty(supportGiveModeNameSet)) {
            enterpriseManager.doSignRecipe(doSignRecipe, "根据药品库存判断，未找到可供药的药房或药企");
            return;
        }
        StringBuilder msg = new StringBuilder("根据药品库存判断，本处方支持");
        supportGiveModeNameSet.forEach(name -> msg.append("【").append(name).append("】"));
        enterpriseManager.doSignRecipe(doSignRecipe, msg.toString());
        doSignRecipe.setCanContinueFlag("1");
    }

}
