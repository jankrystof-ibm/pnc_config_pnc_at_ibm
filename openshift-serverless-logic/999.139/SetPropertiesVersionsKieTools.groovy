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
* Set specific properties on kie-tools project
*
* The -DquarkusVersion, -DquarkusVersionCommunity, -DquarkusPlatformVersion and -DdroolsVersion flags are mandatory
*/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class SetPropertiesVersionsKieToolsExecutor
{
    BaseScript pme
    Project project

    def execute() {
        log.info("SetPropertiesVersionsKieTools running .. ")
        def kogitoVersion = project.getVersion()
        def quarkusVersionCommunity = pme.getUserProperties().getProperty("quarkusVersionCommunity")
        def quarkusVersion = pme.getUserProperties().getProperty("quarkusVersion")
        def quarkusPlatformVersion = pme.getUserProperties().getProperty("quarkusPlatformVersion")
        def droolsVersion = pme.getUserProperties().getProperty("droolsVersion")

        /**
        update root-env index.js
        */
        def rootEnvIndexJs = "packages/root-env/env/index.js"
        replaceInFile(new File(rootEnvIndexJs), "999.*SNAPSHOT", "${kogitoVersion}")
        replaceInFile(new File(rootEnvIndexJs), quarkusVersionCommunity, quarkusVersion)

        /**
        update maven-base index.js
        */
        def mavenBaseIndexJs = "packages/maven-base/index.js"
        // removing the usage of a settings.xml so maven will use the default one
        replaceInFile(new File(mavenBaseIndexJs), '--settings=\\$\\{SETTINGS_XML_PATH\\}', '')

        /**
        * maven-base/pom.xml is not part of midstream project structure as we use a special root pom in midstream which does the required setup
        * like installing npm, node and so on. And as it is not part of the project strucutre PME is not able to update it in a common way.
        * And note that even that maven-base/pom.xml is not part of midstream maven project structure, this pom is used by some maven modules that are basically called in the following structure:
        *   maven-frondend-plugin calls pnpm and pnpm calls mvn in some specific modules.
        **/
        def baseMavenPom = "packages/maven-base/pom.xml"
        replaceInFile(new File(baseMavenPom), "<version.org.kie.kogito>.*</version.org.kie.kogito>", "<version.org.kie.kogito>${kogitoVersion}</version.org.kie.kogito>")
        replaceInFile(new File(baseMavenPom), "<version.quarkus>.*</version.quarkus>", "<version.quarkus>${quarkusVersion}</version.quarkus>")

        String quarkusPlatformGroupId = "com.redhat.quarkus.platform"
        replaceInFile(new File(baseMavenPom), "<groupId>io.quarkus</groupId>\n        <artifactId>quarkus-bom</artifactId>\n        <version>.*</version>", "<groupId>${quarkusPlatformGroupId}</groupId>\n        <artifactId>quarkus-bom</artifactId>\n        <version>${quarkusPlatformVersion}</version>")

        String droolsLatestVersion = getDroolsBomVersion(SimpleProjectVersionRef.parse("org.drools:drools-bom:" + droolsVersion))
        replaceInFile(new File(baseMavenPom), "<artifactId>drools-bom</artifactId>\n        <version>.*</version>", "<artifactId>drools-bom</artifactId>\n        <version>${droolsLatestVersion}</version>")
    }

    def replaceInFile(file, oldText, newText) {
        def newConfig = file.text.replaceAll(oldText, newText)
        file.text = newConfig
    }

    private String getDroolsBomVersion(ProjectVersionRef gav) {
        List<ProjectVersionRef> source = new ArrayList()
        source.add(gav);
        Map<ProjectVersionRef, String> restResult = this.getRESTAPI().lookupVersions(source)
        String targetBuild = (String) restResult.get(gav)
        if (targetBuild == null) {
            log.error("REST result was {}", restResult);
            throw new ManipulationException("Multiple results returned ; unable to reset version.")
        } else {
            return targetBuild;
        }
    }

    private Translator getRESTAPI() throws ManipulationException {
        RESTState rs = (RESTState)((ManipulationSession)pme.getSession()).getState(RESTState.class);
        return rs.getVersionTranslator();
    }
}

def SetPropertiesVersionsKieToolsExecutor setPropertiesVersionsKieTools = new SetPropertiesVersionsKieToolsExecutor(pme: pme, project: pme.getProject())
setPropertiesVersionsKieTools.execute()
