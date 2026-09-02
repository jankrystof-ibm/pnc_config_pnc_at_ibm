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
* This sets version.org.kie.kogito to the last built version, -DproductVersion
* is required as a basis for the lookup. This should be used for the rest of the
* build.
*
* This was designed to be used inside of the optaplanner-build-parent. Update as
* necessary below (see comments inline) for other projects.
*
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class OverrideKogitoVersionPropertyLastExecutor
{
    Project project
    BaseScript pme;

    def execute() {
        log.info("Running OverrideKogitoVersionPropertyLast ...")
        def productVersion = pme.getUserProperties().getProperty("kogitoVersion")
        String newPropertyValue = getKogitoRuntimesVersion(SimpleProjectVersionRef.parse("org.kie.kogito:kogito-runtimes:" + productVersion))
        String kogitoVersionProperty = 'version.org.kie.kogito'

        // Required otherwise property is inserted into root pom and existing is not updated.
        for(Project p: pme.getProjects()) {
            if(p.getModel().getProperties().getProperty(kogitoVersionProperty)) {
                p.getModel().addProperty(kogitoVersionProperty, newPropertyValue)
                log.info("property {} set to {} ", kogitoVersionProperty, newPropertyValue )
            }
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
            return targetBuild;
        }
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class);
        return rs.getVersionTranslator();
    }
}

def OverrideKogitoVersionPropertyLastExecutor setKogitoVersionToRuntimesVersion = new OverrideKogitoVersionPropertyLastExecutor(pme: pme, project: pme.getProject())
setKogitoVersionToRuntimesVersion.execute()
