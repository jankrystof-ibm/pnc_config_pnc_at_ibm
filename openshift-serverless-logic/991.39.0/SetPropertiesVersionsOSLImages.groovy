import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Set required properties of osl-images pom.xml to productized.
*
* The -DquarkusPlatformGroupId, -DquarkusPlatformVersion and -Dbrotli4jVersion flags are mandatory
*/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetPropertiesVersionsKogitoImagesExecutor
{
    BaseScript pme
    Project project

    def execute() {
        log.info("SetPropertiesVersionsKogitoImages running .. ")

        def quarkusPlatformGroupId = pme.getUserProperties().getProperty("quarkusPlatformGroupId")
        String[] quarkusPlatformGroupIdPropertyKeys = ['build.quarkusapp.argument.quarkusplatform.groupid']
        setProperty(quarkusPlatformGroupId, quarkusPlatformGroupIdPropertyKeys)

        def quarkusPlatformVersion = pme.getUserProperties().getProperty("quarkusPlatformVersion")
        String[] quarkusPlatformVersionPropertyKeys = ['build.quarkusapp.argument.quarkusplatform.version']
        setProperty(quarkusPlatformVersion, quarkusPlatformVersionPropertyKeys)

        def quarkusVersion = pme.getUserProperties().getProperty("quarkusVersion")
        String[] quarkusVersionPropertyKeys = ['build.quarkusapp.argument.quarkus.version']
        setProperty(quarkusVersion, quarkusVersionPropertyKeys)

        def kogitoVersion = project.getVersion()
        String[] kogitoVersionPropertyKeys = ['build.quarkusapp.argument.kogitoversion']
        setProperty(kogitoVersion, kogitoVersionPropertyKeys)

        def brotli4jVersion = pme.getUserProperties().getProperty("brotli4jVersion")
        String[] brotli4jPropertyKeys = ['version.brotli4j']
        setProperty(brotli4jVersion, brotli4jPropertyKeys)
    }

    def setProperty(String newPropertyValue, String[] propertyKeys) {
        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("SetPropertiesVersionsKogitoImages. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, newPropertyValue)
                    p.getModel().addProperty(propertyKey, newPropertyValue)
                }
            }
        }
    }
}

def SetPropertiesVersionsKogitoImagesExecutor setPropertiesVersionsKogitoImages = new SetPropertiesVersionsKogitoImagesExecutor(pme: pme, project: pme.getProject())
setPropertiesVersionsKogitoImages.execute()
