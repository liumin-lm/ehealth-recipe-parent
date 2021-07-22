package recipe.client;

import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RevisitClient extends BaseClient {

    @Autowired
    private IRevisitExService revisitExService;


    public RevisitExDTO getByRegisterId(String registeredId) {

        RevisitExDTO consultExDTO = revisitExService.getByRegisterId(registeredId);
        return consultExDTO;
    }
}
