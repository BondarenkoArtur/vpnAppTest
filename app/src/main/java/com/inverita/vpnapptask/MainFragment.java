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
import android.widget.TextView;

import com.inverita.vpnapptask.utils.InternetUtils;
import com.inverita.vpnapptask.utils.PopupHelper;

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
    private static final int MSG_UPDATE_MY_IP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int ICS_OPENVPN_PERMISSION = 7;
    private final PopupHelper popupHelper = new PopupHelper(getActivity());
    private final InternetUtils internetUtils = new InternetUtils();

    private IOpenVPNAPIService mService;
    private Handler mHandler;
    private TextView mStatus;

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
        view.findViewById(R.id.start).setOnClickListener(this);
        mStatus = view.findViewById(R.id.status);
        return view;
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
                try {
                    prepareStartProfile();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    popupHelper.openMarketPopup();
                }
                break;
            case R.id.disconnect:
                try {
                    mService.disconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    popupHelper.openMarketPopup();
                }
                break;
            case R.id.getMyIP:
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            final String myip = internetUtils.getMyOwnIP();
                            final Message msg = Message.obtain(mHandler, MSG_UPDATE_MY_IP, myip);
                            msg.sendToTarget();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                break;
            default:
                break;
        }

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
        if (msg.what == MSG_UPDATE_STATE || msg.what == MSG_UPDATE_MY_IP) {
            mStatus.setText((CharSequence) msg.obj);
        }
        return true;
    }
}
