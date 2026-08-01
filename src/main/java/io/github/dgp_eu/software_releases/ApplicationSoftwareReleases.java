/**
 * Copyright 2026 Daniel-Gheorghe Popiniuc
 */
package io.github.dgp_eu.software_releases;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import io.github.dgp_eu.tools.cli.CommonInteractiveClass;
import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.FileOperationsClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.RegularExpressionsClass;
import io.github.dgp_eu.tools.databases.SpecificMySqlClass;
import io.github.dgp_eu.tools.databases.SpecificSnowflakeClass;
import io.github.dgp_eu.tools.databases.SpecificSqLiteClass;
import io.github.dgp_eu.tools.json.JsonOperationsClass;
import io.github.dgp_eu.tools.undertow.UndertowClass;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import tools.jackson.databind.JsonNode;

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
        CommonInteractiveClass.startMeUpWithParameters("logs/Software-Releases-", "/software-releases-pom.xml");
        final int intWebExitCode = new CommandLine(new ApplicationSoftwareReleases()).execute(args);
        CommonInteractiveClass.shutMeDownWithParameters(intWebExitCode, args[0]);
    }

    /** Constructor */
    private ApplicationSoftwareReleases() {
        super();
    }

}

/**
 * Captures execution environment details into Log file
 */
@CommandLine.Command(name = "CaptureEnvironmentDetailsIntoJsonFile",
                     description = "Captures execution environment details into Log file")
class CaptureEnvironmentDetailsIntoJsonFile implements Runnable {
    /**
     * adds the options defined in 
     * CommonInteractiveClass.OutFileNameOptionMixinClass to this command
     */
    @Mixin
    private final CommonInteractiveClass.OutFileNameOptionMixinClass optionOut = new CommonInteractiveClass.OutFileNameOptionMixinClass();

    @Override
    public void run() {
        final String strEnvDetails = EnvironmentCapturingAssembleClass.packageCurrentEnvironmentDetailsIntoJson();
        final String strOutFileName = optionOut.getOutFileName();
        final String strFeedback = String.format("Environment details are %s and will intend to write it to %s file", strEnvDetails, strOutFileName);
        LogExposureClass.LOGGER.info(strFeedback);
        FileOperationsClass.ContentWritingSubClass.writeRawTextToFile(strOutFileName, strEnvDetails);
    }

    /**
     * Private constructor to prevent instantiation
     */
    protected CaptureEnvironmentDetailsIntoJsonFile() {
        super();
    }

}

/**
 * clean files older than a given number of days
 */
@CommandLine.Command(name = "GetInformationFromDatabase",
                     description = "Gets information from Database into Log file")
class GetInformationFromDatabase implements Runnable {

    /**
     * Known Database Types
     */
	/* default */ static final List<String> LST_DB_TYPES = Arrays.asList(
        "MySQL",
        "Snowflake"
    );

    /**
     * Known Information Types
     */
	/* default */ static final List<String> LST_INFO_TYPES = Arrays.asList(
        "Columns",
        "Databases",
        "Schemas",
        "TablesAndViews",
        "Views",
        "ViewsLight"
    );

    /**
     * String for Database Type
     */
    @CommandLine.Option(
        names = { "-dbTp", "--databaseType" },
        description = "Type of Database",
        arity = BasicStructuresClass.ARITY_ONLY_ONE,
        required = true,
        completionCandidates = DatabaseTypes.class)
    private String strDbType;

    /**
     * String for Information Type
     */
    @CommandLine.Option(
        names = { "-infTp", "--informationType" },
        description = "Type of Information",
        arity = BasicStructuresClass.ARITY_ONE_OR_MORE,
        required = true,
        completionCandidates = InfoTypes.class)
    private String strInfoType;

