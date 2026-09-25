/** Copyright 2026 Daniel-Gheorghe Popiniuc */
package io.github.dgp_eu.software_releases.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.ProjectClass;
import io.github.dgp_eu.tools.core.ShellingClass;

/**
 * Capturing current environment details
 */
public final class EnvironmentCapturingAssembleClass {
    /** Constant to expose instead of NULL values */
    /* default */ public static final String STR_INSTEAD_NULL = "---";

    /**
     * Constructor
     */
    private EnvironmentCapturingAssembleClass() {
        super();
    }

    /**
     * Environment details gathered
     * @return Map
     */
    private static Map<String, Object> gatherEnvironmentDetails() {
        final String strComputer = getComputerName(STR_INSTEAD_NULL);
        final String username = getUserName(STR_INSTEAD_NULL);
        final String userAccount = ShellingClass.getCurrentUserAccount();
        return Map.of(
                "Computer", strComputer,
                "Country", System.getProperty("user.country", STR_INSTEAD_NULL),
                "Country.Format", System.getProperty("user.country.format", STR_INSTEAD_NULL),
                "Language", System.getProperty("user.language", STR_INSTEAD_NULL),
                "Language.Format", System.getProperty("user.language.format", STR_INSTEAD_NULL),
                "Home", System.getProperty("user.home", STR_INSTEAD_NULL).replace("\\", "\\\\"),
                ConfigurationClass.STR_NAME, System.getProperty("user.name", STR_INSTEAD_NULL),
                "Timezone", System.getProperty("user.timezone", STR_INSTEAD_NULL),
                "Username", username,
                "User Account", userAccount);
    }

    /**
     * Hardware details gathered
     * @return Map
     */
    private static Map<String, Object> gatherHardwareDetails() {
        return Map.of(
                "CPU", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutCentralProcessorUnit()),
                "GPU", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutGraphicCards()),
                "Mainboard", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.MotherboardAndSystemSubClass.getDetailsAboutMainboard()),
                "Monitor", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutMonitor()),
                "Network Interface", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutNetworkInterfaces()),
                "RAM", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutRandomAccessMemory()));
    }

    /**
     * Environment details gathered
     * @return Map
     */
    private static Map<String, Object> gatherJavaDetails() {
        return Map.of(
                "Runtime Name", System.getProperty("java.runtime.name", STR_INSTEAD_NULL),
                "Runtime Version", System.getProperty("java.runtime.version", STR_INSTEAD_NULL),
                ConfigurationClass.STR_VENDOR, System.getProperty("java.vendor", STR_INSTEAD_NULL),
                "Vendor Version", System.getProperty("java.vendor.version", STR_INSTEAD_NULL),
                ConfigurationClass.STR_VERSION, System.getProperty("java.version", STR_INSTEAD_NULL),
                "Version Date", System.getProperty("java.version.date", STR_INSTEAD_NULL),
                "VM Name", System.getProperty("java.vm.name", STR_INSTEAD_NULL),
                "VM Version", System.getProperty("java.vm.version", STR_INSTEAD_NULL),
                "VM Specification Name", System.getProperty("java.vm.specification.name", STR_INSTEAD_NULL),
                "VM Specification Vendor", System.getProperty("java.vm.specification.vendor", STR_INSTEAD_NULL));
    }

    /**
     * Software details gathered
     * @return Map
     */
    private static Map<String, Object> gatherSoftwareDetails() {
        return Map.of(
                "Java", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(gatherJavaDetails()),
                "OS", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutOperatingSystem()),
                "Network", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(EnvironmentHardwareClass.getDetailsAboutNetwork()),
                "Storage", BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(OshiUsageClass.getDetailsAboutAvailableStoragePartitions()));
    }

    /**
     * Capturing computer name
     * @param strInsteadOfNull alternative text if not found
     * @return String with computer name
     */
    public static String getComputerName(final String strInsteadOfNull) {
        String strComputer = System.getenv("COMPUTERNAME");
        if (strComputer == null) {
            strComputer = System.getenv("HOSTNAME");
        }
        if (strComputer == null) {
            strComputer = strInsteadOfNull;
        }
        return strComputer;
    }

    /**
     * Capturing user name
     * @param strInsteadOfNull alternative text if not found
     * @return String with user name
     */
    private static String getUserName(final String strInsteadOfNull) {
        String username = System.getenv("USERNAME");
        if (username == null) {
            username = System.getProperty("user.name", strInsteadOfNull);
        }
        return username;
    }

    /**
     * Capturing current Environment details
     * 
     * @return String
     */
    public static String packageCurrentEnvironmentDetailsIntoJson() {
        final StringBuilder strJsonString = new StringBuilder(1000).append('{');
        final String strFeedback = "Capturing information...";
        LogExposureClass.LOGGER.info(strFeedback);
        final String strHardware = BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(gatherHardwareDetails());
        if (strHardware != null) {
            strJsonString.append("\"Hardware\":").append(strHardware);
        }
        final String strFeedbackH = "I just captured Hardware information...";
        LogExposureClass.LOGGER.debug(strFeedbackH);
        final String strSoftware = BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(gatherSoftwareDetails());
        if (strSoftware != null) {
            strJsonString.append(",\"Software\":").append(strSoftware);
        }
        final String strFeedbackS = "I just captured Software information...";
        LogExposureClass.LOGGER.debug(strFeedbackS);
        final String strAppDetails = ProjectClass.ApplicationSubClass.getApplicationDetails();
        if (strAppDetails != null) {
            strJsonString.append(',').append(strAppDetails);
        }
        final String strEnvironment = BasicStructuresClass.ListAndMapSubClass.getMapIntoJsonString(gatherEnvironmentDetails());
        final String strFeedbackEnv = "I just captured Environment information...";
        LogExposureClass.LOGGER.debug(strFeedbackEnv);
        if (strEnvironment != null) {
            strJsonString.append(",\"Environment\":").append(strEnvironment);
        }
        return BasicStructuresClass.StringCleaningSubClass.ensureEscapingOnEndOfLineAndTabs(strJsonString.append('}').toString());
    }

    /**
     * Capturing current Environment details
     * 
     * @return String
     */
    public static List<Properties> packageCurrentEnvironmentDetailsIntoListOfProperties() {
        final List<Properties> resultReleases = new ArrayList<>();
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Environment", gatherEnvironmentDetails()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - CPU", EnvironmentHardwareClass.getDetailsAboutCentralProcessorUnit()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - GPU", EnvironmentHardwareClass.getDetailsAboutGraphicCards()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - Mainboard", EnvironmentHardwareClass.MotherboardAndSystemSubClass.getDetailsAboutMainboard()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - Monitors", EnvironmentHardwareClass.getDetailsAboutMonitor()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - Network Interfaces", EnvironmentHardwareClass.getDetailsAboutNetworkInterfaces()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Hardware - RAM", EnvironmentHardwareClass.getDetailsAboutRandomAccessMemory()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Software - Java", gatherJavaDetails()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Software - OS", EnvironmentHardwareClass.getDetailsAboutOperatingSystem()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Software - Network", EnvironmentHardwareClass.getDetailsAboutNetwork()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Software - Storage", OshiUsageClass.getDetailsAboutAvailableStoragePartitions()));
        resultReleases.addAll(BasicStructuresClass.ListAndMapSubClass.convertMapOfStringsIntoListOfProperties("Application", ProjectClass.ApplicationSubClass.getApplicationDetailsIntoMap()));
        return resultReleases;
    }

}
