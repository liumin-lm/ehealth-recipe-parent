package com.ngari.recipe.drugsenterprise.model;

import ctd.schema.annotation.ItemProperty;
import ctd.schema.annotation.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 药企
 *
 * @company: ngarihealth
 * @author: 刘敏
 * @date:2021/12/22.
 */

@Schema
@Data
public class DrugsEnterpriseBeanNoDS extends DrugsEnterpriseBean implements Serializable {

    private static final long serialVersionUID = 3811188626885371263L;

    @ItemProperty(alias = "药企联系电话")
    private String tel;

    @ItemProperty(alias = "寄件人手机号")
    private String consignorMobile;

    @ItemProperty(alias = "寄件人详细地址")
    private String consignorAddress;
}
