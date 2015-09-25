package com.mmpud.socketexercise;

public interface OnRespondListener {

    public void onReceived(String jsonStr);

    public void onError(Exception e);

}
