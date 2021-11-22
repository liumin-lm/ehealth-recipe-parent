package recipe.atop.open;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.CheckItemListVo;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.api.open.ITherapyItemOpenAtopService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.constant.PageInfoConstant;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.ItemListBean;

import java.util.Date;
import java.util.List;

/**
 * 诊疗项目
 *
 * @author 刘敏
 * @date 2021/11/8
 */
@RpcBean("therapyItemOpenAtop")
public class TherapyItemOpenAtop extends BaseAtop implements ITherapyItemOpenAtopService {

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
            result = therapyItemBusinessService.saveItemList(itemList);
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
            result = therapyItemBusinessService.updateItemList(itemList);
        } catch (DAOException e1) {
            logger.error("TherapyItemOpenAtop updateItemList  error", e1);
        }
        return result;
    }

    /**
     * 运营平台批量修改数据（启用禁用、删除）
     *
     * @param itemLists
     * @return
     */
    @RpcService
    public boolean batchUpdateItemList(List<ItemList> itemLists) {
        validateAtop(itemLists);
        boolean result = false;
        try {
            therapyItemBusinessService.batchUpdateItemList(itemLists);
            result = true;
        } catch (DAOException e1) {
            logger.error("TherapyItemOpenAtop updateItemList  error", e1);
        }
        return result;
    }

    /**
     * 修改保存前数据校验
     *
     * @param itemList
     * @return true表示可向下执行流程 false抛出错误
     */
    @RpcService
    public CheckItemListVo checkItemList(ItemList itemList) {
        CheckItemListVo checkItemListVo = new CheckItemListVo();
        validateAtop(itemList);
        try {
            checkItemListVo = therapyItemBusinessService.checkItemList(itemList);
        } catch (DAOException e1) {
            logger.error("TherapyItemOpenAtop updateItemList  error", e1);
        }
        return checkItemListVo;
    }

    @Override
    @RpcService
    public Boolean checkExistByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode) {
        List<ItemList> list = therapyItemBusinessService.findItemListByOrganIdAndItemNameOrCode(organId, itemName, itemCode);
        return CollectionUtils.isNotEmpty(list);
    }

    @Override
    @RpcService
    public Boolean checkExistByOrganIdAndItemNameOrCode2(Integer organId, String itemName, String itemCode) {
        List<ItemList> list = therapyItemBusinessService.findItemListByOrganIdAndItemNameOrCode2(organId, itemName, itemCode);
        return CollectionUtils.isNotEmpty(list);
    }

    @Override
    @RpcService
    public void saveOrUpdateBean(ItemListBean itemListBean) {
        ItemList itemListInfo = ObjectCopyUtils.convert(itemListBean, ItemList.class);
        List<ItemList> existList = therapyItemBusinessService.findItemListByOrganIdAndItemNameOrCode2(itemListInfo.getOrganID(), itemListInfo.getItemName(), itemListInfo.getItemCode());
        //更新
        if (CollectionUtils.isNotEmpty(existList)) {
            for (ItemList item : existList) {
                item.setStatus(1);
                item.setItemName(itemListInfo.getItemName());
                item.setItemCode(itemListInfo.getItemCode());
                item.setItemUnit(itemListInfo.getItemUnit());
                item.setItemPrice(itemListInfo.getItemPrice());
                item.setGmtModified(new Date());
                item.setOrganName(itemListInfo.getOrganName());
                updateItemList(item);
            }
        } else {
            //新增
            itemListInfo.setDeleted(0);
            itemListInfo.setStatus(1);
            itemListInfo.setGmtCreate(new Date());
            itemListInfo.setGmtModified(new Date());
            saveItemList(itemListInfo);
        }
    }
}
