package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.base.currentuserinfo.service.ICurrentUserInfoService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.patient.service.IPatientHisService;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.his.recipe.service.IRecipeToTestService;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.constant.HisErrorCodeEnum;
import recipe.util.DictionaryUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * his调用基类
 *
 * @author fuzi
 */
public class BaseClient {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected IRecipeHisService recipeHisService;
    @Autowired
    protected IPatientHisService patientHisService;
    @Autowired
    protected ICurrentUserInfoService currentUserInfoService;
    @Autowired
    protected IRecipeToTestService recipeToTestService;


    /**
     * 解析前置机 出参
     *
     * @param hisResponse 前置机出参
     * @param <T>         范型
     * @return 返回封装的data
     * @throws DAOException 自定义前置机异常
     * @throws Exception    运行异常
     */
    protected <T> T getResponse(HisResponseTO<T> hisResponse) throws DAOException, Exception {
        logger.info("BaseClient getResponse  hisResponse= {}", JSON.toJSONString(hisResponse));
        if (null == hisResponse) {
            throw new DAOException(HisErrorCodeEnum.HIS_NULL_ERROR.getCode(), HisErrorCodeEnum.HIS_NULL_ERROR.getMsg());
        }
        if (!String.valueOf(HisErrorCodeEnum.HIS_SUCCEED.getCode()).equals(hisResponse.getMsgCode())) {
            throw new DAOException(HisErrorCodeEnum.HIS_CODE_ERROR.getCode(), HisErrorCodeEnum.HIS_CODE_ERROR.getMsg());
        }
        if (null == hisResponse.getData()) {
            throw new DAOException(HisErrorCodeEnum.HIS_PARAMETER_ERROR.getCode(), HisErrorCodeEnum.HIS_PARAMETER_ERROR.getMsg());
        }
        T result = hisResponse.getData();
        logger.info("BaseClient getResponse result= {}", JSON.toJSONString(result));
        return result;
    }

    protected <T> T getResponseMsg(HisResponseTO<T> hisResponse) throws DAOException, Exception {
        logger.info("BaseClient getResponseMsg  hisResponse= {}", JSON.toJSONString(hisResponse));
        if (null == hisResponse) {
            throw new DAOException(HisErrorCodeEnum.HIS_NULL_ERROR.getCode(), HisErrorCodeEnum.HIS_NULL_ERROR.getMsg());
        }
        if (!String.valueOf(HisErrorCodeEnum.HIS_SUCCEED.getCode()).equals(hisResponse.getMsgCode())) {
            throw new DAOException(HisErrorCodeEnum.HIS_CODE_ERROR.getCode(), hisResponse.getMsg());
        }
        if (null == hisResponse.getData()) {
            throw new DAOException(HisErrorCodeEnum.HIS_PARAMETER_ERROR.getCode(), HisErrorCodeEnum.HIS_PARAMETER_ERROR.getMsg());
        }
        T result = hisResponse.getData();
        logger.info("BaseClient getResponseMsg result= {}", JSON.toJSONString(result));
        return result;
    }


    /**
     * 扩展 当 前置机没实现接口时特殊处理返回值
     * 不建议使用，特殊处理新老医院对接问题
     *
     * @param hisResponse 前置机出参
     * @param <T>         范型
     * @return 返回封装的data
     */
    protected <T> T getResponseCatch(HisResponseTO<T> hisResponse) {
        try {
            return getResponse(hisResponse);
        } catch (DAOException e) {
            if (HisErrorCodeEnum.HIS_NULL_ERROR.getCode() == e.getCode()) {
                logger.warn("BaseClient getResponseCatch is null ");
                return null;
            }
            throw new DAOException(e);
        } catch (Exception e1) {
            logger.error("BaseClient getResponseCatch hisResponse= {}", JSON.toJSONString(hisResponse));
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        }
    }

    /**
     * 获取地址枚举
     *
     * @param area
     */
    protected String getAddress(String area) {
        return DictionaryUtil.getDictionary("eh.base.dictionary.AddrArea", area);
    }


    protected List<DrugInfoTO> drugInfoList(List<Recipedetail> detailList, List<OrganDrugList> organDrugList, List<PharmacyTcm> pharmacyTcms) {
        Map<Integer, PharmacyTcm> pharmacyTcmMap = pharmacyTcms.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
        Map<String, Recipedetail> detailMap = detailList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        List<DrugInfoTO> data = new LinkedList<>();
        organDrugList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(String.valueOf(a.getPack()));
            drugInfo.setManfcode(a.getProducerCode());
            drugInfo.setDrname(a.getDrugName());
            drugInfo.setDrugId(a.getDrugId());
            drugInfo.setDrugItemCode(a.getDrugItemCode());
            Recipedetail recipedetail = detailMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == recipedetail) {
                data.add(drugInfo);
                return;
            }
            drugInfo.setDrugType(recipedetail.getDrugType());
            drugInfo.setPackUnit(recipedetail.getDrugUnit());
            drugInfo.setUseTotalDose(recipedetail.getUseTotalDose());
            PharmacyTcm tcm = pharmacyTcmMap.get(recipedetail.getPharmacyId());
            if (null != tcm) {
                drugInfo.setPharmacyCode(tcm.getPharmacyCode());
                drugInfo.setPharmacy(tcm.getPharmacyName());
            }
            data.add(drugInfo);
        });
        return data;
    }
}
