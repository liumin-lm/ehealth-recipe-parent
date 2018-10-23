package recipe.drugsenterprise;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.recipe.common.utils.VerifyUtils;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.StandardFinishDTO;
import recipe.drugsenterprise.bean.StandardRecipeDetailDTO;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.drugsenterprise.bean.UpdatePrescriptionDTO;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企标准服务
 * @version： 1.0
 */
@RpcBean("distributionService")
public class StandardEnterpriseCallService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEnterpriseCallService.class);

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeOrderDAO orderDAO;

    @RpcService
    public StandardResultDTO send(List<Map<String, Object>> list) {
        //TODO
        LOGGER.info("send param : " + JSONUtils.toString(list));
        StandardResultDTO result = new StandardResultDTO();
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }


        return null;
    }

    @RpcService
    public StandardResultDTO finish(List<StandardFinishDTO> list) {
        LOGGER.info("finish param : " + JSONUtils.toString(list));
        StandardResultDTO result = new StandardResultDTO();
        //默认为失败
        result.setCode(StandardResultDTO.FAIL);
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }

        for (StandardFinishDTO finishDTO : list) {
            try {
                Multimap<String, String> verifyMap = VerifyUtils.verify(finishDTO);
                if (!verifyMap.keySet().isEmpty()) {
                    result.setMsg(verifyMap.toString());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("finish 参数对象异常数据，StandardFinishDTO={}", JSONUtils.toString(finishDTO), e);
                result.setMsg("参数对象异常数据");
                return result;
            }

            boolean isSuccess = finishDTO.getCode().equals(StandardFinishDTO.SUCCESS) ? true : false;
            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                clinicOrgan = getClinicOrganByOrganId(finishDTO.getOrganId());
            } catch (Exception e) {
                LOGGER.warn("finish 查询机构异常，organId={}", finishDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("finish 平台未匹配到该组织机构编码，organId={}", finishDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(finishDTO.getRecipeCode(), clinicOrgan);
            if (null == dbRecipe) {
                result.setMsg("[" + finishDTO.getRecipeCode() + "]处方单不存在");
                return result;
            }

            //重复处理
            if (RecipeStatusConstant.FINISH == dbRecipe.getStatus()) {
                continue;
            }

            Integer recipeId = dbRecipe.getRecipeId();
            if (isSuccess) {
                Map<String, Object> attrMap = Maps.newHashMap();
                attrMap.put("giveDate", StringUtils.isEmpty(finishDTO.getDate()) ? DateTime.now().toDate() :
                        DateConversion.parseDate(finishDTO.getDate(), DateConversion.DEFAULT_DATE_TIME));
                attrMap.put("giveFlag", 1);
                attrMap.put("giveUser", finishDTO.getSender());
                attrMap.put("payFlag", 1);
                attrMap.put("payDate", DateTime.now().toDate());
                //更新处方信息
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                    //完成订单，不需要检查订单有效性，就算失效的订单也直接变成已完成
                    orderService.finishOrder(dbRecipe.getOrderCode(), dbRecipe.getPayMode(), null);
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(),
                            RecipeStatusConstant.FINISH, "处方单配送完成,配送人：" + finishDTO.getSender());
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);
                }

                //HOS处方发送完成短信
                if (RecipeBussConstant.FROMFLAG_HIS_USE == dbRecipe.getFromflag()) {
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_FINISH_4HIS, dbRecipe);
                }

            } else {
                //患者未取药
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG, null);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    orderService.cancelOrderByCode(dbRecipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                }

                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), RecipeStatusConstant.NO_DRUG,
                        "处方单配送失败:" + finishDTO.getMsg());
            }

        }

        result.setCode(StandardResultDTO.SUCCESS);
        LOGGER.info("处方单[{}] finish 处理完成.", list.get(0).getRecipeCode());
        return result;
    }

    @RpcService
    public StandardResultDTO updatePrescription(List<UpdatePrescriptionDTO> list) {
        LOGGER.info("updatePrescription param : " + JSONUtils.toString(list));
        StandardResultDTO result = new StandardResultDTO();
        //默认为失败
        result.setCode(StandardResultDTO.FAIL);
        if (CollectionUtils.isEmpty(list)) {
            result.setMsg("参数错误");
            return result;
        }

        for (UpdatePrescriptionDTO updatePrescriptionDTO : list) {
            try {
                Multimap<String, String> verifyMap = VerifyUtils.verify(updatePrescriptionDTO);
                if (!verifyMap.keySet().isEmpty()) {
                    result.setMsg(verifyMap.toString());
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 参数对象异常数据，updatePrescriptionDTO={}", JSONUtils.toString(updatePrescriptionDTO), e);
                result.setMsg("参数对象异常数据");
                return result;
            }

            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                clinicOrgan = getClinicOrganByOrganId(updatePrescriptionDTO.getOrganId());
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 查询机构异常，organId={}", updatePrescriptionDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("updatePrescription 平台未匹配到该组织机构编码，organId={}", updatePrescriptionDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(updatePrescriptionDTO.getRecipeCode(), clinicOrgan);
            if (null == dbRecipe) {
                result.setMsg("[" + updatePrescriptionDTO.getRecipeCode() + "]处方单不存在");
                return result;
            }

            Integer recipeId = dbRecipe.getRecipeId();
            //更新处方信息
            String recipeFeeStr = updatePrescriptionDTO.getRecipeFee();
            Map<String, Object> attrMap = Maps.newHashMap();
            Map<String, Object> orderAttrMap = Maps.newHashMap();
            if (StringUtils.isNotEmpty(recipeFeeStr)) {
                BigDecimal recipeFee = new BigDecimal(recipeFeeStr);
                attrMap.put("totalMoney", recipeFee);
                orderAttrMap.put("recipeFee", recipeFee);
                orderAttrMap.put("actualPrice", recipeFee.doubleValue());
            }

            boolean success = false;
            try {
                if (!attrMap.isEmpty()) {
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, dbRecipe.getStatus(), attrMap);
                }
                //修改处方单详情
                updateRecipeDetainInfo(dbRecipe, updatePrescriptionDTO);
                if (!orderAttrMap.isEmpty()) {
                    //修改订单详情
                    orderDAO.updateByOrdeCode(dbRecipe.getOrderCode(), orderAttrMap);
                }
                success = true;
            } catch (Exception e) {
                LOGGER.warn("updatePrescription 处方更新异常, recipeId={}", dbRecipe.getRecipeId(), e);
            } finally {
                if (!success) {
                    result.setMsg("药品信息更新异常");
                    return result;
                }
            }
        }

        result.setCode(StandardResultDTO.SUCCESS);
        LOGGER.info("处方单[{}] updatePrescription 处理完成.", list.get(0).getRecipeCode());
        return result;
    }

    private Integer getClinicOrganByOrganId(String organId) throws Exception {
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        Integer clinicOrgan = null;
        List<OrganBean> organList = organService.findByOrganizeCode(organId);
        if (CollectionUtils.isNotEmpty(organList)) {
            clinicOrgan = organList.get(0).getOrganId();
        }

        return clinicOrgan;
    }

    /**
     * 更新处方详细信息
     *
     * @param recipe
     * @param paramMap
     */
    private void updateRecipeDetainInfo(Recipe recipe, UpdatePrescriptionDTO updatePrescriptionDTO) throws Exception {
        List<StandardRecipeDetailDTO> list = updatePrescriptionDTO.getDetails();
        if (CollectionUtils.isNotEmpty(list)) {
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            SaleDrugListDAO saleDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugsEnterpriseDAO depDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);

            //获取药企ID
            DrugsEnterprise dep = depDAO.getByAccount(updatePrescriptionDTO.getAccount());
            if (null == dep) {
                throw new Exception("药企不存在");
            }

            List<String> drugCodeList = Lists.newArrayList(Collections2.transform(list, new Function<StandardRecipeDetailDTO, String>() {
                @Nullable
                @Override
                public String apply(@Nullable StandardRecipeDetailDTO input) {
                    return input.getDrugCode();
                }
            }));

            List<SaleDrugList> saleList = saleDAO.findByOrganIdAndDrugCodes(dep.getId(), drugCodeList);
            Map<Integer, StandardRecipeDetailDTO> mapInfo = Maps.newHashMap();
            for (SaleDrugList sale : saleList) {
                for (StandardRecipeDetailDTO dto : list) {
                    if (sale.getOrganDrugCode().equals(dto.getDrugCode())) {
                        mapInfo.put(sale.getDrugId(), dto);
                        break;
                    }
                }
            }

            if (mapInfo.isEmpty()) {
                LOGGER.warn("updateRecipeDetainInfo mapInfo is empty. depId={}, drugCodeList={}",
                        dep.getId(), JSONUtils.toString(drugCodeList));
                throw new Exception("药企维护数据异常");
            }

            List<Recipedetail> detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
            StandardRecipeDetailDTO detailDTO;
            Integer drugId;
            String salePrice;
            String drugCost;
            String drugBatch;
            String validDate;
            Map<String, Object> changeAttr = Maps.newHashMap();
            for (Recipedetail detailInfo : detailList) {
                changeAttr.clear();
                drugId = detailInfo.getDrugId();
                detailDTO = mapInfo.get(drugId);
                if (null == detailDTO) {
                    continue;
                }
                //更新信息
                salePrice = detailDTO.getSalePrice();
                if (StringUtils.isNotEmpty(salePrice)) {
                    changeAttr.put("salePrice", new BigDecimal(salePrice));
                }
                drugCost = detailDTO.getDrugCost();
                if (StringUtils.isNotEmpty(drugCost)) {
                    changeAttr.put("drugCost", new BigDecimal(drugCost));
                }
                drugBatch = detailDTO.getDrugBatch();
                if (StringUtils.isNotEmpty(drugBatch)) {
                    changeAttr.put("drugBatch", drugBatch);
                }
                validDate = detailDTO.getValidDate();
                if (StringUtils.isNotEmpty(validDate)) {
                    changeAttr.put("validDate", DateConversion.parseDate(validDate, DateConversion.DEFAULT_DATE_TIME));
                }

                if (!changeAttr.isEmpty()) {
                    detailDAO.updateRecipeDetailByRecipeDetailId(detailInfo.getRecipeDetailId(), changeAttr);
                }
            }
        }
    }
}
