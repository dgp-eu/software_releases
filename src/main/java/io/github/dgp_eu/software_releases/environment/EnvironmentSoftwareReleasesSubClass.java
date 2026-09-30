package io.github.dgp_eu.software_releases.environment;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.github.dgp_eu.software_releases.WebClass;
import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseOperationsClass;
import io.github.dgp_eu.tools.dynamic.database.DatabaseSpecificSqLiteClass;
import org.jspecify.annotations.NonNull;

/**
 * Handling Software releases logic
 */
public final class EnvironmentSoftwareReleasesSubClass {

    // Private constructor to prevent instantiation
    private EnvironmentSoftwareReleasesSubClass() {
        // intentional empty
    }

    /**
     * expose Software Release details from internal DB
     * @return List software releases details
     */
    public static @NonNull List<Properties> consolidateSoftwareReleases() {
        final List<Properties> softwareReleases = new ArrayList<>();
        final List<Properties> resultReleases = getSoftwareReleasesFromDatabase();
        if (!resultReleases.isEmpty()) {
            resultReleases.forEach(recordProperties -> {
                final Properties newProperties = new Properties();
                newProperties.put("Organization",
                        recordProperties.get("OrganizationName")
                        + "<div style=\"text-align:right;\">["
                        + recordProperties.get("OrganizationId")
                        + "]</div>");
                newProperties.put("Product",
                        "<a href=\""
                        + recordProperties.get("Releases")
                        + "\" target=\"_blank\"><span style=\"float:left;\">"
                        + recordProperties.get("ProductName")
                        + "<br/>["
                        + recordProperties.get("ProductId")
                        + "]</span><span style=\"float:right;text-align:right;\">"
                        + recordProperties.get("BranchName")
                        + "<br/>["
                        + recordProperties.get("BranchId")
                        + "]</span></a>");
                newProperties.put("Version",
                        recordProperties.get("Latest release version")
                        + "<div style=\"text-align:right;\">["
                        + recordProperties.get("VersionId")
                        + "]</div>");
                newProperties.put("Date",
                        recordProperties.get("Latest release date")
                        + "<br>==> "
                        + recordProperties.get("Latest release aging full").toString());
                newProperties.put("Files",
                        recordProperties.get("File Kit Name")
                        + " ["
                        + recordProperties.get("File Kit Id")
                        + "]<br/>==> "
                        + recordProperties.get("File Installed Name")
                        + " ["
                        + recordProperties.get("File Installed Id")
                        + "]");
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
    private static @NonNull String establishRowStyle(final @NonNull String agingDays) {
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
    private static @NonNull List<Properties> getSoftwareReleasesFromDatabase() {
        List<Properties> resultReleases = new ArrayList<>();
        try (Connection objConnection = DatabaseSpecificSqLiteClass.getSqLiteConnection();
             Statement objStatement = DatabaseOperationsClass.ConnectivitySubClass.createSqlStatement(ConfigurationClass.STR_SQLITE, objConnection)) {
            final String queryToUse = DatabaseOperationsClass.getPreDefinedQuery(ConfigurationClass.STR_SQLITE, "ReleasesListProductBranches");
            final Properties rsProperties = DatabaseOperationsClass.packageResultSetProperties(WebClass.STR_SOFT_RELEASES, queryToUse);
            resultReleases = DatabaseOperationsClass.ResultSettingSubClass.getResultSetStandardized(objStatement, rsProperties, new Properties());
        } catch (SQLException e) {
            final String strFeedbackErr = String.format("%s connection has failed %s", ConfigurationClass.STR_SQLITE, e.getLocalizedMessage());
            LogExposureClass.LOGGER.debug(strFeedbackErr);
        }
        return resultReleases;
    }

}