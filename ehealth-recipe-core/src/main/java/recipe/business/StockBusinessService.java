package recipe.business;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.DrugsEnterpriseBean;
import com.ngari.platform.recipe.mode.MedicineStationDTO;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.*;
import recipe.constant.ErrorCode;
import recipe.core.api.IStockBusinessService;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrganDrugListManager;
import recipe.manager.RecipeManager;
import recipe.util.DistanceUtil;
import recipe.util.MapValueUtil;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.DrugForGiveModeVO;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.doctor.EnterpriseStockVO;
import recipe.vo.patient.MedicineStationVO;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * 药企处理实现类
 *
 * @author fuzi
 */
@Service
public class StockBusinessService extends BaseService implements IStockBusinessService {
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private EnterpriseManager enterpriseManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private OrganClient organClient;
    @Resource
    private EnterpriseClient enterpriseClient;
    @Resource
    private IConfigurationClient configurationClient;
    @Resource
    private RecipeDAO recipeDAO;
    @Resource
    private RecipeDetailDAO recipeDetailDAO;
    @Resource
    private OrganDrugListDAO organDrugListDAO;


    @Override
    public List<DrugEnterpriseStockVO> stockList(Integer organId, Integer recipeType, String decoctionId, List<Recipedetail> recipeDetails) {
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(organId, recipeDetails);
        //药企库存
        List<EnterpriseStock> enterpriseStockButton = buttonManager.enterpriseStockCheck(organId, recipeType, decoctionId);
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheck(organId, recipeDetails, enterpriseStockButton);
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
    public List<EnterpriseStock> stockList(Recipe recipe, String decoctionId, List<Recipedetail> recipeDetails) {
        //药企库存
        List<EnterpriseStock> result = this.enterpriseStockCheckAll(recipe, decoctionId, recipeDetails, null);
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        if (null != organStock) {
            result.add(organStock);
        }
        return result;
    }

    @Override
    public Map<String, Object> enterpriseStock(Integer recipeId) {
        logger.info("StockBusinessService enterpriseStock recipeId={}", recipeId);
        Recipe recipe = recipeDAO.get(recipeId);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        RecipeExtend recipeExtend = recipeManager.recipeExtend(recipeId);
        //药企库存
        List<EnterpriseStock> enterpriseStockList = this.enterpriseStockCheckAll(recipe, recipeExtend.getDecoctionId(), recipeDetails, null);
        List<EnterpriseStock> enterpriseStock = enterpriseStockList.stream().filter(a -> CollectionUtils.isNotEmpty(a.getGiveModeButton())).collect(Collectors.toList());
        logger.info("StockBusinessService enterpriseStock enterpriseStock={}", JSON.toJSONString(enterpriseStock));
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        logger.info("StockBusinessService enterpriseStock organStock:{}", JSON.toJSONString(organStock));
        DoSignRecipeDTO doSignRecipe = new DoSignRecipeDTO(true, false, null, "", recipeId, null);
        //机构配置购药方式
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(giveModeButtonBeans) ||
                (new Integer(1).equals(giveModeButtonBeans.size())
                        && Objects.equals(RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText()), "下载处方笺"))) {
            enterpriseManager.doSignRecipe(doSignRecipe, "根据药品库存判断，未找到可供药的药房或药企");
            logger.info("StockBusinessService enterpriseStock recipeId:{},reason:没有配置购药方式", recipeId);
            return MapValueUtil.beanToMap(doSignRecipe);
        }
        //保存药品购药方式
        List<GiveModeButtonDTO> supportGiveModeList = saveGiveMode(recipe, organStock, enterpriseStock, recipeDetails);
        //获取弹框文本
        getSupportGiveModeNameText(recipe, doSignRecipe, supportGiveModeList, giveModeButtonBeans);
        return MapValueUtil.beanToMap(doSignRecipe);
    }

    /**
     * 获取弹框文本
     * @param doSignRecipe
     * @param supportGiveModeList
     */
    private void getSupportGiveModeNameText(Recipe recipe, DoSignRecipeDTO doSignRecipe, List<GiveModeButtonDTO> supportGiveModeList, List<GiveModeButtonDTO> giveModeButtonBeans) {
        logger.info("StockBusinessService getSupportGiveModeNameText recipe:{}, supportGiveModeList:{}, giveModeButtonBeans:{}", JSON.toJSONString(recipe), JSON.toJSONString(supportGiveModeList), JSON.toJSONString(giveModeButtonBeans));
        String recipeShowTipOption = configurationClient.getValueCatchReturnArr(recipe.getClinicOrgan(), "recipeShowTipOption", "0");
        Set<String> supportGiveModeNameSet = supportGiveModeList.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getShowButtonName).collect(Collectors.toSet());
        if ("0".equals(recipeShowTipOption)) {
            //任何情况下都需要弹框提示
            showSupportGiveMode(doSignRecipe, supportGiveModeNameSet);
        } else if ("1".equals(recipeShowTipOption)) {
            //存在不满足库存的购药方式时告知
            Set<String> giveModeKeys = giveModeButtonBeans.stream().map(GiveModeButtonDTO::getShowButtonKey).collect(Collectors.toSet());
            Set<String> supportGiveModeKeySet = supportGiveModeList.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getShowButtonKey).collect(Collectors.toSet());
            logger.info("StockBusinessService getSupportGiveModeNameText giveModeKeys:{},supportGiveModeKeySet:{}", JSON.toJSONString(giveModeKeys), JSON.toJSONString(supportGiveModeKeySet));
            giveModeKeys.removeAll(supportGiveModeKeySet);
            logger.info("StockBusinessService getSupportGiveModeNameText giveModeKeys:{}", JSON.toJSONString(giveModeKeys));
            if (CollectionUtils.isNotEmpty(giveModeKeys)) {
                showSupportGiveMode(doSignRecipe, supportGiveModeNameSet);
            }
        }
    }

    private void showSupportGiveMode(DoSignRecipeDTO doSignRecipe, Set<String> supportGiveModeNameSet) {
        if (CollectionUtils.isNotEmpty(supportGiveModeNameSet)) {
            StringBuilder msg = new StringBuilder("根据药品库存判断，本处方支持");
            supportGiveModeNameSet.forEach(supportGiveModeName -> {
                msg.append("【").append(supportGiveModeName).append("】");
            });
            enterpriseManager.doSignRecipe(doSignRecipe, msg.toString());
            doSignRecipe.setCanContinueFlag("1");
        } else {
            enterpriseManager.doSignRecipe(doSignRecipe, "根据药品库存判断，未找到可供药的药房或药企");
        }
    }

    @Override
    public EnterpriseStock enterpriseStockCheck(Recipe recipe, List<Recipedetail> recipeDetails, Integer enterpriseId) {
        String decoctionId = null;
        if (null != recipe.getRecipeId()) {
            RecipeExtend recipeExtend = recipeManager.recipeExtend(recipe.getRecipeId());
            decoctionId = recipeExtend.getDecoctionId();
        }
        List<EnterpriseStock> enterpriseStockList = this.enterpriseStockCheckAll(recipe, decoctionId, recipeDetails, enterpriseId);
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
    public List<DrugForGiveModeVO> drugForGiveMode(DrugQueryVO drugQueryVO) {
        logger.info("drugForGiveMode DrugQueryVO={}", JSONArray.toJSONString(drugQueryVO));
        List<String> drugNames = new ArrayList<>();
        List<Integer> drugIds = new ArrayList<>();

        List<Recipedetail> recipeDetails = drugQueryVO.getRecipeDetails().stream().map(recipeDetailBean -> {
            drugNames.add(recipeDetailBean.getDrugName());
            drugIds.add(recipeDetailBean.getDrugId());
            Recipedetail recipedetail = new Recipedetail();
            recipeDetailBean.setPharmacyId(drugQueryVO.getPharmacyId());
            BeanUtils.copy(recipeDetailBean, recipedetail);
            return recipedetail;
        }).collect(Collectors.toList());
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(drugQueryVO.getOrganId(), recipeDetails);
        //药企库存
        List<EnterpriseStock> enterpriseStockButton = buttonManager.enterpriseStockCheck(drugQueryVO.getOrganId(), drugQueryVO.getRecipeType(), drugQueryVO.getDecoctionId());
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheck(drugQueryVO.getOrganId(), recipeDetails, enterpriseStockButton);
        enterpriseStock.add(organStock);
        logger.info("drugForGiveMode enterpriseStock={}", JSONArray.toJSONString(enterpriseStock));
        List<DrugForGiveModeVO> list = Lists.newArrayList();
        for (EnterpriseStock stock : enterpriseStock) {
            if(Objects.isNull(stock)){
                continue;
            }
            List<GiveModeButtonDTO> giveModeButton = stock.getGiveModeButton();
            if (CollectionUtils.isEmpty(giveModeButton)) {
                continue;
            }
            List<Integer> ids = stock.getDrugInfoList().stream().filter(DrugInfoDTO::getStock).map(DrugInfoDTO::getDrugId).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(ids)){
                continue;
            }
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugIds(drugQueryVO.getOrganId(), ids);
            List<String> drugName = organDrugLists.stream().map(OrganDrugList::getDrugName).collect(Collectors.toList());
            giveModeButton.forEach(giveModeButtonDTO -> {
                DrugForGiveModeVO drugForGiveModeVO = new DrugForGiveModeVO();
                drugForGiveModeVO.setGiveModeKey(giveModeButtonDTO.getShowButtonKey());
                drugForGiveModeVO.setGiveModeKeyText(giveModeButtonDTO.getShowButtonName());
                if (!RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getText().equals(giveModeButtonDTO.getShowButtonKey())) {
                    drugForGiveModeVO.setEnterpriseName(stock.getDrugsEnterprise().getName());
                }
                drugForGiveModeVO.setDrugsName(drugName);
                list.add(drugForGiveModeVO);
            });
        }
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(drugQueryVO.getOrganId());

        //下载处方签
        String supportDownloadButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
        if (StringUtils.isNotEmpty(supportDownloadButton)) {
            List<OrganDrugList> organDrugList = organDrugListDAO.findOrganDrugListBySupportDownloadPrescriptionPad(drugQueryVO.getOrganId(), drugIds);
            List<String> drug = organDrugList.stream().map(OrganDrugList::getDrugName).collect(Collectors.toList());
            DrugForGiveModeVO drugForGiveModeVO = new DrugForGiveModeVO();
            drugForGiveModeVO.setGiveModeKey(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            drugForGiveModeVO.setGiveModeKeyText(supportDownloadButton);
            drugForGiveModeVO.setDrugsName(drug);
            list.add(drugForGiveModeVO);
        }
        logger.info("drugForGiveMode result={}", JSONArray.toJSONString(list));
        return list;
    }

    @Override
    public List<EnterpriseStock> drugRecipeStock(Integer organId, Integer recipeType, String decoctionId, List<Recipedetail> recipeDetails) {
        EnterpriseStock result = new EnterpriseStock();
        result.setStock(true);
        //下载处方签
        String supportDownloadButton = organDrugListManager.organStockDownload(organId, recipeDetails);
        if (StringUtils.isNotEmpty(supportDownloadButton)) {
            return Collections.singletonList(result);
        }
        //例外支付
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(organId);
        String supportMedicalPaymentButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
        if (StringUtils.isNotEmpty(supportMedicalPaymentButton)) {
            return Collections.singletonList(result);
        }
        //机构库存
        EnterpriseStock organStock = organDrugListManager.organStock(organId, recipeDetails);
        //药企库存
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(organId);
        recipe.setRecipeType(recipeType);
        List<EnterpriseStock> enterpriseStock = this.enterpriseStockCheckAll(recipe, decoctionId, recipeDetails, null);
        if (null != organStock) {
            enterpriseStock.add(organStock);
        }
        return enterpriseStock;
    }

    @Override
    public Boolean getOrderStockFlag(List<Integer> recipeIds, Integer enterpriseId, String giveModeKey) {
        Recipe recipe = recipeDAO.get(recipeIds.get(0));
        Integer giveMode = RecipeSupportGiveModeEnum.getGiveMode(giveModeKey);
        if (giveMode == 0) {
            return true;
        }
        recipe.setGiveMode(giveMode);
        return this.getStockFlag(recipeIds, recipe, enterpriseId);
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfig(Integer organId, Integer drugsEnterpriseId) {
        return enterpriseManager.getOrganDrugsSaleConfig(organId,drugsEnterpriseId,GiveModeEnum.GIVE_MODE_HOSPITAL_DRUG.getType());
    }

    @Override
    public List<MedicineStationVO> getMedicineStationList(MedicineStationVO medicineStationVO){
        logger.info("getMedicineStationList medicineStationVO:{}", JSONUtils.toString(medicineStationVO));
        OrganDTO organDTO = organClient.organDTO(medicineStationVO.getOrganId());
        DrugsEnterprise drugsEnterprise = enterpriseManager.drugsEnterprise(medicineStationVO.getEnterpriseId());
        DrugsEnterpriseBean enterpriseBean = ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
        MedicineStationDTO medicineStationDTO = ObjectCopyUtils.convert(medicineStationVO, MedicineStationDTO.class);
        OrganBean organBean = ObjectCopyUtils.convert(organDTO, OrganBean.class);
        //获取取药站点列表
        List<MedicineStationDTO> medicineStationDTOList = enterpriseClient.getMedicineStationList(medicineStationDTO, organBean, enterpriseBean);
        List<MedicineStationVO> medicineStationVOList = ObjectCopyUtils.convert(medicineStationDTOList, MedicineStationVO.class);

        //根据坐标计算距离
        medicineStationVOList.forEach(medicineStation->{
            try {
                if (StringUtils.isNotEmpty(medicineStation.getLat()) && StringUtils.isNotEmpty(medicineStation.getLng())) {
                    Double distance = DistanceUtil.getDistance(Double.parseDouble(medicineStationVO.getLat()), Double.parseDouble(medicineStationVO.getLng()),
                            Double.parseDouble(medicineStation.getLat()), Double.parseDouble(medicineStation.getLng()));
                    medicineStation.setDistance(Double.parseDouble(String.format("%.2f",distance)));
                } else {
                    medicineStation.setDistance(0D);
                }
            } catch (Exception e) {
                logger.error("getMedicineStationList error", e);
                medicineStation.setDistance(0D);
            }
        });
        return medicineStationVOList;
    }

    @Override
    public OrganDrugsSaleConfig getOrganDrugsSaleConfigOfPatient(Integer organId, Integer drugsEnterpriseId) {
        return enterpriseManager.getOrganDrugsSaleConfigOfPatient(organId,drugsEnterpriseId);
    }

    /**
     * 保存购药按钮
     *
     * @param recipe
     */
    public void setSupportGiveMode(Recipe recipe) {
        logger.info("StockBusinessService setSupportGiveMode recipe={}", JSONArray.toJSONString(recipe));
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
        RecipeExtend recipeExtend = recipeManager.recipeExtend(recipe.getRecipeId());
        //药企库存
        List<EnterpriseStock> enterpriseStockList = this.enterpriseStockCheckAll(recipe, recipeExtend.getDecoctionId(), recipeDetails, null);
        List<EnterpriseStock> enterpriseStock = enterpriseStockList.stream().filter(a -> CollectionUtils.isNotEmpty(a.getGiveModeButton())).collect(Collectors.toList());
        logger.info("DrugEnterpriseBusinessService setSupportGiveMode enterpriseStock={}", JSON.toJSONString(enterpriseStock));
        //医院库存
        EnterpriseStock organStock = organDrugListManager.organStock(recipe, recipeDetails);
        saveGiveMode(recipe, organStock, enterpriseStock, recipeDetails);
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
                EnterpriseStock enterpriseStock = this.enterpriseStockCheck(recipe, recipeDetails, enterpriseId);
                logger.info("StockBusinessService getStockFlag enterpriseStock={}", JSONArray.toJSONString(enterpriseStock));
                if (Objects.nonNull(enterpriseStock)) {
                    stockFlag = enterpriseStock.getStock();
                }
                break;

            case GIVE_MODE_HOSPITAL_DRUG:
                // 到院取药
                // 医院库存
                EnterpriseStock organStock = organDrugListManager.organStock(recipe.getClinicOrgan(), recipeDetails);
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
     * @param organId             机构id
     * @param recipeDetails       药品信息 drugId，code
     * @param enterpriseStockList 指定某药企List
     * @return 药品信息 一定存在于出参
     */
    private List<EnterpriseStock> enterpriseStockCheck(Integer organId, List<Recipedetail> recipeDetails, List<EnterpriseStock> enterpriseStockList) {
        //校验药企库存
        List<FutureTask<EnterpriseStock>> futureTasks = new LinkedList<>();
        //根据药企配置查询 库存
        for (EnterpriseStock enterpriseStock : enterpriseStockList) {
            Recipe recipe = new Recipe();
            recipe.setClinicOrgan(organId);
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStockFutureTask(enterpriseStock, recipe, recipeDetails, null));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks);
    }

    /**
     * 校验 药品库存在 遍历药企 同一个药企下的库存数量
     * 验证能否药品配送以及能否开具到一张处方单上
     *
     * @param recipe        处方机构id
     * @param recipeDetails 药品信息 drugId，code
     * @return 药品信息非必须
     */
    private List<EnterpriseStock> enterpriseStockCheckAll(Recipe recipe, String decoctionId, List<Recipedetail> recipeDetails, Integer enterpriseId) {
        //获取需要查询库存的药企对象
        List<EnterpriseStock> enterpriseStockList = buttonManager.enterpriseStockCheck(recipe.getClinicOrgan(), recipe.getRecipeType(), decoctionId);
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
            FutureTask<EnterpriseStock> ft = new FutureTask<>(() -> enterpriseStockFutureTask(enterpriseStock, recipe, recipeDetails, enterpriseDrugIdGroup));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        }
        return super.futureTaskCallbackBeanList(futureTasks);
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
                                                      List<Recipedetail> recipeDetails, Map<Integer, List<Integer>> enterpriseDrugIdGroup) {
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
            enterpriseStock(enterpriseStock, recipe, recipeDetails);
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
     */
    private void enterpriseStock(EnterpriseStock enterpriseStock, Recipe recipe, List<Recipedetail> recipeDetails) {
        DrugsEnterprise drugsEnterprise = enterpriseStock.getDrugsEnterprise();
        Integer checkInventoryFlag = drugsEnterprise.getCheckInventoryFlag();
        if (null == checkInventoryFlag) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, drugsEnterprise.getName() + "checkInventoryFlag is null");
        }
        List<Integer> drugIds = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        List<SaleDrugList> saleDrugList = enterpriseManager.saleDrugListEffectivity(drugsEnterprise.getId(), drugIds);
        Map<Integer, SaleDrugList> saleDrugMap = saleDrugList.stream().collect(Collectors.toMap(SaleDrugList::getDrugId, a -> a, (k1, k2) -> k1));
        List<Recipedetail> recipeDetailList = recipeDetails.stream().filter(a -> null != saleDrugMap.get(a.getDrugId())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeDetailList)) {
            enterpriseStock.setStock(false);
            enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetails, false));
            logger.info("DrugEnterpriseBusinessService enterpriseStock recipeDetailList is null");
            return;
        }
        if (0 == drugsEnterprise.getCheckInventoryFlag()) {
            //机构药企 药品列表对不上
            enterpriseStock.setStock(true);
            enterpriseStock.setDrugInfoList(DrugStockClient.getDrugInfoDTO(recipeDetailList, true));
            logger.info("DrugEnterpriseBusinessService enterpriseStock 0 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //医院自建药企-查询医院库存
        if (3 == drugsEnterprise.getCheckInventoryFlag()) {
            DrugStockAmountDTO organStock = organDrugListManager.scanDrugStockByRecipeId(recipe, recipeDetailList);
            enterpriseStock.setStock(organStock.isResult());
            enterpriseStock.setDrugName(organStock.getNotDrugNames());
            enterpriseStock.setDrugInfoList(organStock.getDrugInfoList());
            logger.info("DrugEnterpriseBusinessService enterpriseStock 3 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //通过前置机调用
        if (1 == drugsEnterprise.getOperationType()) {
            DrugStockAmountDTO code = enterpriseManager.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetailList);
            enterpriseStock.setStock(code.isResult());
            enterpriseStock.setDrugInfoList(code.getDrugInfoList());
            logger.info("DrugEnterpriseBusinessService enterpriseStock 1 enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
            return;
        }
        //通过平台调用药企
        AccessDrugEnterpriseService drugEnterpriseService = RemoteDrugEnterpriseService.getServiceByDep(drugsEnterprise);
        DrugStockAmountDTO result = drugEnterpriseService.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetailList);
        enterpriseStock.setStock(result.isResult());
        enterpriseStock.setDrugInfoList(result.getDrugInfoList());
        logger.info("DrugEnterpriseBusinessService enterpriseStock else enterpriseStock= {}", JSON.toJSONString(enterpriseStock));
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
     * * 异步保存处方购药方式
     *
     * @param recipe          处方信息
     * @param organStock      机构库存
     * @param enterpriseStock 药企库存
     * @return 支持的购药方式文案
     */
    private List<GiveModeButtonDTO> saveGiveMode(Recipe recipe, EnterpriseStock organStock, List<EnterpriseStock> enterpriseStock, List<Recipedetail> recipeDetails) {
        logger.info("DrugEnterpriseBusinessService saveGiveMode start recipe={}", JSON.toJSONString(recipe));
        List<GiveModeButtonDTO> giveModeButton = new LinkedList<>();
        if (null != organStock && organStock.getStock()) {
            giveModeButton.addAll(organStock.getGiveModeButton());
        }
        enterpriseStock.stream().filter(EnterpriseStock::getStock).forEach(a -> giveModeButton.addAll(a.getGiveModeButton()));
        List<GiveModeButtonDTO> giveModeButtonBeans = operationClient.getOrganGiveModeMap(recipe.getClinicOrgan());
        String supportDownloadButton = organDrugListManager.organStockDownload(recipe.getClinicOrgan(), recipeDetails);
        if (StringUtils.isNotEmpty(supportDownloadButton)) {
            GiveModeButtonDTO supportDownload = new GiveModeButtonDTO();
            supportDownload.setType(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType());
            supportDownload.setShowButtonName(supportDownloadButton);
            supportDownload.setShowButtonKey(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText());
            giveModeButton.add(supportDownload);
        }
//        String supportMedicalPaymentButton = RecipeSupportGiveModeEnum.getGiveModeName(giveModeButtonBeans, RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
//        if (StringUtils.isNotEmpty(supportMedicalPaymentButton)) {
//            GiveModeButtonDTO supportMedicalPayment = new GiveModeButtonDTO();
//            supportMedicalPayment.setType(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getType());
//            supportMedicalPayment.setShowButtonName(supportMedicalPaymentButton);
//            supportMedicalPayment.setShowButtonKey(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText());
//            giveModeButton.add(supportMedicalPayment);
//        }
        if (CollectionUtils.isEmpty(giveModeButton)) {
            return giveModeButton;
        }
        Set<Integer> recipeGiveMode = giveModeButton.stream().filter(Objects::nonNull).map(GiveModeButtonDTO::getType).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(recipeGiveMode)) {
            String join = StringUtils.join(recipeGiveMode, ",");
            Recipe recipeUpdate = new Recipe();
            recipeUpdate.setRecipeId(recipe.getRecipeId());
            recipeUpdate.setRecipeSupportGiveMode(join);
            recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
        }
        logger.info("DrugEnterpriseBusinessService saveGiveMode 异步保存处方购药方式,购药按钮 {},{},{}", recipe.getRecipeId(), JSON.toJSONString(recipeGiveMode), JSON.toJSONString(giveModeButton));
        return giveModeButton;
    }
}
