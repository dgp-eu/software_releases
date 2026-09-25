package io.github.dgp_eu.software_releases.environment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.dgp_eu.tools.core.BasicStructuresClass;
import io.github.dgp_eu.tools.core.ConfigurationClass;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.DisplayInfo;
import oshi.hardware.Firmware;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.NetworkIF.IfOperStatus;
import oshi.software.os.NetworkParams;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.PlatformEnum;

/**
 * Hardware class
 */
public final class EnvironmentHardwareClass {

    /**
     * Constructor
     */
    private EnvironmentHardwareClass() {
        // intentionally blank
    }

    /**
     * Display details
     *
     * @param crtDisplay current Display object
     * @return String
     */
    private static Map<String, Object> digestSingleDisplayDetails(final Display crtDisplay) {
        final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>();
        final String[] arrayDetails = crtDisplay.toString().replaceAll("[^a-zA-Z0-9\\s]", "").split("\n");
        for (final String crtLine : arrayDetails) {
            final String strSlimLine = crtLine.trim();
            if (strSlimLine.endsWith(" in") && strSlimLine.contains(" cm ")) {
                final int intCmPos = strSlimLine.indexOf(" cm ");
                arrayAttributes.put(ConfigurationClass.STR_PHYSC_DIM + " [in]", strSlimLine.substring(0, intCmPos));
                final int intInPos = strSlimLine.indexOf(" in");
                arrayAttributes.put(ConfigurationClass.STR_PHYSC_DIM + " [cm]", strSlimLine.substring(intCmPos + 4, intInPos));
            }
            if (strSlimLine.startsWith(ConfigurationClass.STR_MONITOR_NAME)) {
                arrayAttributes.put(ConfigurationClass.STR_MONITOR_NAME, strSlimLine.replace(ConfigurationClass.STR_MONITOR_NAME + " ", ""));
            }
            if (strSlimLine.startsWith(ConfigurationClass.STR_PRFRD_TM_CLCK)) {
                final int intClockLen = ConfigurationClass.STR_PRFRD_TM_CLCK.length();
                final int intPixelPos = strSlimLine.indexOf(ConfigurationClass.STR_ACTV_PXLS);
                arrayAttributes.put(ConfigurationClass.STR_PRFRD_TM_CLCK, strSlimLine.substring(intClockLen, intPixelPos).trim());
                arrayAttributes.put(ConfigurationClass.STR_ACTV_PXLS, strSlimLine.substring(intPixelPos)
                        .replace(ConfigurationClass.STR_ACTV_PXLS + " ", "").trim());
            }
            if (strSlimLine.startsWith(ConfigurationClass.STR_RANGE_LMTS)) {
                arrayAttributes.put(ConfigurationClass.STR_RANGE_LMTS, strSlimLine.replace(ConfigurationClass.STR_RANGE_LMTS + " ", ""));
            }
            if (strSlimLine.startsWith(ConfigurationClass.STR_SRL_NUM)) {
                arrayAttributes.put(ConfigurationClass.STR_SRL_NUM, strSlimLine.replace(ConfigurationClass.STR_SRL_NUM + " ", ""));
            }
        }
        return arrayAttributes;
    }

