package com.mmpud.socketexercise;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class opens a socket and listen to server's broadcast.
 */
public class SocketClient implements Runnable {

    private final String mAddress;
    private final int mPort;
    private final Thread mClientThread;
    private final Handler mHandler;

    private boolean mRun;

    public SocketClient(String address, int port, OnRespondListener listener) {
        this.mAddress = address;
        this.mPort = port;
        this.mClientThread = new Thread(this);
        this.mHandler = new MyHandler(listener);
    }

    public void startClient() {
        mRun = true;
        mClientThread.start();
    }

    public void stopClient() {
        mRun = false;
        mClientThread.interrupt();
    }

    static class MyHandler extends Handler {

        private final WeakReference<OnRespondListener> mListener;

        public MyHandler(OnRespondListener listener) {
            mListener = new WeakReference<>(listener);
        }

        @Override public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OnRespondListener listener = mListener.get();
            if (listener != null) {
                listener.onReceived((String) msg.obj);
            }
        }

    }

    @Override public void run() {
        Socket socket = null;
        BufferedReader bufferReader = null;
        try {
            socket = new Socket(mAddress, mPort);
            while (true) {
                if (!mRun) {
                    break;
                }
                bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String json;
                while ((json = bufferReader.readLine()) != null) {
                    Message message = Message.obtain();
                    message.obj = json;
                    mHandler.sendMessage(message);
                }
            }
        } catch (IOException e) {
            // interruptIdException is caught too
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
            if (bufferReader != null) {
                try {
                    bufferReader.close();
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    public static void startSocketOneShot(String address, int port, JSONObject json,
        OnRespondListener listener) {
        new SocketOneShotTask(address, port, json, listener).execute();
    }

    /**
     * This is a one shot communication with the server.
     */
    public static class SocketOneShotTask extends AsyncTask<Void, Void, Pair<Integer, Object>> {

        private static final int RESPOND_SUCCESS = 0;
        private static final int RESPOND_ERROR = 1;

        private final String mAddress;
        private final int mPort;
        private final JSONObject mJson;
        private OnRespondListener mListener;

        SocketOneShotTask(String address, int port, JSONObject json, OnRespondListener listener) {
            this.mAddress = address;
            this.mPort = port;
            this.mJson = json;
            this.mListener = listener;
        }

        protected Pair<Integer, Object> doInBackground(Void... arg0) {
            Socket socket = null;
            try {
                socket = new Socket(mAddress, mPort);
                OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
                out.write(mJson.toString() + "\n");
                out.flush();
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                return new Pair<Integer, Object>(RESPOND_SUCCESS, reader.readLine());
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return new Pair<Integer, Object>(RESPOND_ERROR, e);
            } catch (IOException e) {
                e.printStackTrace();
                return new Pair<Integer, Object>(RESPOND_ERROR, e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Pair<Integer, Object> result) {
            if (result.first == RESPOND_SUCCESS) {
                mListener.onReceived((String) result.second);
            } else {
                mListener.onError((Exception) result.second);
            }
        }

    }

}