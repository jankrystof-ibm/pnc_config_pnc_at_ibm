import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Force Quarkus test version to a specific community version.
*
* The -DquarkusVersionCommunity flag is mandatory
*
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetQuarkusTestVersionCommunityExecutor
{
    BaseScript pme
    Project project

    def execute() {
        log.info("SetQuarkusTestVersionCommunity running ..")
        def newPropertyValue = pme.getUserProperties().getProperty("quarkusVersionCommunity")

        String[] propertyKeys = ['version.io.quarkus.quarkus-test']
        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("SetQuarkusTestVersionCommunity. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, newPropertyValue)
                    p.getModel().addProperty(propertyKey, newPropertyValue)
                }
            }
        }
    }
}

def SetQuarkusTestVersionCommunityExecutor setQuarkusTestVersionCommunity = new SetQuarkusTestVersionCommunityExecutor(pme: pme, project: pme.getProject())
setQuarkusTestVersionCommunity.execute()
