import groovy.util.logging.Slf4j
import org.apache.maven.model.Parent
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.ManipulationSession
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.state.RESTState
import org.jboss.pnc.mavenmanipulator.io.rest.Translator

/**
*
* The script forces the parent version to be the latest one from DA service
* IMPORTANT: The -DversionOverride flag is mandatory
*
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
class OverrideKogitoRuntimesVersionLastExecutor {

    BaseScript pme
    Project project

    def execute() {
        log.info("Running OverrideKogitoRuntimesVersionLast...")
        Parent parent = project.getModelParent()
        if ("kogito-build-parent".equals(parent.getArtifactId()) || "kogito-runtimes".equals(parent.getArtifactId()) || "kogito-build-no-bom-parent".equals(parent.getArtifactId())) {
            def versionOverride = pme.getUserProperties().getProperty("versionOverride")
            log.info("OverrideKogitoRuntimesVersionLast versionOverride property {} ", versionOverride)
            String projectVersion = getKogitoRuntimesVersion(SimpleProjectVersionRef.parse("org.kie.kogito:kogito-runtimes:" + versionOverride));
            parent.setVersion(projectVersion)
        }
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
            log.info("kogito-runtimes Version set to {}", targetBuild);
            return targetBuild;
        }
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class);
        return rs.getVersionTranslator();
    }
}

def OverrideKogitoRuntimesVersionLastExecutor overrideKogitoRuntimesVersionLast = new OverrideKogitoRuntimesVersionLastExecutor(pme: pme, project: pme.getProject())
overrideKogitoRuntimesVersionLast.execute()
