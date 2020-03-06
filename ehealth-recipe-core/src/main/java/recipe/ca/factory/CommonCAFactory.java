package recipe.ca.factory;

import recipe.ca.CAInterface;
import recipe.ca.impl.ShanghaiCAImpl;
import recipe.ca.impl.ShanxiCAImpl;

public class CommonCAFactory {

    public CAInterface useCAFunction(Integer organId) {
        // 通过机构获取类型，建立机构和CA类型的关系
//         String type = getCATypeByOrganId(organId);

        if ("shanxi".equals(organId)) {
            return new ShanxiCAImpl();
        } else if ("shanghai".equals(organId)) {
            return new ShanghaiCAImpl();
        } else {
            System.out.println("请输入正确的类型!");
            return null;
        }
    }

}
