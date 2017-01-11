package mesosphere.marathon.client;

import com.netflix.client.ClientFactory;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.ribbon.LBClient;
import feign.ribbon.LBClientFactory;
import feign.ribbon.RibbonClient;
import mesosphere.marathon.client.utils.ModelUtils;
import org.springframework.util.StringUtils;

/**
 * Created by aleksandr on 11.01.17.
 */
public class RibbonMarathonClient extends MarathonClient {
    static final String MARATHON_SERVICE_ID_RIBBON_PREFIX = "marathon.ribbon.";
    static final int DEFAULT_MARATHON_SERVICE_RETRY_COUNT = 3;
    static final int DEFAULT_MARATHON_SERVICE_CONNECTION_TIMEOUT = 1000;
    static final int DEFAULT_MARATHON_SERVICE_READ_TIMEOUT = 5000;

    public static class Builder {
        private String baseEndpoint;

        private String listOfServers;
        private String username;
        private String password;

        private int maxRetryCount = DEFAULT_MARATHON_SERVICE_RETRY_COUNT;
        private int connectionTimeout = DEFAULT_MARATHON_SERVICE_CONNECTION_TIMEOUT;
        private int readTimeout = DEFAULT_MARATHON_SERVICE_READ_TIMEOUT;

        public Builder(String baseEndpoint) {
            this.baseEndpoint = baseEndpoint;
        }

        public Builder withListOfServers(String listOfServers) {
            this.listOfServers = listOfServers;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Marathon build() {
            if (null == listOfServers) {
                if (StringUtils.isEmpty(username)) {
                    return getInstance(baseEndpoint);
                } else {
                    return getInstanceWithBasicAuth(baseEndpoint, username, password);
                }
            } else {
                setMarathonRibbonProperty("listOfServers", listOfServers);
                setMarathonRibbonProperty("OkToRetryOnAllOperations", Boolean.TRUE.toString());
                setMarathonRibbonProperty("MaxAutoRetriesNextServer", maxRetryCount);
                setMarathonRibbonProperty("ConnectTimeout", connectionTimeout);
                setMarathonRibbonProperty("ReadTimeout", readTimeout);

                Feign.Builder b = Feign.builder()
                        .client(RibbonClient.builder().lbClientFactory(new MarathonLBClientFactory()).build())
                        .encoder(new GsonEncoder(ModelUtils.GSON))
                        .decoder(new GsonDecoder(ModelUtils.GSON))
                        .errorDecoder(new MarathonErrorDecoder());

                if (!StringUtils.isEmpty(username)) {
                    new BasicAuthRequestInterceptor(username,password);
                }

                b.requestInterceptor(new MarathonHeadersInterceptor());
                return b.target(Marathon.class, baseEndpoint);
            }
        }

        void setMarathonRibbonProperty(String suffix, Object value) {
            ConfigurationManager.getConfigInstance().setProperty(MARATHON_SERVICE_ID_RIBBON_PREFIX + suffix, value);
        }
    }

    public static class MarathonLBClientFactory implements LBClientFactory {

        @Override
        public LBClient create(String clientName) {
            LBClient client = new LBClientFactory.Default().create(clientName);
            IClientConfig config = ClientFactory.getNamedConfig(clientName);
            client.setRetryHandler(new DefaultLoadBalancerRetryHandler(config));
            return client;
        }
    }
}