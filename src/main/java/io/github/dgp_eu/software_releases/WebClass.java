/** Copyright 2026 Daniel-Gheorghe Popiniuc */
package io.github.dgp_eu.software_releases;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SequencedMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

import gg.jte.TemplateEngine;
import gg.jte.output.Utf8ByteOutput;
import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.FileOperationsClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.ProjectClass;
import io.github.dgp_eu.tools.core.time.TimingClass;
import io.github.dgp_eu.tools.dynamic.JsonOperationsClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseOperationsClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseSpecificSqLiteClass;
import io.github.dgp_eu.tools.dynamic.web.HtmlClass;
import io.github.dgp_eu.tools.dynamic.web.UndertowClass;
import io.undertow.server.HttpHandler;
import tools.jackson.databind.JsonNode;

/**
 * Web interface class
 */
public final class WebClass {
    /** Constant for "Software Releases" */
    public static final String STR_SOFT_RELEASES = "Software Releases";
    /** Menu */
    private static final SequencedMap<String, Map<String, String>> MAP_MENU = new LinkedHashMap<>();
    /** Intentionally empty table properties for views that require no extra options. */
    private static final Properties EMPTY_TABLE_PROPS = new Properties();
    /** Variable for Folders relevant for Checksum Exposure */
    private static String[] strFolderNames = new String[0];
    /** Variable for JSON file with Locations */
    private static String jsonLocations;

    static {
        buildMenu();
    }

