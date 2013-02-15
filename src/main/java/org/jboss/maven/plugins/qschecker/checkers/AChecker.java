package org.jboss.maven.plugins.qschecker.checkers;

import org.codehaus.plexus.component.annotations.Component;
import org.jboss.maven.plugins.qschecker.QSChecker;

@Component(role = QSChecker.class, hint="achecker")
public class AChecker implements QSChecker {

    @Override
    public void check() {
        System.out.println("Funcionou");

    }

}