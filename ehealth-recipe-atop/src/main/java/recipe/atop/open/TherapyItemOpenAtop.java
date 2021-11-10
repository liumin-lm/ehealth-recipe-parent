package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.PageInfoConstant;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.util.ValidateUtil;

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
     * 运营平台搜索诊疗项目（查询诊疗项目列表）
     *
     * @param itemListVO itemListVO
     * @return List<ItemListVO>
     */
    @RpcService
    public QueryResult<ItemList> listItemList(ItemListVO itemListVO) {
        validateAtop(itemListVO, itemListVO.getOrganId());
        try {
            if (ValidateUtil.integerIsEmpty(itemListVO.getStart())) {
                itemListVO.setStart(PageInfoConstant.PAGE_NO);
            }
            if (ValidateUtil.integerIsEmpty(itemListVO.getLimit())) {
                itemListVO.setLimit(PageInfoConstant.PAGE_SIZE);
            }
            QueryResult<ItemList> result = therapyItemBusinessService.pageItemList(itemListVO);
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
     * 获取诊疗项目详情
     *
     * @param itemList
     * @return
     */
    @RpcService
    public ItemList getItemListById(ItemList itemList) {
        validateAtop(itemList, itemList.getId());
        try {
            itemList = therapyItemBusinessService.getItemListById(itemList);
        } catch (Exception e1) {
            logger.error("TherapyItemOpenAtop getItemListById  error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        }
        return itemList;
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
            logger.error("TherapyItemOpenAtop saveItemList  error", e1);
        }
        return result;
    }

    /**
     * 运营平台修改诊疗项目(1、启用、禁用 2、删除 3、修改数据保存)
     *
     * @param itemList
     * @return
     */
    @RpcService
    public boolean updateItemList(ItemList itemList) {
        validateAtop(itemList, itemList.getId());
        boolean result = false;
        try {
            therapyItemBusinessService.updateItemList(itemList);
            result = true;
        } catch (DAOException e1) {
            logger.error("TherapyItemOpenAtop updateItemList  error", e1);
        }
        return result;
    }


}
