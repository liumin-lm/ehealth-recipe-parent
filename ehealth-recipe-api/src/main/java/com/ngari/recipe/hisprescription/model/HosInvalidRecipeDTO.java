package com.ngari.recipe.hisprescription.model;

import java.util.List;

/**
 * @author yinsheng
 * @date 2019\12\25 0025 15:32
 */
public class HosInvalidRecipeDTO {

    private String recipeno;
    private String invaliddate;
    private String optmanname;
    private List<DetaillistDTO> detaillist;

    public String getRecipeno() {
        return recipeno;
    }

    public void setRecipeno(String recipeno) {
        this.recipeno = recipeno;
    }

    public String getInvaliddate() {
        return invaliddate;
    }

    public void setInvaliddate(String invaliddate) {
        this.invaliddate = invaliddate;
    }

    public String getOptmanname() {
        return optmanname;
    }

    public void setOptmanname(String optmanname) {
        this.optmanname = optmanname;
    }

    public List<DetaillistDTO> getDetaillist() {
        return detaillist;
    }

    public void setDetaillist(List<DetaillistDTO> detaillist) {
        this.detaillist = detaillist;
    }
}
