package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ThirdGiveModeRequest implements Serializable{
    private static final long serialVersionUID = -784876232768995742L;

    private String appkey;

    private String tid;

    private List<Integer> recipeIds;
}