    /**
     * Environment details gathered
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutCentralProcessorUnit() {
        final CentralProcessor processor = OshiUsageClass.HardwareSubClass.getOshiProcessor();
        final CentralProcessor.ProcessorIdentifier procIdentif = OshiUsageClass.HardwareSubClass.getOshiProcessorIdentifier();
        final List<String> featureFlags = processor.getFeatureFlags().stream()
                .sorted()
                .toList();
        return Map.of(
                "CPU Identifier", procIdentif.getIdentifier(),
                "Family", procIdentif.getFamily(),
                "Feature Flags", featureFlags.toString().replace("[", "[\"").replace(", ", "\", \"").replace("]", "\"]"),
                "Logical Processors", processor.getLogicalProcessorCount(),
                "Maximum Frequency", FormatUtil.formatHertz(processor.getMaxFreq()),
                ConfigurationClass.STR_MODEL, procIdentif.getModel(),
                ConfigurationClass.STR_NAME, procIdentif.getName(),
                "Processor ID", procIdentif.getProcessorID(),
                "Physical Processors", processor.getPhysicalProcessorCount(),
                ConfigurationClass.STR_VENDOR, procIdentif.getVendor());
    }

    /**
     * GPU info
     *
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutGraphicCards() {
        final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>();
        final List<GraphicsCard> graphicCards = OshiUsageClass.HardwareSubClass.getOshiGraphicsCards();
        for (final GraphicsCard  graphicCard : graphicCards) {
            final String strIdentifier = "Video Card ID#" + BasicStructuresClass.StringTransformationSubClass.computeStringSignature(graphicCard.getName()) + " ";
            arrayAttributes.putAll(Map.of(
                    strIdentifier + ConfigurationClass.STR_NAME, graphicCard.getName(),
                    strIdentifier + ConfigurationClass.STR_VENDOR, graphicCard.getVendor(),
                    strIdentifier + "VRAM", FormatUtil.formatBytes(graphicCard.getVRam()),
                    strIdentifier + "Driver Version", graphicCard.getVersionInfo()
            ));
        }
        return arrayAttributes;
    }

    /**
     * Monitors info as Map
     *
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutMonitor() {
        final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>();
        final List<Display> displays = OshiUsageClass.HardwareSubClass.getOshiMonitor();
        for (final Display crtDisplay : displays) {// The EDID is the "fingerprint" of the monitor hardware
            final DisplayInfo crtDisplayInfo = OshiUsageClass.HardwareSubClass.getDisplayInfo(crtDisplay);
            final String uniqueId = "Monitor #" + crtDisplayInfo.getEdid();
            final Map<String, Object> crtMonitor = digestSingleDisplayDetails(crtDisplay);
            crtMonitor.forEach((strKey, strValue) -> arrayAttributes.put(uniqueId + " " + strKey, strValue));
        }
        return arrayAttributes;
    }

    /**
     * Network details gathered
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutNetwork() {
        final NetworkParams networkParams = OshiUsageClass.SoftwareSubClass.getOshiNetworkParameters();
        return Map.of(
                //"DNS Servers", String.join(", ", networkParams.getDnsServers()),
                "Domain Name", networkParams.getDomainName(),
                "Host Name", networkParams.getHostName(),
                "IPv4 Gateway", networkParams.getIpv4DefaultGateway(),
                "IPv6 Gateway", networkParams.getIpv6DefaultGateway());
    }

    /**
     * Sensors Information
     *
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutNetworkInterfaces() {
        final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>();
        final List<NetworkIF> networkIFs = OshiUsageClass.HardwareSubClass.getOshiNetworkInterfaces();
        for (final NetworkIF net : networkIFs) {
            net.updateAttributes(); // Refresh interface stats
            final IfOperStatus status = net.getIfOperStatus();
            final String[] addressIPV4 =  net.getIPv4addr();
            final String[] addressIPV6 =  net.getIPv6addr();
            boolean expose = false;
            if (status == NetworkIF.IfOperStatus.UP
                    && (addressIPV4.length != 0
                            || addressIPV6.length != 0)) {
                expose = true;
            }
            if (expose) {
                final String strIdentifier = "Memory MAC#" + net.getMacaddr() + " ";
                arrayAttributes.putAll(Map.of(
                        strIdentifier + ConfigurationClass.STR_NAME, net.getName(),
                        strIdentifier + "Display Name", net.getDisplayName(),
                        strIdentifier + "IPv4", String.join(", ", addressIPV4),
                        strIdentifier + "IPv6", String.join(", ", addressIPV6),
                        strIdentifier + "MTU", net.getMTU(),
                        strIdentifier + "NDIS Physical Medium Type", OshiUsageClass.getNetworkPhysicalMediumType(net.getNdisPhysicalMediumType()),
                        strIdentifier + "Status", status,
                        strIdentifier + "Speed", FormatUtil.formatBytes(net.getSpeed())));
            }
        }
        return arrayAttributes;
    }

    /**
     * Operating System details gathered
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutOperatingSystem() {
        final OperatingSystem.OSVersionInfo version = OshiUsageClass.SoftwareSubClass.getOshiVersionInfo();
        return Map.of(
                "Architecture", System.getProperty("os.arch", EnvironmentCapturingAssembleClass.STR_INSTEAD_NULL),
                "Build", version.getBuildNumber(),
                "Code", version.getCodeName(),
                "Family", OshiUsageClass.SoftwareSubClass.getOshiFamily(),
                ConfigurationClass.STR_MANUFACTURER, OshiUsageClass.SoftwareSubClass.getOshiManufacturer(),
                ConfigurationClass.STR_NAME, System.getProperty("os.name", EnvironmentCapturingAssembleClass.STR_INSTEAD_NULL),
                "Platform", PlatformEnum.getCurrentPlatform().toString(),
                ConfigurationClass.STR_VERSION, version.getVersion());
    }

    /**
     * Capturing RAM information
     *
     * @return Map
     */
    public static Map<String, Object> getDetailsAboutRandomAccessMemory() {
        final GlobalMemory globalMemory = OshiUsageClass.HardwareSubClass.getOshiMemory();
        final VirtualMemory virtualMemory = OshiUsageClass.HardwareSubClass.getOshiVirtualMemory();
        final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>(Map.of(
                "Available", FormatUtil.formatBytes(globalMemory.getAvailable()),
                "Page Size", FormatUtil.formatBytes(globalMemory.getPageSize()),
                "Total", FormatUtil.formatBytes(globalMemory.getTotal()),
                "Virtual Memory Swap In Use", FormatUtil.formatBytes(virtualMemory.getVirtualInUse()),
                "Virtual Memory Swap Used", FormatUtil.formatBytes(virtualMemory.getSwapUsed()),
                "Virtual Memory Swap Total", FormatUtil.formatBytes(virtualMemory.getSwapTotal())));
        final List<PhysicalMemory> physicalMemories = globalMemory.getPhysicalMemory();
        for (final PhysicalMemory physicalMemory : physicalMemories) {
            final String strIdentifier = "Bank SN#" + physicalMemory.getSerialNumber() + " ";
            arrayAttributes.putAll(Map.of(
                    strIdentifier + "Bank/Slot Label", physicalMemory.getBankLabel(),
                    strIdentifier + "Capacity", FormatUtil.formatBytes(physicalMemory.getCapacity()),
                    strIdentifier + "Clock Speed", FormatUtil.formatHertz(physicalMemory.getClockSpeed()),
                    strIdentifier + ConfigurationClass.STR_MANUFACTURER, physicalMemory.getManufacturer(),
                    strIdentifier + "Type", physicalMemory.getMemoryType(),
                    strIdentifier + "Part Number", physicalMemory.getPartNumber().trim()));
        }
        return arrayAttributes;
    }

