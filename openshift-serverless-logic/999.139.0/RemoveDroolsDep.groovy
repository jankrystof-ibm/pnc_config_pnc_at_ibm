import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.apache.maven.model.Dependency

/**
 * Remove dummy drools dependency after the orphaned version.org.drools property has
 * been aligned in kogito-examples. This is used alongside InjectDroolsDep.groovy which 
 * is ran FIRST.
 */
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class RemoveDroolsDep_exec
{
    BaseScript pme
    Project project

    def execute() {
        Dependency droolsDep = null
        for (proj: pme.getProjects())
        {
            if (proj.groupId == "org.kie.kogito.examples" && proj.artifactId == "kogito-examples") {
                for (dep: proj.model.dependencies)
                {
                    if (dep.groupId == "org.drools" && dep.artifactId == "drools-core")
                    {
                        droolsDep = dep
                    }
                }
                if (droolsDep) 
                {
                    proj.model.dependencies.remove(droolsDep)
                    break
                }
            }
        }
    }

}

def RemoveDroolsDep_exec RemoveDroolsDep_exec = new RemoveDroolsDep_exec(pme: pme, project: pme.getProject())
RemoveDroolsDep_exec.execute()
