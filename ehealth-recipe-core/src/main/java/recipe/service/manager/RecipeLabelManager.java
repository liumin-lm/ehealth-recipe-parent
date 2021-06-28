package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.drugsenterprise.model.RecipeLabelVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.persistence.exception.DAOException;
import eh.entity.base.Scratchable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.bussutil.openapi.request.province.SignImgNode;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.comment.DictionaryUtil;
import recipe.constant.CacheConstant;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeOrderDAO;
import recipe.util.ByteUtils;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import java.util.*;

/**
 * 处方签
 *
 * @author fuzi
 */
@Service
public class RecipeLabelManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 运营平台机构配置，处方签配置， 特殊字段替换展示
     */
    private final static List<String> CONFIG_STRING = Arrays.asList("recipeDetailRemark");

    @Autowired
    private IScratchableService scratchableService;
    @Autowired
    private IConfigurationCenterUtilsService configService;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private SignManager signManager;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;

    /**
     * 更新 pdf 核对发药
     *
     * @param recipe 处方
     * @return
     */
    public Recipe giveUserUpdate(Recipe recipe) {
        logger.info("RecipeLabelManager giveUserUpdate recipe={}", JSON.toJSONString(recipe));
        //获取 核对发药药师签名id
        ApothecaryVO apothecaryVO = signManager.giveUser(recipe.getClinicOrgan(), recipe.getGiveUser(), recipe.getRecipeId());
        //判断发药状态
        if (StringUtils.isEmpty(recipe.getOrderCode())) {
            return null;
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (null == recipeOrder || null == recipeOrder.getDispensingTime()) {
            return null;
        }
        //判断配置是否有核对发药
        List<Scratchable> scratchableList = scratchableList(recipe.getClinicOrgan(), "moduleFour");
        if (CollectionUtils.isEmpty(scratchableList)) {
            return null;
        }
        boolean isGiveUser = scratchableList.stream().noneMatch(a -> "recipe.giveUser".equals(a.getBoxLink()));
        if (isGiveUser) {
            return null;
        }
        //修改pdf文件
        SignImgNode signImgNode = new SignImgNode(recipe.getRecipeId().toString(), recipe.getRecipeId().toString()
                , apothecaryVO.getGiveUserSignImg(), null, 50f, 20f, 210f, 99f, true);
        Recipe recipeUpdate = new Recipe();
        if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
            signImgNode.setSignFileFileId(recipe.getChemistSignFile());
            String newPfd = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isEmpty(newPfd)) {
                return null;
            }
            recipeUpdate.setChemistSignFile(newPfd);
        } else if (StringUtils.isNotEmpty(recipe.getSignFile())) {
            signImgNode.setSignFileFileId(recipe.getSignFile());
            String newPfd = CreateRecipePdfUtil.generateSignImgNode(signImgNode);
            if (StringUtils.isEmpty(newPfd)) {
                return null;
            }
            recipeUpdate.setSignFile(newPfd);
        }
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        logger.info("RecipeLabelManager giveUserUpdate recipeUpdate={}", JSON.toJSONString(recipeUpdate));
        return recipeUpdate;
    }

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
     * 获取pdf 特殊字段坐标
     *
     * @param recipeId   处方id
     * @param coordsName 特殊字段名称
     * @return
     */
    public CoOrdinateVO getPdfCoordsHeight(Integer recipeId, String coordsName) {
        if (ValidateUtil.integerIsEmpty(recipeId) || StringUtils.isEmpty(coordsName)) {
            return null;
        }
        List<CoOrdinateVO> coOrdinateList = redisClient.getList(CacheConstant.KEY_RECIPE_LABEL + recipeId.toString());
        logger.info("RecipeLabelManager getPdfReceiverHeight recipeId={}，coOrdinateList={}", recipeId, JSONUtils.toString(coOrdinateList));

        if (CollectionUtils.isEmpty(coOrdinateList)) {
            logger.error("RecipeLabelManager getPdfReceiverHeight recipeId为空 recipeId={}", recipeId);
            return null;
        }

        for (CoOrdinateVO coOrdinate : coOrdinateList) {
            if (coordsName.equals(coOrdinate.getName())) {
                coOrdinate.setY(499 - coOrdinate.getY());
                coOrdinate.setX(coOrdinate.getX() + 5);
                return coOrdinate;
            }
        }
        return null;
    }



    /**
     * 获取处方签 配置 给前端展示。
     * 获取处方信息，获取运营平台配置，替换运营平台配置字段值，返回对象给前端展示
     *
     * @param recipeMap 处方
     * @param organId   机构id
     * @return
     */
    public Map<String, List<RecipeLabelVO>> queryRecipeLabelById(Integer organId, Map<String, Object> recipeMap) {
        logger.info("RecipeLabelManager queryRecipeLabelById ,organId={}", organId);
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构id为空");
        }
        Map<String, Object> labelMap = scratchableService.findRecipeListDetail(organId.toString());
        if (CollectionUtils.isEmpty(labelMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "运营平台配置为空");
        }
        //处理特殊字段
        setRecipeMap(recipeMap, (List<Scratchable>) labelMap.get("moduleFive"));

        Map<String, List<RecipeLabelVO>> resultMap = new HashMap<>();
        labelMap.forEach((k, v) -> {
            List<Scratchable> value = (List<Scratchable>) v;
            if (CollectionUtils.isEmpty(value)) {
                logger.warn("RecipeLabelManager queryRecipeLabelById value is null k={} ", k);
                return;
            }
            try {
                List<RecipeLabelVO> list = getValue(value, recipeMap, organId);
                resultMap.put(k, list);
            } catch (Exception e) {
                logger.error("RecipeLabelManager queryRecipeLabelById error  value ={} recipeMap={}", JSONUtils.toString(value), JSONUtils.toString(recipeMap), e);
            }
        });
        logger.info("RecipeLabelManager queryRecipeLabelById resultMap={}", JSONUtils.toString(resultMap));
        return resultMap;
    }


    /**
     * 根据运营平台配置 组织字段值对象
     * todo 暂时需求如此 仅支持固定格式解析 （patient.patientName 这种一层对象解析）
     *
     * @param scratchableList
     * @param recipeMap
     * @return
     */
    private List<RecipeLabelVO> getValue(List<Scratchable> scratchableList, Map<String, Object> recipeMap, Integer organId) {
        List<RecipeLabelVO> recipeLabelList = new LinkedList<>();
        scratchableList.forEach(a -> {
            if (StringUtils.isEmpty(a.getBoxLink())) {
                return;
            }

            String boxLink = a.getBoxLink().trim();
            /**根据模版匹配 value*/
            Object value = recipeMap.get(boxLink);
            if (CONFIG_STRING.contains(boxLink)) {
                value = configService.getConfiguration(organId, boxLink);
                if (null == value) {
                    return;
                }
            }

            if (null == value) {
                //对象获取字段
                String[] boxLinks = boxLink.split(ByteUtils.DOT);
                Object key = recipeMap.get(boxLinks[0]);
                if (2 == boxLinks.length && null != key) {
                    if (key instanceof List && !CollectionUtils.isEmpty((List) key)) {
                        key = ((List) key).get(0);
                    }
                    value = MapValueUtil.getFieldValueByName(boxLinks[1], key);
                } else {
                    logger.warn("RecipeLabelManager getValue boxLinks ={}", JSONUtils.toString(boxLinks));
                }
            }

            //组织返回对象
            recipeLabelList.add(new RecipeLabelVO(a.getBoxTxt(), boxLink, value));
        });
        return recipeLabelList;
    }

    /**
     * 处理特殊模版匹配规则
     *
     * @param recipeMap
     */
    private void setRecipeMap(Map<String, Object> recipeMap, List<Scratchable> list) {
        if (CollectionUtils.isEmpty(recipeMap)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeMap is null!");
        }
        //处理性别转化
        PatientDTO patientDTO = (PatientDTO) recipeMap.get("patient");
        if (null != patientDTO && StringUtils.isNotEmpty(patientDTO.getPatientSex())) {
            patientDTO.setPatientSex(DictionaryUtil.getDictionary("eh.base.dictionary.Gender", String.valueOf(patientDTO.getPatientSex())));
        }
        RecipeBean recipeBean = (RecipeBean) recipeMap.get("recipe");
        //药品金额
        RecipeOrder recipeOrder = recipeOrderDAO.getRecipeOrderByRecipeId(recipeBean.getRecipeId());
        if (null != recipeOrder && null != recipeOrder.getRecipeFee()) {
            recipeBean.setActualPrice(recipeOrder.getRecipeFee());
        }
        /**签名字段替换*/
        //医生签名图片
        ApothecaryVO doctor = new ApothecaryVO();
        doctor.setDoctorSignImg(ByteUtils.objValueOf(recipeMap.get("doctorSignImg")));
        doctor.setDoctorSignImgToken(ByteUtils.objValueOf(recipeMap.get("doctorSignImgToken")));
        recipeMap.put("doctorSignImg,doctorSignImgToken", JSON.toJSONString(doctor));
        //审方药师签名图片
        ApothecaryVO checker = new ApothecaryVO();
        checker.setCheckerSignImg(ByteUtils.objValueOf(recipeMap.get("checkerSignImg")));
        checker.setCheckerSignImgToken(ByteUtils.objValueOf(recipeMap.get("checkerSignImgToken")));
        checker.setCheckApothecaryName(ByteUtils.objValueOf(recipeBean.getCheckerText()));
        recipeMap.put("checkerSignImg,checkerSignImgToken", JSON.toJSONString(checker));
        //核发药师签名图片
        if (null != recipeOrder && null != recipeOrder.getDispensingTime()) {
            ApothecaryVO apothecaryVO = signManager.giveUser(recipeBean.getClinicOrgan(), recipeBean.getGiveUser(), recipeBean.getRecipeId());
            recipeBean.setGiveUser(JSON.toJSONString(apothecaryVO));
        }
        //机构名称替换
        if (!CollectionUtils.isEmpty(list)) {
            for (Scratchable scratchable : list) {
                if ("recipe.organName".equals(scratchable.getBoxLink()) && StringUtils.isNotEmpty(scratchable.getBoxDesc())) {
                    recipeBean.setOrganName(scratchable.getBoxDesc());
                    break;
                }
            }
        }
    }

}
