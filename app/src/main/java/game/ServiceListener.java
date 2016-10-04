package game;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.net.InetAddress;

public class ServiceListener {
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mService;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    private NsdManager mNsdManager;

    public int port;
    public InetAddress host;

    public boolean resolved = false;

    public ServiceListener(Context context){

        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        mNsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {

            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                if (service.getServiceType().equals("_http._tcp.") && service.getServiceName().startsWith("DiscMultiplayerGame")) {
                    mResolveListener = createResolveListener();
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private NsdManager.ResolveListener createResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mService = serviceInfo;
                if (!resolved) {

                    port = mService.getPort();
                    host = mService.getHost();

                    resolved = true;
                }
            }
        };
    }

    public void tearDown(){
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
         }
        catch(IllegalArgumentException ignored){
        }
    }
}
