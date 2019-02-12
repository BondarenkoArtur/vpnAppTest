package com.inverita.vpnapptask.utils;

/**
 * This Listener is needed to handle all events from InternetTester.
 */
public interface TesterListener {
    /**
     * This method is called when DNS and HTTPS mainTest started.
     */
    void onTestStarted();

    /**
     * This method is called when tested address changed.
     * @param address New tested address.
     */
    void onAddressChanged(String address);

    /**
     * This method is called when DNS mainTest is finished.
     * @param dnsResult Returns is DNS mainTest successfully finished or not.
     */
    void onDNSResultReceived(boolean dnsResult);

    /**
     * This method is called when HTTPS mainTest is finished.
     * @param httpsResult Returns is HTTPS mainTest successfully finished or not.
     */
    void onHttpsResultReceived(boolean httpsResult);

    /**
     * This method is called when external IP address received.
     * @param address Your current external IP address.
     */
    void onExternalIPReceived(String address);
}
