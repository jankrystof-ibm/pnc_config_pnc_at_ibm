import groovy.util.logging.Slf4j
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

/**
* Inline BOMS versions for kie-tools project.
*/
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class InlineBOMKieToolsExecutor {
    BaseScript pme
    Project project

    def execute() {
        log.info("InlineBOMKieToolsExecutor running .. ")
        for (proj : pme.getProjects()) {
            // inline quarkus-bom to it can be changed to platform version without messing up with other places where requires core Quarkus version
            pme.inlineProperty(proj, SimpleProjectRef.parse("io.quarkus:quarkus-bom"));
        }
    }
}

def InlineBOMKieToolsExecutor inlineBOMKieTools = new InlineBOMKieToolsExecutor(pme: pme, project: pme.getProject())
inlineBOMKieTools.execute()
