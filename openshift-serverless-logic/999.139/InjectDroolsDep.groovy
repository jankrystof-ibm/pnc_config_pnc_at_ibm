import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.apache.maven.model.Dependency

/**
 * Inject a dummy drools dependency to the kogito-examples top level
 * to align the orphaned version.org.drools property
 */
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class InjectDroolsDep_exec
{
    BaseScript pme
    Project project

    def execute() {
        log.info "Running script"
        for (proj: pme.getProjects())
        {
            if (proj.groupId == "org.kie.kogito.examples" && proj.artifactId == "kogito-examples") {
                addDependency(proj, "org.drools", "drools-core", '${version.org.drools}')
            }
        }
    }

    def addDependency(Project proj, String groupId, String artifactId, String version) {
        def dep = new Dependency()
        dep.groupId = groupId
        dep.artifactId = artifactId
        dep.version = version
        proj.model.dependencies.add(dep)
    }

}

def InjectDroolsDep_exec InjectDroolsDep_exec = new InjectDroolsDep_exec(pme: pme, project: pme.getProject())
InjectDroolsDep_exec.execute()
