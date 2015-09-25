package com.mmpud.socketexercise;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String IP_ADDRESS = "139.162.10.146";
    private static final int PORT = 19001;
    private static final String SHA1_SALT = "sha1salt"; // TODO change this

    @Bind(android.R.id.edit) EditText mEditText;
    @Bind(android.R.id.text1) TextView mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(android.R.id.button1) public void sendMessage() {
        String msg = mEditText.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) {
            return;
        }
    }

    @OnClick(android.R.id.button2) public void connectSocket() {
        JSONObject json = new JSONObject();

        String account = "account";
        String password = "password";

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update((password + SHA1_SALT).getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String encryptedPassword = null;
        if (messageDigest != null) {
            encryptedPassword = (new BigInteger(messageDigest.digest())).toString(16);
        }
        mResult.append("\nhash=" + encryptedPassword);
        try {
            json.put("action", "authenticate");
            JSONObject jsonSub = new JSONObject();
            jsonSub.put("username", account);
            jsonSub.put("password", encryptedPassword);
            json.put("request", jsonSub);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SocketClient.startSocketOneShot(IP_ADDRESS, PORT, json, new OnRespondListener() {
            @Override public void onReceived(String jsonStr) {
                mResult.append("\n" + jsonStr);
            }

            @Override public void onError(Exception e) {
                mResult.append("\n [error]" + e.getMessage());
            }
        });
    }

}
