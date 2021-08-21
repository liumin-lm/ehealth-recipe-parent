package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IBaseItemListBusinessService;
import recipe.vo.doctor.ItemListVO;

import java.util.List;

/**
 * 医生端诊疗项目
 * @author yinsheng
 * @date 2021\8\21 0021 11:16
 */
@RpcBean("therapyItemListDoctorAtop")
public class TherapyItemListDoctorAtop extends BaseAtop {

    @Autowired
    private IBaseItemListBusinessService itemListBusinessService;

    /**
     * 搜索诊疗项目
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO){
        logger.info("TherapyRecipeDoctorAtop searchItemListByKeyWord itemListVO:{}.", JSON.toJSONString(itemListVO));
        validateAtop(itemListVO, itemListVO.getOrganID(),itemListVO.getItemName(), itemListVO.getLimit());
        try {
            List<ItemListVO> result = itemListBusinessService.searchItemListByKeyWord(itemListVO);
            logger.info("TherapyRecipeDoctorAtop searchItemListByKeyWord result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyRecipeDoctorAtop searchItemListByKeyWord  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyRecipeDoctorAtop searchItemListByKeyWord  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
