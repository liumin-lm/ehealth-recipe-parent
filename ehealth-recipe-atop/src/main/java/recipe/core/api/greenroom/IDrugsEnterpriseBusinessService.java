package recipe.core.api.greenroom;

/**
 * 药企相关
 */
public interface IDrugsEnterpriseBusinessService {

    /**
     * 根据名称查询药企是否存在
     * @param name 药企名称
     * @return 是否存在
     */
    Boolean existEnterpriseByName(String name);
}
