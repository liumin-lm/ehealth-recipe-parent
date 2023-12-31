package recipe.thread;

import com.alibaba.fastjson.JSON;
import com.ngari.base.healthcard.model.CardUploadDTO;
import com.ngari.base.healthcard.service.IWholesomeService;
import ctd.spring.AppDomainContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author： liumin
 * @date： 2020/08/18
 * @description： 健康卡数据上传
 *                http://confluence.ngarihealth.com:8002/pages/viewpage.action?pageId=29298992
 */
public class CardDataUploadRunable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardDataUploadRunable.class);

    private int organId;

    private String mpiid;

    private String scene;

    public CardDataUploadRunable(int  organId, String mpiid,String scene) {
        this.organId = organId;
        this.mpiid = mpiid;
        this.scene=scene;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        LOGGER.info("CardDataUploadRunable start. organId={} mpiid={} scene={}", organId,mpiid,scene);
        try {
            CardUploadDTO cardUploadDTO=new CardUploadDTO();
            cardUploadDTO.setOrganId(organId);
            cardUploadDTO.setMpiId(mpiid);
            cardUploadDTO.setScene(scene);
            LOGGER.info("CardDataUploadRunable start. cardUploadDTO:{}", JSON.toJSONString(cardUploadDTO));
            IWholesomeService iWholesomeService = AppDomainContext.getBean("eh.wholesomeService", IWholesomeService.class);
            iWholesomeService.cardDataUpload(cardUploadDTO);
        }catch (Exception e){
            LOGGER.error("CardDataUploadRunable error :{}",e);
        }finally {
            LOGGER.info("CardDataUploadRunable finish. organId={} mpiid={} scene={}", organId,mpiid,scene);
            long elapsedTime = System.currentTimeMillis() - start;
            LOGGER.info("RecipeBusiThreadPool CardDataUploadRunable 健康卡数据上传 执行时间:{}.", elapsedTime);
        }

    }

}
