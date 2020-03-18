package regulation;
import com.google.common.collect.Lists;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import ctd.mvc.controller.support.JSONRequester;
import ctd.util.JSONUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class RegulationRecipeQueryServiceTest extends AbstractJUnit4SpringContextTests {
    private static JSONRequester jsonRequester;
    @Before
    public void setUp() {
        jsonRequester = new JSONRequester();
    }

    @Test
    public void queryRegulationChargeDetailListTest(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        QueryRegulationUnitReq queryRegulationUnit = new QueryRegulationUnitReq();
        List<Integer> organIds = new ArrayList<>();

        LocalDate localDate = LocalDate.now().minusDays(30);
        Date stardate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        organIds.add(1);
        queryRegulationUnit.setEndTime(new Date());
        queryRegulationUnit.setStartTime(stardate);
        queryRegulationUnit.setNgariOrganIds(organIds);
        request.setMethod("POST");
        request.setContentType("application/json");
        request.addHeader("X-Service-Id", "eh.remoteRecipeQueryService");
        request.addHeader("X-Service-Method", "queryRegulationChargeDetailList");
        request.addHeader("X-Access-Token", "eae53b7f-673c-479e-a293-155d90e7b3ba");
        request.addHeader("X-Client-Id", 19129);
        List<Object> parameters = Lists.newArrayList();
        parameters.add(queryRegulationUnit);
        request.setContent(JSONUtils.toBytes(parameters));
        jsonRequester.doJSONRequest(request, response);
        assertEquals(200, response.getStatus());
        Map<String,Object> result = JSONUtils.parse(response.getContentAsByteArray(), Map.class);
        System.out.println(result.toString());
        assertEquals(200, result.get("code"));
        assertNotNull(result);
    }
}
