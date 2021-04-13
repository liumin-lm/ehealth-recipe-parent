package recipe.service.afterpay;

/**
 * @author yinsheng
 * @date 2021\4\12 0012 17:43
 */
public interface IAfterPayBussService {

    //复诊类型
    Integer REVISIT_TYPE = 2;
    //药品支付类型  1 线上  2 线下
    Integer PAY_MODE_ONLINE_TYPE = 1;
    //处方来源 1 线上 2 线下
    Integer RECIPE_SOURCE_ONLINE = 1;
    //正在进行中的复诊状态
    Integer REVISIT_STATUS_IN = 4;
}
