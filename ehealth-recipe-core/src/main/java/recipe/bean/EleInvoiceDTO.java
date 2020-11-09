package recipe.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName EleInvoiceDTO
 * @Description
 * @Author maoLy
 * @Date 2020/5/8
 **/
public class EleInvoiceDTO implements Serializable {
    private static final long serialVersionUID = 6820457888091344342L;

    private Integer id;
    /*患者mpiid*/
    private String mpiid;
    /*机构id*/
    private Integer organId;
    /*卡类型*/
    private String cardType;
    /*卡号*/
    private String cardId;
    /*业务类型 0 在线复诊 1处方*/
    private String type;
    /*挂号序号*/
    private String ghxh;

    /**
     * 合并处方列表处方id
     */
    private List<Integer> recipeIds;

    public String getMpiid() {
        return mpiid;
    }

    public void setMpiid(String mpiid) {
        this.mpiid = mpiid;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGhxh() {
        return ghxh;
    }

    public void setGhxh(String ghxh) {
        this.ghxh = ghxh;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Integer> getRecipeIds() {
        return recipeIds;
    }

    public void setRecipeIds(List<Integer> recipeIds) {
        this.recipeIds = recipeIds;
    }
}
