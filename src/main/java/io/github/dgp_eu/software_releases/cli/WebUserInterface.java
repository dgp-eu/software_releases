package io.github.dgp_eu.software_releases.cli;

import io.github.dgp_eu.software_releases.WebClass;
import io.github.dgp_eu.tools.core.CommonInteractiveClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseSpecificSqLiteClass;
import io.github.dgp_eu.tools.dynamic.web.UndertowClass;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;

/**
 * Supports web interface
 */
@CommandLine.Command(
        name = "WebUserInterface",
        description = "Initiate Web User Interface")
class WebUserInterface implements Runnable {

    /**
     * adds the options defined in
     * CommonInteractiveClass.LocalDatabaseFileMixinClass to this command
     */
    @Mixin
    private final CommonInteractiveClass.LocalDatabaseFileMixinClass optLocalDbFile = new CommonInteractiveClass.LocalDatabaseFileMixinClass();

    /**
     * adds the options defined in
     * CommonInteractiveClass.PortOptionMixinClass to this command
     */
    @Mixin
    private final CommonInteractiveClass.PortOptionMixinClass optPortNumber = new CommonInteractiveClass.PortOptionMixinClass();

    /**
     * adds the options defined in
     * CommonInteractiveClass.FolderNameOptionMixinClass to this command
     */
    @Mixin
    private final CommonInteractiveClass.FolderNameOptionMixinClass optFolderNames = new CommonInteractiveClass.FolderNameOptionMixinClass();

    /**
     * String for out FileName
     */
    @CommandLine.Option(
            names = {"-jl", "--jsonLocations"},
            description = "JSON file name with array of Locations",
            arity = CommonInteractiveClass.ARITY_ONLY_ONE,
            required = true)
    private String strJsonLocations;

    @Override
    public void run() {
        UndertowClass.setWebPort(String.valueOf(optPortNumber.getPortNumber()));
        DatabaseSpecificSqLiteClass.setInternalDatabase(optLocalDbFile.getLocalDbFile());
        WebClass.setFolderNamesForChecksumExposure(optFolderNames.getFolderNames());
        WebClass.setJsonLocationsFile(strJsonLocations);
        UndertowClass.setRootHandler(WebClass.handleWebContent());
        UndertowClass.runWebServer();
    }

    /**
     * Constructor
     */
    protected WebUserInterface() {
        // intentionally blank
    }
}