package recipe.bussutil;

import lombok.*;

import java.io.Serializable;

/**
 * 模版数据对象
 *
 * @author fuzi
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordToPdfBean implements Serializable {
    private static final long serialVersionUID = -6763301404155764957L;
    private String key;
    private String value;
    /**
     * 1 文字类的内容处理,2 将图片写入指定的field
     */
    private int type;
}
