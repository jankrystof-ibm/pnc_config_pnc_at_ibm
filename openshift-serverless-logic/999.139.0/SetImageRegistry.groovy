import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Set productized registry value for data index ephemeral image.
*
* The -DimageRegistry flag is mandatory
*/
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetImageRegistryRunner
{
    BaseScript pme
    Project project

    def execute() {
        log.info("Running SetImageRegistry...")

        String[] propertyKeys = ['data-index-ephemeral.image']
        String imageRegistry = pme.getUserProperties().getProperty("imageRegistry")
        if(!imageRegistry) {
            String errorMsg = 'imageRegistry not set'
            log.error(errorMsg)
            throw new RuntimeException(errorMsg)
        }

        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("setImageRegistry. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, imageRegistry)
                    p.getModel().addProperty(propertyKey, imageRegistry)
                }
            }
        }
    }
}

def SetImageRegistryRunner setImageRegistry = new SetImageRegistryRunner(pme: pme, project: pme.getProject())
setImageRegistry.execute()
