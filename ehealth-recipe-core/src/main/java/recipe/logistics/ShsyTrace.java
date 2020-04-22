package recipe.logistics;

import java.io.Serializable;

/**
 * @ClassName ShsyTrace
 * @Description
 * @Author maoLy
 * @Date 2020/4/15
 **/
public class ShsyTrace implements Serializable {
    private static final long serialVersionUID = 7967759099215216195L;

    private String processNo;
    private String processTime;
    private String processRemark;

    public String getProcessNo() {
        return processNo;
    }

    public void setProcessNo(String processNo) {
        this.processNo = processNo;
    }

    public String getProcessRemark() {
        return processRemark;
    }

    public void setProcessRemark(String processRemark) {
        this.processRemark = processRemark;
    }

    public String getProcessTime() {
        return processTime;
    }

    public void setProcessTime(String processTime) {
        this.processTime = processTime;
    }
}
