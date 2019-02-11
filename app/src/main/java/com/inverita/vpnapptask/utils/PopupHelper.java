package com.inverita.vpnapptask.utils;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.inverita.vpnapptask.R;

/**
 * This class is needed to show popups for case if OpenVPN is not installed.
 */
public class PopupHelper {
    private static final String OPENVPN_PACKAGE_NAME = "de.blinkt.openvpn";
    private final Context context;

    public PopupHelper(final Context context) {
        this.context = context;
    }

    /**
     * Shows popup with information that OpenVPN is needed.
     */
    public void openMarketPopup() {
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getString(R.string.openvpn_needed));
        alertDialog.setMessage(context.getString(R.string.openvpn_needed_text));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.download),
            new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int which) {
                    openOpenVpnMarket();
                    dialog.dismiss();
                }
            });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    private void openOpenVpnMarket() {
        final String appPackageName = OPENVPN_PACKAGE_NAME;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + appPackageName)));
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
