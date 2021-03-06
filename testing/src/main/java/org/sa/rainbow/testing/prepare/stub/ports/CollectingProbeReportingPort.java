package org.sa.rainbow.testing.prepare.stub.ports;

import org.sa.rainbow.core.ports.IProbeReportPort;
import org.sa.rainbow.translator.probes.IProbeIdentifier;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * This is the IProbeReportPort that stores all reported data from a probe.
 */
public class CollectingProbeReportingPort implements IProbeReportPort {
    private BlockingDeque<String> reportedData = new LinkedBlockingDeque<>();

    /**
     * Wait for the next output from the probe.
     *
     * @return the next output as a string
     */
    public String takeOutput() throws InterruptedException {
        return reportedData.take();
    }

    /**
     * Wait for the next output from the probe, with timeout.
     *
     * @param milliseconds timeout in millisecond
     * @return the next output as a string
     */
    public String takeOutput(long milliseconds) throws InterruptedException {
        return reportedData.poll(milliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void reportData(IProbeIdentifier probe, String data) {
        try {
            reportedData.put(data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Should be called when this port is no longer required. Implementors should dispose of all resources.
     */
    @Override
    public void dispose() {

    }
}
