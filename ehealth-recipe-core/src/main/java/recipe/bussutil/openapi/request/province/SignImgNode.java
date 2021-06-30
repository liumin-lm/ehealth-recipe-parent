package recipe.bussutil.openapi.request.province;

import lombok.*;

import java.io.Serializable;

/**
 * 通用 根据 x，y坐标写入 图片放置对象
 *
 * @author fuzi
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignImgNode implements Serializable {
    private static final long serialVersionUID = -6566326647606097783L;
    /**
     * 处方id 用于表示处方pdf 文件名称
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
    private String signFileId;

    private byte[] signFileData;
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
    /**
     * 是否覆盖 true 覆盖
     */
    private Boolean repeatWrite;

}
