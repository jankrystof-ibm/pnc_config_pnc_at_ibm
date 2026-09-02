import groovy.util.logging.Slf4j
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
*
**/
@InvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
@Slf4j
public class ProcessKogitoBOMexec
{
  BaseScript pme;
  String communityVersion;

  def execute()
  {
    communityVersion = pme.getUserProperties().getProperty('kogitoVersion')
    setParentVersions()
    setProjectVersions()
  }

  /**
  * Set the project versions from SNAPSHOT to something real. Requires -DcommunityVersion to be set.
  * We need this so properties inlined later are inlined as a real version and not a SNAPSHOT version.
  **/
  def setProjectVersions()
  {
    log.info "--------------------------------------------------------------------------------------------------------"
    log.info "Setting Project Versions"
    log.info "--------------------------------------------------------------------------------------------------------"
    for (proj : pme.getProjects())
    {
      log.info "Setting project {} version {} -> {}", proj.getArtifactId(), proj.getVersion(), communityVersion
      proj.getModel().setVersion(communityVersion)
      inlineProps(proj)
      log.info "Profiles : {}", proj.getModel().getProfiles()
    }
  }

  def setParentVersions()
  {
    log.info "--------------------------------------------------------------------------------------------------------"
    log.info "Setting Project Parent Versions"
    log.info "--------------------------------------------------------------------------------------------------------"
    for (proj : pme.getProjects())
    {
      Parent parent = proj.getModelParent()
      if ('org.kie.kogito'.equals(parent.getGroupId()))
      {
        parent.setVersion(communityVersion)
      }
    }
  }
  /**
  * Inline properties, for Kogito BOM this changes from project.version -> 1.11.0.Final (for example). This relies on
  * the version changes above in setProjectVersions.
  **/
  def inlineProps(Project proj)
  {
    log.info "--------------------------------------------------------------------------------------------------------"
    log.info "Inlining Properties for {}", proj
    log.info "--------------------------------------------------------------------------------------------------------"
    pme.inlineProperty(proj, SimpleProjectRef.parse("org.kie.kogito:*"));
  }
}

def ProcessKogitoBOMexec p = new ProcessKogitoBOMexec(pme: pme)
p.execute()
