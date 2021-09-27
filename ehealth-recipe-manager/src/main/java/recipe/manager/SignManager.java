package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.util.FileAuth;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.DoctorClient;
import recipe.client.IConfigurationClient;
import recipe.constant.CARecipeTypeConstant;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 签名处理通用类
 *
 * @author fuzi
 */
@Service
public class SignManager extends BaseManager {
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
    private DoctorClient doctorClient;
    @Autowired
    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;

    /**
     * 获取全部药师签名信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param checker  审方药师id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, String giveUser, Integer recipeId) {
        ApothecaryDTO apothecaryDTO = attachSealPic(organId, doctorId, checker, recipeId);
        ApothecaryDTO giveUserApothecary = giveUser(organId, giveUser, recipeId);
        apothecaryDTO.setGiveUserId(giveUserApothecary.getGiveUserId());
        apothecaryDTO.setGiveUserName(giveUserApothecary.getGiveUserName());
        apothecaryDTO.setGiveUserSignImg(giveUserApothecary.getGiveUserSignImg());
        apothecaryDTO.setGiveUserSignImgToken(giveUserApothecary.getGiveUserSignImgToken());
        return apothecaryDTO;
    }

    /**
     * 获取医生药师签名信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param checker  审方药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, Integer recipeId) {
        logger.info("SignManager attachSealPic param organId:{},doctorId:{},checker:{},recipeId:{}", organId, doctorId, checker, recipeId);
        ApothecaryDTO attachSealPicDTO = new ApothecaryDTO();
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


    /**
     * 获取 发药药师签名图片id
     *
     * @param organId  机构id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO giveUser(Integer organId, String giveUser, Integer recipeId) {
        logger.info("SignManager giveUser organId:{} giveUser:{} recipeId:{}", organId, giveUser, recipeId);
        Integer giveUserId = ByteUtils.strValueOf(giveUser);
        ApothecaryDTO apothecaryDTO;
        if (!ValidateUtil.integerIsEmpty(giveUserId)) {
            apothecaryDTO = giveUser(organId, giveUserId, recipeId);
        } else {
            DoctorDTO doctorDTO = doctorClient.oragnDefaultDispensingApothecary(organId);
            apothecaryDTO = giveUser(organId, doctorDTO.getDoctorId(), recipeId);
        }
        if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserSignImg())) {
            String giveUserSignImgToken = FileAuth.instance().createToken(apothecaryDTO.getGiveUserSignImg(), 3600L);
            apothecaryDTO.setGiveUserSignImgToken(giveUserSignImgToken);
        }
        logger.info("SignManager giveUser attachSealPicDTO:{}", JSON.toJSONString(apothecaryDTO));
        return apothecaryDTO;
    }

    /**
     * 签名图片取值规则：根据运营平台-机构配置里面"处方单和处方笺签名取值配置"来定，拿不到在拿平台
     *
     * @param organId  机构id
     * @param doctorId 药师id
     * @param recipeId 处方id
     * @return
     */
    private ApothecaryDTO giveUser(Integer organId, Integer doctorId, Integer recipeId) {
        ApothecaryDTO apothecaryDTO = new ApothecaryDTO();
        apothecaryDTO.setGiveUserId(doctorId);
        Recipe recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setGiveUser(ByteUtils.objValueOf(doctorId));
        recipe.setClinicOrgan(organId);
        //获取签名图片
        String signImg = getSignImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA);
        if (StringUtils.isNotEmpty(signImg)) {
            apothecaryDTO.setGiveUserSignImg(signImg);
        } else {
            ApothecaryDTO giveUserDefault = doctorClient.getGiveUser(recipe);
            apothecaryDTO.setGiveUserSignImg(giveUserDefault.getGiveUserSignImg());
        }
        return apothecaryDTO;
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
        if (new Integer(CARecipeTypeConstant.CA_RECIPE_PHA).equals(type)) {
            Recipe recipe = recipeDAO.get(recipeId);
            if (recipe == null) {
                return null;
            }
            if (new Integer(5).equals(recipe.getCheckMode())) {
                return thirdSeal(recipeId, type);
            }
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
     * 获取手签图片
     *
     * @param organId  机构id
     * @param doctorId 医生/药师id
     * @param recipeId 处方id
     * @param type     职业类型：医生/药师
     * @return
     */
    private String getSignImg(Integer organId, Integer doctorId, Integer recipeId, Integer type) {
        logger.info("SignManager giveUser param organId:{},doctorId:{},recipeId:{},type:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId)) {
            return null;
        }
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            return thirdSealV1(recipeId, doctorId, type);
        } else if (CA_SEAL_OFFLINE.equals(sealDataFrom)) {
            return offlineSeal(doctorId);
        } else {
            //平台手签
            return doctorClient.getDoctor(doctorId).getSignImage();
        }
    }


    /**
     * todo 新方法：thirdSealV1
     * 获取第三方手签图片
     *
     * @param recipeId 处方id
     * @return
     */
    @Deprecated
    private String thirdSeal(Integer recipeId, Integer type) {
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type)) {
            logger.info("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type);
            return null;
        }
        try {
            SignDoctorRecipeInfo docInfo = getSignInfoByRecipeIdAndServerType(recipeId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
            return null;
        }
    }

    private String thirdSealV1(Integer recipeId, Integer doctorId, Integer type) {
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type) || ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("SignManager thirdSealV1 使用第三方签名，recipeId:{},type:{},doctorId:{}", recipeId, type, doctorId);
            return null;
        }
        try {
            SignDoctorRecipeInfo docInfo = getSignInfoByRecipeIdAndDoctorId(recipeId, doctorId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("SignManager thirdSealV1 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
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
            String signImgId = doctorClient.getOfflineCaPictureByDocId(doctorId);
            logger.info("SignManager offlineSeal 如果线上处方设置成线下手签，signImgId:{},doctorId:{}", signImgId, doctorId);
            if (StringUtils.isEmpty(signImgId)) {
                return null;
            }
            return signImgId;
        } catch (Exception e) {
            logger.warn("SignManager offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId, e);
            return null;
        }
    }


    private SignDoctorRecipeInfo getSignInfoByRecipeIdAndServerType(Integer recipeId, Integer serverType) {
        List<SignDoctorRecipeInfo> resultList = signDoctorRecipeInfoDAO.findRecipeInfoByRecipeIdAndServerType(recipeId, serverType);
        if (CollectionUtils.isNotEmpty(resultList)) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    private SignDoctorRecipeInfo getSignInfoByRecipeIdAndDoctorId(Integer recipeId, Integer doctorId, Integer serverType) {
        List<SignDoctorRecipeInfo> resultList = signDoctorRecipeInfoDAO.findSignInfoByRecipeIdAndDoctorId(recipeId, doctorId, serverType);
        if (CollectionUtils.isNotEmpty(resultList)) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

}
