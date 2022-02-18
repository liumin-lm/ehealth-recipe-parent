package recipe.client;

import coupon.api.service.ICouponBaseService;
import coupon.api.vo.Coupon;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 优惠券服务
 * @author yins
 */
@Service
public class CouponClient extends BaseClient {

    @Autowired
    private ICouponBaseService couponService;

    /**
     * 获取优惠券
     * @param couponId 优惠券ID
     * @param totalFee 总金额
     * @return 优惠券
     */
    public Coupon getCouponById(Integer couponId, BigDecimal totalFee) {
        logger.info("CouponClient getCouponById couponId:{}, totalFee:{}.", couponId, totalFee);
        if (null == couponId || couponId <= 0) {
            return null;
        }
        Coupon coupon = couponService.lockCouponById(couponId, totalFee);
        logger.info("CouponClient getCouponById coupon:{}.", JSONUtils.toString(coupon));
        return coupon;
    }
}
