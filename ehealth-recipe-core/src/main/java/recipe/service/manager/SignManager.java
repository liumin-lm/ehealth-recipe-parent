package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipe.model.AttachSealPicDTO;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.util.FileAuth;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.CARecipeTypeConstant;
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
        String signImg = signImg(organId, giveUserId, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA);
        ApothecaryVO apothecaryVO = new ApothecaryVO();
        if (StringUtils.isNotEmpty(signImg)) {
            apothecaryVO.setGiveUserSignImg(signImg);
        } else {
            Recipe recipe = new Recipe();
            recipe.setRecipeId(recipeId);
            recipe.setGiveUser(giveUser);
            recipe.setClinicOrgan(organId);
            ApothecaryVO giveUserDefault = doctorClient.getGiveUserDefault(recipe);
            apothecaryVO.setGiveUserSignImg(giveUserDefault.getGiveUserSignImg());
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
    public AttachSealPicDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, Integer recipeId) {
        logger.info("SignManager attachSealPic param organId:{},doctorId:{},checker:{},recipeId:{}", organId, doctorId, checker, recipeId);
        AttachSealPicDTO attachSealPicDTO = new AttachSealPicDTO();
        attachSealPicDTO.setDoctorSignImg(signImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_DOC));
        attachSealPicDTO.setCheckerSignImg(signImg(organId, checker, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA));
        logger.info("SignManager attachSealPic attachSealPicDTO:{}", JSON.toJSONString(attachSealPicDTO));
        return attachSealPicDTO;
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
        logger.info("SignManager signImg param organId:{},doctorId:{},recipeId:{},recipeId:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId) || ValidateUtil.integerIsEmpty(doctorId) || ValidateUtil.integerIsEmpty(recipeId)) {
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
