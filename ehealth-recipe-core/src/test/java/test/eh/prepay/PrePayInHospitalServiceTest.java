package test.eh.prepay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import recipe.audit.auditmode.AbstractAuidtMode;
import recipe.audit.auditmode.AuditPreMode;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class PrePayInHospitalServiceTest {



    @Test
    public void queryInHospPayConfig() {
        AuditPreMode preMode=new AuditPreMode();
    }


}
