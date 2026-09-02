import groovy.util.logging.Slf4j
import org.apache.maven.model.Dependency
import org.apache.maven.model.DependencyManagement
import org.apache.maven.model.Parent
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException
import org.jboss.pnc.mavenmanipulator.core.state.RESTState
import org.jboss.pnc.mavenmanipulator.core.ManipulationSession
import org.jboss.pnc.mavenmanipulator.io.rest.Translator
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef

/**
* Hack to remove non productized artifacts from kogito-bom.
**/

@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class KogitoBOMRemoveDeps_exec {
    BaseScript pme

    def execute() {
        def previousProductizedVersion = "9.103.0.redhat-000001"  // update me with first prod version of a release
        for (proj: pme.getProjects()) {
            if (proj.artifactId.equals('kogito-bom')) {
                removeNonProdDependencies(proj, previousProductizedVersion)
            }
        }
    }

    def removeNonProdDependencies(proj, previousProductizedVersion) {
        List<Dependency> depToRemove = []
        def depManagement = proj.model.dependencyManagement

        for (dep: depManagement.getDependencies()) {
            def isProd = checkIfArtifactIsProductized(SimpleProjectVersionRef.parse("${dep.groupId}:${dep.artifactId}:${previousProductizedVersion}"))
            if (!isProd) {
                dep.setVersion("REMOVEME")
                // depToRemove.add(dep)
            }
        }
        // FIXME: remove deps make a little mess in bom format
        // for (dep: depToRemove) {
        //     depManagement.removeDependency(dep)
        // }
    }

    private Boolean checkIfArtifactIsProductized(ProjectVersionRef gav) {
        List<ProjectVersionRef> source = new ArrayList()
        source.add(gav);
        Map<ProjectVersionRef, String> restResult = this.getRESTAPI().lookupVersions(source)
        String targetBuild = (String) restResult.get(gav)
        if (targetBuild == null) {
           return false
        } else {
            return true
        }
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class);
        return rs.getVersionTranslator();
    }
}

def KogitoBOMRemoveDeps_exec p = new KogitoBOMRemoveDeps_exec(pme: pme)
p.execute()

