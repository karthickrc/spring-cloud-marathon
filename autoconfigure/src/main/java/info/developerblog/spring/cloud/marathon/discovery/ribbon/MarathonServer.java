package info.developerblog.spring.cloud.marathon.discovery.ribbon;

import com.netflix.loadbalancer.Server;
import mesosphere.marathon.client.model.v2.HealthCheckResults;

import java.util.Collection;

/**
 * Created by aleksandr on 07.07.16.
 */
public class MarathonServer extends Server {

    private Collection<HealthCheckResults> healthChecks;

    public MarathonServer(String host, int port, Collection<HealthCheckResults> healthChecks) {
        super(host, port);
        this.healthChecks = healthChecks;
    }

    public MarathonServer withZone(String zone) {
        this.setZone(zone);
        return this;
    }

    public boolean isHealthChecksPassing() {
        return healthChecks.parallelStream()
                .allMatch(HealthCheckResults::getAlive);
    }

}
