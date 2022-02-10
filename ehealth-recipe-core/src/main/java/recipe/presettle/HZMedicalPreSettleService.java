package recipe.presettle;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.MedicalPreSettleReqNTO;
import com.ngari.his.recipe.mode.RecipeMedicalPreSettleInfo;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.pay.api.service.bus.IBusPaySettlementFacade;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.util.ArrayList;
import java.util.Map;

/**
 * @description： 杭州医保预结算对接支付平台版本
 * @author： whf
 * @date： 2022-02-10 17:38
 */
@Service
public class HZMedicalPreSettleService implements IRecipePreSettleService {
    @Autowired
    private IBusPaySettlementFacade busPaySettlementFacade;
    private static final Logger LOGGER = LoggerFactory.getLogger(HZMedicalPreSettleService.class);
    @Override
    public Map<String, Object> recipePreSettle(Integer recipeId, Map<String, Object> extInfo) {
        LOGGER.info("HZMedicalPreSettleService.recipePreSettle req recipeId={} extInfo={}",recipeId, JSONArray.toJSONString(extInfo));
        Map<String, Object> result = Maps.newHashMap();
        result.put("code", "-1");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            result.put("msg", "查不到该处方");
            return result;
        }
        try {
            MedicalPreSettleReqNTO request = new MedicalPreSettleReqNTO();
            request.setClinicId(String.valueOf(recipe.getClinicId()));
            request.setClinicOrgan(recipe.getClinicOrgan());
            request.setRecipeId(String.valueOf(recipeId));
            request.setHisRecipeNo(recipe.getRecipeCode());
            String recipeCodeS = MapValueUtil.getString(extInfo, "recipeNoS");
            if (recipeCodeS != null) {
                request.setHisRecipeNoS(JSONUtils.parse(recipeCodeS, ArrayList.class));
            }
            request.setDoctorId(recipe.getDoctor() + "");
            request.setDoctorName(recipe.getDoctorName());
            request.setDepartId(recipe.getDepart() + "");
            //参保地区行政区划代码
            request.setInsuredArea(MapValueUtil.getString(extInfo, "insuredArea"));
            IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
            //获取医保支付流程配置（2-原省医保 3-长三角）
            Integer insuredAreaType = (Integer) configService.getConfiguration(recipe.getClinicOrgan(), "provincialMedicalPayFlag");
            if (new Integer(3).equals(insuredAreaType)) {
                if (StringUtils.isEmpty(request.getInsuredArea())) {
                    result.put("msg", "参保地区行政区划代码为空,无法进行预结算");
                    return result;
                }
                //省医保参保类型 1 长三角 没有赋值就是原来的省直医保
                request.setInsuredAreaType("1");
                //结算的时候会用到
                recipeExtendDAO.updateRecipeExInfoByRecipeId(recipe.getRecipeId(), ImmutableMap.of("insuredArea",request.getInsuredArea()));
            }
            RecipeExtend ext = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (ext != null) {
                if (StringUtils.isNotEmpty(ext.getRegisterID())) {
                    request.setRegisterID(ext.getRegisterID());
                }
                //默认是医保，医生选择了自费时，强制设置为自费
                if (ext.getMedicalType() != null && "0".equals(ext.getMedicalType())) {
                    request.setIszfjs("1");
                } else {
                    request.setIszfjs("0");
                }
            }
            try {
                request.setDepartName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(recipe.getDepart()));
            } catch (ControllerException e) {
                LOGGER.warn("HZMedicalPreSettleService 字典转化异常");
            }
            //患者信息
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientBean = patientService.get(recipe.getMpiid());
            request.setPatientId(recipe.getPatientID());
            request.setPatientName(patientBean.getPatientName());
            request.setIdcard(patientBean.getIdcard());
            request.setCertificate(patientBean.getCertificate());
            request.setCertificateType(patientBean.getCertificateType());
            request.setBirthday(patientBean.getBirthday());
            request.setAddress(patientBean.getAddress());
            request.setMobile(patientBean.getMobile());
            request.setGuardianName(patientBean.getGuardianName());
            request.setGuardianTel(patientBean.getLinkTel());
            request.setGuardianCertificate(patientBean.getGuardianCertificate());

            DrugsEnterpriseDAO drugEnterpriseDao = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            Integer depId = MapValueUtil.getInteger(extInfo, "depId");
            //获取杭州市市民卡
            if (depId != null) {
                DrugsEnterprise drugEnterprise = drugEnterpriseDao.get(depId);
                if (drugEnterprise != null) {
                    HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
                    //杭州市互联网医院监管中心 管理单元eh3301
                    OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
                    OrganDTO organDTO = organService.getByManageUnit("eh3301");
                    String bxh = null;
                    if (organDTO != null) {
                        bxh = healthCardService.getMedicareCardId(recipe.getMpiid(), organDTO.getOrganId());
                    }
                    request.setBxh(bxh);
                }
            }

            LOGGER.info("HZMedicalPreSettleService recipeId={} req={}", recipeId, JSONUtils.toString(request));
            HisResponseTO<RecipeMedicalPreSettleInfo> hisResult = busPaySettlementFacade.recipeMedicalPreSettleSyt(request);
            LOGGER.info("HZMedicalPreSettleService recipeId={} res={}", recipeId, JSONUtils.toString(hisResult));
            if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
                if (hisResult.getData() != null) {
                    //自费金额
                    String cashAmount = hisResult.getData().getZfje();
                    //医保支付金额
                    String fundAmount = hisResult.getData().getYbzf();
                    //总金额
                    String totalAmount = hisResult.getData().getZje();
                    if (ext != null) {
                        Map<String, String> map = Maps.newHashMap();
                        //杭州互联网用到registerNo、hisSettlementNo，支付的时候需要回写
                        //不知道registerNo有什么用
                        map.put("registerNo", hisResult.getData().getGhxh());
                        map.put("hisSettlementNo", hisResult.getData().getSjh());
                        //平台和杭州互联网都用到
                        map.put("preSettleTotalAmount", totalAmount);
                        map.put("fundAmount", fundAmount);
                        map.put("cashAmount", cashAmount);
                        //此时订单已经生成还需要更新订单信息
                        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                        if (recipeOrder != null) {
                            RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                            if (!recipeOrderService.dealWithOrderInfo(map, recipeOrder, recipe)) {
                                result.put("msg", "预结算更新订单信息失败");
                                return result;
                            }
                        }
                    }
                    result.put("totalAmount", totalAmount);
                    result.put("fundAmount", fundAmount);
                    result.put("cashAmount", cashAmount);
                }
                result.put("code", "200");
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "处方预结算成功");
            } else {
                String msg;
                if (hisResult != null) {
                    msg = "his返回:" + hisResult.getMsg();
                } else {
                    msg = "前置机未实现预结算接口";
                }
                result.put("msg", msg);
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "处方预结算失败-原因:" + msg);
            }
        } catch (Exception e) {
            LOGGER.error("HZMedicalPreSettleService recipeId={} error", recipeId, e);
            throw new DAOException(609, "处方预结算异常");
        }
        return result;
    }
}
