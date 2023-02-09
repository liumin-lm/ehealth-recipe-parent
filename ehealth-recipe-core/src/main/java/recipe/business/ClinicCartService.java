package recipe.business;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.*;
import ctd.util.BeanUtils;
import eh.utils.BeanCopyUtils;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IClinicCartBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.dao.ClinicCartDAO;
import recipe.dao.FastRecipeDAO;
import recipe.dao.FastRecipeDetailDAO;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.second.ClinicCartVO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 购物车核心业务类
 *
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Service
public class ClinicCartService implements IClinicCartBusinessService {

    @Autowired
    ClinicCartDAO clinicCartDAO;

    @Autowired
    FastRecipeDAO fastRecipeDAO;

    @Autowired
    FastRecipeDetailDAO fastRecipeDetailDAO;

    @Resource
    private IConfigurationCenterUtilsService configService;

    @Autowired
    private IStockBusinessService iStockBusinessService;

    /**
     * 方便门诊、便捷购药 购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    @Override
    public List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId, Integer workType) {
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByOrganIdAndUserId(organId, userId, workType);
        if (CollectionUtils.isNotEmpty(clinicCartList)) {
            List<ClinicCartVO> clinicCartVOS = BeanCopyUtils.copyList(clinicCartList, ClinicCartVO::new);
            if (!Integer.valueOf("2").equals(workType)) {
                return clinicCartVOS;
            }
            Object config = configService.getConfiguration(organId, "fastRecipeUsePlatStock");
            boolean fastRecipeUsePlatStock = Objects.nonNull(config) && (Boolean) config;
            Iterator<ClinicCartVO> it = clinicCartVOS.iterator();
            while (it.hasNext()) {
                ClinicCartVO clinicCartVO = it.next();
                FastRecipe fastRecipe = fastRecipeDAO.get(Integer.valueOf(clinicCartVO.getItemId()));
                List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipe.getId());

                if (!Integer.valueOf("1").equals(fastRecipe.getStatus())) {
                    it.remove();
                }  else {
                    if (fastRecipeUsePlatStock) {
                        // 获取最新库存值
                        clinicCartVO.setStockNum(fastRecipe.getStockNum());
                    } else {
                        // his库存
                        DrugQueryVO drugQueryVO = new DrugQueryVO();
                        drugQueryVO.setOrganId(organId);
                        drugQueryVO.setRecipeType(fastRecipe.getRecipeType());
                        RecipeDTO recipeDTO = this.recipeDTO(drugQueryVO, fastRecipeDetailList);
                        List<EnterpriseStock> result = iStockBusinessService.drugRecipeStock(recipeDTO, StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
                        if (CollectionUtils.isEmpty(result) || result.stream().noneMatch(EnterpriseStock::getStock)) {
                            clinicCartVO.setStockNum(0);
                        } else {
                            clinicCartVO.setStockNum(1);
                        }
                    }
                }
            }
            return clinicCartVOS;
        } else {
            return new ArrayList<>();
        }
    }

    private RecipeDTO recipeDTO(DrugQueryVO drugQueryVO, List<FastRecipeDetail> fastRecipeDetailList) {
        List<Recipedetail> detailList = new ArrayList<>();
        fastRecipeDetailList.forEach(fastRecipeDetail -> {
            Recipedetail recipedetail = ObjectCopyUtils.convert(fastRecipeDetail, Recipedetail.class);
            if (null != recipedetail && !recipe.util.ValidateUtil.integerIsEmpty(drugQueryVO.getPharmacyId())) {
                recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            }
            detailList.add(recipedetail);
        });
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(drugQueryVO.getOrganId());
        recipe.setRecipeType(drugQueryVO.getRecipeType());
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend.setDecoctionId(drugQueryVO.getDecoctionId());
        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setRecipe(recipe);
        recipeDTO.setRecipeDetails(detailList);
        recipeDTO.setRecipeExtend(recipeExtend);
        return recipeDTO;
    }


    /**
     * 方便门诊购物车列表新增
     *
     * @param clinicCartVO
     * @return
     */
    @Override
    public Integer addClinicCart(ClinicCartVO clinicCartVO) {
        ClinicCart clinicCart = BeanUtils.map(clinicCartVO, ClinicCart.class);
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByParam(clinicCartVO);
        if (CollectionUtils.isEmpty(clinicCartList)) {
            clinicCart.setDeleteFlag(0);
            ClinicCart result = clinicCartDAO.save(clinicCart);
            return result.getId();
        } else {
            return 0;
        }
    }

    /**
     * 方便门诊购物车列表删除
     *
     * @param ids
     * @return
     */
    @Override
    public Boolean deleteClinicCartByIds(List<Integer> ids) {
        clinicCartDAO.deleteClinicCartByIds(ids, 1);
        return true;
    }

    /**
     * 方便门诊购物车列表更新
     *
     * @param clinicCartVO
     * @return
     */
    @Override
    public Boolean updateClinicCartById(ClinicCartVO clinicCartVO) {
        ClinicCart clinicCart = clinicCartDAO.get(clinicCartVO.getId());
        if (ValidateUtil.nullOrZeroInteger(clinicCartVO.getAmount())) {
            return false;
        }
        if (Objects.nonNull(clinicCart)) {
            clinicCart.setAmount(clinicCartVO.getAmount());
            clinicCartDAO.update(clinicCart);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean deleteClinicCartByUserId(ClinicCartVO clinicCartVO) {
        clinicCartDAO.deleteClinicCartByUserId(clinicCartVO.getOrganId(), clinicCartVO.getUserId(), clinicCartVO.getWorkType());
        return true;
    }

}
