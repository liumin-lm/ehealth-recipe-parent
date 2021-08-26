package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.doctor.DrugBookVo;

/**
 * @description： 二方药品请求入口
 * @author： whf
 * @date： 2021-08-26 9:36
 */
public interface IDrugAtopService {

    /**
     * 获取药品说明书
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品编码
     * @returno
     */
    @RpcService
    DrugBookVo getDrugBook(Integer organId, String organDrugCode);
}
