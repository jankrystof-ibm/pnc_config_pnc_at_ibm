import groovy.util.logging.Slf4j
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript

import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef
import org.jboss.pnc.mavenmanipulator.common.util.PropertyResolver;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
* Set Quarkus extension maven artifacts to Quarkus version as it uses the same property as other artifacts from Quarkus platform
*
* The -DquarkusVersion flag is mandatory
*/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetQuarkusPluginVersionQuarkusExec
{
    BaseScript pme
    Project project

    def setPluginToVersion(Project project, ProjectRef projectRef, String newVersion) {
        // Update artifact version in a plugin block
        project.getResolvedPlugins()
            .entrySet().stream()
            .filter(a -> (a.getKey().getGroupId().equals(projectRef.getGroupId()) && a.getKey().getArtifactId().equals(projectRef.getArtifactId())))
            .forEach(a -> {
                log.info("Plugin {} set to version {}", a.getKey(), newVersion);
                a.getValue().setVersion(newVersion);
            });

        // Update artifact version in a plugin configuration block
        project.getResolvedPlugins().each{ k, v ->
            final Xpp3Dom xpp3Dom = v.getConfiguration()
            if (xpp3Dom != null) {
                Xpp3Dom pathDom = findChild(xpp3Dom, "path")
                if (pathDom) {
                    String foundGroupId = findChild(pathDom, "groupId").getValue()
                    String foundArtifactId = findChild(pathDom, "artifactId").getValue()
                    if (foundGroupId.equals(projectRef.getGroupId()) && foundArtifactId.equals(projectRef.getArtifactId())) {
                        Xpp3Dom versionDom = findChild(pathDom, "version")
                        if (versionDom) {
                            String current = versionDom.getValue()
                            log.info("Plugin configuration version {} set to version {}", current, newVersion)
                            versionDom.setValue(newVersion)
                        }
                    }
                }
            }
        }
    }

    def Xpp3Dom findChild(Xpp3Dom parent, String name) {
        Xpp3Dom foundChild
        for (Xpp3Dom child : parent.getChildren()) {
            if (name.equals(child.getName())) {
                foundChild = child
                break
            } else {
                foundChild = findChild(child, name)
                if (foundChild) {
                    break
                }
            }
        }
        return foundChild
     }

    def execute() {
        def quarkusVersion = pme.getUserProperties().getProperty("quarkusVersion")
        for(Project p: pme.getProjects()) {
            setPluginToVersion(p, SimpleProjectRef.parse("io.quarkus:quarkus-extension-maven-plugin"), quarkusVersion)
            setPluginToVersion(p, SimpleProjectRef.parse("io.quarkus:quarkus-extension-processor"), quarkusVersion)
        }
    }
}

SetQuarkusPluginVersionQuarkusExec setQuarkusPluginVersionQuarkus = new SetQuarkusPluginVersionQuarkusExec(pme: pme, project: pme.getProject())
setQuarkusPluginVersionQuarkus.execute()
