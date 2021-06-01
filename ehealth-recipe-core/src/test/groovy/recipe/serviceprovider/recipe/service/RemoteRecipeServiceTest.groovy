package recipe.serviceprovider.recipe.service

import com.google.common.collect.Lists
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import recipe.dao.RecipeDAO
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
@ContextConfiguration(locations = ["classpath:spring.xml"])
class RemoteRecipeServiceTest extends Specification {

@Autowired
private RecipeDAO recipeDAO

    def setupSpec() {
        given:
        System.setProperty("app.id", "ehealth-recipe");
        System.setProperty("env", "DEV");
        System.setProperty("apollo.cluster", "DEV");
    }

    def "getCountByOrganAndDeptIds"(){
        expect:
        recipeDAO.getCountByOrganAndDeptIds(1, Lists.newArrayList(47))==20;
    }

    def "getRecipeByConsultId"(){
        expect:
        recipeDAO.findByClinicId(6851).size()==1
    }
}
