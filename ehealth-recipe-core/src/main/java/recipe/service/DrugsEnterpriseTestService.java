package recipe.service;


import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 药企数据准备类，前端不进行调用
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/7/18.
 */
public class DrugsEnterpriseTestService {
    public static final Logger LOGGER = Logger.getLogger(DrugsEnterpriseTestService.class);
    private static final String OS_NAME = "os.name";
    private static final String WINDOWS = "windows";

    /**
     * 分析药品数据，哪些需要加，哪些需要改
     *
     * @return
     */
    public Map<String, Object> analysisDrugList(List<Integer> drugIdList, int organId, boolean useFile) throws IOException {
        String filePth = "/home/cdr_update_1.sql";
        if (System.getProperty(OS_NAME).toLowerCase().contains(WINDOWS)) {
            filePth = "d:/cdr_update_1.sql";
        }
        FileWriter fw = null;
        if (useFile) {
            fw = new FileWriter(filePth, false);
        }
        String lineSign = System.getProperty("line.separator");

        Map<String, Object> reMap = new LinkedHashMap<>();

        if (CollectionUtils.isNotEmpty(drugIdList)) {
            //分析base_saledruglist
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

            SaleDrugList saleDrug;
            List<Integer> saleUpdateId = new ArrayList<>(10);
            List<Integer> saleAddId = new ArrayList<>(10);
            for (Integer drugId : drugIdList) {
                saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(drugId, organId);
                if (null != saleDrug) {
                    if (!Integer.valueOf(1).equals(saleDrug.getStatus())) {
                        //需要修改成status=1
                        saleUpdateId.add(drugId);
                    }
                } else {
                    saleAddId.add(drugId);
                }
            }

            if (CollectionUtils.isNotEmpty(saleAddId)) {
                if (useFile) {
                    fw.write("sale_AddId  size=" + saleAddId.size() + " , detail=" + JSONUtils.toString(saleAddId) + lineSign);
                } else {
                    reMap.put("sale_AddId", "size=" + saleAddId.size() + " , detail=" + JSONUtils.toString(saleAddId));
                }
                StringBuilder addSql = new StringBuilder();
                List<Integer> notExistDrug = new ArrayList<>(10);
                DrugList drug;
                for (Integer drugId : saleAddId) {
                    drug = drugListDAO.get(drugId);
                    if (null != drug) {
                        addSql.append("INSERT INTO base_saledruglist(OrganID,DrugId,OrganDrugCode,Price,Rate,RatePrice,Status) " +
                                "VALUES ( " + organId + ", " + drugId + ", '', " + drug.getPrice1() + ", NULL, NULL, 1);" + lineSign);
                    } else {
                        notExistDrug.add(drugId);
                    }
                }

                if (useFile) {
                    fw.write("sale_NotExistDrug : " + JSONUtils.toString(notExistDrug) + lineSign);
                    fw.write("sale_AddSql : " + lineSign + addSql.toString() + lineSign);
                } else {
                    reMap.put("sale_NotExistDrug", JSONUtils.toString(notExistDrug));
                    reMap.put("sale_AddSql", addSql.toString());
                }
            }

            if (CollectionUtils.isNotEmpty(saleUpdateId)) {
                if (useFile) {
                    fw.write("sale_UpdateId  size=" + saleUpdateId.size() + " , detail=" + JSONUtils.toString(saleUpdateId) + lineSign);
                } else {
                    reMap.put("sale_UpdateId", "size=" + saleUpdateId.size() + " , detail=" + JSONUtils.toString(saleUpdateId));
                }
                StringBuilder updateSql = new StringBuilder();
                updateSql.append("update base_saledruglist set Status=1 where OrganID=" + organId + " and DrugId in (" + StringUtils.join(saleUpdateId, ",") + ");");

                if (useFile) {
                    fw.write("sale_UpdateSql : " + lineSign + updateSql.toString() + lineSign);
                } else {
                    reMap.put("sale_UpdateSql", updateSql.toString());
                }
            }

            if (useFile) {
                fw.write(lineSign + lineSign + lineSign);
            }

            //分析base_organdruglist
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            OrganDrugList organDrug;
            List<Integer> organUpdateId = new ArrayList<>(10);
            //可能是带*的一些药品
            List<Integer> organUpdateNotFilterId = new ArrayList<>(10);
            List<Integer> organAddId = new ArrayList<>(10);
            DrugList drug;
            for (Integer drugId : drugIdList) {
                organDrug = organDrugListDAO.getByDrugIdAndOrganId(drugId, organId);
                if (null != organDrug) {
                    if (!Integer.valueOf(1).equals(organDrug.getStatus())) {
                        drug = drugListDAO.get(drugId);
                        if (null != drug) {
                            boolean use = isOrganCanUseDrug(drug.getDrugName());
                            if (use) {
                                organUpdateId.add(drugId);
                            } else {
                                organUpdateNotFilterId.add(drugId);
                            }
                        }
                    }
                } else {
                    organAddId.add(drugId);
                }
            }

            if (CollectionUtils.isNotEmpty(organAddId)) {
                if (useFile) {
                    fw.write("organ_AddId  size=" + organAddId.size() + " , detail=" + JSONUtils.toString(organAddId) + lineSign);
                } else {
                    reMap.put("organ_AddId", "size=" + organAddId.size() + " , detail=" + JSONUtils.toString(organAddId));
                }
                StringBuilder addSql = new StringBuilder();
                List<Integer> notExistDrug = new ArrayList<>(10);
                for (Integer drugId : organAddId) {
                    drug = drugListDAO.get(drugId);
                    if (null != drug) {
                        boolean use = isOrganCanUseDrug(drug.getDrugName());

                        addSql.append("INSERT INTO base_organdruglist(OrganID,DrugId,OrganDrugCode,salePrice,ProducerCode,Status) " +
                                "VALUES (" + organId + ", " + drugId + ", '"+drugId+"', " + drug.getPrice1() + ", '', " + (use ? 1 : 0) + ");" + lineSign);
                    } else {
                        notExistDrug.add(drugId);
                    }
                }

                if (useFile) {
                    fw.write("organ_NotExistDrug : " + JSONUtils.toString(notExistDrug) + lineSign);
                    fw.write("organ_AddSql : " + lineSign + addSql.toString() + lineSign);
                } else {
                    reMap.put("organ_NotExistDrug", JSONUtils.toString(notExistDrug));
                    reMap.put("organ_AddSql", addSql.toString());
                }
            }

            if (CollectionUtils.isNotEmpty(organUpdateId)) {
                if (useFile) {
                    fw.write("organ_notFilterId  size=" + organUpdateNotFilterId.size() + " , detail=" + JSONUtils.toString(organUpdateNotFilterId) + lineSign);
                    fw.write("organ_UpdateId  size=" + organUpdateId.size() + " , detail=" + JSONUtils.toString(organUpdateId) + lineSign);
                } else {
                    reMap.put("organ_notFilterId", "size=" + organUpdateNotFilterId.size() + " , detail=" + JSONUtils.toString(organUpdateNotFilterId));
                    reMap.put("organ_UpdateId", "size=" + organUpdateId.size() + " , detail=" + JSONUtils.toString(organUpdateId));
                }

                StringBuilder updateSql = new StringBuilder();
                updateSql.append("update base_organdruglist set Status=1 where OrganID=" + organId + " and DrugId in (" + StringUtils.join(organUpdateId, ",") + ");");

                if (useFile) {
                    fw.write("organ_UpdateSql : " + lineSign + updateSql.toString() + lineSign);
                } else {
                    reMap.put("organ_UpdateSql", updateSql.toString());
                }
            }

        }

        if (useFile) {
            fw.flush();
            fw.close();
            reMap.put("info", "see file path:" + filePth);
        }

        return reMap;
    }

    /**
     * base_organdruglist不能开具的药品规则
     *
     * @param drugName
     * @return
     */
    private boolean isOrganCanUseDrug(String drugName) {
        boolean bl = true;

        if (StringUtils.isEmpty(drugName)) {
            bl = false;
        }

        if (bl) {
            String s1 = "*";
            String s2 = "赠药";
            if (drugName.startsWith(s1) || drugName.contains(s2)) {
                bl = false;
            }
        }

        return bl;
    }
}
