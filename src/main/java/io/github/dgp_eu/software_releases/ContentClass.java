package io.github.dgp_eu.software_releases;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SequencedMap;

import gg.jte.output.Utf8ByteOutput;
import io.github.dgp_eu.software_releases.environment.EnvironmentCapturingAssembleClass;
import io.github.dgp_eu.software_releases.environment.EnvironmentSoftwareReleasesSubClass;
import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.FileStatisticsClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.time.TimingClass;
import io.github.dgp_eu.tools.dynamic.JsonOperationsClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseSpecificSqLiteClass;
import io.github.dgp_eu.tools.dynamic.web.HtmlClass;
import io.github.dgp_eu.tools.dynamic.web.JavaTemplateRenderingClass;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import tools.jackson.databind.JsonNode;

/**
 * Handling Software releases logic
 */
public final class ContentClass {

    // Private constructor to prevent instantiation
    private ContentClass() {
        // intentional empty
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
        return "<div><a href=\"?page=downloadEnvironmentDetailsAsJSONfile\">Dowload to a JSON file</a></div>"
                + HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(orderedList, objFeatures);
    }

    /**
     * Outputs file statistics into an HTML table
     * @return String
     */
    private static String getFileHashingAsHtmlTable() {
        final String[] inAlgorithms = {"SHA-256"};
        FileStatisticsClass.setChecksumAlgorithms(inAlgorithms);
        final String[] folderNames = WebClass.getFolderNames();
        final List<Properties> foldersStatistics = new ArrayList<>();
        for(final String crtFolderName: folderNames) {
            final String strFeedback = String.format("Will process folder %s", crtFolderName);
            LogExposureClass.LOGGER.info(strFeedback);
            final ZonedDateTime refTimeStamp = TimingClass.getCurrentZonedDateTime();
            final List<Properties> crtFileStatistics = FileStatisticsClass.getFileStatisticsIntoListOfProperties(crtFolderName, refTimeStamp);
            foldersStatistics.addAll(crtFileStatistics);
        }
        final List<String> desiredOrder = List.of("Folder", "File", "Size [bytes]", ConfigurationClass.STR_SIZE, "SHA-256", "Last Modified Timestamp", "Last Modified Aging");
        final List<SequencedMap<Object, Object>> orderedList = foldersStatistics.stream()
                .map(prop -> BasicStructuresClass.ListAndMapSubClass.sortProperties(prop, desiredOrder))
                .toList();
        return HtmlClass.TableSubClass.getListOfSequencedMapIntoHtmlTable(orderedList, WebClass.EMPTY_TABLE_PROPS);
    }

    /**
     * Sun details for all Locations with JSON
     * @return String with UI of Locations as tabs
     */
    private static String getLocationSunDetailsAsHtmlTable() {
        final JsonNode jsonArray = JsonOperationsClass.getJsonFileNodes(Path.of(WebClass.getJsonLocationsFile()));
        final StringBuilder sbReturn = new StringBuilder(1000);
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
            final SequencedMap<String, Object> sortedSun = BasicStructuresClass.ListAndMapSubClass.sortMapByKey(mapSunRiseAndSet);
            sbReturn.append("<div class=\"tabbertab\" title=\"")
                    .append(strTabTitle)
                    .append("\"><table style=\"float:left;\">");
            sortedSun.forEach((crtKey, crtValue) -> {
                if (!crtValue.equals(strTabTitle)) {
                    sbReturn.append("<tr><th style=\"text-align:left;\">")
                           .append(crtKey)
                           .append("</th><td>")
                           .append(crtValue)
                           .append("</td></tr>");
                }
            });
            sbReturn.append("</table>"
                    + "<div style=\"float:none;clear:both;height:5px;\">&nbsp;</div>"
                    + "</div><!-- %s -->");
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
        final List<Properties> softwareReleases = EnvironmentSoftwareReleasesSubClass.consolidateSoftwareReleases();
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
                    WebClass.EMPTY_TABLE_PROPS);
            default                                   -> String.format("Welcome %s",
                    System.getProperty("user.name", "UNKNOWN user.name"));
        });
    }

    /**
     * Handle JSON content with Environment details
     * @param inExchange input Exchange
     */
    public static void handleJsonContent(final HttpServerExchange inExchange) {
        final String jsonEnvironment = EnvironmentCapturingAssembleClass.packageCurrentEnvironmentDetailsIntoJson();
        final Utf8ByteOutput outputJson = new Utf8ByteOutput();
        outputJson.writeContent(jsonEnvironment);
        JavaTemplateRenderingClass.setOutput(outputJson);
        JavaTemplateRenderingClass.setContentTypeValue("application/json");
        final String strContentDisp = String.format("attachment; filename=\"environment__%s__%s.json\";",
                EnvironmentCapturingAssembleClass.getComputerName("UNNAMED_COMPUTER"),
                TimingClass.getCurrentDateTimeUniveralTimeCoordination().replaceAll("[-:\\s\\.]", "_"));
        JavaTemplateRenderingClass.setContentDisposition(strContentDisp);
        final HeaderMap header = inExchange.getResponseHeaders();
        JavaTemplateRenderingClass.handleResponseHeader(header);
        final Sender response = inExchange.getResponseSender();
        JavaTemplateRenderingClass.handleRawResponseSender(response, jsonEnvironment);
    }

}