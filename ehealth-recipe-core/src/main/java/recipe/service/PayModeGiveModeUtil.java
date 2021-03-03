package recipe.service;

import recipe.constant.RecipeBussConstant;

/**
 * @description：
 * @author： whf
 * @date： 2021-03-02 16:39
 */
public class PayModeGiveModeUtil {

    /**
     * 临时转换老版paymode使用
     * @param payMode
     * @param giveMode
     * @return
     */
    public static Integer getPayMode(Integer payMode,Integer giveMode) {
        Integer payModeNew = null;
        switch (giveMode){
            case 1:
                if(RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)){
                    payModeNew = 1;
                }else if(RecipeBussConstant.PAYMODE_OFFLINE.equals(payMode)){
                    payModeNew = 2;
                }
                break;
            case 2:
                payModeNew = 3;
                break;
            case 3:
                payModeNew = 4;
                break;
            case 5:
                payModeNew = 6;
                break;
            default:
                break;
        }
        return payModeNew;
    }

    /**
     * 根据老版本paymode转换givemode
     * @param payMode
     * @return
     */
    public static Integer getGiveMode(Integer payMode){
        Integer giveMode = null;
        switch(payMode){
            case 1:
            case 2:
                giveMode = 1;
                break;
            case 3:
                giveMode = 2;
                break;
            case 4:
                giveMode = 3;
                break;
            case 5:
            case 6:
                giveMode = 6;
                break;
            default:
                break;

        }
        return giveMode;
    }
}
