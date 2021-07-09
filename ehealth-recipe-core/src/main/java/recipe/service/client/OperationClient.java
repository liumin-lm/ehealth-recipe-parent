package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.persistence.exception.DAOException;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.bean.RecipeInfoDTO;
import recipe.constant.ErrorCode;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 运营平台信息处理类
 *
 * @author fuzi
 */
@Service
public class OperationClient extends BaseClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 运营平台机构配置，处方签配置， 特殊字段替换展示
     */
    @Autowired
    private IScratchableService scratchableService;
    @Autowired
    private IConfigurationCenterUtilsService configService;


    /**
     * 获取处方签配置模块
     *
     * @param organId    机构id
     * @param moduleName 模块名称
     * @return
     */
    public List<Scratchable> scratchableList(Integer organId, String moduleName) {
        Map<String, Object> labelMap = scratchableService.findRecipeListDetail(organId.toString());
        if (CollectionUtils.isEmpty(labelMap)) {
            return new LinkedList<>();
        }
        return (List<Scratchable>) labelMap.get(moduleName);
    }

    /**
     * 获取运营平台页面配置转换 值方法
     *
     * @param recipePdfDTO 处方信息
     * @return
     */
    public Map<String, List<RecipeLabelVO>> queryRecipeLabel(RecipeInfoDTO recipePdfDTO) {
        logger.info("OperationClient queryRecipeLabel ,recipePdfDTO={}", JSON.toJSONString(recipePdfDTO));
        Integer organId = recipePdfDTO.getRecipe().getClinicOrgan();
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构id为空");
        }
        Map<String, Object> labelMap = scratchableService.findRecipeListDetail(organId.toString());
        logger.info("OperationClient queryRecipeLabel labelMap={}", JSON.toJSONString(labelMap));
        if (CollectionUtils.isEmpty(labelMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "运营平台配置为空");
        }
        Map<String, List<RecipeLabelVO>> resultMap = new HashMap<>();
        labelMap.forEach((k, v) -> {
            List<Scratchable> value = (List<Scratchable>) v;
            if (CollectionUtils.isEmpty(value)) {
                return;
            }
            List<RecipeLabelVO> recipeLabelList = new LinkedList<>();
            value.forEach(a -> {
                if (StringUtils.isEmpty(a.getBoxLink())) {
                    return;
                }
                Object fieldValue = getFieldValue(a, recipePdfDTO);
                //组织返回对象
                recipeLabelList.add(new RecipeLabelVO(a.getBoxTxt(), a.getBoxLink(), fieldValue));
            });
            resultMap.put(k, recipeLabelList);
        });
        logger.info("OperationClient queryRecipeLabel resultMap={}", JSON.toJSONString(resultMap));
        return resultMap;
    }


    private Object getFieldValue(Scratchable scratchable, RecipeInfoDTO recipePdfDTO) {
        if (StringUtils.isEmpty(scratchable.getBoxLink())) {
            return "";
        }
        String[] boxLink = scratchable.getBoxLink().trim().split(ByteUtils.DOT);
        //对象字段处理
        if (1 == boxLink.length) {
            String fieldName = boxLink[0];
            return MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO);
        }
        //对象字段处理
        if (2 == boxLink.length) {
            String objectName = boxLink[0];
            String fieldName = boxLink[1];
            return invokeFieldName(objectName, fieldName, recipePdfDTO, scratchable.getBoxDesc());
        }
        //特殊节点处理
        if (3 == boxLink.length) {
            String identifyName = boxLink[0];
            String objectName = boxLink[1];
            String fieldName = boxLink[2];
            //条形码
            if ("barCode".equals(identifyName)) {
                return invokeFieldName(objectName, fieldName, recipePdfDTO, null);
            }
            //二维码
            if ("qrCode".equals(identifyName)) {
                return invokeFieldName(objectName, fieldName, recipePdfDTO, null);
            }
        }
        return "";
    }

    private String invokeFieldName(String objectName, String fieldName, RecipeInfoDTO recipePdfDTO, String def) {
        if ("patient".equals(objectName)) {
            return MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getPatientBean());
        }
        if ("recipeExtend".equals(objectName)) {
            return MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getRecipeExtend());
        }
        if ("recipeDetail".equals(objectName)) {
            List<Recipedetail> recipeDetails = recipePdfDTO.getRecipeDetails();
            if (!CollectionUtils.isEmpty(recipeDetails)) {
                return MapValueUtil.getFieldValueByName(fieldName, recipeDetails.get(0));
            }
        }
        if ("recipe".equals(objectName)) {
            if ("organName".equals(fieldName) && StringUtils.isNotEmpty(def)) {
                return def;
            }
            if ("recipeMemo".equals(fieldName)) {
                Object recipeDetailRemark = configService.getConfiguration(recipePdfDTO.getRecipe().getClinicOrgan(), "recipeDetailRemark");
                if (!ObjectUtils.isEmpty(recipeDetailRemark)) {
                    return recipeDetailRemark.toString();
                }
            }
            RecipeOrder recipeOrder = recipePdfDTO.getRecipeOrder();
            if ("actualPrice".equals(fieldName) && null != recipeOrder && null != recipeOrder.getRecipeFee()) {
                return recipeOrder.getRecipeFee().toString();
            }
            ApothecaryVO apothecaryVO = recipePdfDTO.getApothecary();
            //医生签名图片
            if ("doctor".equals(fieldName) && StringUtils.isNotEmpty(apothecaryVO.getDoctorSignImg())) {
                ApothecaryVO doctor = new ApothecaryVO();
                doctor.setDoctorSignImg(apothecaryVO.getDoctorSignImg());
                doctor.setDoctorSignImgToken(apothecaryVO.getDoctorSignImgToken());
                doctor.setDoctorId(apothecaryVO.getDoctorId());
                return JSON.toJSONString(doctor);
            }
            //审方药师签名图片
            if ("checker".equals(fieldName) && StringUtils.isNotEmpty(apothecaryVO.getCheckerSignImg())) {
                ApothecaryVO checker = new ApothecaryVO();
                checker.setCheckerSignImg(apothecaryVO.getCheckerSignImg());
                checker.setCheckerSignImgToken(apothecaryVO.getCheckerSignImgToken());
                checker.setCheckApothecaryName(apothecaryVO.getCheckApothecaryName());
                return JSON.toJSONString(checker);
            }
            //核发药师签名图片
            if ("giveUser".equals(fieldName) && StringUtils.isNotEmpty(apothecaryVO.getGiveUserSignImg())) {
                ApothecaryVO giveUser = new ApothecaryVO();
                giveUser.setGiveUserSignImg(apothecaryVO.getGiveUserSignImg());
                giveUser.setGiveUserSignImgToken(apothecaryVO.getGiveUserSignImgToken());
                giveUser.setGiveUserName(apothecaryVO.getGiveUserName());
                giveUser.setGiveUserId(apothecaryVO.getGiveUserId());
                return JSON.toJSONString(giveUser);
            }
            return MapValueUtil.getFieldValueByName(fieldName, recipePdfDTO.getRecipe());
        }
        return "";
    }
}
