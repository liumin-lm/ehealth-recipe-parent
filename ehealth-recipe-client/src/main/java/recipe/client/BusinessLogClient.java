package recipe.client;

import com.ngari.opbase.base.service.IBusActionLogService;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 业务操作日志记录
 *
 * @Author liumin
 * @Date 2022/10/11 下午2:26
 * @Description
 */
@Service
public class BusinessLogClient extends BaseClient {


    @Autowired
    private IBusActionLogService busActionLogService;


    /**
     * 业务操作日志记录
     *
     * @param actionType 业务类型
     * @param bizId      业务id
     * @param bizClass   操作业务对象
     * @param content    记录日志内容
     */
    @RpcService
    public  void recordBusinessLog(String actionType, String bizId, String bizClass, String content,String subjectName) {
        try {
            busActionLogService.recordBusinessLogRpcNew(actionType, bizId, bizClass, content,subjectName);
        } catch (Exception e) {
            logger.error("日志记录失败：" + content + ",失败原因：" + e.getMessage());
        }
    }
}