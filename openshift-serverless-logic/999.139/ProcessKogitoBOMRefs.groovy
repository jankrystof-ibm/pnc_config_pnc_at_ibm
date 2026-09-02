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

/**
* When we force align from SNAPSHOT -> a real version using ProcessKogitoBOM there
* are some dependencyManagement sections referencing kogito-bom that don't get updated
* automatically and stay as SNAPSHOT versions.
*
* This script runs LAST since it uses the project version thats already aligned to
* use for the version of kogito-bom.
**/
@InvocationPoint(invocationPoint = InvocationStage.LAST)
@PMEBaseScript BaseScript pme
@Slf4j
public class ProcessKogitoBOMRefs_exec
{
  BaseScript pme;

  def execute()
  {
    for (proj: pme.getProjects())
    {
      searchAndUpdateDepMgmt(proj)
    }
  }

  def searchAndUpdateDepMgmt(Project proj)
  {
    log.info "Searching {} for kogito-bom refs", proj
    if (proj.model.dependencyManagement != null)
    {
      for (dep: proj.model.dependencyManagement.getDependencies())
      {
        if (dep.groupId.equals('org.kie.kogito') & dep.artifactId.contains('bom'))
        {
          log.info "Found org.kie.kogito BOM references in {} - rewriting", proj
          dep.version = proj.getVersion()
        }
      }
    }
  }
}

def ProcessKogitoBOMRefs_exec p = new ProcessKogitoBOMRefs_exec(pme: pme)
p.execute()
