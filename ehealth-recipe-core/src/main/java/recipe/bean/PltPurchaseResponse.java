package recipe.bean;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/19
 * @description： 平台购药方式返回
 * @version： 1.0
 */
public class PltPurchaseResponse implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 配送到家 按钮
     */
    private boolean sendToHome = false;

    /**
     * 药店取药 按钮
     */
    private boolean tfds = false;

    /**
     * 到院取药 按钮
     */
    private boolean toHos = false;

    public boolean isSendToHome() {
        return sendToHome;
    }

    public void setSendToHome(boolean sendToHome) {
        this.sendToHome = sendToHome;
    }

    public boolean isTfds() {
        return tfds;
    }

    public void setTfds(boolean tfds) {
        this.tfds = tfds;
    }

    public boolean isToHos() {
        return toHos;
    }

    public void setToHos(boolean toHos) {
        this.toHos = toHos;
    }
}
