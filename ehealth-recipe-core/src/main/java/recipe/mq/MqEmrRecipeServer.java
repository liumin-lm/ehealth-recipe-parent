package recipe.mq;

import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeExtendDAO;

/**
 * @author fuzi
 */
public class MqEmrRecipeServer implements Observer<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(MqEmrRecipeServer.class);

    @Override
    public void onMessage(Integer docIndexId) {
        logger.info("MqEmrRecipeServer onMessage docIndexId={}", docIndexId);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        recipeExtendDAO.updateDocIndexId(docIndexId);
    }
}
