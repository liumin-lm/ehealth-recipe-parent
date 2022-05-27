package recipe.vo.second.enterpriseOrder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class EnterpriseConfirmOrderVO implements Serializable {
    private static final long serialVersionUID = 4562967984853525953L;

    private String appKey;
    private List<String> orderCodeList;
}
