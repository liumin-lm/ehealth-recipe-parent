package recipe.drugsenterprise;

import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.filedownload.service.IFileDownloadService;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.service.common.RecipeCacheService;
import recipe.util.DistanceUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
* @Description: YtRemoteService 类（或接口）是 对接英特药企服务接口
* @Author: JRK
* @Date: 2019/7/8
*/
@RpcBean("ytRemoteService")
public class YtRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YtRemoteService.class);

    private String ORGANIZATION;

    private static final String pushRecipeHttpUrl = "prescriptions/digit";

    private static final String getStock = "medicine";

    private static final String searchMapRANGE = "range";

    private static final String searchMapLatitude = "latitude";

    private static final String searchMapLongitude = "longitude";

    private static final String ytPayMethod = "0";

    private static final String ytSendMethod = "1";

    private static final String requestHeadJsonKey = "Content-Type";

    private static final String requestHeadJsonValue = "application/json";

    private static final String requestHeadPowerKey = "Authorization";

    private static final Integer ytHzMedicalFlag = 2;

    private static final String ytpTimeCheck = "yyyy-MM-dd";

    private static final Integer ytValidDay = 3;

    private static final Integer ytCostType = 1;

    private static final Double ytTransFee = 0d;

    private static final Integer ytIfPay = 1;

    private static final Integer ytSource = 4;

    public YtRemoteService() {
        RecipeCacheService recipeService = ApplicationUtils.getRecipeService(RecipeCacheService.class);
        ORGANIZATION = recipeService.getRecipeParam("organization", "");
    }

    @Override
    @RpcService
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        Integer depId = drugsEnterprise.getId();
        String depName = drugsEnterprise.getName();
        LOGGER.info("YtRemoteService.tokenUpdateImpl:[{}][{}]token更新", depId, depName);
        //获取药企对应的用户名，密码和机构
        YtTokenRequest request = new YtTokenRequest();
        //组装请求，判断是否组装成功
        if (!AssembleRequest(drugsEnterprise, depId, depName, request)) return;
        //发送http请求获取
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            if (drugsEnterprise.getAuthenUrl().contains("http:")) {
                sendTokenAndUpdateHttpRequest(drugsEnterprise, drugsEnterpriseDAO, depId, depName, request, httpclient);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("YtRemoteService.tokenUpdateImpl:[{}][{}]更新token异常：{}", depId, depName, e.getMessage());
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("YtRemoteService.tokenUpdateImpl:http请求资源关闭异常: {}", e.getMessage());
            }
        }

    }
    /**
     * @method  sendTokenAndUpdateHttpRequest
     * @description 发送http请求获得新的token信息,并更新
     * @date: 2019/7/8
     * @author: JRK
     * @param drugsEnterprise
     * @param drugsEnterpriseDAO 药企数据访问层
     * @param depId 药企id
     * @param depName 药企名
     * @param request 英特请求对象
     * @param httpclient http请求服务
     * @return void
     */
    private void sendTokenAndUpdateHttpRequest(DrugsEnterprise drugsEnterprise, DrugsEnterpriseDAO drugsEnterpriseDAO, Integer depId, String depName, YtTokenRequest request, CloseableHttpClient httpclient) throws IOException {
        //生成post请求
        HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
        httpPost.setHeader("Content-Type", "application/json");
        //将请求参数转成json
        StringEntity requestEntity = new StringEntity(JSONUtils.toString(request), ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);
        //获取响应消息
        LOGGER.info("YtRemoteService.tokenUpdateImpl:发送[{}][{}]token更新请求", depId, depName);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        String responseStr =  EntityUtils.toString(responseEntity);
        if(200 == response.getStatusLine().getStatusCode()){
            LOGGER.info("YtRemoteService.tokenUpdateImpl:[{}][{}]token更新请求返回:{}", depId, depName, responseStr);
            YtTokenResponse tokenResponse = JSONUtils.parse(responseStr, YtTokenResponse.class);
            String newToken = tokenResponse.getTOKEN();
            //当更新请求返回的新token不为空的时候进行更新token的操作
            if(null != tokenResponse && null != newToken){
                drugsEnterpriseDAO.updateTokenById(depId, newToken);
                LOGGER.info("YtRemoteService.tokenUpdateImpl:[{}][{}]token更新成功:{}", depId, depName, newToken);
            }else{
                LOGGER.warn("YtRemoteService.tokenUpdateImpl:[{}][{}]token更新token请求失败", depId, depName);
            }
        }else{
            LOGGER.warn("YtRemoteService.tokenUpdateImpl:[{}][{}]token更新请求，请求未成功,原因：{}", depId, depName, responseStr);
        }
        //关闭 HttpEntity 输入流
        EntityUtils.consume(responseEntity);
        response.close();
    }

    /**
     * @method  AssembleRequest
     * @description 组装请求对象
     * @date: 2019/7/8
     * @author: JRK
     * @param drugsEnterprise
     * @param depId 药企id
     * @param depName 药企名称
     * @param request 请求对象
     * @return java.lang.Boolean 是否组装成功
     */
    private Boolean AssembleRequest(DrugsEnterprise drugsEnterprise, Integer depId, String depName, YtTokenRequest request) {
        String userId = drugsEnterprise.getUserId();
        String password = drugsEnterprise.getPassword();
        if (null != userId){
            request.setUser(userId.toString());
        }else{
            LOGGER.warn("YtRemoteService.tokenUpdateImpl:[{}][{}]的userId为空。", depId, depName);
            return false;
        }
        request.setPassword(password);
        request.setOrganization(ORGANIZATION);
        return true;
    }

    @Override
    @RpcService
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        //首先校验请求的数据是否合规
        if (StringUtils.isEmpty(enterprise.getBusinessUrl())) {
            LOGGER.warn("YtRemoteService.pushRecipeInfo:[{}][{}]药企的访问url为空", depId, depName);
            getFailResult(result, "药企的访问url为空");
            return result;
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方列表为空");
            getFailResult(result, "处方列表为空");
            return result;
        }
        //组装YtRecipeDTO和处方明细下的YtDrugDTO
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);

        YtRecipeDTO sendYtRecipe = new YtRecipeDTO();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isNotEmpty(recipeList)) {
            Recipe nowRecipe = recipeList.get(0);
            DrugEnterpriseResult assembleResult = assemblePushRecipeMessage(result, recipeIds, organService, sendYtRecipe, nowRecipe, enterprise);
            if (DrugEnterpriseResult.FAIL == assembleResult.getCode())
                return assembleResult;

            //发送请求，获得推送的结果
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                if (enterprise.getBusinessUrl().contains("http:")) {
                    pushRecipeHttpRequest(result, enterprise, depId, depName, sendYtRecipe, httpClient);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("YtRemoteService.pushRecipeInfo:[{}][{}]更新token异常：{}", depId, depName, e.getMessage());
            } finally {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("YtRemoteService.pushRecipeInfo:http请求资源关闭异常: {}！", e.getMessage());
                }
            }

        }else{
            LOGGER.warn("YtRemoteService.pushRecipeInfo:未查询到匹配的处方列表");
            getFailResult(result, "未查询到匹配的处方列表");
            return result;
        }
        return result;
    }
    /**
     * @method  pushRecipeHttpRequest
     * @description 推送处方http请求
     * @date: 2019/7/10
     * @author: JRK
     * @param enterprise 药企
     * @param depId 药企id
     * @param depName 药企name
     * @param sendYtRecipe 推送的处方信息
     * @param httpClient 请求服务
     * @return void
     */
    private void pushRecipeHttpRequest(DrugEnterpriseResult result, DrugsEnterprise enterprise, Integer depId, String depName, YtRecipeDTO sendYtRecipe, CloseableHttpClient httpClient) throws IOException {
        HttpPost httpPost = new HttpPost(enterprise.getBusinessUrl() + pushRecipeHttpUrl);
        //组装请求参数(组装权限验证部分)
        httpPost.setHeader(requestHeadJsonKey, requestHeadJsonValue);
        httpPost.setHeader(requestHeadPowerKey, enterprise.getToken());
        String requestStr = JSONUtils.toString(sendYtRecipe);
        LOGGER.info("YtRemoteService.pushRecipeInfo:[{}][{}]推送处方请求，请求内容：{}", depId, depName, requestStr);
        StringEntity requestEntry = new StringEntity(requestStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntry);

        //获取响应消息
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity httpEntity = response.getEntity();
        String responseStr = EntityUtils.toString(httpEntity);
        if(200 == response.getStatusLine().getStatusCode()){
            LOGGER.info("YtRemoteService.pushRecipeInfo:[{}][{}]处方推送成功，请求返回:{}", depId, depName, responseStr);
        }else{
            LOGGER.warn("YtRemoteService.pushRecipeInfo:[{}][{}]处方推送失败, 原因：{}", depId, depName, responseStr);
            getFailResult(result, "处方推送失败");
        }
        //关闭 HttpEntity 输入流
        EntityUtils.consume(httpEntity);
        response.close();
    }

    /**
     * @method  assemblePushRecipeMessage
     * @description 组装推送的处方数据
     * @date: 2019/7/10
     * @author: JRK
     * @param result 返回的操作结果
     * @param recipeIds 推送的处方单
     * @param organService 机构service
     * @param sendYtRecipe 组装的订单信息
     * @param nowRecipe 当前的订单
     * @return recipe.bean.DrugEnterpriseResult 操作的结果集
     */
    private DrugEnterpriseResult assemblePushRecipeMessage(DrugEnterpriseResult result, List<Integer> recipeIds, OrganService organService, YtRecipeDTO sendYtRecipe, Recipe nowRecipe, DrugsEnterprise enterprise) {
        //检验并组装处方对应的机构信息
        if(null == nowRecipe){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},处方不存在,", recipeIds.get(0));
            getFailResult(result, "处方单不存在");
            return result;
        }
        if (null == nowRecipe.getClinicOrgan()) {
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定的机构id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "开处方机构编码不存在");
            return result;
        }
        OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
        if(null != organ){
            //这里保存的医院的社保编码
            sendYtRecipe.setHospitalCode(organ.getOrganizeCode());
            sendYtRecipe.setHospitalName(organ.getName());
        }else{
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},对应的开处方机构不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "开处方机构不存在");
            return result;
        }
        //检验并组装处方信息（上面已经校验过了）
        assembleRecipeMsg(sendYtRecipe, nowRecipe);
        //检验并组装处方对应的医生信息
        assembleDoctorMsg(result, sendYtRecipe, nowRecipe);
        if(result.FAIL == result.getCode()){
            return result;
        }
        //检验并组装处方对应的患者信息
        assemblePatientMsg(result, sendYtRecipe, nowRecipe);
        if(result.FAIL == result.getCode()){
            return result;
        }
        //检验并组装处方对应的科室信息
        assembleDepartMsg(result, sendYtRecipe, nowRecipe);
        if(result.FAIL == result.getCode()){
            return result;
        }
        //检验并组装处方对应的门店信息
        assembleStoreMsg(result, sendYtRecipe, nowRecipe);
        if(result.FAIL == result.getCode()){
            return result;
        }
        //设置处方笺base
        String ossId = nowRecipe.getSignImg();
        StringBuilder sb = new StringBuilder();
        sb.append("data:image/jpeg;base64,/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAPoAxkDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD3+iiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKp6rqtjoel3Gp6ncx21nbpvllfoo/mSTgADkkgDJNXK87+J3hi2vn0vxHJocmuSaU7F7KbU0tbURbSxkk8wFcKVXIGNwxu3KuKAOfsPHnjTW3/wCE007TbuXwnFei0XRre2V7q4h2sGuNxGSVcp8qEjKsCQFZj6J4Y8beH/F1nDNpGpQSzSRea1o0iieIA4O+PORgkDPTkYJBBPzBqVnpcPjTT5Y/BEC6YdPlun02DXvtCXKRiYtKLhScbfLPyg/8ssfxV7/8MbiDV9OXV4vBtjosJtIYLO9hu4rqS5hUbdjOo3jYI0BDnORg8rQBHB46lufi5Fo6NqQ0mTRxItvJpMyMLgz7d53Rh1TbxuOEHc5rn5PiP4rTx5DdweFfF0nhd7fbcWU+ijzY5cHDRFcHGQn3mP3n4+7jL0yHxjrfxIvvENndf2TrN5aTGw0zU49oSyhuLdVEyHe6LIDLwoU70LKcNWB4s8Qan/whuua/p3iPVfO/4TC4sree31Sby/svls6qih9m3OCCB074oA9r1PxyuneA5PFDaDrIIRyunzWrJOpUsMyAZEaYUsXPAXB5JAPmekfGjUvD0DWXiU2OsTxRJdzXtrqlu25HdEMUSRRhTIm4kqW52Od23bXsniWa+tvCurz6WJDqEdlM9qI497GUISmFwdx3Y4wc182fEzUPEFx4N0lPEPie+u7qeVJjpd34fax8pxGd5EpVRJsL7eOu4GgDp/BvxD+Iur6xFoMWreFL+7ubQX0M963VSqkw4gxiRfmJQruG1jkrg19AV4fY21rZ/FDw9r3ibxtfXmpndZWUF34ZnsfP3BkChtoXhps5x3GSK9A1vw74yvtYnudJ8d/2XYvt8uz/ALIhn8vCgH52OTkgnnpnHagDsKK8/wD+ES+If/RT/wDygW/+NbHhvQ/FWmajJNrnjH+2rVoii2/9mRW218gh9yHJwARj39qANDxZfXGmeDdcv7OT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qQKVUPvlfarswIETM44AIbg8f4u8XunwxluPDb6rY2t7aPeRXNzqLT3OUubOP5JBNIUUiV1KEggqcgc5APcKK8n8VeLPEel694gFlLfXVraxTsttbG1i+zpDFYytLvljYnieYEYfOVAUfeGf8K/GniC68QpoXiS4nmu5IpJHEuo2bbJGdn2+SFEysoV1KbmKcAoqkbQD2iivH9F8WeMpdHV4rrzdQubTSpYBrUEPks1y0yuytbldkZ2KV8zLErsClnUHqNJur+90fWNMv5f7WbT5UtlTRJJIJGCt/wA/D3OTIABvVnV1IYHeHRmAO4or5Q/t7W26aj8QFjS7/sxpG1GUySXR6FYhHtDLtOYDLuJdQHwGavd7661Gyg0extrfxHp1pJFcSzPEi313GyOm1Xkfzl2lHlfGSx2KqfN8jAHcUVx4+IWg21hCIp9V1WbykEZg0uZ3unaATKoKxhPMaMh9vy4zyFAOJJviN4eSEPbHUr53RTDHaaZcSGZmhE6op2bd5jIbBIwDzjBwAdZRWPcX1xP4otNMtJNkdvEbu/O0HKMGjij5/vMHfcp48jBGHFcHrniHxBqbrA9td2F3aPGZbbS5btzbzbY5R5k0dnJHKdrbTFgoASSXLL5YB6pRXN+E9X1nWLU3F9HpslpvmRLu2M8LuySlMNbyxgxkbSCC7YK8ZB46SgAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoorj7DVNZu/Gkuii9ga30yWae+cIjPJFKAbWIgH93jfIDwWP2ZWOBKMgHYUVxehajq8fjV9G1HWI7yU6YLq8tWtxGLWbcuPs7bVMsDbnXJDlTGAzAtg6HjbWptH0SCOzvI7TUNSvbfTrOZ4TKEklcKW29CVTew3EAlQDnOCAdJRXDx69rui6XrYuDBq/9kag8ct5cuLXy7X7Mtxvk8tG3Mu8R/Ig3cHA5rn4Ne8Q2etPdpPOWupYbcrqNtqCWk7NMFXarWoFoxDhQQ7rwoYSMTJQB6xRVexe8ks42v4IILo53xwTGVF5OMMVUnjH8I9OetWKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii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PFatJ5EMnBuY1zKPunKo3y9Gx8xXkAjaCcjn/Fdr4k1fSdW0ew0/Sja3tpJbJcz6hIjrvjKljGIGHBJ43c47Z4ANTTdVvL64aK48P6lp6BCwlupLdlJyPlHlyuc856Y4PPTPnd747uIr++1W38X+FIIZruCxt9Oe4F0VgE+z7SSs6BWIkaRgBwiICQQSPQLG58SSXka3+laVBanO+SDU5JXXg4wpgUHnH8Q9eelSa/ps2q6dFbwNGrpe2lwS5IG2K4jlYcA87UIHvjp1oAr+GtZh1a3nVfEGjaxPE4LvpYCrGpHyhl82Q5JDc5GfTiqei6w48M22vXCX11NrXl3cVpbRNKITJEuyFeyKAoy7lULszEoGwOorj7rwheXGiwaLHdWK6dY3am0gu7U3UctqIdqwzJuTdsdiVOTxFGTubJoAsP8AELw4iqfPvm3YBCaXdMUJleFVYCPKMZI3QK2CSOBUena+t1f6VqNq12NP1t5bZ7W6VlltruJGONrfcAWGZXAJG9UKj5nY09K+HSaVaPbRalujMtrIv+iqm0QX0t2BhCFGfN2fKABtyBg7RqaP4amstSM93cRzQW9xdzWcag/euJWlaRweA6hzGuOilzk+ZtQA6SiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoooo");
        sb.append("AKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoqvf/AGz+zrn+zvI+3eU/2f7Rny/Mwdu/HO3OM45xXP8AhVtZi1PV9O1PWP7Yhs/s6i7e2SB1naPdLEQmAVAMTg4483buYqcAHUUUVxfxF1bULLRpbHSLySLULuyujDDBptxdTylVUAxvEw8khnUb2BGWU9uQDtKK4O9vPGTzaPfaZBHcTrZXkk9vcW8lrFcgTQeWuDI3kytHvKGQkj5gyrlgvJ+IPEl1b/GMQoL77Db6rpdjLGmrzxo7zxTMGEQ+TbnyyR38nv5jYAPaKK8z8Ha7fah4qG7xfHqEdw90txpf9m+VJA8blQzKZmkgGFAXcoUgfMBLIC3Car4x8ZaLrGow33iLxHa3AlaSaO38MwzW0eFb/UtLKGMO2GQq2BuCMxGdxoA+h6K4fSdb1bTPhnf65e3E+sX9l9qkmS9SKzP7h2R0AhDqMeW2OWye4B+Xj9H8V+LtY8M+F9Q0p4J7W18qG7nSVJjJN5TRbZjLcQEyF3Q+XtIyUcSPwpAPaKK8/wDHetavafDWe8eLVdNvBaSmae2e0i8qQKVUPvlfarswIETM44AIbg8f4u8XunwxluPDb6rY2t7aPeRXNzqLT3OUubOP5JBNIUUiV1KEggqcgc5APcKK8n8VeLPEel694gFlLfXVraxTsttbG1i+zpDFYytLvljYnieYEYfOVAUfeGf8K/GniC68QpoXiS4nmu5IpJHEuo2bbJGdn2+SFEysoV1KbmKcAoqkbQD2iivH9F8WeMpdHV4rrzdQubTSpYBrUEPks1y0yuytbldkZ2KV8zLErsClnUH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/pWlQWpzvkg1OSV14OMKYFB5x/EPXnpUmv6bNqunRW8DRq6XtpcEuSBtiuI5WHAPO1CB746daAK/hrWYdWt51XxBo2sTxOC76WAqxqR8oZfNkOSQ3ORn04qnousOPDNtr1wl9dTa15d3FaW0TSiEyRLshXsigKMu5VC7MxKBsDqK4+68IXlxosGix3ViunWN2ptILu1N1HLaiHasMybk3bHYlTk8RRk7myaALD/ELw4iqfPvm3YBCaXdMUJleFVYCPKMZI3QK2CSOBUena+t1f6VqNq12NP1t5bZ7W6VlltruJGONrfcAWGZXAJG9UKj5nY09K+HSaVaPbRalujMtrIv+iqm0QX0t2BhCFGfN2fKABtyBg7RqaP4amstSM93cRzQW9xdzWcag/euJWlaRweA6hzGuOilzk+ZtQA6SiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoqvf/bP7Ouf7O8j7d5T/Z/tGfL8zB278c7c4zjnFc34V1G7m8QeINLm1iTVILB4Qr3NukNxDIwbfG6qqBkwqOkgQBhIQC23gA6yiiuL+IurahZaNLY6ReSRahd2V0YYYNNuLqeUqqgGN4mHkkM6jewIyyntyAdpRXB3t54yebR77TII7idbK8knt7i3ktYrkCaDy1wZG8mVo95QyEkfMGVcsF5PxB4kurf4xiFBffYbfVdLsZY01eeNHeeKZgwiHybc+WSO/k9/MbAB7RR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FFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFcP8AEq+tLbw9qBaTXIb+30+ea1m09bxI1fYdpd4f3fBUH5zwOeAecvSZ9Hbx1Nbxy+LjAlvZvbxztqu1ZmlnDGQPwEIWMZf5Dtb0agD0yivM/HereI7HWdN0dvE+jaPYak80w1J4PJks1gaN0XLz7ZSxZUIwMgsduM40NQ1bUrrwL4l1K38T6NqMEGmXQjm0iBo2imWIsD5gnkAIHOMA8g59QDvKK8X1HXLCx8Rz2IuYEtRdmyjF546uopkZEdnllCSSBIyVCKDhg2MgbgF6yS6ubfwHDLo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BRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAf/2Q==");
        String str = sb.toString();
        sendYtRecipe.setImage(str);
        if(null != ossId){
            try {
                IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                String imgStr = fileDownloadService.downloadImg(ossId);
                if(ObjectUtils.isEmpty(imgStr)){
                    LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{}的ossid为{}处方笺不存在", nowRecipe.getRecipeId(), ossId);
                    getFailResult(result, "处方笺不存在");
                    return result;
                }
                sendYtRecipe.setImage(imgStr);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("YtRemoteService.pushRecipeInfo:获取图片异常：{}", e.getMessage());
            }
        }
        //检验并组装处方对应的详情信息
        assembleDrugListMsg(result, sendYtRecipe, nowRecipe, organ, enterprise);
        if(result.FAIL == result.getCode()){
            return result;
        }
        return result;
    }
    /**
     * @method  assembleRecipeMsg
     * @description 组装推送的处方单中的处方信息
     * @date: 2019/7/10
     * @author: JRK
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前的处方单
     * @return void
     */
    private void assembleRecipeMsg(YtRecipeDTO sendYtRecipe, Recipe nowRecipe) {
        sendYtRecipe.setSourceSerialNumber(nowRecipe.getRecipeCode());
        sendYtRecipe.setExternalNumber("");
        sendYtRecipe.setHzMedicalFlag(ytHzMedicalFlag);
        sendYtRecipe.setpTime(getNewTime(nowRecipe.getSignDate(), ytpTimeCheck));
        sendYtRecipe.setValidDay(ytValidDay);
        sendYtRecipe.setCostType(ytCostType);
        //sendYtRecipe.setRecordNo(nowRecipe.getPatientID());
        sendYtRecipe.setRecordNo("dsadas");
        sendYtRecipe.setTotalAmount(nowRecipe.getTotalMoney().doubleValue());
        sendYtRecipe.setTransFee(ytTransFee);
        sendYtRecipe.setIfPay(ytIfPay);
        sendYtRecipe.setSource(ytSource);
    }

    /**
     * @method  assembleDrugListMsg
     * @description 组装推送的处方单中药品详情的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @param organ 所属机构
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDrugListMsg(DrugEnterpriseResult result, YtRecipeDTO sendYtRecipe, Recipe nowRecipe, OrganDTO organ ,DrugsEnterprise enterprise) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定订单不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定订单不存在");
            return result;
        }
        List<YtDrugDTO> drugList = new ArrayList<>();
        sendYtRecipe.setItemList(drugList);
        double price = 0d;
        double quantity = 0d;
        YtDrugDTO nowYtDrugDTO;
        SaleDrugList saleDrug;
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (Recipedetail nowDetail : detailList) {
            nowYtDrugDTO = new YtDrugDTO();
            drugList.add(nowYtDrugDTO);
            nowYtDrugDTO.setSourceSerialNumber(nowRecipe.getRecipeCode());
            nowYtDrugDTO.setHospitalCode(organ.getOrganizeCode());
            if(null == nowDetail.getRecipeDetailId()){
                LOGGER.warn("YtRemoteService.pushRecipeInfo:当前处方细节id不存在");
                getFailResult(result, "当前处方细节id不存在");
                return result;
            }
            nowYtDrugDTO.setItemNo(nowDetail.getRecipeDetailId().toString());
            //根据药品id和所属的药企在salrDrug下获取药品的编码
            saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(nowDetail.getDrugId(), enterprise.getId());
            if(null == saleDrug){
                LOGGER.warn("YtRemoteService.pushRecipeInfo:处方细节ID为{},对应销售药品的信息不存在", nowDetail.getRecipeDetailId());
                getFailResult(result, "销售药品的信息不存在");
                return result;
            }
            nowYtDrugDTO.setCode(saleDrug.getOrganDrugCode());
            if(null == nowDetail.getSalePrice()){
                LOGGER.warn("YtRemoteService.pushRecipeInfo:处方细节ID为{},药品的单价为空", nowDetail.getRecipeDetailId());
                getFailResult(result, "药品的单价为空");
                return result;
            }
            price = nowDetail.getSalePrice().doubleValue();
            quantity = nowDetail.getUseTotalDose();

            nowYtDrugDTO.setQuantity(quantity);
            nowYtDrugDTO.setPrice(price);
            nowYtDrugDTO.setAmount(price * quantity);
            nowYtDrugDTO.setUsage(nowDetail.getUsingRate());
        }
        return null;
    }

    /**
     * @method  assembleDoctorMsg
     * @description 组装推送的处方单中医生的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDoctorMsg(DrugEnterpriseResult result, YtRecipeDTO sendYtRecipe, Recipe nowRecipe) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        if(null == nowRecipe.getDoctor()){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定医生id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定医生id为空");
            return result;
        }
        DoctorDTO doctor = doctorService.get(nowRecipe.getDoctor());
        if(null == doctor){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定医生不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定医生不存在");
            return result;
        }
        sendYtRecipe.setDoctorName(doctor.getName());
        return null;
    }

    /**
     * @method  assembleStoreMsg
     * @description 组装推送的处方单中药店的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleStoreMsg(DrugEnterpriseResult result, YtRecipeDTO sendYtRecipe, Recipe nowRecipe) {
//        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
//        if(null == nowRecipe.getOrderCode()){
//            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定订单code为空.", nowRecipe.getRecipeId());
//            getFailResult(result, "处方绑定订单code为空");
//            return result;
//        }
//        RecipeOrder order = orderDAO.getByOrderCode(nowRecipe.getOrderCode());
//        if(null == order){
//            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定订单不存在.", nowRecipe.getRecipeId());
//            getFailResult(result, "处方绑定订单不存在");
//            return result;
//        }
//        sendYtRecipe.setOrgCode(order.getDrugStoreCode());
        sendYtRecipe.setOrgCode("YMO0138870");
        return null;
    }

    /**
     * @method  assembleDepartMsg
     * @description 组装推送的处方单中科室的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assembleDepartMsg(DrugEnterpriseResult result, YtRecipeDTO sendYtRecipe, Recipe nowRecipe) {
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        if(null == nowRecipe.getDepart()){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定科室id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定科室id为空");
            return result;
        }
        DepartmentDTO department = departmentService.get(nowRecipe.getDepart());
        if(null == department){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定科室不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定科室不存在");
            return result;
        }
        sendYtRecipe.setCategory(department.getName());
        return null;
    }

    /**
     * @method  assemblePatientMsg
     * @description 组装推送的处方单中患者的信息
     * @date: 2019/7/10
     * @author: JRK
     * @param result 结果集
     * @param sendYtRecipe 推送的处方单信息
     * @param nowRecipe 当前处方
     * @return recipe.bean.DrugEnterpriseResult 操作结果集
     */
    private DrugEnterpriseResult assemblePatientMsg(DrugEnterpriseResult result, YtRecipeDTO sendYtRecipe, Recipe nowRecipe) {
        PatientService patientService = BasicAPI.getService(PatientService.class);
        if(null == nowRecipe.getMpiid()){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定患者id为空.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定患者id为空");
            return result;
        }
        PatientDTO patient = patientService.get(nowRecipe.getMpiid());
        if(null == patient){
            LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定患者不存在.", nowRecipe.getRecipeId());
            getFailResult(result, "处方绑定患者不存在");
            return result;
        }
        sendYtRecipe.setPatientName(patient.getPatientName());
        sendYtRecipe.setSex(Integer.parseInt(patient.getPatientSex()));
        //sendYtRecipe.setAge(patient.getAge());
        sendYtRecipe.setAge(11);
        sendYtRecipe.setPhone("");
        sendYtRecipe.setAddress("");
        //sendYtRecipe.setSymptom(patient.getLastSummary());
        sendYtRecipe.setSymptom("dfds");
        sendYtRecipe.setRecipientName("");
        sendYtRecipe.setRecipientAdd("");
        sendYtRecipe.setRecipientTel("");
        sendYtRecipe.setZipCode("");
        return result;
    }

    /**
     * @method  getNewTime
     * @description 根据具体的格式转换时间成字符串
     * @date: 2019/7/10
     * @author: JRK
     * @param date 需要转换的时间
     * @param type 转换的格式
     * @return java.lang.String 转换的时间字符串
     */
    private String getNewTime(Date date, String type) {
        SimpleDateFormat formatter = new SimpleDateFormat(type);
        return formatter.format(date);
    }

    /**
     * @method  getFailResult
     * @description 失败操作的结果对象
     * @date: 2019/7/10
     * @author: JRK
     * @param result 返回的结果集对象
     * @param msg 失败提示的信息
     * @return
     */
    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }

    @Override
    @RpcService
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();

        Integer depId = drugsEnterprise.getId();
        String depName = drugsEnterprise.getName();
        //查询当前处方信息
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe nowRecipe = recipeDAO.get(recipeId);
        //查询当前处方下详情信息
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());
        Map<Integer, DetailDrugGroup> drugGroup = getDetailGroup(detailList);
        //获取药企下所有的药店
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        List<Pharmacy> pharmacyList = pharmacyDAO.findByDrugsenterpriseIdAndStatus(drugsEnterprise.getId(), 1);
        SaleDrugList saleDrug = null;
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        //遍历药店，判断当有一个药店的所有的药品的库存量都够的话判断为库存足够
        boolean checkScan = false;
        for (Pharmacy pharmacy : pharmacyList) {
            GroupSumResult groupSumResult = checkDrugListByDeil(drugGroup, drugsEnterprise, saleDrugListDAO, depId, saleDrug, result, pharmacy, false);
            //只有当某一家药店有所有处方详情下的药品并且库存不超过，查询库存的结果设为成功
            if(groupSumResult.getComplacentNum() >= drugGroup.size()){
                checkScan = true;
                break;
            }
        }
        if(!checkScan){
            getFailResult(result, "当前药企下没有药店的药品库存足够");
        }
        return result;
    }

    /**
     * @method  checkDrugListByDeil
     * @description 校验某一家药店下的药品是否符合库存量
     * @date: 2019/7/11
     * @author: JRK
     * @param drugGroup 处方详情的药品分组
     * @param drugsEnterprise 药企
     * @param saleDrugListDAO 销售药品dao
     * @param depId 药企id
     * @param saleDrug 销售药品
     * @param result 结果集
     * @param pharmacy 药店
     * @return void
    */
    private GroupSumResult checkDrugListByDeil(Map<Integer, DetailDrugGroup> drugGroup, DrugsEnterprise drugsEnterprise, SaleDrugListDAO saleDrugListDAO, int depId, SaleDrugList saleDrug, DrugEnterpriseResult result, Pharmacy pharmacy, boolean sumFlag) {
        GroupSumResult groupSumResult = new GroupSumResult();
        for (Map.Entry<Integer, DetailDrugGroup> entry : drugGroup.entrySet()) {
            //发送请求访问药品的库存
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                if (drugsEnterprise.getBusinessUrl().contains("http:")) {
                    //拼接查询url
                    saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(entry.getKey(), depId);
                    if(null == saleDrug){
                        LOGGER.warn("YtRemoteService.pushRecipeInfo:处方细节ID为{},对应销售药品的信息不存在", entry.getValue().getRecipeDetailId());
                        getFailResult(result, "销售药品的信息不存在");
                        break;
                    }
                    CloseableHttpResponse response = sendStockHttpRequest(drugsEnterprise, saleDrug, pharmacy, httpClient);
                    //当相应状态为200时返回json
                    HttpEntity httpEntity = response.getEntity();
                    String responseStr = EntityUtils.toString(httpEntity);
                    if(200 == response.getStatusLine().getStatusCode()){
                        YtStockResponse stockResponse = JSONUtils.parse(responseStr, YtStockResponse.class);
                        LOGGER.info("YtRemoteService.scanStock:[{}]门店该[{}]药品查询库存，请求返回:{}", pharmacy.getPharmacyCode(), saleDrug.getOrganDrugCode(), responseStr);
                        if(entry.getValue().getSumUsage() <= stockResponse.getStock()){
                            groupSumResult.setComplacentNum(groupSumResult.getComplacentNum() + 1);
                            if(sumFlag){
                                groupSumResult.setFeeSum(groupSumResult.getFeeSum() + stockResponse.getPrice() * entry.getValue().getSumUsage());
                            }
                        }else{
                            break;
                        }
                    }else{
                        LOGGER.warn("YtRemoteService.scanStock:[{}]门店该[{}]药品查询库存失败,失败原因:{}", pharmacy.getPharmacyCode(), saleDrug.getOrganDrugCode(), responseStr);
                        break;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("YtRemoteService.scanStock:[{}]门店该[{}]药品查询库存异常：{}", pharmacy.getPharmacyCode(), saleDrug.getOrganDrugCode(), e.getMessage());
            } finally {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("YtRemoteService.scanStock:http请求资源关闭异常: {}", e.getMessage());
                }
            }
        }
        return groupSumResult;
    }

    /**
     * @method  sendStockHttpRequest
     * @description 发送药店下药品信息的请求
     * @date: 2019/7/11
     * @author: JRK
     * @param drugsEnterprise
     * @param saleDrug 销售药品
     * @param pharmacy 药店
     * @param httpClient 请求服务
     * @return org.apache.http.client.methods.CloseableHttpResponse
     * 返回药店下药品信息的响应
     */
    private CloseableHttpResponse sendStockHttpRequest(DrugsEnterprise drugsEnterprise, SaleDrugList saleDrug, Pharmacy pharmacy, CloseableHttpClient httpClient) throws IOException {
        HttpGet httpGet = new HttpGet(drugsEnterprise.getBusinessUrl() +
                getStock + "/" + pharmacy.getPharmacyCode() + "/" + saleDrug.getOrganDrugCode());
        //组装请求参数(组装权限验证部分)
        httpGet.setHeader(requestHeadJsonKey, requestHeadJsonValue);
        httpGet.setHeader(requestHeadPowerKey, drugsEnterprise.getToken());
        LOGGER.info("YtRemoteService.scanStock:[{}]门店该[{}]药品发送查询库存请求", pharmacy.getPharmacyCode(), saleDrug.getOrganDrugCode());

        //获取响应消息
        return httpClient.execute(httpGet);
    }

    /**
     * @method  getDetailGroup
     * @description 将处方详情中的根据药品分组，叠加需求量
     * @date: 2019/7/10
     * @author: JRK
     * @param detailList 处方详情
     * @return java.util.Map<java.lang.Integer,recipe.drugsenterprise.bean.DetailDrugGroup>
     *     详情药品需求量分组
     */
    private Map<Integer, DetailDrugGroup> getDetailGroup(List<Recipedetail> detailList) {
        Map<Integer, DetailDrugGroup> result = new HashMap<>();
        DetailDrugGroup nowDetailDrugGroup;
        //遍历处方详情，通过drugId判断，相同药品下的需求量叠加
        for (Recipedetail recipedetail : detailList) {
            nowDetailDrugGroup = result.get(recipedetail.getDrugId());
            if(null == nowDetailDrugGroup){
                DetailDrugGroup newDetailDrugGroup = new DetailDrugGroup();
                result.put(recipedetail.getDrugId(), newDetailDrugGroup);
                newDetailDrugGroup.setDrugId(recipedetail.getDrugId());
                newDetailDrugGroup.setRecipeDetailId(recipedetail.getRecipeDetailId());
                newDetailDrugGroup.setSumUsage(recipedetail.getUseTotalDose());
            }else{
                //叠加需求量
                nowDetailDrugGroup.setSumUsage(nowDetailDrugGroup.getSumUsage()+ recipedetail.getUseTotalDose());
                result.put(recipedetail.getDrugId(), nowDetailDrugGroup);
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    @RpcService
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        //首先根据你筛选的距离获取药企下的所有药店
        List<Pharmacy> pharmacyList = getPharmacies(recipeIds, ext, enterprise, result);
        if(DrugEnterpriseResult.FAIL == result.getCode()){
            return result;
        }

        //根据当前处方，对应药店下的处方的药品是否有库存
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //现在默认只有一个处方单
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeIds.get(0));
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("YtRemoteService.findSupportDep:处方单{}的细节信息为空", recipeIds.get(0));
            getFailResult(result, "处方单细节信息为空");
            return result;
        }
        Map<Integer, DetailDrugGroup> drugGroup = getDetailGroup(detailList);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        SaleDrugList saleDrug = null;
        Map<Integer, BigDecimal> feeSumByPharmacyIdMap = new HashMap<>();
        //删除库存不够的药店
        Iterator<Pharmacy> iterator = pharmacyList.iterator();
        Pharmacy next;
        while (iterator.hasNext()) {
            next = iterator.next();
            GroupSumResult groupSumResult = checkDrugListByDeil(drugGroup, enterprise, saleDrugListDAO, depId, saleDrug, result, next, true);
            if(groupSumResult.getComplacentNum() < drugGroup.size()){
                iterator.remove();
            }else{
                feeSumByPharmacyIdMap.put(next.getPharmacyId(), new BigDecimal(Double.toString(groupSumResult.getFeeSum())));
            }
        }

        //数据封装成页面展示数据
        List<DepDetailBean> pharmacyDetailPage = assemblePharmacyPageMsg(ext, enterprise, pharmacyList, depId, depName, feeSumByPharmacyIdMap);
        result.setObject(pharmacyDetailPage);
        return result;
    }

    /**
     * @method  getPharmacies
     * @description 获取搜索信息下的药店
     * @date: 2019/7/12
     * @author: JRK
     * @param recipeIds 处方id集合
     * @param ext 搜索条件
     * @param enterprise 药企
     * @param result 结果集
     * @return java.util.List<com.ngari.recipe.entity.Pharmacy>搜素信息下的药店列表
     */
    private List<Pharmacy> getPharmacies(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise, DrugEnterpriseResult result) {
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        List<Pharmacy> pharmacyList = new ArrayList<Pharmacy>();
        if (ext != null && null != ext.get(searchMapRANGE) && null != ext.get(searchMapLongitude) && null != ext.get(searchMapLatitude)) {
            pharmacyList = pharmacyDAO.findByDrugsenterpriseIdAndStatusAndRangeAndLongitudeAndLatitude(enterprise.getId(), Double.parseDouble(ext.get(searchMapRANGE).toString()), Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(ext.get(searchMapLatitude).toString()));
        }else{
            LOGGER.warn("YtRemoteService.findSupportDep:请求的搜索参数不健全" );
            getFailResult(result, "请求的搜索参数不健全");
        }
        if(CollectionUtils.isEmpty(recipeIds)){
            LOGGER.warn("YtRemoteService.findSupportDep:查询的处方单为空" );
            getFailResult(result, "查询的处方单为空");
        }
        return pharmacyList;
    }

    /**
     * @method  assemblePharmacyPageMsg
     * @description 组装页面展示的药店信息
     * @date: 2019/7/12
     * @author: JRK
     * @param ext 搜索map
     * @param enterprise 药企
     * @param pharmacyList 药店列表
     * @param depId 药企id
     * @param depName 药企名
     * @param feeSumByPharmacyIdMap 药店对应的费用
     * @return java.util.List<com.ngari.recipe.drugsenterprise.model.DepDetailBean> 页面药店信息展示
     */
    private List<DepDetailBean> assemblePharmacyPageMsg(Map ext, DrugsEnterprise enterprise, List<Pharmacy> pharmacyList, Integer depId, String depName, Map<Integer, BigDecimal> feeSumByPharmacyIdMap) {
       List<DepDetailBean> pharmacyDetailPage = new ArrayList<>();
        SaleDrugList checkSaleDrug = null;
        Position position;
        DepDetailBean newDepDetailBean;
        for (Pharmacy pharmacyMsg : pharmacyList) {
            newDepDetailBean = new DepDetailBean();
            pharmacyDetailPage.add(newDepDetailBean);
            newDepDetailBean.setDepId(depId);
            newDepDetailBean.setDepName(pharmacyMsg.getPharmacyName());
            //根据药店信息获取上面跌加出的总价格
            newDepDetailBean.setRecipeFee(feeSumByPharmacyIdMap.get(pharmacyMsg.getPharmacyId()));
            newDepDetailBean.setSendMethod(ytSendMethod);
            newDepDetailBean.setPayMethod(ytPayMethod);
            newDepDetailBean.setPharmacyCode(pharmacyMsg.getPharmacyCode());
            newDepDetailBean.setAddress(pharmacyMsg.getPharmacyAddress());
            position = new Position();
            position.setLatitude(Double.parseDouble(pharmacyMsg.getPharmacyLatitude()));
            position.setLongitude(Double.parseDouble(pharmacyMsg.getPharmacyLongitude()));
            position.setRange(Integer.parseInt(ext.get(searchMapRANGE).toString()));
            newDepDetailBean.setPosition(position);
            newDepDetailBean.setBelongDepName(enterprise.getName());
            newDepDetailBean.setDistance(DistanceUtil.getDistance(Double.parseDouble(ext.get(searchMapLatitude).toString()),
                    Double.parseDouble(ext.get(searchMapLongitude).toString()), Double.parseDouble(pharmacyMsg.getPharmacyLatitude()), Double.parseDouble(pharmacyMsg.getPharmacyLongitude())));
        }
        return pharmacyDetailPage;
    }

    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_YT;
    }
}