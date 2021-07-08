package recipe.bussutil;

import lombok.*;

import java.io.Serializable;
import java.net.URI;

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
    /**
     * 需要替换pdf模版表单字段名
     */
    private String key;
    /**
     * 需要替换pdf模版表单字段值
     */
    private String value;
    /**
     * 需要替换pdf模版表单图片
     */
    private URI uri;
}