    /**
     * Listing available options
     */
    /* default */ static class DatabaseTypes implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return LST_DB_TYPES.iterator();
        }
    }

    /**
     * Listing available options
     */
    /* default */ static class InfoTypes implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return LST_INFO_TYPES.iterator();
        }
    }

    private static Properties getEnvironmentVariableValueForMySql() {
        final InputStream inputStream = BasicStructuresClass.getEnvironmentVariableIntoInputStream("MYSQL");
        final JsonNode ndMySQL = JsonOperationsClass.getJsonFileNodes(inputStream);
        final Properties properties = new Properties();
        properties.put("ServerName", JsonOperationsClass.getJsonValue(ndMySQL, "/ServerName"));
        properties.put("Port", JsonOperationsClass.getJsonValue(ndMySQL, "/Port"));
        properties.put("Username", JsonOperationsClass.getJsonValue(ndMySQL, "/Username"));
        properties.put("Password", JsonOperationsClass.getJsonValue(ndMySQL, "/Password"));
        properties.put("ServerTimezone", JsonOperationsClass.getJsonValue(ndMySQL, "/ServerTimezone"));
        return properties;
    }

    /**
     * Action logic
     *
     * @param strDatabaseType type of Database (predefined values)
     */
    private static void performAction(final String strDatabaseType, final String strLclInfoType) {
        Properties properties = new Properties();
        switch (strDatabaseType) {
            case "MySQL":
                properties = getEnvironmentVariableValueForMySql();
                SpecificMySqlClass.performMySqlPreDefinedAction(strLclInfoType, properties);
                break;
            case "Snowflake":
                SpecificSnowflakeClass.performSnowflakePreDefinedAction(strLclInfoType, properties);
                break;
            default:
                final String strFeedback = String.format("Unknown %s argument received in %s, do not know what to do with it, therefore will quit, bye!", strDatabaseType, StackWalker.getInstance().walk(frames -> frames.findFirst().map(frame -> frame.getClassName() + "." + frame.getMethodName()).orElse(LogExposureClass.STR_I18N_UNKN)));
                LogExposureClass.LOGGER.error(strFeedback);
                break;
        }
    }

    @Override
    public void run() {
        if (!LST_DB_TYPES.contains(strDbType)) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "Invalid value for --databaseType: " + strDbType + ". Valid values are: " + LST_DB_TYPES
            );
        }
        if (!LST_INFO_TYPES.contains(strInfoType)) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "Invalid value for --informationType: " + strInfoType + ". Valid values are: " + LST_INFO_TYPES
            );
        }
        performAction(strDbType, strInfoType);
    }

    /**
     * Constructor
     */
    protected GetInformationFromDatabase() {
        super();
    }
}

/**
 * clean files older than a given number of days
 */
@CommandLine.Command(name = "GetRemoteMavenPackageDetails",
                     description = "Read Maven package details from central Maven repository")
class GetRemoteMavenPackageDetails implements Runnable {

    @Override
    public void run() {
        // no-op
        final String strPackage = "com.github.oshi:oshi-core-ffm";
        final String strVersion = RemoteInformationRetrievalClass.MavenSubClass.getLatestVersionFromMavenCentralRepository(strPackage);
        final String strFeedback = String.format("For package %s latest version is: %s", strPackage, strVersion);
        LogExposureClass.LOGGER.info(strFeedback);
        final String strWebSite = RegularExpressionsClass.buildCentralMavenRepositoryUniformResourceLocator(strPackage);
        final String[] packageParts = strPackage.split(":");
        final String strRemoteFileUrl = String.format("%s%s/%s-%s.jar", strWebSite, strVersion, packageParts[1], strVersion);
        final String strFeedback2 = String.format("Remote file is: %s", strRemoteFileUrl);
        LogExposureClass.LOGGER.info(strFeedback2);
        final Properties urlAttributes = RemoteInformationRetrievalClass.RequestSubClass.requestHttpFile(strRemoteFileUrl, "AttributesFromHeader");
        final String strFeedback3 = String.format("Retrieved attributes from header are: %s", urlAttributes);
        LogExposureClass.LOGGER.info(strFeedback3);
        final String strChecksumUrl = strRemoteFileUrl + ".sha256";
        final String checksumValue = RemoteInformationRetrievalClass.RequestSubClass.requestHttpFile(strChecksumUrl, BasicStructuresClass.STR_CONTENT).getOrDefault(BasicStructuresClass.STR_CONTENT, "MISSING").toString().trim().toLowerCase(Locale.ENGLISH);
        final String strFeedback4 = String.format("SHA-256 from %s has content: %s", strChecksumUrl, checksumValue);
        LogExposureClass.LOGGER.info(strFeedback4);
    }

    /**
     * Constructor
     */
    protected GetRemoteMavenPackageDetails() {
        super();
    }
}

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

    @Override
    public void run() {
        UndertowClass.setWebPort(String.valueOf(optPortNumber.getPortNumber()));
        SpecificSqLiteClass.setInternalDatabase(optLocalDbFile.getLocalDbFile());
        WebClass.setFolderNamesForChecksumExposure(optFolderNames.getFolderNames());
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
