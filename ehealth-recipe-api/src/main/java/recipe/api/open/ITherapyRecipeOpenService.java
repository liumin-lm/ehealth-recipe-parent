package recipe.api.open;

/**
 * 复诊关闭作废处方
 * @author yinsheng
 * @date 2021\8\30 0030 10:10
 */
public interface ITherapyRecipeOpenService {

    boolean abolishTherapyRecipeForRevisitClose(Integer bussSource, Integer clinicId);
}
