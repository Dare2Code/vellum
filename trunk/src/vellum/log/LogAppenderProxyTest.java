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
package vellum.log;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 *
 * @author evan.summers
 */
public class LogAppenderProxyTest {

    public static final void main(String[] args) throws Exception {
        LogAppenderProxy proxy = new LogAppenderProxy();
        proxy.setJarFileName("/home/evans/cvs/bizmonger/dist/bizmonger.jar");
        proxy.setTargetClassName("bizmonger.GroovyMonAppender");
        proxy.setDebug(true);
        BasicConfigurator.configure(proxy);
        for (int i = 0; i < 20; i++) {
            Thread.sleep(2000);
            Logger.getRootLogger().info("" + i);
        }
    }
}