    /**
     * Menu builder
     * @return SequencedMap for HTML menu
     */
    private static void buildMenu() {
        MAP_MENU.put("home", Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-house-user",
                ConfigurationClass.STR_MENU, "HomePage",
                ConfigurationClass.STR_TITLE, "HomePage"));
        MAP_MENU.put(ConfigurationClass.STR_SOFTWARE_RLS, Map.of(
                ConfigurationClass.STR_ICON, "fa-brands fa-dev",
                ConfigurationClass.STR_MENU, STR_SOFT_RELEASES,
                ConfigurationClass.STR_TITLE, STR_SOFT_RELEASES));
        MAP_MENU.put(ConfigurationClass.STR_TS, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-square-poll-horizontal",
                ConfigurationClass.STR_MENU, "SQLite Table Statistics",
                ConfigurationClass.STR_TITLE, "SQLite Table Statistics"));
        MAP_MENU.put(ConfigurationClass.STR_FILE_HASHING, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-hashtag",
                ConfigurationClass.STR_MENU, "Downloads File Hashing",
                ConfigurationClass.STR_TITLE, "Downloads File Hashing"));
        MAP_MENU.put(ConfigurationClass.STR_ENV_DTLS, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-computer",
                ConfigurationClass.STR_MENU, "Environment Details",
                ConfigurationClass.STR_TITLE, "Environment Details"));
        MAP_MENU.put("locationSun", Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-business-time",
                ConfigurationClass.STR_MENU, "Location Time",
                ConfigurationClass.STR_TITLE, "Location Time"));
    }

    /**
     * Outputs file statistics into an HTML table
     * @return String
     */
    private static String getEnvironmentDetailsAsHtmlTable() {
        final Properties objFeatures = new Properties();
        objFeatures.put(ConfigurationClass.STR_NEW_TAB, ConfigurationClass.STR_CATEGORY);
        final List<Properties> envDetails = EnvironmentCapturingAssembleClass.packageCurrentEnvironmentDetailsIntoListOfProperties();
        final List<String> desiredOrder = List.of(ConfigurationClass.STR_CATEGORY, "Element", "Value");
        final List<SequencedMap<Object, Object>> orderedList = envDetails.stream()
                .map(prop -> BasicStructuresClass.ListAndMapSubClass.sortProperties(prop, desiredOrder))
                .toList();
        return HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(orderedList, objFeatures);
    }

    /**
     * Outputs file statistics into an HTML table
     * @return String
     */
    private static String getFileHashingAsHtmlTable() {
        final String[] inAlgorithms = {"SHA-256"};
        FileOperationsClass.StatisticsSubClass.setChecksumAlgorithms(inAlgorithms);
        final String[] folderNames = Arrays.copyOf(strFolderNames, strFolderNames.length);
        final List<Properties> foldersStatistics = new ArrayList<>();
        for(final String crtFolderName: folderNames) {
            final String strFeedback = String.format("Will process folder %s", crtFolderName);
            LogExposureClass.LOGGER.info(strFeedback);
            final ZonedDateTime refTimeStamp = TimingClass.getCurrentZonedDateTime();
            final List<Properties> crtFileStatistics = FileOperationsClass.StatisticsSubClass.getFileStatisticsIntoListOfProperties(crtFolderName, refTimeStamp);
            foldersStatistics.addAll(crtFileStatistics);
        }
        final List<String> desiredOrder = List.of("Folder", "File", "Size [bytes]", ConfigurationClass.STR_SIZE, "SHA-256", "Last Modified Timestamp", "Last Modified Aging");
        final List<SequencedMap<Object, Object>> orderedList = foldersStatistics.stream()
                .map(prop -> BasicStructuresClass.ListAndMapSubClass.sortProperties(prop, desiredOrder))
                .toList();
        return HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(orderedList, EMPTY_TABLE_PROPS);
    }

    /**
     * Sun details for all Locations with JSON
     * @return String with UI of Locations as tabs
     */
    private static String getLocationSunDetailsAsHtmlTable() {
        final JsonNode jsonArray = JsonOperationsClass.getJsonFileNodes(Path.of(jsonLocations));
        StringBuilder sbReturn = new StringBuilder(100);
        sbReturn.append("<div id=\"tabStandard\" class=\"tabber\">");
        jsonArray.forEach(crtLocation -> {
            SunClass.setZoneId(crtLocation.get("TimeZoneName").toString().replace("\"", ""));
            SunClass.setLatitude(Double.parseDouble(crtLocation.get("Latitude").toString()));
            SunClass.setLongitude(Double.parseDouble(crtLocation.get("Longitude").toString()));
            final String strTabTitle = crtLocation.get("LocationPlaceDivisionCountry").toString().replace("\"", "");
            final Map<String, Object> mapSunRiseAndSet = SunClass.getSunRiseAndSet(strTabTitle);
            final String strFeedback = String.format("LocationPlaceDivisionCountry is %s and has details as %s",
                    strTabTitle,
                    mapSunRiseAndSet.toString());
            LogExposureClass.LOGGER.debug(strFeedback);
            final Map<String, Object> sortedSun = mapSunRiseAndSet.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, _) -> e1, // merge function (not used here)
                            LinkedHashMap::new // preserve sorted order
                    ));
            sbReturn.append("<div class=\"tabbertab\" title=\"")
                    .append(strTabTitle)
                    .append("\">")
                    .append("<table style=\"float:left;\">");
            sortedSun.forEach((crtKey, crtValue) -> {
                if (!crtValue.equals(strTabTitle)) {
                    sbReturn.append("<tr>")
                           .append("<th>").append(crtKey).append("</th>")
                           .append("<td>").append(crtValue).append("</td>")
                           .append("</tr>");
                }
            });
            sbReturn.append("</table>")
                    .append("</div><!-- %s -->");
        });
        sbReturn.append("</div><!-- tabStandard -->");
        return sbReturn.toString();
    }

    /**
     * expose Software Release details from internal DB
     * @return String software releases details
     */
    private static String getSoftwareReleasesIntoHtmlTable() {
        String strReturn = "No software releases found.";
        final List<Properties> softwareReleases = SoftwareReleasesSubClass.consolidateSoftwareReleases();
        if (!softwareReleases.isEmpty()) {
            final List<String> desiredOrder = List.of("Organization", "Product", "Version", "Date", "Files");
            final List<SequencedMap<Object, Object>> orderedList = softwareReleases.stream()
                    .map(prop -> BasicStructuresClass.ListAndMapSubClass.sortProperties(prop, desiredOrder))
                    .toList();
            final Properties objFeatures = new Properties();
            objFeatures.put(ConfigurationClass.STR_NEW_TAB, "Profile");
            strReturn = HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(orderedList, objFeatures);
        }
        return strReturn;
    }

    /**
     * Body content handler
     * @param page page identifier
     * @return web Content
     */
    public static gg.jte.Content handleBodyContent(final String page) {
        return output -> output.writeContent(switch(page) {
            case ConfigurationClass.STR_ENV_DTLS      -> getEnvironmentDetailsAsHtmlTable();
            case ConfigurationClass.STR_FILE_HASHING  -> getFileHashingAsHtmlTable();
            case ConfigurationClass.STR_SOFTWARE_RLS  -> getSoftwareReleasesIntoHtmlTable();
            case "locationSun"                        -> getLocationSunDetailsAsHtmlTable();
            case ConfigurationClass.STR_TS            -> HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(
                    DatabaseSpecificSqLiteClass.SqLiteStatisticsSubClass.getTableStatisticsIntoListForHtmlTable(),
                    EMPTY_TABLE_PROPS);
            default                                   -> String.format("Welcome %s",
                    System.getProperty("user.name", "UNKNOWN user.name"));
        });
    }

    /**
     * Info context handler
     * @param page page identifier
     * @return info Context
     */
    private static gg.jte.Content handleInfoContext(final String page) {
        return output -> output.writeContent( switch (page) {
            case ConfigurationClass.STR_ENV_DTLS     -> HtmlClass.FileInfoSubClass.gatherFileStatistics(Path.of(ProjectClass.getPomFile()));
            case ConfigurationClass.STR_SOFTWARE_RLS,
                    ConfigurationClass.STR_TS        -> HtmlClass.FileInfoSubClass.gatherFileStatistics(Path.of(DatabaseSpecificSqLiteClass.getInternalDatabase()));
            default                                    -> "<script>document.getElementById('infoContextId').style = 'display:none;';</script>";
        });
    }

    /**
     * Handle web content
     * @return PathHandler web content
     */
    public static HttpHandler handleWebContent() {
        return exchange -> {
            final ZonedDateTime startWebTimeStamp = TimingClass.getCurrentZonedDateTime();
            UndertowClass.handleCommonThings(exchange);
            final TemplateEngine templateEngine = UndertowClass.createTemplateEngine();
            final Utf8ByteOutput output = new Utf8ByteOutput();
            UndertowClass.TemplateRenderingSubClass.setOutput(output);
            UndertowClass.TemplateRenderingSubClass.setServerExchange(exchange);
            packAllParameters();
            UndertowClass.TemplateRenderingSubClass.renderTemplate(templateEngine, "index.jte");
            final String page = UndertowClass.ParametersSubClass.getPageParameter();
            final ZonedDateTime stopWebTimeStamp = TimingClass.getCurrentZonedDateTime();
            final String strFeedbackEnd = TimingClass.logDuration(startWebTimeStamp,
                    stopWebTimeStamp,
                    String.format("Page %s processing got completed", page));
            LogExposureClass.LOGGER.info(strFeedbackEnd);
        };
    }

    /**
     * Packing all parameters to Template
     */
    private static void packAllParameters() {
        final String page = UndertowClass.ParametersSubClass.getPageParameter();
        UndertowClass.TemplateRenderingSubClass.packParameter("page", page);
        String title = page;
        if (!ConfigurationClass.STR_LOCALIZATION.equalsIgnoreCase(page)) {
            final Map<String, String> menuEntry = MAP_MENU.get(page);
            title = menuEntry != null ? menuEntry.getOrDefault(ConfigurationClass.STR_TITLE, page) : page;
        }
        UndertowClass.TemplateRenderingSubClass.packParameter("title", title);
        final gg.jte.Content myMenu = output -> output.writeContent(HtmlClass.buildMenuString(MAP_MENU));
        UndertowClass.TemplateRenderingSubClass.packParameter("menu", myMenu);
        UndertowClass.TemplateRenderingSubClass.packParameter("infoContext", handleInfoContext(page));
        UndertowClass.TemplateRenderingSubClass.packParameter("mainContent", handleBodyContent(page));
        UndertowClass.TemplateRenderingSubClass.packCommonParameters();
    }

    /**
     * Setter for strFolderNames
     * @param inFolderNames list of Folders relevant for checksum exposure
     */
    public static void setFolderNamesForChecksumExposure(@NonNull final String... inFolderNames) {
        strFolderNames = Arrays.copyOf(inFolderNames, inFolderNames.length);
    }

    /**
     * Setter for jsonLocations
     * @param inJsonLocations JSON file with Locations as array
     */
    public static void setJsonLocationsFile(@NonNull final String inJsonLocations) {
        jsonLocations = inJsonLocations;
    }

    /**
     * Handling Software releases logic
     */
    public static final class SoftwareReleasesSubClass {

        /**
         * expose Software Release details from internal DB
         * @return List software releases details
         */
        public static List<Properties> consolidateSoftwareReleases() {
            final List<Properties> softwareReleases = new ArrayList<>();
            final List<Properties> resultReleases = getSoftwareReleasesFromDatabase();
            if (!resultReleases.isEmpty()) {
                resultReleases.forEach(recordProperties -> {
                    final Properties newProperties = new Properties();
                    newProperties.put("Organization",
                            String.format("%s<div style=\"text-align:right;\">[%s]</div>",
                                    recordProperties.get("OrganizationName"),
                                    recordProperties.get("OrganizationId")));
                    newProperties.put("Product",
                            String.format("<a href=\"%s\" target=\"_blank\"><span style=\"float:left;\">%s<br/>[%s]</span><span style=\"float:right;text-align:right;\">%s<br/>[%s]</span></a>",
                                    recordProperties.get("Releases"),
                                    recordProperties.get("ProductName"),
                                    recordProperties.get("ProductId"),
                                    recordProperties.get("BranchName"),
                                    recordProperties.get("BranchId")));
                    newProperties.put("Version",
                            String.format("%s<div style=\"text-align:right;\">[%s]</div>",
                                    recordProperties.get("Latest release version"),
                                    recordProperties.get("VersionId")));
                    newProperties.put("Date",
                            String.format("%s<br>==> %s",
                                    recordProperties.get("Latest release date"),
                                    recordProperties.get("Latest release aging full").toString()));
                    newProperties.put("Files",
                            String.format("%s [%s]<br/>==> %s [%s]",
                                    recordProperties.get("File Kit Name"),
                                    recordProperties.get("File Kit Id"),
                                    recordProperties.get("File Installed Name"),
                                    recordProperties.get("File Installed Id")));
                    newProperties.put("Profile",
                            recordProperties.get("Profile Name"));
                    String lastRlsAgingDays = String.valueOf(recordProperties.get("Latest release aging days"));
                    if (ConfigurationClass.STR_NULL.equals(lastRlsAgingDays)) {
                        lastRlsAgingDays = "";
                    }
                    newProperties.put(ConfigurationClass.STR_ROW_STYLE,
                            establishRowStyle(lastRlsAgingDays.replaceAll("\\.0$", "")));
                    softwareReleases.add(newProperties);
                });
            }
            return softwareReleases;
        }

        /**
         * Row Style logic
         * @param agingDays number of days
         * @return String row style
         */
        private static String establishRowStyle(final String agingDays) {
            String strRowColor = "#fff"; // white
            if (!agingDays.isEmpty()) {
                final long[] longRanges = {14, 30, 90};
                final long longAging = BasicStructuresClass.convertStringIntoLong(agingDays);
                if (longAging <= longRanges[0]) {
                    strRowColor = "#51ff6d"; // bright green
                } else if (longAging <= longRanges[1]) {
                    strRowColor = "#ccffe8"; // washed out green
                } else if (longAging <= longRanges[2]) {
                    strRowColor = "#fdffcc"; // washed out yellow
                }
            }
            return String.format("background-color:%s;", strRowColor);
        }

        /**
         * expose Software Release details from internal DB
         * @return List software releases details
         */
        private static List<Properties> getSoftwareReleasesFromDatabase() {
            List<Properties> resultReleases = new ArrayList<>();
            try (Connection objConnection = DatabaseSpecificSqLiteClass.getSqLiteConnection();
                 Statement objStatement = DatabaseOperationsClass.ConnectivitySubClass.createSqlStatement(ConfigurationClass.STR_SQLITE, objConnection)) {
                final String queryToUse = DatabaseOperationsClass.getPreDefinedQuery(ConfigurationClass.STR_SQLITE, "ReleasesListProductBranches");
                final Properties rsProperties = DatabaseOperationsClass.packageResultSetProperties(STR_SOFT_RELEASES, queryToUse);
                resultReleases = DatabaseOperationsClass.ResultSettingSubClass.getResultSetStandardized(objStatement, rsProperties, new Properties());
            } catch (SQLException e) {
                final String strFeedbackErr = String.format("%s connection has failed %s", ConfigurationClass.STR_SQLITE, e.getLocalizedMessage());
                LogExposureClass.LOGGER.debug(strFeedbackErr);
            }
            return resultReleases;
        }

        // Private constructor to prevent instantiation
        private SoftwareReleasesSubClass() {
            // intentional empty
        }

    }

    private WebClass() {
        // intentionally blank
    }

}
