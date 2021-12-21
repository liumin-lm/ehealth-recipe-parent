package recipe.atop.greenroom;

import ctd.util.PyConverter;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeDictionaryBusinessService;
import recipe.vo.second.RecipeDictionaryVO;

import java.util.List;

@RpcBean(value = "recipeDictionaryGmAtop")
public class RecipeDictionaryGmAtop extends BaseAtop {

    @Autowired
    private IRecipeDictionaryBusinessService recipeDictionaryBusinessService;

    /**
     * 保存字典数据
     * @param recipeDictionaryVO
     * @return
     */
    @RpcService
    public Integer saveRecipeDictionary(RecipeDictionaryVO recipeDictionaryVO){
        validateAtop(recipeDictionaryVO, recipeDictionaryVO.getDictionaryCode(),
                recipeDictionaryVO.getDictionaryName());
        validateAtop(recipeDictionaryVO.getOrganId(), recipeDictionaryVO.getDictionaryType());
        //将字典名称转为拼音码
        recipeDictionaryVO.setDictionaryPingying(PyConverter.getFirstLetter(recipeDictionaryVO.getDictionaryName()));
        return recipeDictionaryBusinessService.saveRecipeDictionary(recipeDictionaryVO);
    }

    /**
     * 根据机构和搜索条件查询
     * @param organId     机构ID
     * @param searchName  搜索条件
     * @return 字典列表
     */
    @RpcService
    public List<RecipeDictionaryVO> findRecipeDictionaryByName(Integer organId, String searchName){
        validateAtop(organId, searchName);
        return recipeDictionaryBusinessService.findRecipeDictionaryByName(organId, searchName);
    }
}
