package com.padyun.network;

import java.net.Socket;

/**
 * Created by litao on 2016/8/10.
 */
public interface NetWorkEvent {
    public void onConnect(Socket socket) ;
    public void onDisconnect(Socket socket) ;
    public void onTimeout() ;
    public void onRecv(Socket socket, byte[] data, int size) ;
    public void onRecvStr(Socket socket, String line) ;
}
