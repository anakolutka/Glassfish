package com.sun.enterprise.v3.admin;

import com.sun.enterprise.module.ModulesRegistry;
import org.jvnet.glassfish.api.ActionReport;
import org.jvnet.glassfish.api.ActionReport.ExitCode;
import org.jvnet.glassfish.api.admin.AdminCommand;
import org.jvnet.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Dumps the currently configured HK2 modules and their contents.
 *
 * <p>
 * Useful for debugging classloader related issues.
 *
 * @author Kohsuke Kawaguchi
 */
@Service(name="dump-hk2")
public class DumpHK2Command implements AdminCommand {
    public void execute(AdminCommandContext context) {
        ModulesRegistry r = ModulesRegistry.find(getClass());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        r.dumpState(new PrintStream(baos));

        ActionReport report = context.getActionReport();
        report.setActionExitCode(ExitCode.SUCCESS);
        report.setMessage(baos.toString());
    }
}
