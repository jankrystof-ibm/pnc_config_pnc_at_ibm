import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
 * It sets the version to version.org.kie and version.org.drools properties
 * IMPORTANT: droolsVersion property needs to be defined. Like -DdroolsVersion=X.Y.Z
 */
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetDroolsVersionExec
{
    BaseScript pme
    Project project

    def executeKie(droolsVersion) {
        String propertyKey = 'version.org.kie'

        for(Project p: pme.getProjects().findAll { "kogito-kie-bom".equals(it.getArtifactId()) }) {
            log.info("Artifact ID {}. property {} set to {} ", p.getArtifactId(), propertyKey, droolsVersion)
            p.getModel().addProperty(propertyKey, droolsVersion)
        }
    }

    def executeDrools(droolsVersion) {
        String propertyKey = 'version.org.drools'

        for(Project p: pme.getProjects().findAll { it.getModel().getProperties().getProperty(propertyKey) }) {
            if (p.groupId == "org.kie.kogito.examples") {
                log.info("Artifact ID {}. property {} set to {} ", p.getArtifactId(), propertyKey, droolsVersion)
                p.getModel().addProperty(propertyKey, droolsVersion)
            }
        }
    }

    def updateArtifactVersion(Project proj, String droolsVersion, String groupdId, String artifactId) {
        for (dep: proj.model.dependencies) {
            if (dep.groupId.equals(groupdId) && dep.artifactId.equals(artifactId)) {
            log.info "Found references to {}:{}, setting to version {}", groupdId, artifactId, droolsVersion
            dep.version = droolsVersion
            }
        }
    }

    def execute() {
        log.info("userProps = {}", pme.getUserProperties())
        String droolsVersion = pme.getUserProperties().getProperty("droolsVersion")
        executeKie(droolsVersion)
        executeDrools(droolsVersion)

        for (proj: pme.getProjects()) {
            // force specific deps to drools version
            updateArtifactVersion(proj, droolsVersion, 'org.kie', 'kie-dmn-test-resources')
        }
    }
}

def SetDroolsVersionExec setDroolsVersionExec = new SetDroolsVersionExec(pme: pme, project: pme.getProject())
setDroolsVersionExec.execute()
