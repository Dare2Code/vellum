/*
 * Source https://code.google.com/p/vellum by @evanxsummers

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.  
       
 */
package dualcontrol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import vellum.util.Threads;

/**
 *
 * @author evan
 */
public class ServerThread extends Thread {
    final static Logger logger = Logger.getLogger(ClientThread.class);

    private int count;
    private String errorMessage = "";
    private SSLServerSocket serverSocket; 

    public void start(SSLContext sslContext, int port, int count) throws IOException {
        this.count = count;
        serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().
                createServerSocket(port);
        serverSocket.setNeedClientAuth(true);
        start();
    }
    
    @Override
    public void run() {
        try {
            while (count-- > 0) {
                handle(serverSocket.accept());
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            errorMessage = e.getMessage();
        } finally {
            Streams.close(serverSocket);
            Threads.sleep(100);
        }
    }

    static void handle(Socket socket) throws IOException {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            Assert.assertEquals("clienthello", dis.readUTF());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("serverhello");
        } finally {
            socket.close();
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }    
    
    public void close() {
        Streams.close(serverSocket);
    }
}
