package recipe.audit.list.request;

import java.io.Serializable;
import java.util.List;

/**
 * 审核处方列表请求
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/3/8
 */
public class AuditListReq implements Serializable {

    private static final long serialVersionUID = 6157100388773406840L;

    private Integer doctorId;

    private Integer status;

    private List<Integer> organIdList;

    public AuditListReq() {
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Integer> getOrganIdList() {
        return organIdList;
    }

    public void setOrganIdList(List<Integer> organIdList) {
        this.organIdList = organIdList;
    }

}
