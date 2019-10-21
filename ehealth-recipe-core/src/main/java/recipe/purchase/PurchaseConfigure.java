package recipe.purchase;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药配置信息
 * @version： 1.0
 */

@Configuration
public class PurchaseConfigure {

    @Bean
    public IPurchaseService payModeOnlineService() {
        return new PayModeOnline();
    }

    @Bean
    public IPurchaseService payModeTFDSService() {
        return new PayModeTFDS();
    }

    @Bean
    public IPurchaseService payModeToHosService(){
        return new PayModeToHos();
    }

    @Bean
    public IPurchaseService payModeDownloadService(){
        return new PayModeDownload();
    }
}
