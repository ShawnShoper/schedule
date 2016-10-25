package org.shoper.common.rpc.manager.selector;

import org.shoper.common.rpc.connector.Connector;

import java.util.Collection;

/**
 * Created by ShawnShoper on 2016/10/24.
 */
public class SelectorResult {
    private Connector connector;
    private Collection<Connector> connectors;

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public Collection<Connector> getConnectors() {
        return connectors;
    }

    public void setConnectors(Collection<Connector> connectors) {
        this.connectors = connectors;
    }
}
