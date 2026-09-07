import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef
import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.ManipulationSession
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.core.state.RESTState
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.mavenmanipulator.io.rest.Translator

/**
 * Decouples the drools-family managed dependencies in kie-tools
 * (packages/maven-base/pom.xml) from the shared ${version.org.kie.kogito} property.
 *
 * Root cause of the alignment clash:
 *   packages/maven-base/pom.xml manages the kogito BOMs AND org.drools:drools-bom AND
 *   org.kie:kie-dmn-test-resources all via the single ${version.org.kie.kogito} property.
 *   drools and kogito are separate PNC products with independent temporary suffixes.
 *   When their suffixes diverge (e.g. drools=...-00009 vs kogito=...-00010) PME's
 *   DependencyManipulator tries to set that one property to two different values and
 *   aborts with:
 *     "Property replacement clash - updating property 'version.org.kie.kogito' to both
 *      999.106.0.temporary-ibm-00010 and 999.106.0.temporary-ibm-00009"
 *
 * Fix (FIRST stage, i.e. before DependencyManipulator runs):
 *   Pin the drools-family managed deps to their DA-resolved versions as plain literals,
 *   so they no longer reference ${version.org.kie.kogito}. The kogito BOMs keep the
 *   property and align independently to the kogito suffix; the drools deps carry their
 *   own resolved suffix. No shared property -> no clash, regardless of suffix drift.
 *
 * This mirrors what SetPropertiesVersionsKieTools.groovy already does for drools-bom at
 * the LAST stage (getDroolsBomVersion via the DA REST translator), but performed FIRST
 * so the clash is prevented rather than patched after the fact, and extended to also
 * cover org.kie:kie-dmn-test-resources.
 *
 * IMPORTANT: -DdroolsVersion=X.Y.Z must be provided.
 */
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class DecoupleDroolsVersionKieToolsExec
{
    BaseScript pme
    Project project

    // groupId:artifactId of the drools-family managed deps that must NOT share
    // ${version.org.kie.kogito} with the kogito BOMs.
    def targets = [
        ['org.drools', 'drools-bom'],
        ['org.kie',    'kie-dmn-test-resources'],
    ]

    def execute() {
        String droolsVersion = pme.getUserProperties().getProperty("droolsVersion")
        if (droolsVersion == null) {
            throw new ManipulationException("droolsVersion user property is required (-DdroolsVersion=X.Y.Z)")
        }
        log.info("DecoupleDroolsVersionKieTools running; droolsVersion={}", droolsVersion)

        for (t in targets) {
            String groupId = t[0]
            String artifactId = t[1]
            String resolved = lookupVersion(groupId, artifactId, droolsVersion)
            log.info("Decoupling {}:{} -> literal {}", groupId, artifactId, resolved)
            pinDependency(groupId, artifactId, resolved)
        }
    }

    // Sets the version to a literal on every matching dependency (managed or plain)
    // across all projects in the reactor, so the ${version.org.kie.kogito} reference
    // is removed before DependencyManipulator processes it.
    def pinDependency(String groupId, String artifactId, String version) {
        for (Project p : pme.getProjects()) {
            def dm = p.getModel().getDependencyManagement()
            if (dm != null) {
                for (dep in dm.getDependencies()) {
                    if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                        log.info("  [{}] managed {}:{} version {} -> {}", p.getArtifactId(), groupId, artifactId, dep.getVersion(), version)
                        dep.setVersion(version)
                    }
                }
            }
            for (dep in p.getModel().getDependencies()) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    log.info("  [{}] dep {}:{} version {} -> {}", p.getArtifactId(), groupId, artifactId, dep.getVersion(), version)
                    dep.setVersion(version)
                }
            }
        }
    }

    // Resolves the aligned (suffixed) version for a GAV via the DA REST translator.
    private String lookupVersion(String groupId, String artifactId, String version) {
        ProjectVersionRef gav = SimpleProjectVersionRef.parse(groupId + ":" + artifactId + ":" + version)
        List<ProjectVersionRef> source = new ArrayList()
        source.add(gav)
        Map<ProjectVersionRef, String> restResult = getRESTAPI().lookupVersions(source)
        String targetBuild = (String) restResult.get(gav)
        if (targetBuild == null) {
            log.error("REST result was {}", restResult)
            throw new ManipulationException("No aligned version returned for " + gav)
        }
        return targetBuild
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class)
        return rs.getVersionTranslator()
    }
}

def DecoupleDroolsVersionKieToolsExec exec = new DecoupleDroolsVersionKieToolsExec(pme: pme, project: pme.getProject())
exec.execute()
