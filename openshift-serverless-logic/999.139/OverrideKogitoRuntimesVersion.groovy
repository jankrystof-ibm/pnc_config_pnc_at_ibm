import groovy.util.logging.Slf4j
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint

/**
*
* This script replaces the project version by the one from the org.kie.kogito:kogito-runtimes project. Then once PME runs it takes the suffix from DA service
* The purpose of this script is to align the project version with the one from kie-parent.
* IMPORTANT: The -DversionOverride flag is mandatory
*
**/
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
class OverrideKogitoRuntimesVersionExecutor {

    BaseScript pme
    Project project

    def execute() {
        def versionOverride = pme.getUserProperties().getProperty("versionOverride")
        log.info("OverrideKogitoRuntimesVersion versionOverride property {} ", versionOverride)
        pme.overrideProjectVersion(SimpleProjectVersionRef.parse("org.kie.kogito:kogito-runtimes:" + versionOverride))
        log.info("OverrideKogitoRuntimesVersion set project version to {} ", versionOverride)
    }
}

def OverrideKogitoRuntimesVersionExecutor overrideKogitoRuntimesVersion = new OverrideKogitoRuntimesVersionExecutor(pme: pme, project: pme.getProject())
overrideKogitoRuntimesVersion.execute()
