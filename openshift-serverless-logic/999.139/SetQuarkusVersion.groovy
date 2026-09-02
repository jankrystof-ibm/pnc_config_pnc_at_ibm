import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Force Quarkus version to a specific productized version.
*
* The -DquarkusVersion flag is mandatory
*
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetQuarkusVersionExecutor
{
    BaseScript pme
    Project project

    def execute() {
        log.info("SetQuarkusVersion running .. ")
        def newPropertyValue = pme.getUserProperties().getProperty("quarkusVersion")

        String[] propertyKeys = ['version.io.quarkus', 'version.quarkus']
        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("SetQuarkusVersion. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, newPropertyValue)
                    p.getModel().addProperty(propertyKey, newPropertyValue)
                }
            }
        }
    }
}

def SetQuarkusVersionExecutor setQuarkusVersion = new SetQuarkusVersionExecutor(pme: pme, project: pme.getProject())
setQuarkusVersion.execute()