    /**
     * Hardware class
     */
    public static final class MotherboardAndSystemSubClass {

        /**
         * Constructor
         */
        private MotherboardAndSystemSubClass() {
            // intentionally blank
        }

        /**
         * capture Computer System parameters into Map
         * @return Map
         */
        private static Map<String, Object> getDetailsAboutComputerSystemIntoMap() {
            final ComputerSystem computerSystem = OshiUsageClass.HardwareSubClass.getOshiComputerSystem();
            return Map.of(
                    ConfigurationClass.STR_SYSTEM + " " + ConfigurationClass.STR_MANUFACTURER, computerSystem.getManufacturer(),
                    ConfigurationClass.STR_SYSTEM + " " + ConfigurationClass.STR_MODEL, computerSystem.getModel(),
                    ConfigurationClass.STR_SYSTEM + " " + ConfigurationClass.STR_SRL_NUM, computerSystem.getSerialNumber());
        }

        /**
         * capture Firmware parameters into Map
         * @return Map
         */
        private static Map<String, Object> getDetailsAboutFirmwareIntoMap() {
            final Firmware firmware = OshiUsageClass.HardwareSubClass.getOshiFirmware();
            return Map.of(
                    ConfigurationClass.STR_FIRMWARE + " " + ConfigurationClass.STR_MANUFACTURER, firmware.getManufacturer(),
                    ConfigurationClass.STR_FIRMWARE + " " + ConfigurationClass.STR_NAME, firmware.getName(),
                    ConfigurationClass.STR_FIRMWARE + " " + ConfigurationClass.STR_DESCRIPTION, firmware.getDescription(),
                    ConfigurationClass.STR_FIRMWARE + " " + ConfigurationClass.STR_VERSION, firmware.getVersion(),
                    ConfigurationClass.STR_FIRMWARE + " " + "Release Date", firmware.getReleaseDate() == null ? "unknown" : firmware.getReleaseDate());
        }

        /**
         * Main-board details gathered
         * @return Map
         */
        public static Map<String, Object> getDetailsAboutMainboard() {
            final Map<String, Object> arrayAttributes = new ConcurrentHashMap<>(getDetailsAboutMotherboardIntoMap());
            arrayAttributes.putAll(getDetailsAboutFirmwareIntoMap());
            arrayAttributes.putAll(getDetailsAboutComputerSystemIntoMap());
            return arrayAttributes;
        }

        /**
         * capture Motherboard parameters into Map
         * @return Map
         */
        private static Map<String, Object> getDetailsAboutMotherboardIntoMap() {
            final Baseboard baseboard = OshiUsageClass.HardwareSubClass.getOshiMotherboard();
            return Map.of(
                    ConfigurationClass.STR_MAINBOARD + " " + ConfigurationClass.STR_MANUFACTURER, baseboard.getManufacturer(),
                    ConfigurationClass.STR_MAINBOARD + " " + ConfigurationClass.STR_MODEL, baseboard.getModel(),
                    ConfigurationClass.STR_MAINBOARD + " " + ConfigurationClass.STR_VERSION, baseboard.getVersion(),
                    ConfigurationClass.STR_MAINBOARD + " " + ConfigurationClass.STR_SRL_NUM, baseboard.getSerialNumber());
        }
    }

}