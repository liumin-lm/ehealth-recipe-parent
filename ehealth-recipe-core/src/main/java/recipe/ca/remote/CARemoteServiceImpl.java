package recipe.ca.remote;

import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.ApplicationUtils;
import recipe.ca.CAInterface;
import recipe.ca.factory.CommonCAFactory;

@RpcBean("iCARemoteService")
public class CARemoteServiceImpl implements ICARemoteService {

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);

    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @Override
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId) {
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        CommonCAFactory caFactory = new CommonCAFactory();
        //通过工厂获取对应的实现CA类
        CAInterface caInterface = caFactory.useCAFunction(doctorDTO.getOrgan());
        return caInterface.caUserLoginAndGetCertificate(doctorId);
    }

    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    @Override
    @RpcService
    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
        CommonCAFactory caFactory = new CommonCAFactory();
        CAInterface caInterface = caFactory.useCAFunction(requestTO.getOrganId());
        return caInterface.caPasswordBusiness(requestTO);
    }

}
