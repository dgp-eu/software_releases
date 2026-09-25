package io.github.dgp_eu.software_releases.cli;

import java.util.Locale;
import java.util.Properties;

import io.github.dgp_eu.software_releases.RemoteInformationRetrievalClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import io.github.dgp_eu.tools.core.LogExposureClass;
import io.github.dgp_eu.tools.core.RegularExpressionsClass;
import picocli.CommandLine;

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
        final String checksumValue = RemoteInformationRetrievalClass.RequestSubClass.requestHttpFile(strChecksumUrl, ConfigurationClass.STR_CONTENT).getOrDefault(ConfigurationClass.STR_CONTENT, "MISSING").toString().trim().toLowerCase(Locale.ENGLISH);
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