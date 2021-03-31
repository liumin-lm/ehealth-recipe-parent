package recipe.bussutil.openapi.request.province;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通用 根据 x，y坐标写入 图片放置对象
 *
 * @author fuzi
 */
@Setter
@Getter
public class SignImgNode implements Serializable {
    private static final long serialVersionUID = -6566326647606097783L;
    /**
     * 处方id
     */
    private String recipeId;
    /**
     * 图片标识id
     */

    private String signImgId;
    /**
     * 图片文件id
     */
    private String signImgFileId;
    /**
     * 处方pdf文件id
     */
    private String signFileFileId;
    /**
     * 图片在pdf中的宽度
     */
    private Float width;
    /**
     * 图片在pdf中的高度
     */
    private Float height;
    /**
     * 图片在pdf中x坐标
     */
    private Float x;
    /**
     * 图片在pdf中y坐标
     */
    private Float y;

    public SignImgNode(String recipeId, String signImgId, String signImgFileId, String signFileFileId, Float width, Float height, Float x, Float y) {
        this.recipeId = recipeId;
        this.signImgId = signImgId;
        this.signImgFileId = signImgFileId;
        this.signFileFileId = signFileFileId;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
}
