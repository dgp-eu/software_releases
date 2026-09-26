/** Copyright 2026 Daniel-Gheorghe Popiniuc */
package io.github.dgp_eu.software_releases;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

import org.jspecify.annotations.NonNull;

import gg.jte.TemplateEngine;
import gg.jte.output.Utf8ByteOutput;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.ProjectClass;
import io.github.dgp_eu.tools.core.time.TimingClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseSpecificSqLiteClass;
import io.github.dgp_eu.tools.dynamic.web.JavaTemplateRenderingClass;
import io.github.dgp_eu.tools.dynamic.web.HtmlClass;
import io.github.dgp_eu.tools.dynamic.web.UndertowClass;
import io.github.dgp_eu.tools.dynamic.web.UndertowParametersClass;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Web interface class
 */
public final class WebClass {
    /** Constant for "Software Releases" */
    public static final String STR_SOFT_RELEASES = "Software Releases";
    /** Menu */
    private static final SequencedMap<String, Map<String, String>> MAP_MENU = new LinkedHashMap<>();
    /** Variable for Folders relevant for Checksum Exposure */
    private static String[] strFolderNames = new String[0];
    /** Variable for JSON file with Locations */
    private static String jsonLocations;

    static {
        buildMenu();
    }

    /**
     * Getter for jsonLocations
     */
    public static String[] getFolderNames() {
        return Arrays.copyOf(strFolderNames, strFolderNames.length);
    }

    /**
     * Getter for jsonLocations
     */
    public static String getJsonLocationsFile() {
        return jsonLocations;
    }

    /**
     * Menu builder
     * @return SequencedMap for HTML menu
     */
    private static void buildMenu() {
        MAP_MENU.put("home", Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-house-user fa-2x",
                ConfigurationClass.STR_MENU, "HomePage",
                ConfigurationClass.STR_TITLE, "HomePage"));
        MAP_MENU.put(ConfigurationClass.STR_SOFTWARE_RLS, Map.of(
                ConfigurationClass.STR_ICON, "fa-brands fa-dev fa-2x",
                ConfigurationClass.STR_MENU, STR_SOFT_RELEASES,
                ConfigurationClass.STR_TITLE, STR_SOFT_RELEASES));
        MAP_MENU.put(ConfigurationClass.STR_TS, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-square-poll-horizontal fa-2x",
                ConfigurationClass.STR_MENU, "SQLite Table Statistics",
                ConfigurationClass.STR_TITLE, "SQLite Table Statistics"));
        MAP_MENU.put(ConfigurationClass.STR_FILE_HASHING, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-hashtag fa-2x",
                ConfigurationClass.STR_MENU, "Downloads File Hashing",
                ConfigurationClass.STR_TITLE, "Downloads File Hashing"));
        MAP_MENU.put(ConfigurationClass.STR_ENV_DTLS, Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-computer fa-2x",
                ConfigurationClass.STR_MENU, "Environment Details",
                ConfigurationClass.STR_TITLE, "Environment Details"));
        MAP_MENU.put("locationSun", Map.of(
                ConfigurationClass.STR_ICON, "fa-solid fa-business-time fa-2x",
                ConfigurationClass.STR_MENU, "Location Time",
                ConfigurationClass.STR_TITLE, "Location Time"));
    }

    /**
     * Handle HTML content
     * @param inExchange input Exchange
     * @param page id for the content
     */
    private static void handleHtmlContent(final HttpServerExchange inExchange, final String page) {
        JavaTemplateRenderingClass.setContentDispositionAndTypeValuesForHtmlContent();
        UndertowClass.handleCommonThings(inExchange);
        final TemplateEngine templateEngine = UndertowClass.createTemplateEngine();
        final Utf8ByteOutput output = new Utf8ByteOutput();
        JavaTemplateRenderingClass.setOutput(output);
        packAllParameters(page);
        JavaTemplateRenderingClass.renderTemplate(templateEngine, "sr.jte");
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
            default                                  -> "<script>document.getElementById('infoContextId').style = 'display:none;';</script>";
        });
    }

    /**
     * Handle web content
     * @return PathHandler web content
     */
    public static HttpHandler handleWebContent() {
        return exchange -> {
            final ZonedDateTime startWebTimeStamp = TimingClass.getCurrentZonedDateTime();
            UndertowClass.handleQueryParametersAndPage(exchange);
            JavaTemplateRenderingClass.setServerExchange(exchange);
            final String page = UndertowParametersClass.getPageParameter();
            final String jsonPage = "downloadEnvironmentDetailsAsJSONfile";
            if (page.equalsIgnoreCase(jsonPage)) {
                ContentClass.handleJsonContent(exchange);
            } else {
                handleHtmlContent(exchange, page);
            }
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
    private static void packAllParameters(final String page) {
        JavaTemplateRenderingClass.packParameter("page", page);
        String title = page;
        if (!ConfigurationClass.STR_LOCALIZATION.equalsIgnoreCase(page)) {
            final Map<String, String> menuEntry = MAP_MENU.get(page);
            title = menuEntry != null ? menuEntry.getOrDefault(ConfigurationClass.STR_TITLE, page) : page;
        }
        JavaTemplateRenderingClass.packParameter("title", title);
        final gg.jte.Content myMenu = output -> output.writeContent(HtmlClass.buildMenuString(MAP_MENU));
        JavaTemplateRenderingClass.packParameter("menu", myMenu);
        JavaTemplateRenderingClass.packParameter("infoContext", handleInfoContext(page));
        JavaTemplateRenderingClass.packParameter("mainContent", ContentClass.handleBodyContent(page));
        JavaTemplateRenderingClass.packCommonParameters();
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

    private WebClass() {
        // intentionally blank
    }

}
