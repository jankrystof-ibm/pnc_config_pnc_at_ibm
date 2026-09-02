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
 * It sets the version to kogito.bom.version property
 * IMPORTANT: kogitoVersion property needs to be defined. Like -DkogitoVersion=X.Y.Z
 */
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetKogitoBomVersionRunner
{
    BaseScript pme
    Project project

    def execute() {
        log.info("Running SetKogitoBomVersion...")

        String[] propertyKeys = ['kogito.bom.version']
        String kogitoVersion = pme.getUserProperties().getProperty("kogitoVersion")
        
        if (!kogitoVersion) {
            String errorMsg = 'SetKogitoBomVersion. kogitoVersion not set'
            log.error(errorMsg)
            throw new RuntimeException(errorMsg)
        }

        String newKogitoVersion = getKogitoBomVersion(SimpleProjectVersionRef.parse("org.kie.kogito:kogito-bom:" + kogitoVersion))

        for(Project p: pme.getProjects().findAll{ propertyKeys.collect{propertyKey -> it.getModel().getProperties().getProperty(propertyKey)}.findAll() }) {
            for(String propertyKey: propertyKeys) {
                if(p.getModel().getProperties().getProperty(propertyKey)) {
                    log.info("SetKogitoBomVersion. Artifact ID {}. property {} set to {}", p.getArtifactId(), propertyKey, kogitoVersion)
                    p.getModel().addProperty(propertyKey, newKogitoVersion)
                }
            }
        }
        log.info("SetKogitoBomVersion OK")
    }

    private String getKogitoBomVersion(ProjectVersionRef parentGav) {
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

def SetKogitoBomVersionRunner setKogitoBomVersion = new SetKogitoBomVersionRunner(pme: pme, project: pme.getProject())
setKogitoBomVersion.execute()