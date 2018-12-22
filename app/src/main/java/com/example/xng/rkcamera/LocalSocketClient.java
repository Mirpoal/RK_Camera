package com.example.xng.rkcamera;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class LocalSocketClient {
    private String TAG = "deviceSocket";

    private final String SOCKET_HOST = "127.0.0.1";
    private final int SOCKET_CONNECT_PORT = 56505;
    private final int SOCKET_PORT = 56506;
    private final int TIME_OUT = 20000;
    public static String SEND_START = "network connect start";
    public static String SEND_STOP = "network connect stop";
    public static String RECEIVE_START_OK = "network connect ok";
    public static String SEND_NFS_MOUNT = "nfs mount";
    public static String SEND_NFS_UMOUNT = "nfs umount";
    private LocalSocketListener mListener;
    private ServerThread mServerThread;
    private DatagramSocket mServerSocket;
    private byte[] mBuffer = new byte[1024];

    public LocalSocketClient(LocalSocketListener listener) {
        mListener = listener;
    }

    public void startSocketServer() {
        if (null != mServerThread) {
            mServerThread.interrupt();
            mServerThread.isCancel = true;
        }
        mServerThread = new ServerThread();
        mServerThread.start();
    }

    public void stopSocketServer() {
        logd("stopSocketServer");
        sendMsgInThread(SEND_STOP);
    }

    private class ServerThread extends Thread {
        private boolean isCancel;

        @Override
        public void run() {
            isCancel = false;
            if (null == mServerSocket) {
                try {
                    logd("start socket server");
                    mServerSocket = new DatagramSocket(null);
                    mServerSocket.setReuseAddress(true);
                    mServerSocket.bind(new InetSocketAddress(SOCKET_CONNECT_PORT));
                    sendMsg(SEND_START);
                } catch (SocketException e) {
                    logd("start socket server error");
                    e.printStackTrace();
                    isCancel = true;
                }
            }
            while (!isCancel) {
                try {
                    logd("wait receive msg");
                    DatagramPacket packet = new DatagramPacket(mBuffer, mBuffer.length);
                    mServerSocket.receive(packet);
                    String result = new String(packet.getData(), 0, packet.getLength());
                    try {
                        logd("connect" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " success:" + result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (null != mListener && null != result) {
                        mListener.receiverLocalMsg(result);
                    }
                } catch (IOException e) {
                    loge("receive socket error");
                    e.printStackTrace();
                }
                //isCancel = true;
            }
        }
    }

    public void sendMsg(final String msg) {
        try {
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                    InetAddress.getByName(SOCKET_HOST), SOCKET_PORT);
            logd(packet.getAddress().getHostAddress() + ":" + packet.getPort() + "===sendMsg:" + msg);
            mServerSocket.send(packet);
        } catch (Exception e) {
            loge("happen error when send message");
            e.printStackTrace();
        }
    }

    public void sendMsgInThread(final String msg) {
        new Thread() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                            InetAddress.getByName(SOCKET_HOST), SOCKET_PORT);
                    logd(packet.getAddress().getHostAddress() + ":" + packet.getPort() + "===sendMsg:" + msg);
                    mServerSocket.send(packet);
                } catch (Exception e) {
                    loge("happen error when send message");
                    e.printStackTrace();
                }
                if (SEND_STOP.equals(msg)) {
                    close();
                }
            }
        }.start();
    }

    public void close() {
        try {
            mServerSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (null != mServerThread) {
            mServerThread.interrupt();
            mServerThread.isCancel = true;
            mServerThread = null;
        }
    }

    private void logd(String ss) {
        Log.d(TAG, ss);
    }

    private void loge(String ss) {
        Log.e(TAG, ss);
    }

    public interface LocalSocketListener {
        public void receiverLocalMsg(String msg);
    }
}
