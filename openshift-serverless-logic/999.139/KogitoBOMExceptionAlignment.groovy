import groovy.util.logging.Slf4j
import org.apache.maven.model.Dependency
import org.apache.maven.model.DependencyManagement
import org.apache.maven.model.Parent
import org.apache.maven.model.Profile
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef
import org.jboss.pnc.mavenmanipulator.common.model.Project
import org.jboss.pnc.mavenmanipulator.core.groovy.PMEBaseScript
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationPoint
import org.jboss.pnc.mavenmanipulator.core.groovy.InvocationStage
import org.jboss.pnc.mavenmanipulator.core.groovy.BaseScript
import org.jboss.pnc.mavenmanipulator.common.exception.ManipulationException

/**
* There are some deps in the Kogito BOM not part of the Kogito Runtimes Project which
* means the version is not determined yet and they will likely not align correctly.
**/

@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class KogitoBOMExceptionAlignment_exec
{
  BaseScript pme;
  def execute()
  {
    for (proj: pme.getProjects())
    {
        if (proj.artifactId.equals('kogito-bom')) {
        updateDepMgmt(proj, 'kogito-quarkus-serverless-workflow-devui')
        updateDepMgmt(proj, 'kogito-quarkus-serverless-workflow-devui-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-inmemory')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-inmemory-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-postgresql')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-postgresql-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-common-runtime')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-common-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-jobs-service-embedded')
        updateDepMgmt(proj, 'kogito-addons-quarkus-jobs-service-embedded-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-persistence-common-runtime')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-persistence-common-deployment')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-persistence-postgresql')
        updateDepMgmt(proj, 'kogito-addons-quarkus-data-index-persistence-postgresql-deployment')
        guardForSuspectedDeps(proj)
      }
    }
  }

  def updateDepMgmt(Project proj, String aId)
  {
    log.info "Searching {} for refs to {} and updating..", proj, aId
    for (dep: proj.model.dependencyManagement.getDependencies())
      {
        if (dep.groupId.equals('org.kie.kogito') & dep.artifactId.equals(aId))
        {
          log.info "Found references to {}, setting to project version", aId
          dep.version = proj.getVersion()
        }
      }
  }
/*
* Guard against suspected badly aligned deps, this will check that there are no deps remaining
* that have neither the community version or the assigned Red Hat version
*/
  def guardForSuspectedDeps(proj) 
  {
    String kogitoVersion = pme.getUserProperties().getProperty("kogitoVersion") // Community Version
    String projectVersion = proj.getVersion() // Red Hat Version
    List<Dependency> badDeps = [];
    for (dep: proj.model.dependencyManagement.getDependencies())
    {
        if (dep.getVersion() != projectVersion && dep.getVersion() != kogitoVersion)
        {
            badDeps.add(dep)
        }
    }
    if (!badDeps.isEmpty())
    {
        for (dep: badDeps) {
            log.error "{}", dep
        }
        throw new ManipulationException("Suspected badly aligned dependencies found - see above for list")
    }
  }
}

def KogitoBOMExceptionAlignment_exec p = new KogitoBOMExceptionAlignment_exec(pme: pme)
p.execute()
