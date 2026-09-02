import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import static groovy.io.FileType.FILES


// Nasty code to temporarily override the version of quarkus-grpc-protoc-plugin as it's configured
// inside of protobuf-maven-plugin. It's not productized and shares the same version property by
// default.

@InvocationPoint(invocationPoint = InvocationStage.PREPARSE)
@PMEBaseScript BaseScript pme

assert pme.getBaseDir() != null
assert pme.getGAV() == null
assert pme.getProjects().isEmpty()

def quarkusVersion = pme.getUserProperties().getProperty("quarkusVersionCommunity")
pme.getBaseDir().traverse(type: groovy.io.FileType.FILES) { pomFile ->
  if(pomFile.name.equals('pom.xml')) {
      def pom = new XmlSlurper( false, false ).parse(pomFile)
      final def pluginToFix = pom.'**'.find {
        final protocPlugin ->  protocPlugin.artifactId.text() == "quarkus-grpc-protoc-plugin"
      }
      if (pluginToFix != null) {
        pluginToFix.replaceNode
        {
            protocPlugin() {
                id("quarkus-grpc-protoc-plugin")
                groupId("io.quarkus")
                artifactId("quarkus-grpc-protoc-plugin")
                version(quarkusVersion)
                mainClass("io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator")
            }
        }
        pomFile.write XmlUtil.serialize( pom )
      }
  }
}
