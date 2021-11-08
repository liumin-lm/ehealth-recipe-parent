package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.PageInfoConstant;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 诊疗项目
 *
 * @author 刘敏
 * @date 2021/11/8
 */
@RpcBean
public class TherapyItemOpenAtop extends BaseAtop {

    @Autowired
    private ITherapyItemBusinessService therapyItemBusinessService;

    /**
     * 运营平台搜索诊疗项目
     *
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public List<ItemListVO> listItemList(ItemListVO itemListVO) {
        validateAtop(itemListVO, itemListVO.getOrganId());
        try {
            if (ValidateUtil.integerIsEmpty(itemListVO.getStart())) {
                itemListVO.setStart(PageInfoConstant.PAGE_NO);
            }
            if (ValidateUtil.integerIsEmpty(itemListVO.getLimit())) {
                itemListVO.setLimit(PageInfoConstant.PAGE_SIZE);
            }
            List<ItemListVO> result = therapyItemBusinessService.listItemList(itemListVO);
            logger.info("TherapyItemOpenAtop listItemList result:{}.", JSON.toJSONString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("TherapyItemOpenAtop listItemList  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("TherapyItemOpenAtop listItemList  error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 运营平台添加诊疗项目
     *
     * @param itemList
     * @return
     */
    @RpcService
    public boolean saveItemList(ItemList itemList) {
        validateAtop(itemList, itemList.getOrganID());
        boolean result = false;
        try {
            therapyItemBusinessService.saveItemList(itemList);
            result = true;
        } catch (DAOException e1) {
            logger.error("TherapyItemOpenAtop searchItemListByKeyWord  error", e1);
        }
        logger.info("TherapyItemOpenAtop saveItemList result:{}", result);
        return result;
    }

}
