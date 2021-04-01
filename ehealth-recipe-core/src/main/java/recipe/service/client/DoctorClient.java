package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipeorder.model.ApothecaryVO;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ByteUtils;
import recipe.util.ValidateUtil;

/**
 * 获取医生信息处理类
 *
 * @author fuzi
 */
@Service
public class DoctorClient extends BaseClient {
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private IConfigurationClient configurationClient;

    /**
     * 获取平台药师信息 用于前端展示：无选中发药药师 则获取默认发药药师
     *
     * @param recipe 处方
     * @return
     */
    public ApothecaryVO getApothecary(Integer organId, Recipe recipe) {
        ApothecaryVO apothecaryVO = getApothecary(recipe);
        if (StringUtils.isNotEmpty(apothecaryVO.getGiveUserName())) {
            return apothecaryVO;
        }
        DoctorDTO doctorDTO = oragnDefaultDispensingApothecary(organId);
        apothecaryVO.setGiveUserIdCard(ByteUtils.hideIdCard(doctorDTO.getIdNumber()));
        apothecaryVO.setGiveUserName(doctorDTO.getName());
        apothecaryVO.setGiveUserSignImg(doctorDTO.getSignImage());
        return apothecaryVO;
    }

    /**
     * 获取药师信息用于前端展示
     *
     * @param recipe
     * @return
     */
    public ApothecaryVO getApothecary(Recipe recipe) {
        logger.info("getApothecary recipe:{} ", JSON.toJSONString(recipe));
        ApothecaryVO apothecaryVO = getGiveUser(recipe);
        Integer apothecaryId = recipe.getChecker();
        if (!ValidateUtil.integerIsEmpty(apothecaryId)) {
            DoctorDTO doctorDTO = getDoctor(apothecaryId);
            apothecaryVO.setCheckApothecaryIdCard(ByteUtils.hideIdCard(doctorDTO.getIdNumber()));
            apothecaryVO.setCheckApothecaryName(doctorDTO.getName());
        }
        logger.info("getApothecary apothecaryVO:{} ", JSONUtils.toString(apothecaryVO));
        return apothecaryVO;
    }

    /**
     * 获取 核发药师
     *
     * @param recipe
     * @return
     */
    public ApothecaryVO getGiveUser(Recipe recipe) {
        ApothecaryVO apothecaryVO = new ApothecaryVO();
        apothecaryVO.setRecipeId(recipe.getRecipeId());
        if (StringUtils.isEmpty(recipe.getGiveUser())) {
            return apothecaryVO;
        }
        Integer giveUserId = ByteUtils.strValueOf(recipe.getGiveUser());
        if (!ValidateUtil.integerIsEmpty(giveUserId)) {
            DoctorDTO doctorDTO = getDoctor(giveUserId);
            apothecaryVO.setGiveUserIdCardCleartext(doctorDTO.getIdNumber());
            apothecaryVO.setGiveUserIdCard(ByteUtils.hideIdCard(doctorDTO.getIdNumber()));
            apothecaryVO.setGiveUserName(doctorDTO.getName());
            apothecaryVO.setGiveUserSignImg(doctorDTO.getSignImage());
        }
        return apothecaryVO;
    }

    /**
     * 获取医生信息
     *
     * @param doctorId
     * @return
     */
    public DoctorDTO getDoctor(Integer doctorId) {
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("DoctorClient getDoctor  doctorId:{}", doctorId);
            return new DoctorDTO();
        }
        try {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
            if (null == doctorDTO) {
                return new DoctorDTO();
            }
            return doctorDTO;
        } catch (Exception e) {
            logger.warn("DoctorClient getDoctor doctorId:{}", doctorId, e);
            return new DoctorDTO();
        }
    }

    /**
     * 获取 机构默认发药药师
     *
     * @param organId
     * @return
     */
    public DoctorDTO oragnDefaultDispensingApothecary(Integer organId) {
        logger.info("DoctorClient oragnDefaultDispensingApothecary organId:{}", organId);
        String giveUserId = configurationClient.getValueCatch(organId, "oragnDefaultDispensingApothecary", "");
        if (StringUtils.isEmpty(giveUserId)) {
            return new DoctorDTO();
        }
        Integer id = ByteUtils.strValueOf(giveUserId);
        if (ValidateUtil.integerIsEmpty(id)) {
            return new DoctorDTO();
        }
        DoctorDTO dispensingApothecary = doctorService.get(id);
        if (null == dispensingApothecary) {
            return new DoctorDTO();
        }
        logger.info("DoctorClient oragnDefaultDispensingApothecary dispensingApothecary:{}", JSON.toJSONString(dispensingApothecary));
        return dispensingApothecary;
    }
}
