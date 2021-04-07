package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.util.FileAuth;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.CARecipeTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.service.client.DoctorClient;
import recipe.service.client.IConfigurationClient;
import recipe.sign.SignRecipeInfoService;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

/**
 * 签名处理通用类
 *
 * @author fuzi
 */
@Service
public class SignManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 第三方手签
     */
    public static final String CA_SEAL_THIRD = "thirdSeal";
    /**
     * 线下手签
     */
    public static final String CA_SEAL_OFFLINE = "offlineSeal";
    /**
     * 平台手签
     */
    public static final String CA_SEAL_PLAT_FORM = "platFormSeal";

    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private SignRecipeInfoService signRecipeInfoService;
    @Autowired
    private DoctorClient doctorClient;

    /**
     * 获取 发药药师签名图片id
     *
     * @param organId  机构id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryVO giveUser(Integer organId, String giveUser, Integer recipeId) {
        logger.info("SignManager giveUser organId:{} giveUser:{} recipeId:{}", organId, giveUser, recipeId);
        Integer giveUserId = ByteUtils.strValueOf(giveUser);
        ApothecaryVO apothecaryVO;
        if (!ValidateUtil.integerIsEmpty(giveUserId)) {
            apothecaryVO = giveUser(organId, giveUserId, recipeId);
        } else {
            DoctorDTO doctorDTO = doctorClient.oragnDefaultDispensingApothecary(organId);
            apothecaryVO = giveUser(organId, doctorDTO.getDoctorId(), recipeId);
        }
        if (StringUtils.isNotEmpty(apothecaryVO.getGiveUserSignImg())) {
            String giveUserSignImgToken = FileAuth.instance().createToken(apothecaryVO.getGiveUserSignImg(), 3600L);
            apothecaryVO.setGiveUserSignImgToken(giveUserSignImgToken);
        }
        logger.info("SignManager giveUser attachSealPicDTO:{}", JSON.toJSONString(apothecaryVO));
        return apothecaryVO;
    }


    /**
     * 根据配置项sealDataFrom获取签章图片
     *
     * @param doctorId
     * @param
     * @Author liumin
     */
    public ApothecaryVO attachSealPic(Integer organId, Integer doctorId, Integer checker, Integer recipeId) {
        logger.info("SignManager attachSealPic param organId:{},doctorId:{},checker:{},recipeId:{}", organId, doctorId, checker, recipeId);
        ApothecaryVO attachSealPicDTO = new ApothecaryVO();
        attachSealPicDTO.setRecipeId(recipeId);
        attachSealPicDTO.setDoctorSignImg(signImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_DOC));
        if (StringUtils.isNotEmpty(attachSealPicDTO.getDoctorSignImg())) {
            attachSealPicDTO.setDoctorId(doctorId);
            attachSealPicDTO.setDoctorSignImgToken(FileAuth.instance().createToken(attachSealPicDTO.getDoctorSignImg(), 3600L));
        }
        attachSealPicDTO.setCheckerSignImg(signImg(organId, checker, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA));
        if (StringUtils.isNotEmpty(attachSealPicDTO.getCheckerSignImg())) {
            attachSealPicDTO.setCheckerId(checker);
            attachSealPicDTO.setCheckerSignImgToken(FileAuth.instance().createToken(attachSealPicDTO.getCheckerSignImg(), 3600L));
        }
        logger.info("SignManager attachSealPic attachSealPicDTO:{}", JSON.toJSONString(attachSealPicDTO));
        return attachSealPicDTO;
    }

    @Autowired
    private RecipeDAO recipeDAO;

    /**
     * 签名图片取值规则：根据运营平台-机构配置里面"处方单和处方笺签名取值配置"来定，拿不到在拿平台
     *
     * @param organId  机构id
     * @param doctorId 药师id
     * @param recipeId 处方id
     * @return
     */
    private ApothecaryVO giveUser(Integer organId, Integer doctorId, Integer recipeId) {
        ApothecaryVO apothecaryVO = new ApothecaryVO();
        Recipe recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setGiveUser(doctorId.toString());
        recipe.setClinicOrgan(organId);
        //todo 第三方ca特殊处理  等CA在表中增加doctorId后修改
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            Recipe recipeChecker = recipeDAO.getByRecipeId(recipeId);
            if (null == recipeChecker.getChecker() || !recipeChecker.getChecker().equals(doctorId)) {
                ApothecaryVO giveUserDefault = doctorClient.getGiveUser(recipe);
                apothecaryVO.setGiveUserSignImg(giveUserDefault.getGiveUserSignImg());
                return apothecaryVO;
            }
        }
        //获取签名图片
        String signImg = signImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA);
        if (StringUtils.isNotEmpty(signImg)) {
            apothecaryVO.setGiveUserSignImg(signImg);
        } else {
            ApothecaryVO giveUserDefault = doctorClient.getGiveUser(recipe);
            apothecaryVO.setGiveUserSignImg(giveUserDefault.getGiveUserSignImg());
        }
        return apothecaryVO;
    }


    /**
     * 获取手签图片
     *
     * @param organId  机构id
     * @param doctorId 医生/药师id
     * @param recipeId 处方id
     * @param type     职业类型：医生/药师
     * @return
     */
    private String signImg(Integer organId, Integer doctorId, Integer recipeId, Integer type) {
        logger.info("SignManager signImg param organId:{},doctorId:{},recipeId:{},type:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId)) {
            return null;
        }
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            return thirdSeal(recipeId, type);
        } else if (CA_SEAL_OFFLINE.equals(sealDataFrom)) {
            return offlineSeal(doctorId);
        } else {
            //平台手签
            return doctorClient.getDoctor(doctorId).getSignImage();
        }
    }

    /**
     * 获取第三方手签图片
     *
     * @param recipeId 处方id
     * @return
     */
    private String thirdSeal(Integer recipeId, Integer type) {
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type)) {
            logger.info("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type);
            return null;
        }
        try {
            SignDoctorRecipeInfo docInfo = signRecipeInfoService.getSignInfoByRecipeIdAndServerType(recipeId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
            return null;
        }
    }

    /**
     * 如果线上处方设置成线下手签
     *
     * @param doctorId 医生/药师id
     * @return
     */
    private String offlineSeal(Integer doctorId) {
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("SignManager offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId);
            return null;
        }
        try {
            String signImgId = signRecipeInfoService.getOfflineCaPictureByDocId(doctorId);
            if (StringUtils.isEmpty(signImgId)) {
                return null;
            }
            return signImgId;
        } catch (Exception e) {
            logger.warn("SignManager offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId, e);
            return null;
        }
    }

}
