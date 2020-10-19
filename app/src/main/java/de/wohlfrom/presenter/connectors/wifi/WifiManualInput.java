/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Presenter. Android Client to remote control a presentation.          *
 *  Copyright (C) 2019 Felix Wohlfrom                                    *
 *                                                                       *
 *  This program is free software: you can redistribute it and/or modify *
 *  it under the terms of the GNU General Public License as published by *
 *  the Free Software Foundation, either version 3 of the License, or    *
 *  (at your option) any later version.                                  *
 *                                                                       *
 *  This program is distributed in the hope that it will be useful,      *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 *  GNU General Public License for more details.                         *
 *                                                                       *
 *  You should have received a copy of the GNU General Public License    *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package de.wohlfrom.presenter.connectors.wifi;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import de.wohlfrom.presenter.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceSelector.DeviceListResultListener} interface
 * to handle interaction events.
 * Use the {@link WifiManualInput#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WifiManualInput extends Fragment {
    private DeviceSelector.DeviceListResultListener mListener;
    
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WifiManualInput.
     */
    public static WifiManualInput newInstance() {
        return new WifiManualInput();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wifi_manual_input, container, false);
    }

    // Required for backwards compatibility
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        initListener(activity);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        initListener(context);
    }

    /**
     * Initializes the device list result listener.
     *
     * @param context The context that should be used as listener. Needs to implement
     *                {@link DeviceSelector.DeviceListResultListener}
     */
    private void initListener(Context context) {
        try {
            mListener = (DeviceSelector.DeviceListResultListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement DeviceListResultListener");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        Button connectToIp = getActivity().findViewById(R.id.connect);
        connectToIp.setOnClickListener(view -> {
            String hostname = 
                    ((TextView) getActivity().findViewById(R.id.wifi_name)).getText().toString();
            String address =
                    ((TextView) getActivity().findViewById(R.id.ip_address)).getText().toString();
            if (hostname.isEmpty()) {
                hostname = address;
            }
            
            if (address.isEmpty()) {
                Toast.makeText(
                        this.getActivity(), R.string.manual_config_no_ip, Toast.LENGTH_LONG).show();
                return;
            }
            
            mListener.onDeviceSelected(hostname, address);
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
