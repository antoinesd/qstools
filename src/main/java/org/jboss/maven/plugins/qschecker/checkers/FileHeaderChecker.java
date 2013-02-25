package org.jboss.maven.plugins.qschecker.checkers;

import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qschecker.QSChecker;

@Component(role = QSChecker.class, hint = "fileHeaderChecker")
public class FileHeaderChecker extends AbstractCheckstyleChecker {

    /* (non-Javadoc)
     * @see org.jboss.maven.plugins.qschecker.QSChecker#getCheckerDescription()
     */
    @Override
    public String getCheckerDescription() {
        return "Verifies if project files (*.java, *.xml, *.properties) contains license header";
    }

    @Override
    String getIncludes() {
       return "**/*.java, **/*.xml, **/*.properties";
    }

    @Override
    String getCheckstyleConfig() {
       return "checkstyle-header.xml";
    }

}