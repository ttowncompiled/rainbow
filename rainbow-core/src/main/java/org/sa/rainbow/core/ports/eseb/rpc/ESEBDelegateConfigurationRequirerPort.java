package org.sa.rainbow.core.ports.eseb.rpc;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.sa.rainbow.core.gauges.GaugeInstanceDescription;
import org.sa.rainbow.core.models.EffectorDescription.EffectorAttributes;
import org.sa.rainbow.core.models.ProbeDescription.ProbeAttributes;
import org.sa.rainbow.core.ports.eseb.ESEBProvider;
import org.sa.rainbow.core.ports.eseb.ESEBRPCConnector;

import edu.cmu.cs.able.eseb.participant.ParticipantException;

public class ESEBDelegateConfigurationRequirerPort implements IESEBDelegateConfigurationPort {

    private ESEBRPCConnector               m_connectionRole;
    private IESEBDelegateConfigurationPort m_stub;

    public ESEBDelegateConfigurationRequirerPort (String delegateID) throws IOException, ParticipantException {
        // All these messages go on the HEALTH channel. Port is on the master
        m_connectionRole = new ESEBRPCConnector (ESEBProvider.getESEBClientHost (), ESEBProvider.getESEBClientPort (),
                delegateID);
        m_stub = m_connectionRole.createRemoteStub (IESEBDelegateConfigurationPort.class, delegateID
                + IESEBDelegateConfigurationPort.class.getSimpleName ());

    }

    @Override
    public void sendConfigurationInformation (Properties props,
            List<ProbeAttributes> probes,
            List<EffectorAttributes> effectors, List<GaugeInstanceDescription> gauges) {
        m_stub.sendConfigurationInformation (props, probes, effectors, gauges);
    }

}