import groovy.util.logging.Slf4j
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.ManipulationSession
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.core.state.RESTState
import org.jboss.pnc.mavenmanipulator.io.rest.Translator

/**
*
* The kogito.version property value set to ${project.version}
*
**/
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetKogitoVersionToRuntimesVersionExecutor
{
    Project project
    BaseScript pme;

    def execute() {
        def versionOverride = pme.getUserProperties().getProperty("versionOverride")
        String newPropertyValue = getKogitoRuntimesVersion(SimpleProjectVersionRef.parse("org.kie.kogito:kogito-runtimes:" + versionOverride))
        String kogitoVersionProperty = 'kogito.version'
        project.getModel().addProperty(kogitoVersionProperty, newPropertyValue)
        log.info("property {} set to {} ", kogitoVersionProperty, newPropertyValue )
    }

    private String getKogitoRuntimesVersion(ProjectVersionRef parentGav) {
        List<ProjectVersionRef> source = new ArrayList();
        source.add(parentGav);
        source.add(pme.getGAV());
        Map<ProjectVersionRef, String> restResult = this.getRESTAPI().lookupVersions(source);
        String targetBuild = (String) restResult.get(parentGav);
        if (targetBuild == null) {
            log.error("REST result was {}", restResult);
            throw new ManipulationException("Multiple results returned ; unable to reset version.", new String[0]);
        } else {
            return targetBuild;
        }
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class);
        return rs.getVersionTranslator();
    }
}

def SetKogitoVersionToRuntimesVersionExecutor setKogitoVersionToRuntimesVersion = new SetKogitoVersionToRuntimesVersionExecutor(pme: pme, project: pme.getProject())
setKogitoVersionToRuntimesVersion.execute()
