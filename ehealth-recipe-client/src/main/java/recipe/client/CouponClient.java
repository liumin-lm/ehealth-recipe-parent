package recipe.client;

import coupon.api.request.CouponCalcReq;
import coupon.api.service.ICouponBaseService;
import coupon.api.vo.Coupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;

/**
 * @description： 优惠券服务
 * @author： whf
 * @date： 2022-09-20 15:11
 */
@Service
public class CouponClient extends BaseClient {
    @Autowired
    private ICouponBaseService couponBaseService;

    /**
     * 获取优惠金额
     * @param couponCalcReq
     * @return
     */
    @LogRecord
    public Coupon getCouponByRecipeOrder(CouponCalcReq couponCalcReq) {
        Coupon coupon = couponBaseService.calculateCoupon(couponCalcReq);
        return coupon;
    }
    /**
     * 解锁优惠券
     * @param couponId
     * @return
     */
    @LogRecord
    public Coupon unlockCouponById(Integer couponId) {
        Coupon coupon = couponBaseService.unlockCouponById(couponId);
        return coupon;
    }
    /**
     * 锁定优惠券
     * @param couponCalcReq
     * @return
     */
    @LogRecord
    public Coupon lockCouponByBus(CouponCalcReq couponCalcReq) {
        Coupon coupon = couponBaseService.lockCouponByBus(couponCalcReq);
        return coupon;
    }
}
