package de.blinkt.openvpn.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * APIVpnProfile used in OpenVPN app that connect by AIDL in our app.
 */
public class APIVpnProfile implements Parcelable {

    /**
     * Main Profile Creator.
     */
    public static final Creator<APIVpnProfile> CREATOR = new Creator<APIVpnProfile>() {
        public APIVpnProfile createFromParcel(final Parcel in) {
            return new APIVpnProfile(in);
        }

        public APIVpnProfile[] newArray(final int size) {
            return new APIVpnProfile[size];
        }
    };

    private final String mUUID;
    private final String mName;
    private final boolean mUserEditable;

    public APIVpnProfile(final Parcel in) {
        mUUID = in.readString();
        mName = in.readString();
        mUserEditable = in.readInt() != 0;
    }

    public APIVpnProfile(final String uuidString, final String name, final boolean userEditable) {
        mUUID = uuidString;
        mName = name;
        mUserEditable = userEditable;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mUUID);
        dest.writeString(mName);
        if (mUserEditable) {
            dest.writeInt(0);
        } else {
            dest.writeInt(1);
        }
    }
}
