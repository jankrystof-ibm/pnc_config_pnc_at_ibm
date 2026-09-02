import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef
import org.jboss.pnc.mavenmanipulator.common.util.PropertyResolver;

/**
* Set Quarkus bootstrap plugin to upstream version as it is not productized for Quarkus 2.13.x
*
* The -DquarkusVersionCommunity flag is mandatory
*/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetQuarkusPluginVersionCommunityExec
{
    BaseScript pme
    Project project

    def setPluginToVersion(Project project, ProjectRef projectRef, String newVersion) {
        project.getResolvedPlugins()
            .entrySet().stream()
            .filter(a -> (a.getKey().getGroupId().equals(projectRef.getGroupId()) && a.getKey().getArtifactId().equals(projectRef.getArtifactId())))
            .forEach(a -> {
                log.info("Plugin {} set to version {}", a.getKey(), newVersion);
                a.getValue().setVersion(newVersion);
            });
    }

    def execute() {
        for(Project p: pme.getProjects()) {
            setPluginToVersion(p, SimpleProjectRef.parse("io.quarkus:quarkus-bootstrap-maven-plugin"), pme.getUserProperties().getProperty("quarkusVersionCommunity"))
        }
    }
}

SetQuarkusPluginVersionCommunityExec setQuarkusPluginVersionCommunity = new SetQuarkusPluginVersionCommunityExec(pme: pme, project: pme.getProject())
setQuarkusPluginVersionCommunity.execute()
