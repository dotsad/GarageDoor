package com.petka.garagedoor;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pubnub.api.*;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private final Pubnub pubnub = new Pubnub("pub-c-0aa8c1c5-2486-407f-af09-5df76bca3070", "sub-c-bbf6c2e6-3d2a-11e5-9dc5-02ee2ddab7fe");
    private SwipeRefreshLayout swipeLayout;
    TextView textView = null;
    ImageView imageView = null;
    String statusText = "";
    int garageImageId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.statusBox);
        imageView = (ImageView) this.findViewById(R.id.imageGarage);

        Button btnGarage = (Button) findViewById(R.id.buttonGarage);
        btnGarage.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             pubnub.publish("petkadoor_action", "{'Action':'door'}", pubishCallback);
                                         }
                                     }
        );

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        pubnub.publish("petkadoor_action", "{'Action':'status'}", pubishCallback);
                    }
                }
        );
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        try {
            pubnub.subscribe("petkadoor_status", subscribeCallback);
            //invoke status from the IoT
            pubnub.publish("petkadoor_action", "{'Action':'status'}", pubishCallback);
        } catch (PubnubException e) {

        }
    }

    // ignore publish callbacks
    Callback pubishCallback = new Callback() {
        public void successCallback(String channel, Object message) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    swipeLayout.setRefreshing(false);
                }
            });
        }

        public void errorCallback(String channel, PubnubError error) {
            System.out.println(error.toString());
            statusText = error.toString();
            swipeLayout.setRefreshing(false);
        }
    };

    //show status callback
    Callback subscribeCallback = new Callback() {

        @Override
        public void errorCallback(String channel, PubnubError error) {
            statusText = "SUBSCRIBE : ERROR on channel " + channel + " : " + error.toString();
        }

        @Override
        public void successCallback(String channel, Object message) {
            try {

                JSONObject jsonMessage = new JSONObject(message.toString());

                String action = jsonMessage.getString("Action");
                Boolean status = jsonMessage.getBoolean("Status");
                String time = jsonMessage.getString("Time");

                if (action.equalsIgnoreCase("status")) {

                    if (status) {
                        garageImageId = R.drawable.garage_open;
                        statusText = "Open for : " + time;
                    } else {
                        garageImageId = R.drawable.garage_closed;
                        statusText = "Closed for : " + time;
                    }

                    //statusText = action +" : "+ status;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTextBox();
                }
            });
        }
    };

    private void updateTextBox() {
        imageView.setImageResource(garageImageId);
        textView.setText(statusText);
    }
}
