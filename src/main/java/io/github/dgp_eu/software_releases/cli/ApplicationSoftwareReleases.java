/**
 * Copyright 2026 Daniel-Gheorghe Popiniuc
 */
package io.github.dgp_eu.software_releases.cli;

import io.github.dgp_eu.tools.core.CommonInteractiveClass;
import picocli.CommandLine;

/**
 * Main Command Line
 */
@CommandLine.Command(
        name = "top",
        subcommands = {
                CaptureEnvironmentDetailsIntoJsonFile.class,
                GetInformationFromDatabase.class,
                GetRemoteMavenPackageDetails.class,
                WebUserInterface.class
        }
)
public final class ApplicationSoftwareReleases {

    /**
     * Constructor
     *
     * @param args command-line arguments
     */
    /* default */ static void main(final String... args) {
        final String logFullFilePath = System.getProperty("java.io.tmpdir")
                + "LogsSoftwareReleases/Software-Releases";
        CommonInteractiveClass.startMeUpWithParameters(logFullFilePath, "/software-releases-pom.xml");
        final int intWebExitCode = new CommandLine(new ApplicationSoftwareReleases()).execute(args);
        CommonInteractiveClass.shutMeDownWithParameters(intWebExitCode, args[0]);
    }

    /** Constructor */
    private ApplicationSoftwareReleases() {
        super();
    }

}
