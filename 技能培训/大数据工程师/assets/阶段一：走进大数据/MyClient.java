package com.imooc.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.net.InetSocketAddress;

public class MyClient {
    public static void main(String[] args) throws Exception{
        //通过socket连接server
        InetSocketAddress addr = new InetSocketAddress("localhost",1234);
        Configuration conf = new Configuration();
        MyProtocal proxy = RPC.getProxy(MyProtocal.class,MyProtocal.versionID,addr,conf);
        String result = proxy.hello("RPC");
        System.out.println("客户机收到的结果："+result);
    }
}
