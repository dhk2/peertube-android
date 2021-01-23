/*
 * Copyright 2018 Stefan Schüller <sschueller@techdroid.com>
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.anticlimacticteleservices.peertube.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.tooltip.TooltipDrawable;

import net.anticlimacticteleservices.peertube.R;
import net.anticlimacticteleservices.peertube.activity.SearchServerActivity;
import net.anticlimacticteleservices.peertube.activity.ServerAddressBookActivity;
import net.anticlimacticteleservices.peertube.database.Server;
import net.anticlimacticteleservices.peertube.helper.APIUrlHelper;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class AddServerFragment extends Fragment {

    public static final String TAG = "AddServerFragment";
    public static final Integer PICK_SERVER = 1;

    private OnFragmentInteractionListener mListener;
    private Server oldServer=null;
    private View mView;

    public AddServerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment

        mView = inflater.inflate(R.layout.fragment_add_server, container, false);

        // bind button click
        Button addServerButton = mView.findViewById(R.id.addServerButton);
        addServerButton.setOnClickListener(view -> {

            Activity act = getActivity();

            boolean formValid = true;

            // close keyboard
            try {
                assert act != null;
                InputMethodManager inputManager = (InputMethodManager)
                        act.getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(Objects.requireNonNull(act.getCurrentFocus()).getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {

            }

            EditText selectedLabel = mView.findViewById(R.id.serverLabel);
            if ( TextUtils.isEmpty(selectedLabel.getText())){
                selectedLabel.setError( act.getString(R.string.server_book_label_is_required ));
                Toast.makeText(act, R.string.invalid_url, Toast.LENGTH_LONG).show();
                formValid = false;
            } else {
                oldServer.setServerName(selectedLabel.getText().toString());
            }

            // validate url
            EditText selectedUrl = mView.findViewById(R.id.serverUrl);
            String serverUrl = APIUrlHelper.cleanServerUrl(selectedUrl.getText().toString());
            selectedUrl.setText(serverUrl);
            oldServer.setServerHost(serverUrl);

            if (!Patterns.WEB_URL.matcher(serverUrl).matches()) {
                selectedUrl.setError( act.getString(R.string.server_book_valid_url_is_required ) );
                Toast.makeText(act, R.string.invalid_url, Toast.LENGTH_LONG).show();
                formValid = false;
            }
            EditText selectedUserName =mView.findViewById(R.id.serverUsername);
            oldServer.setUsername(String.valueOf(selectedUserName.getText()));

            EditText selectedUserPassword =mView.findViewById(R.id.serverPassword);
            oldServer.setPassword(String.valueOf(selectedUserPassword.getText()));
            Log.e("wtf",oldServer.toString());
            if (formValid) {
                if (act instanceof ServerAddressBookActivity) {
                    if (oldServer !=null) {
                        ((ServerAddressBookActivity) act).addServer(oldServer);
                    } else {
                        ((ServerAddressBookActivity) act).addServer(mView);
                    }
                }
            }

        });

//        Button testServerButton = mView.findViewById(R.id.testServerButton);
//        testServerButton.setOnClickListener(view -> {
//            Activity act = getActivity();
//            if (act instanceof ServerAddressBookActivity) {
//                ((ServerAddressBookActivity) act).testServer();
//            }
//        });

        Button pickServerUrl = mView.findViewById(R.id.pickServerUrl);
        pickServerUrl.setOnClickListener(view -> {
            Intent intentServer = new Intent(getActivity(), SearchServerActivity.class);
            this.startActivityForResult(intentServer, PICK_SERVER);
        });
        oldServer=TvFragment.getEditServer();
        if (null != oldServer){
            Log.e("wtf",oldServer.toString());
            EditText serverUrl = mView.findViewById(R.id.serverUrl);
            serverUrl.setText(oldServer.getServerHost());
            EditText serverLabel = mView.findViewById(R.id.serverLabel);
            serverLabel.setText(oldServer.getServerName());
            EditText serverUsername = mView.findViewById(R.id.serverUsername);
            serverUsername.setText(oldServer.getUsername());
            EditText serverPassword = mView.findViewById(R.id.serverPassword);
            serverPassword.setText(oldServer.getPassword());

            addServerButton.setText("Save");
        }


        return mView;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SERVER) {
            if(resultCode == RESULT_OK) {

                String serverUrlTest = data.getStringExtra("serverUrl");
                //Log.d(TAG, "serverUrl " + serverUrlTest);
                EditText serverUrl = mView.findViewById(R.id.serverUrl);
                serverUrl.setText(serverUrlTest);

                EditText serverLabel = mView.findViewById(R.id.serverLabel);
                if ("".equals(serverLabel.getText().toString())) {
                    serverLabel.setText(data.getStringExtra("serverName"));
                }

            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
