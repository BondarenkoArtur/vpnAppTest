package com.inverita.vpnapptask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

public class MainFragment extends Fragment implements View.OnClickListener, Handler.Callback {

    public static final String OPENVPN_PACKAGE_NAME = "de.blinkt.openvpn";
    private TextView mStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        v.findViewById(R.id.disconnect).setOnClickListener(this);
        v.findViewById(R.id.getMyIP).setOnClickListener(this);
        v.findViewById(R.id.start).setOnClickListener(this);
        mStatus = v.findViewById(R.id.status);
        return v;
    }

    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_MY_IP = 1;
    private static final int START_PROFILE_EMBEDDED = 2;
    private static final int ICS_OPENVPN_PERMISSION = 7;

    protected IOpenVPNAPIService mService = null;
    private Handler mHandler;

    private void startEmbeddedProfile() {
        try {
            InputStream conf = getActivity().getAssets().open("openvpnconfig.conf");
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder config = new StringBuilder();
            String line;
            while (true) {
                line = br.readLine();
                if (line == null)
                    break;
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


    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */

        @Override
        public void newStatus(String uuid, String state, String message, String level) {
            Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
            msg.sendToTarget();
        }

    };


    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.

            mService = IOpenVPNAPIService.Stub.asInterface(service);

            try {
                // Request permission to use the API
                Intent i = mService.prepare(getActivity().getPackageName());
                if (i != null) {
                    startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                } else {
                    onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

        }
    };

    private void bindService() {

        Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
        icsopenvpnService.setPackage("de.blinkt.openvpn");

        getActivity().bindService(icsopenvpnService, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        getActivity().unbindService(mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                try {
                    prepareStartProfile();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    openMarketPopup();
                }
                break;
            case R.id.disconnect:
                try {
                    mService.disconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    openMarketPopup();
                }
                break;
            case R.id.getMyIP:
                // Socket handling is not allowed on main thread
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String myip = getMyOwnIP();
                            Message msg = Message.obtain(mHandler, MSG_UPDATE_MY_IP, myip);
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

    private void openMarketPopup() {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(getString(R.string.openvpn_needed));
        alertDialog.setMessage(getString(R.string.openvpn_needed_text));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.download),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    openOpenVpnMarket();
                    dialog.dismiss();
                }
            });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    private void openOpenVpnMarket() {
        final String appPackageName = OPENVPN_PACKAGE_NAME;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    private void prepareStartProfile() throws RemoteException {
        Intent requestPermission = mService.prepareVPNService();
        if (requestPermission == null) {
            onActivityResult(START_PROFILE_EMBEDDED, Activity.RESULT_OK, null);
        } else {
            // Have to call an external Activity since services cannot used onActivityResult
            startActivityForResult(requestPermission, START_PROFILE_EMBEDDED);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == START_PROFILE_EMBEDDED)
                startEmbeddedProfile();
            if (requestCode == ICS_OPENVPN_PERMISSION) {
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    String getMyOwnIP() throws IOException,
        IllegalArgumentException {
        StringBuilder resp = new StringBuilder();
        Socket client = new Socket();
        // Setting Keep Alive forces creation of the underlying socket, otherwise getFD returns -1
        client.setKeepAlive(true);

        client.connect(new InetSocketAddress("ifconfig.co", 80), 20000);
        client.shutdownOutput();
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while (true) {
            String line = in.readLine();
            if (line == null)
                return resp.toString();
            resp.append(line);
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_UPDATE_STATE || msg.what == MSG_UPDATE_MY_IP) {
            mStatus.setText((CharSequence) msg.obj);
        }
        return true;
    }
}