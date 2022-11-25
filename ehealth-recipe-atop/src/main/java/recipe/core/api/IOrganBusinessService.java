package recipe.core.api;

import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.ServiceLogDTO;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import recipe.vo.second.OrganVO;

import java.util.List;
import java.util.Map;

/**
 * 机构相关服务
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IOrganBusinessService {

    /**
     * 获取公众号下机构列表
     * @return 机构列表
     */
    List<Integer> getOrganForWeb();

    /**
     * 获取机构购药方式配置
     *
     * @param organId organId
     * @return 购药方式列表
     */
    List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId);
    /**
     * 校验  推送的购药方式配置 是否满足机构配置项
     *
     * @param orderId 订单id
     * @return
     */
    boolean giveModeValidate(Integer orderId);

    /**
     * 校验  推送的购药方式配置 是否满足机构配置项
     *
     * @param organId     机构id
     * @param giveModeKey 购药方式
     * @return
     */
    boolean giveModeValidate(Integer organId, String giveModeKey);

    /**
     * 获取处方配置
     * @param key
     * @return
     */
    String getRecipeParameterValue(String key);

    /**
     * 获取机构信息
     * @param organId
     * @return
     */
    OrganVO getOrganVOByOrganId(Integer organId);

    /**
     * 判断运营平台账户越权校验
     *
     * @param organId
     * @return
     */
    Boolean isAuthorisedOrgan(Integer organId);

    /**
     * 通过机构ID从运营平台获取购药方式的基本配置项
     *
     * @param organId 机构id
     * @return
     */
    List<GiveModeButtonDTO> organGiveMode(Integer organId);

    /**
     * 返回订单支付状态
     *
     * @param orderId
     * @return
     */
    Integer getOrderPayFlag(Integer orderId);

    /**
     * 日志记录分析接口
     *
     * @param serviceLog
     */
    void serviceTimeLog(ServiceLogDTO serviceLog);

    /**
     * 获取机构下的药房
     *
     * @param orderId
     * @return
     */
    Map<Integer, PharmacyTcm> pharmacy(Integer orderId);
}
