package recipe.bussutil.drugdisplay;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.OrganDrugListDAO;

import java.util.List;

/**
 * created by shiyuping on 2021/3/12
 */
public class DrugNameDisplayUtil {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugNameDisplayUtil.class);

    private static IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);

    public static String[] getDrugNameConfigByDrugType(Integer organId, Integer drugType) {
        if (organId != null) {
            String drugNameConfigKey = getDrugNameConfigKey(drugType);
            if (StringUtils.isNotEmpty(drugNameConfigKey)) {
                String[] config = (String[]) configurationService.getConfiguration(organId, drugNameConfigKey);
                LOGGER.info("getDrugNameConfig organId:{},drugType:{},config:{}", organId, drugType, JSONUtils.toString(config));
                return config;
            }
        }
        return null;
    }

    public static String[] getSaleNameConfigByDrugType(Integer organId, Integer drugType) {
        if (organId != null) {
            String saleNameConfigKey = getSaleNameConfigKey(drugType);
            if (StringUtils.isNotEmpty(saleNameConfigKey)) {
                String[] config = (String[]) configurationService.getConfiguration(organId, saleNameConfigKey);
                LOGGER.info("getSaleNameConfig organId:{},drugType:{},config:{}", organId, drugType, JSONUtils.toString(config));
                return config;
            }
        }
        return null;
    }


    public static String getDrugNameConfigKey(Integer drugType) {
        if (drugType != null) {
            switch (drugType) {
                //西药/中成药
                case 1:
                case 2:
                    return DisplayNameEnum.WM_DRUGNAME.getConfigKey();
                //中药
                case 3:
                    return DisplayNameEnum.TCM_DRUGNAME.getConfigKey();
                default:
                    return null;
            }
        }
        return null;
    }

    public static String getSaleNameConfigKey(Integer drugType) {
        if (drugType != null) {
            switch (drugType) {
                //西药/中成药
                case 1:
                case 2:
                    return DisplayNameEnum.WM_SALENAME.getConfigKey();
                default:
                    return null;
            }
        }
        return null;
    }

    public static String dealwithRecipedetailName(List<OrganDrugList> organDrugLists, Recipedetail recipedetail, Integer drugType) {
        StringBuilder stringBuilder = new StringBuilder();
        /*if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            //所有页面中药药品显示统一“药品名称”和“剂量单位”以空格间隔
            stringBuilder.append(recipedetail.getDrugName()).append(StringUtils.SPACE);
            if (StringUtils.isNotEmpty(recipedetail.getUseDoseStr())) {
                stringBuilder.append(recipedetail.getUseDoseStr());
            } else {
                stringBuilder.append(recipedetail.getUseDose());
            }
            stringBuilder.append(recipedetail.getUseDoseUnit());
            if (StringUtils.isNotEmpty(recipedetail.getMemo())) {
                stringBuilder.append("(").append(recipedetail.getMemo()).append(")");
            }
        } else {*/
        if (CollectionUtils.isEmpty(organDrugLists)) {
            stringBuilder.append(recipedetail.getDrugName());
        } else {
            //机构药品名称、剂型、药品规格、单位
            stringBuilder.append(organDrugLists.get(0).getDrugName());
            if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                stringBuilder.append(organDrugLists.get(0).getDrugForm());
            }
        }
        //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
        stringBuilder.append(StringUtils.SPACE);
        stringBuilder.append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit());
        //}
        return stringBuilder.toString();
    }

    public static String dealwithRecipedetailSaleName(List<OrganDrugList> organDrugLists, Recipedetail recipedetail, Integer drugType) {
        StringBuilder stringBuilder = new StringBuilder();
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            stringBuilder.append("/");
        } else {
            if (CollectionUtils.isEmpty(organDrugLists)) {
                stringBuilder.append(StringUtils.isEmpty(recipedetail.getSaleName()) ? "/" : recipedetail.getSaleName());
            } else {
                //机构药品名称、剂型、药品规格、单位
                stringBuilder.append(StringUtils.isEmpty(recipedetail.getSaleName()) ? organDrugLists.get(0).getSaleName() : recipedetail.getSaleName());
                if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                    stringBuilder.append(organDrugLists.get(0).getDrugForm());
                }
            }
        }

        return stringBuilder.toString();
    }

    public static String dealwithCommonDrugName(CommonRecipeDrugDTO item, Integer drugType) {
        StringBuilder stringBuilder = new StringBuilder();
        /*if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            //所有页面中药药品显示统一“药品名称”和“剂量单位”以空格间隔
            stringBuilder.append(item.getDrugName()).append(StringUtils.SPACE);
            if (StringUtils.isNotEmpty(item.getUseDoseStr())) {
                stringBuilder.append(item.getUseDoseStr());
            } else {
                stringBuilder.append(item.getUseDose());
            }
            stringBuilder.append(item.getUseDoseUnit());
            if (StringUtils.isNotEmpty(item.getMemo())) {
                stringBuilder.append("(").append(item.getMemo()).append(")");
            }
        } else {*/
        //机构药品名称、剂型、药品规格、单位
        stringBuilder.append(item.getDrugName());
        if (StringUtils.isNotEmpty(item.getDrugForm())) {
            stringBuilder.append(item.getDrugForm());
        }
        //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
        stringBuilder.append(StringUtils.SPACE);
        stringBuilder.append(item.getDrugSpec()).append("/").append(item.getDrugUnit());
        //}
        return stringBuilder.toString();
    }

    public static String dealwithCommonDrugSaleName(CommonRecipeDrugDTO item, Integer drugType) {
        StringBuilder stringBuilder = new StringBuilder();
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            stringBuilder.append("/");
        } else {
            //机构药品名称、剂型、药品规格、单位
            stringBuilder.append(StringUtils.isEmpty(item.getSaleName()) ? "/" : item.getSaleName());
            if (StringUtils.isNotEmpty(item.getDrugForm())) {
                stringBuilder.append(item.getDrugForm());
            }
        }
        return stringBuilder.toString();
    }


    /**
     * 后台处理药品显示名---卡片消息/处方笺/处方列表页第一个药名/电子病历详情
     *
     * @param recipedetail
     * @param drugType
     * @return
     */
    public static String dealwithRecipeDrugName(Recipedetail recipedetail, Integer drugType, Integer organId) {
        StringBuilder stringBuilder = new StringBuilder();
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(drugType)) {
            //所有页面中药药品显示统一“药品名称”和“剂量单位”以空格间隔
            stringBuilder.append(recipedetail.getDrugName()).append(StringUtils.SPACE);
            if (StringUtils.isNotEmpty(recipedetail.getUseDoseStr())) {
                stringBuilder.append(recipedetail.getUseDoseStr());
            } else {
                stringBuilder.append(recipedetail.getUseDose());
            }
            stringBuilder.append(recipedetail.getUseDoseUnit());
            if (StringUtils.isNotEmpty(recipedetail.getMemo())) {
                stringBuilder.append("(").append(recipedetail.getMemo()).append(")");
            }
        } else {
            if (StringUtils.isEmpty(recipedetail.getDrugDisplaySplicedName())) {
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndOrganDrugCodeAndDrugIdWithoutStatus(organId, recipedetail.getOrganDrugCode(), recipedetail.getDrugId());
                if (CollectionUtils.isEmpty(organDrugLists)) {
                    stringBuilder.append(recipedetail.getDrugName());
                } else {
                    //机构药品名称、剂型、药品规格、单位
                    stringBuilder.append(organDrugLists.get(0).getDrugName());
                    if (StringUtils.isNotEmpty(organDrugLists.get(0).getDrugForm())) {
                        stringBuilder.append(organDrugLists.get(0).getDrugForm());
                    }
                }
                //【"机构药品名称”、“机构商品名称”、“剂型”】与【“药品规格”、“单位”】中间要加空格
                stringBuilder.append(StringUtils.SPACE);
                stringBuilder.append(recipedetail.getDrugSpec()).append("/").append(recipedetail.getDrugUnit());
            } else {
                return recipedetail.getDrugDisplaySplicedName();
            }
        }
        return stringBuilder.toString();
    }
}
