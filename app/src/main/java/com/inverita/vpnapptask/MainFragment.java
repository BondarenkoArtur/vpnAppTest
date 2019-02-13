package com.inverita.vpnapptask;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.inverita.vpnapptask.utils.NetworkUtils;
import com.inverita.vpnapptask.utils.PopupHelper;
import com.inverita.vpnapptask.utils.TesterListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

/**
 * This main fragment that used by Main Activity.
 */
public class MainFragment extends Fragment implements View.OnClickListener, Handler.Callback {

    private static final int MSG_UPDATE_STATE = 0;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private PopupHelper mPopupHelper;
    private NetworkUtils mNetworkUtils;

    private IOpenVPNAPIService mService;
    private Handler mHandler;
    private TextView mStatus;
    private Button mStart;
    private View mTesterContainer;
    private TextView mAddress;
    private ImageView mDNSIndicator;
    private ImageView mHttpsIndicator;
    private View mCurrentIPContainer;
    private TextView mCurrentIPAddress;

    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        @Override
        public void newStatus(final String uuid, final String state,
                              final String message, final String level) {
            final Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();
        }
    };

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            mService = IOpenVPNAPIService.Stub.asInterface(service);
            try {
                final Intent intent = mService.prepare(getActivity().getPackageName());
                if (intent != null) {
                    startActivityForResult(intent, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(final ComponentName className) {
            mService = null;
        }
    };

    private void bindService() {
        final Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("de.blinkt.openvpn");
        getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main, container, false);
        view.findViewById(R.id.disconnect).setOnClickListener(this);
        view.findViewById(R.id.getMyIP).setOnClickListener(this);
        view.findViewById(R.id.check_internet_connection).setOnClickListener(this);
        mStart = view.findViewById(R.id.start);
        mStart.setOnClickListener(this);
        mStatus = view.findViewById(R.id.status);
        mTesterContainer = view.findViewById(R.id.internet_tester_container);
        mAddress = view.findViewById(R.id.internet_tester_address);
        mDNSIndicator = view.findViewById(R.id.internet_tester_dns);
        mHttpsIndicator = view.findViewById(R.id.internet_tester_https);
        mCurrentIPContainer = view.findViewById(R.id.current_ip_container);
        mCurrentIPAddress = view.findViewById(R.id.current_ip_address);
        return view;
    }

    @Override
    public void onAttach(final Activity activity) {
        mPopupHelper = new PopupHelper(activity);
        mNetworkUtils = new NetworkUtils(activity, new MainTesterListener());
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mPopupHelper = null;
        mNetworkUtils = null;
        super.onDetach();
    }

    private void startVPNConnection() {
        try {
            final InputStream conf = getActivity().getAssets().open("openvpnconfig.conf");
            final InputStreamReader isr = new InputStreamReader(conf);
            final BufferedReader br = new BufferedReader(isr);
            final StringBuilder config = new StringBuilder();
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                config.append(line).append("\n");
            }
            br.readLine();
            mService.startVPN(config.toString());
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler = new Handler(this);
        bindService();
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.start:
                onStartClicked();
                break;
            case R.id.disconnect:
                onDisconnectClicked();
                break;
            case R.id.getMyIP:
                onGetMyIPClicked();
                break;
            case R.id.check_internet_connection:
                onCheckInternetConnectionClicked();
                break;
            default:
                break;
        }

    }

    private void onStartClicked() {
        try {
            prepareStartProfile();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
            if (mPopupHelper != null) {
                mPopupHelper.openMarketPopup();
            }
        }
    }

    private void onDisconnectClicked() {
        try {
            mService.disconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
            if (mPopupHelper != null) {
                mPopupHelper.openMarketPopup();
            }
        }
    }

    private void onGetMyIPClicked() {
        mCurrentIPAddress.setText(R.string.no_address);
        MainApplication.getInstance().getAppExecutors().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mNetworkUtils != null) {
                        mNetworkUtils.getMyOwnIP();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onCheckInternetConnectionClicked() {
        checkInternetConnection();
    }

    private void checkInternetConnection() {
        MainApplication.getInstance().getAppExecutors().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                if (mNetworkUtils != null) {
                    final boolean result = mNetworkUtils.mainTest();
                    setVPNButtonsEnabled(result);
                }
            }
        });
    }

    private void setVPNButtonsEnabled(final boolean result) {
        MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
            @Override
            public void run() {
                mStart.setEnabled(result);
            }
        });
    }

    private void prepareStartProfile() throws RemoteException {
        final Intent requestPermission = mService.prepareVPNService();
        if (requestPermission == null) {
            onActivityResult(START_PROFILE_EMBEDDED, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestPermission, START_PROFILE_EMBEDDED);
        }
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it. The resultCode will be
     * RESULT_CANCELED if the activity explicitly returned that, didn't return any result,
     * or crashed during its operation.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == START_PROFILE_EMBEDDED) {
                startVPNConnection();
            } else if (requestCode == ICS_OPENVPN_PERMISSION) {
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean handleMessage(final Message msg) {
        if (msg.what == MSG_UPDATE_STATE) {
            mStatus.setText((CharSequence) msg.obj);
            checkInternetConnection();
        }
        return true;
    }

    /**
     * Custom Tester Listener that needed to handle all events from Internet Tester.
     */
    private class MainTesterListener implements TesterListener {

        @Override
        public void onTestStarted() {
            MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    mTesterContainer.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onAddressChanged(final String address) {
            MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    mAddress.setText(address);
                    mDNSIndicator.setImageResource(android.R.drawable.presence_away);
                    mHttpsIndicator.setImageResource(android.R.drawable.presence_away);
                }
            });
        }

        @Override
        public void onDNSResultReceived(final boolean dnsResult) {
            MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    mDNSIndicator.setImageResource(dnsResult
                        ? android.R.drawable.presence_online
                        : android.R.drawable.presence_busy);
                }
            });
        }

        @Override
        public void onHttpsResultReceived(final boolean httpsResult) {
            MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    mHttpsIndicator.setImageResource(httpsResult
                        ? android.R.drawable.presence_online
                        : android.R.drawable.presence_busy);
                }
            });

        }

        @Override
        public void onExternalIPReceived(final String address) {
            MainApplication.getInstance().getAppExecutors().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    mCurrentIPContainer.setVisibility(View.VISIBLE);
                    mCurrentIPAddress.setText(address);
                }
            });
        }
    }
}
