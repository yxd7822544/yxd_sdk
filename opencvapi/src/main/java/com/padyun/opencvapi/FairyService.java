package com.padyun.opencvapi;


import com.padyun.YpModule2;
import com.padyun.network.ServerNio2;
import com.padyun.network.StickPackageForNio2;

public class FairyService {

    private int mPort ;
    private StickPackageForNio2 mServer ;
    private StickPackageForNio2.StickPackageCallback mCallback ;
    public FairyService( StickPackageForNio2.StickPackageCallback callback){
        mCallback = callback ;
    }

    public void start(int port){
        mPort = port ;
        if(mPort > 0 ) {
            mServer = new StickPackageForNio2();
            mServer.setDataCallback(mCallback);
            mServer.start(mPort,false);
        }

    }
    public void stop(){
        if(mServer != null){
            mServer.stop();
        }
    }
    public void sendPackage(ServerNio2.ServerNioObject client, YpModule2 ypModule2){
        client.sendBuffer = ypModule2.toDataWithLen() ;
        mServer.sendMessage(client);
    }



}
