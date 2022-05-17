package recipe.core.api;

import com.ngari.platform.recipe.mode.ListOrganDrugReq;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;

import java.util.List;
import java.util.Map;

/**
 * @description： 药品service 接口
 * @author： whf
 * @date： 2021-08-23 19:02
 */
public interface IDrugBusinessService {
    /**
     * 患者端搜索药品信息
     *
     * @param searchDrugReqVo
     * @return
     */
    List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo);

    /**
     * 通过es检索药品
     *
     * @param drugInfo 药品检索条件
     * @param start    开始条数
     * @param limit    每夜条数
     * @return
     */
    List<SearchDrugDetailDTO> searchOrganDrugEs(DrugInfoDTO drugInfo, int start, int limit);

    /**
     * 获取药品说明书
     *
     * @param organId       机构id
     * @param organDrugCode 机构药品编码
     * @return
     */
    Dispensatory getDrugBook(Integer organId, String organDrugCode);


    /**
     * 十八反十九畏的规则
     *
     * @param list
     * @param ruleId
     * @return
     */
    List<RecipeRulesDrugcorrelation> getListDrugRules(List<Integer> list, Integer ruleId);

    /**
     * 查询his 药品说明书
     *
     * @param organId      机构id
     * @param recipeDetail 药品数据
     * @return
     */
    DrugSpecificationInfoDTO hisDrugBook(Integer organId, Recipedetail recipeDetail);

    /**
     * 根据ID获取平台药品列表
     *
     * @param drugIds 平台药品ids
     * @return
     */
    List<DrugList> drugList(List<Integer> drugIds);

    /**
     * 获取机构药品
     *
     * @param organId 机构id
     * @param drugIds 平台药品id
     * @return 机构药品列表
     */
    Map<String, OrganDrugList> organDrugMap(Integer organId, List<Integer> drugIds);

    /**
     * 定时 获取用药提醒的线下处方
     */
    void queryRemindRecipe(String dataTime);

    /**
     * 获取机构药品目录
     *
     * @param listOrganDrugReq
     * @return
     */
    List<OrganDrugList> listOrganDrug(ListOrganDrugReq listOrganDrugReq);

    /**
     * 查询医院药品信息
     * @param hospitalDrugListReqVO
     * @return
     */
    List<HospitalDrugListVO> findHospitalDrugList(HospitalDrugListReqVO hospitalDrugListReqVO);

    /**
     * 查询机构药品信息
     *
     * @param organId
     * @param drugId
     * @return
     */
    DispensatoryDTO getOrganDrugList(Integer organId, Integer drugId);

    List<OrganDrugList> organDrugList(Integer organId, List<String> organDrugCodes);

    OrganDrugList getOrganDrugList(Integer organId, String organDrugCode, Integer drugId);

    List<DrugList> findByDrugIdsAndStatus(List<Integer> drugIds);
}
