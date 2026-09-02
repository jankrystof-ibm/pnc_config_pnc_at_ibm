import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Force Quarkus Platform version to a specific productized version.
*
* The -DquarkusPlatformVersion flag is mandatory
*
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetQuarkusPlatformVersionExecutor
{
    BaseScript pme
    Project project

    def execute() {
        log.info("SetQuarkusPlatformVersion running .. ")
        def newPropertyValue = pme.getUserProperties().getProperty("quarkusPlatformVersion")

        String[] propertyKeys = ['quarkus-plugin.version', 'quarkus.platform.version']
        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("SetQuarkusPlatformVersion. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, newPropertyValue)
                    p.getModel().addProperty(propertyKey, newPropertyValue)
                }
            }
        }
    }
}

def SetQuarkusPlatformVersionExecutor setQuarkusPlatformVersion = new SetQuarkusPlatformVersionExecutor(pme: pme, project: pme.getProject())
setQuarkusPlatformVersion.execute()
