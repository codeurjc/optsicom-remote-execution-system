package es.optsicom.res.ssh.launcher.remote;

import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
 
public class Shell{
	public void run(){
	 String host="localhost";
     String user="javier";
     String password="15j5r61f";
     String command="ls";
     //try{
         JSch jsch = new JSch();
         /*Session session=jsch.getSession(user, host, 22);
         java.util.Properties config = new java.util.Properties(); 
         config.put("StrictHostKeyChecking", "no");
         session.setConfig(config);
         session.setPassword(password);
         session.connect();*/
          
         
     //}catch(Exception e){
     //    e.printStackTrace();
     //}

 }
}