/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seafile.sharing;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;

import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.data.DatabaseHelper;
import com.seafile.seadroid2.data.SeafDirent;
import com.seafile.sharing.recvThread.recvTask;
import com.seafile.sharing.respondSync.recv_SyncTableTask;

import android.telephony.TelephonyManager;
import android.view.View;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
                     

public class FileSharing
{
	
	public static String ipaddress="";
	public static String bcastaddress="";
	public static int port=40000;
	public static int control_port=40001; //发送控制信息端口号，如文件删除
	public static int sync_port=40004;    //新加入节点要求同步的端口号
	public static int  FileMtimePort=40005;
	public static final int blocklength=1024;      //每包的大小1k
	public static final int maxfilelength=102400;    //文件块大小限制为100k
	public static int sendFileID=0;  //文件的id号
	public static String sharedPath="/mnt/sdcard/Seafile";
	public static Map<String,Timer> recvTimers=new HashMap<String,Timer>();
	
	public static Handler myHandler=null;
    public static Handler recvHandler= null;
	public static sendThread sThread=new sendThread();
	public recvThread rThread=null;	
    public respondSync resp=null;

	public FileObserver mFileObserver=null;
	public static BufferedWriter bw=null;
	public static long total_encode_timer=0;  //编码总共花费的时间
	public static long total_sending_timer=0; //发送总共花费的时间
	public static long total_sending_length=0; 
	public static int stored_blocks=20;  //存储编好码的块
	public String message=null;
    public OtherTask othertask=null;
    public Timer othertimer=null;
    public int otherPcks_time=2000; //别人反馈包的过期时间 ms
    public int selfFeedPcks_time=1000; //收到对自己反馈包的过期时间 ms
	public static int sleeptime=3000;  //计时3秒之后在开始监控队列。
	
	public static ArrayList<String>exitsFiles=new ArrayList<String>();        //当前目录已经存在的文件列表
    public static HashMap<String,String>sendFiles=new HashMap<String,String>(); //发送文件列表
    public static Map<String,Long>recvFiles=new HashMap<String,Long>();        //接收文件列表
    public static Map<String,Integer>nextseq=new HashMap<String,Integer>(); //next packetID
    public static ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //反馈包的文件号，丢包数
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//其他人的反馈包信息
	public static Map<String,Timer> feedTimers=new HashMap<String,Timer>();//收到反馈包时开始计时
	
	
	
	public static Queue<FileMtimeData > SendFilequeue = new LinkedList<FileMtimeData>();   
	public static listenQueue listenqueue=null;     
	public static ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();//放置收到的文件块
	public static Map<String,Integer>subFile_nums=new HashMap<String,Integer>(); //收到对应文件（<10m）的块数
	public static Map<String,Integer>sub_nums=new HashMap<String,Integer>();//大于10m的文件接收到的块数
	public static Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //存储收到的对应文件的块号
	public static Map<String,Timer> subfileTimers=new HashMap<String,Timer>();  //每当收到一个文件块就会计时
	public static final int block_time=4000;  //文件发送块反馈包的时间
	
	
	
	//public static final int syn_watie_time=4000;//li,等其他表的计时器时间
	//public static boolean Synced=false;//li,已经同步了的标志
	public static boolean nonetupdate=false;
	public static boolean netupdate=false;
	public static ArrayList<String>adhocreceive=new ArrayList<String>();//用于上网节点判断是否是从adhoc接收到的
	public static int ipCount=1;//li
	public static Map<String,ArrayList<String[]>> savedMap=new LinkedHashMap<String,ArrayList<String[]>>();//*0522，保存在每一个结点的整理表，
	public static boolean noteType=false;
	public static ArrayList<String> onlyInServer=new ArrayList<String>();//用于存放只有服务器有本地没有的文件
	//public Map<String,Timer> synTableTimers=new HashMap<String,Timer>(); //li,用于放置每个IP的计时器 
	//public Timer synTableTimer=null;
	//public recv_SyncTableTask syntask=null;
	
	//public static String lastsub_id=new String();//用来存放找到的最后一个文件的最后一块的ID号
	//public static int blockid=0;
	//设置联网节点比较时找到的服务器有，但联网节点没有的文件，存放在列表里，因为当整理后，发件这些文件中有要发送的，要像
	//下载文件那样下载下来
	
	
	
	public static long currentLength=0;  //当前文件列表中文件的长度
	public static long  maxQueueLength=1024*10240; //队列的最大容量
	public static int file_number=0;
	public static int maxNumber=20;  //队列容许的最大文件数
	public static int maxStoredLength=1024*10240; //超过10m的文件则接收到文件块直接输出
	public static fecFuntion fecfunction=new fecFuntion(); //程序中值实现一个fecFuntion实例，且用了synchronized发送，避免内存溢出
	public static ArrayList<Packet[]> encodedPacket=new  ArrayList<Packet[]>(); //编码完成的块
	SDCardFileObserver  fileobver=null;
	sendContInfo sendInfo=new sendContInfo();
	recv_control_info recvInfo=new recv_control_info();
	//public static String front_path=null;
    public void running()
    {
    	ipaddress=BrowserActivity.getIp();
        total_encode_timer=0;
        bcastaddress="192.168.1.255";
        /* WIFI:255.255.255.255
         * Adhoc:192.168.1.255
         */
        total_encode_timer=0;  //编码总共花费的时间
    	total_sending_timer=0; //发送总共花费的时间
    	total_sending_length=0; 
        System.out.println("我的IP地址是："+ipaddress+"\n");
        System.out.println("我的广播地址是："+bcastaddress+"\n");
        rThread=new recvThread(ipaddress,port);
        rThread.start();
        listenqueue=new listenQueue();
        listenqueue.start();
        sendInfo= new sendContInfo();
        recvInfo.start();
        resp=new respondSync(ipaddress,sync_port);
        resp.start();
        
  	    SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss");
	    Date curDate = new Date(System.currentTimeMillis());
	    String m = formatter.format(curDate);
	    String sPath="//sdcard/Log";
        File SDir=new File(sPath);
        if (!SDir.exists())
        {
        	SDir.mkdirs();
        }
        String logPath=sPath+"/log-"+m+".txt";
  		File f=new File(logPath);		  
    	 try {
			bw=new BufferedWriter(new FileWriter(f));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	 writeLog("filename"+",	");
    	 writeLog("fileID"+",	");
	     writeLog("fileLength"+",	");
	     writeLog("time"+",	");
         writeLog("formatter-time"+",	"+"\r\n");
        myHandler = new Handler()
        {
        	public void handleMessage(Message Msg)
        	{
        		if(Msg.arg1==0)
        		{
        		  String s = (String) Msg.obj;
        		  if(s.equals("生成文件"))
        			  System.out.println("生成文件" );
        		}  
        		else if(Msg.arg1==1)
        		{
        		    Packet[] recvPacket=(Packet[]) Msg.obj;	  
        		    Packet p=recvPacket[0];
        	        if(p.type==0&&recvPacket.length>=p.data_blocks)
        		    {
        	        	System.out.println( "收到的是数据包\n");	
        		       fecfunction.decode(recvPacket);	  //调用解码函数
        		    }	
        		    if(p.type==1)
        		    {		
        		    	FeedBackData fb=null;	  
        		    	 try {  
        					    ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
        					    ObjectInputStream ois = new ObjectInputStream(bais);
        					    fb = (FeedBackData)ois.readObject(); 
        					    bais.close();  
        					    ois.close();  
        					  }  
        					  catch(Exception e)
        					  {    
        					    System.out.println(e.toString());
        					    e.printStackTrace();
        					  }			   
        			      handleFeedBackPackets(fb);
        		     }
        		 } 
        		 else if(Msg.arg1==2)
          		{
        			 String filename=(String)Msg.obj;
        			 File f=new File(filename);
        			 System.out.println("删除文件");	
        			 if(f.isDirectory())
        			 {
        				 System.out.println("删除目录");	
        				 File [] files=f.listFiles();
        				 for (File file: files)
        		          {
        					 if(file!=null)
        					 {
        					    file.delete();
        					    deletefile(file.getAbsolutePath());
        					 }
        		          }
        				 f.delete();
        				 deletefile(filename);
        			 }
        			else if(f.exists())
        			{
        				deletefile(filename);
        				f.delete();	
        			} 
          		}
        		 else if(Msg.arg1==3)  
           		{
        			 
        			 SynPacket syn=(SynPacket)Msg.obj;
        			 int type=syn.type;
        			 switch(type)
        			 {
        			 case 0:
        				 Map<String,Long> map=new HashMap<String,Long>();
        				 Map<String, ArrayList<String[]>> map1=new LinkedHashMap<String, ArrayList<String[]>>();
        				 if(Utils.isNetworkOn()) //自己是可以链接服务器的节点
        				 {
        					   //先与服务器同步，然后再与请求节点同步 
             				   String mess ="自己是可以链接服务器的节点收到要求同步信息";
             			       System.out.println(mess); 
             			      BrowserActivity.showSynInfo(mess);
             				 BrowserActivity ba=new  BrowserActivity();
             	    		   map=ba.syncWithServer();//联网节点首先和服务器进行表同步
             	    		   ArrayList<String[]> infos=new ArrayList<String[]>();
           				       Iterator it = map.keySet().iterator(); 
           				       while (it.hasNext())
           				       { 
           				    	 String key; 
           			    	     key=(String)it.next(); 
           			    	     Long value;
           			    	     value=map.get(key);
           			    	     String[] dir=new String[2];
           			    	     dir[0]=key;
           			    	     dir[1]=Long.toString(value);
           			    	     infos.add(dir);
           			    	     
           				       }
           				       map1.put(BrowserActivity.getIp(), infos);
             	    		   String infos1="IP地址为"+BrowserActivity.getIp()+"的主机发送了自己的文件表";
             	    		   requestSyncFunction rsf=new  requestSyncFunction(1,map1,infos1);
             	    		   rsf.start();
        				 }
        				 else//不联网节点
        				 {
        					  //直接将自己的local表发送出去
           				      String mess ="自己是不联网节点 收到要求同步信息";
           				      System.out.println(mess); 
           				      BrowserActivity.showSynInfo(mess);
           				      map=FileSharing.getFileModified();
           				      ArrayList<String[]> infos=new ArrayList<String[]>();
           				      System.out.println("map.size():"+map.size()); 
           				      Iterator it = map.keySet().iterator(); 
           				      while (it.hasNext())
           				      { 
           				    	 String key; 
           			    	     key=(String)it.next(); 
           			    	     Long value;
           			    	     value=map.get(key);
           			    	     String[] dir=new String[2];
           			    	     dir[0]=key;
           			    	     dir[1]=Long.toString(value);
           			    	     infos.add(dir);
           				      }
           				      map1.put(BrowserActivity.getIp(), infos);
           				      String infos1="IP地址为"+BrowserActivity.getIp()+"的主机发送了自己的文件表";
           				      System.out.println(infos1); 
           				      requestSyncFunction rsf=new requestSyncFunction(1,map1,infos1);  
           				      rsf.start();
        				 }
        				 break;
        			 case 2:
        				//type=2的时候，收到是发起同步结点整理好的表
        				 System.out.println("******** 节点接收到的savedMap--"+syn.map);
    				     if(syn.map==null)//传送来的整理表，不可能为null，因为
    				     {
    					     System.out.println("没有要同步的信息");
    				    	 String mess ="没有要同步的信息";
    					     BrowserActivity.showSynInfo(mess);
    					     //没有要同步的信息后应该讲所有的标志恢复
    				     }
    				     else
    				     {
        				      System.out.println("@@@@@@@节点收到整理的settlie表 ");
    					      FileSharing.savedMap=syn.map;//将收到的map放进静态变量中为了以后使用
    					      Iterator it = syn.map.keySet().iterator(); 
    				          while(it.hasNext())
    				          {
    					          String key; 
    					          ArrayList<String[]> value=new ArrayList<String[]>();
    			    	          key=(String)it.next(); 
    			    	          System.out.println("节点收到整理的settle表，找到表中第一个IP "+key);
    			    	          if(BrowserActivity.getIp().equals(key))//如果发起同步结点是第一个的话，首先发送他要发送的文件
    			    	          {
    			    	        	  System.out.println("@@@@@@ 本节点是第一个发送的节点-- "+BrowserActivity.getIp());
    			    	        	  System.out.println("respond----if 是否为第一个 ");
    			    	        	   if(Utils.isNetworkOn())//如果是可以上网的结点
    			    		            {
    				    	    		    value=FileSharing.savedMap.get(key);
    					    		        for(int i=0;i<value.size();i++)
    					    		        {    
    					    			        String path=new String();
    					    			        String time=new String();
    					    		            path=value.get(i)[0]; 
    					    		            time=value.get(i)[1];
    					    		            Long curr_time=new Long(time);
    					    		            //是第一个，还要判断这个结点是不联网节点，如果是的话，判断他要同步的文件里，如果有服务器有本地没有的
    					    		            //要从服务器下载
    					    		            if(FileSharing.onlyInServer.contains(path))//要从服务器找到并下载的
    					    		            {
    					    		            	if(i==(value.size()-1))
    					    		            	{
    					    		            		//如果要下载的这个文件还是最后一个文件的话，记下来
    					    		            		BrowserActivity.downIsLast=true;
    					    		            		
    					    		            	}
    					    		            	//将这个文件从服务器下载，并发送出去
    					    		            	//要从服务器下载，这里子线程中不能对UI线程的操作，所以
    					    		            	 Message msg = new Message();
    				                                 msg .obj = path;
    				                                 msg .arg1=4;
    				                                 FileSharing.myHandler.sendMessage(msg);
    				                                 System.out.println("联网节点同步---从服务器下载文件时：path的路径"+path);
    					    		            }
    					    		            else//本地有的
    					    		            {
    					    		            	 if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
    							    		            {
    							    			             FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
    							    		            }
    							    		            else
    							    		            {
    							    		            	FileSharing.addToQueue(path, curr_time, false,false);
    							    		            }
    					    		            }
    					    		         }//for找要发送的每一个文件，每一个文件和onlyInServer对比完后，onlyInServer应该清空
    					    		        FileSharing.onlyInServer.clear();
  					    		            BrowserActivity.reposInfo.clear();
    			    		            }
    			    	          else
    			    	          {
    			    	        	  System.out.println("节点收到整理的settle表，判断自己是否为非联网 ");
    			    		           value=syn.map.get(key);//这里的key是IP
    			    		           for(int i=0;i<value.size();i++)
    			    		           {     
    			    			            String path=new String();
    			    			            String time=new String();
    			    		                path=value.get(i)[0]; 
    			    		                time=value.get(i)[1];
    			    		                Long curr_time=new Long(time);
    			    		                System.out.println("case  value.size()个 "+value.size());
    			    		                if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
    			    		                {
    			    		                	  System.out.println("case 2 是最后一个文件 " );
    			    			                 FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
    			    			                 //将自己要发送的文件发送完毕后，发送一个控制信息，end加上下一个要发送的IP，将这个消息广播出去
    			    			                 //结点收到这个广播消息后，将IP取出和自己的的IP对比，如果相同，说明他是第二个，以此类推
    			    		                 }
    			    		                else
    			    		                {
    			    		                	  System.out.println("case 2 不是最后一个文件 ");
    			    		                      FileSharing.addToQueue(path, curr_time, false,false);
    			    		                 }
    			    		            
    			    		            }
    			    	          }
    			    	        }
    			    	        break;
    				           }
    				        //将表中的第一条记录取出，判断和自己的IP是否相等，相等说明这个IP是第一个要发送的，并找到下一条记录的IP
    		    				 //第一个IP将自己要发送的文件依次放进康的发送程序中，当接收完后下一个IP发送自己的文件
    		        				 
    				      }
    				     break;
        			 case 3:
        				//收到type等于3的情况，收到的是从第一个发送IP开始的文件发送完的通知，广播发出去后，收到的节点如果ip
    					 //和字符串中的ip相同，说明他是下一个要发送的ip，开始发送
    					   //FileSharing.savedMap=syn.map;//将收到的map放进静态变量中为了以后使用？？？？？？？？这里对静态变量的使用？？？？？？？？    
        				     String nextip=syn.information;
        				      System.out.println("节点收到nextIP消息，  FileSharing的case 3 进行处理 "+nextip );
					           if(BrowserActivity.getIp().equals(nextip))
					           {  
					        	   System.out.println("@@@@ 本节点收到nextIP后，发现与自己的IP一致，本节点开始同步文件" );
					        	   synchronized(FileSharing.savedMap)
					               {
					        	     ArrayList<String[]> value=new ArrayList<String[]>();
					                 value=FileSharing.savedMap.get(nextip);
					        	    if(Utils.isNetworkOn())//如果是可以上网的结点
			    		             {
					    		        for(int i=0;i<value.size();i++)
					    		        {    
					    			        String path=new String();
					    			        String time=new String();
					    		            path=value.get(i)[0]; 
					    		            time=value.get(i)[1];
					    		            Long curr_time=new Long(time);
					    		            //是第一个，还要判断这个结点是不联网节点，如果是的话，判断他要同步的文件里，如果有服务器有本地没有的
					    		            //要从服务器下载
					    		            if(FileSharing.onlyInServer.contains(path))//如果该文件是服务器上才有的文件
					    		            {
					    		            	if(i==(value.size()-1))
					    		            	{
					    		            		//如果要下载的这个文件还是最后一个文件的话，记下来
					    		            		BrowserActivity.downIsLast=true;
					    		            	}
					    		            	//将这个文件从服务器下载，并发送出去
					    		            	 sync_download(path);
					    		            }
					    		            else//本地有的
					    		            {
					    		            	 if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
							    		            {
							    			             FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
							    			             //将自己要发送的文件发送完毕后，发送一个控制信息，end加上下一个要发送的IP，将这个消息广播出去
							    			             //结点收到这个广播消息后，将IP取出和自己的的IP对比，如果相同，说明他是第二个，以此类推
							    			             //如果这个文件是最后一个的话，在最后一个文件发送完要发送end+nextIP
							    		            }
							    		            else
							    		            {
							    		                  FileSharing.addToQueue(path, curr_time, false,false);
							    		            }
					    		            }
					    		         }//for
					    		        FileSharing.onlyInServer.clear();
					    		          BrowserActivity.reposInfo.clear();
			    		            }
				    	    	   else//不可以上网的话
				    	    	   {
				    	    		  
					    		        for(int i=0;i<value.size();i++)
					    		        {    
					    			        String path=new String();
					    			        String time=new String();
					    		            path=value.get(i)[0]; 
					    		            time=value.get(i)[1];
					    		            Long curr_time=new Long(time);
					    		            if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
					    		            {
					    			             FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
					    			             //将自己要发送的文件发送完毕后，发送一个控制信息，end加上下一个要发送的IP，将这个消息广播出去
					    			             //结点收到这个广播消息后，将IP取出和自己的的IP对比，如果相同，说明他是第二个，以此类推
					    			             //如果这个文件是最后一个的话，在最后一个文件发送完要发送end+nextIP
					    		            }
					    		            else
					    		            {
					    		                  FileSharing.addToQueue(path, curr_time, false,false);
					    		            }
					    		         }
				    	    	   }  
		    		                
					           }
					           break;
					           }
        			 }//switch结束
        				 
       				 
        		}//Msg.arg1==3
        		 else{
        			 if(Msg.arg1==4)
        			 {
        				 String path=(String)Msg.obj;
        				 sync_download(path);
        			 }
        		 }
        		
        }	       	
     };
     
       //定时更新别人的反馈包列表
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,otherPcks_time,otherPcks_time);
   }
    /**
     * 全网同步时，结点需要从服务器下载文件时，用的函数
     * @param path
     */
    public void sync_download(String path)
    {
    	 System.out.println("   ");
    	 System.out.println("！！！！！！！！！！！！！！！       ");
   	     System.out.println("sync_download中传递的参数：   "+path);
   	     String[]infos1=path.split("/");
    	 String reponame=infos1[5];
		 System.out.println("仓库目录名称是：   "+reponame);
		 String repoid=null;
		 String dirpath="";
		 String filename=null;
		 System.out.println("BrowserActivity.reposInfo.size()"+BrowserActivity.reposInfo.size());
		/*
			Iterator it=BrowserActivity.reposInfo.keySet().iterator();
			while(it.hasNext())
			 {
				 System.out.println("BrowserActivity.reposInfo表中记录的--filepath: "+it.next());
			 }
			 */
		 if(BrowserActivity.reposInfo.containsKey(path.trim()))
		 {
			 repoid=BrowserActivity.reposInfo.get(path);
			 String[]infos=path.split("/");
			// System.out.println("infos.length:  "+infos.length);
			 for(int i=6;i<infos.length;i++)
			   dirpath+="/"+infos[i];
			 System.out.println("下载文件所在的仓库id：  "+"repoid： "+repoid); 
			 System.out.println("下载文件所在的仓库naem：  "+" repo_name:"+ reponame); 
			 System.out.println("下载文件所在的仓库dirpath：  "+" dirpath: "+dirpath);
		     BrowserActivity.txService.addDownloadTask(BrowserActivity.account,reponame, repoid, dirpath);
		
		 }
		 //下载文件
		
    }
    
    /*
     * 开始监听共享文件
     */
    public void MonitorDirentsAndfiles ()
    {
        //要先创建一个文件夹
        File SharedDir=new File(sharedPath);
        if (!SharedDir.exists())
        {
        	SharedDir.mkdirs();
        }
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //开始监听
        }     
    }
	public static ArrayList<String> compareStrings(String[] s1,String[] s2)
	{
		ArrayList<String> Files=new ArrayList<String>();
		boolean isEqual=false;
			for(int j=0;j<s1.length;j++)
			{ 
			    for(int i=0;i<s2.length;i++)
			    {
					isEqual=false;
					s1[j].trim();
					s2[i].trim();
					if(s1[j]!="" &&s1[j].equals(s2[i]))
					{
						isEqual=true;
						break;		
					}
				}
				if(isEqual==false)	
					Files.add(s1[j]);  
			}
		return Files;
	}
	public static String compareStringsReturnString(String[] s1,String[] s2)
	{
		String Files="";
		boolean isEqual=false;
			for(int j=0;j<s1.length;j++)
			{ 
			    for(int i=0;i<s2.length;i++)
			    {
					isEqual=false;
					s1[j].trim();
					s2[i].trim();
					if(s1[j]!="" &&s1[j].equals(s2[i]))
					{
						isEqual=true;
						break;		
					}
				}
				if(isEqual==false)	
					Files+=s1[j]+"|";
			}
		return Files;
	}
	  public void deletefile(String path)
	  {
		    synchronized(recvFiles)
  		    {
  		    	if(recvFiles.containsKey(path))
  		           recvFiles.remove(path);  //从接收文件列表删除
  		    }
		    synchronized(exitsFiles)
 		    {
   		     if(exitsFiles.contains(path))
   			   exitsFiles.remove(path); 
 		    } 
		    /*
  		   synchronized(sendFiles)
  		   {
  		     if(sendFiles.size()>0)
  		      {
  		      Iterator it = sendFiles.keySet().iterator();
 		       while (it.hasNext())
 		       {
 		       String key=null;
 		       key=(String)it.next();  //第一次调用Iterator的next()方法时，它返回序列的第一个元素
 	            if(sendFiles.get(key).equals(path)) 
 	             {
 			        sendFiles.remove(key); 
 			        synchronized(nextseq)
   		            {
 			           nextseq.remove(key);     //对应文件的下一个子包序号列表也要删除
   		             }
 			        break;
 		           }
 		         }
  		       }
  		   } 
  		   */
	    }
    public static void writeLog(String info)
    {
    	  try {
      		  bw.append(info);
      		  bw.flush();
  		    } catch (Exception e) 
  		    {		
  			e.printStackTrace();
  		    }

    }
    /*
     * 处理收到的反馈包，包括块丢失反馈包和单个的包丢失反馈包
     */
   public void handleFeedBackPackets (FeedBackData fb)
   {	 
	  String fileid=null;
	  if(fb.type==1) 
        {
		  String []fileID_break=fb.sub_fileID.split("--"); 
		  fileid=fileID_break[0];
        }
	  else
		  fileid=fb.sub_fileID;
	
	  	if(sendFiles.containsKey(fileid)) 
	  	{ 
             synchronized(FileSharing.Feedpkts)
             {     
	           int i=0;
	           System.out.println("Feedpkts.size() :"+Feedpkts.size());
	           for(i=0;i<Feedpkts.size();i++)
	              {
	        	   if(Feedpkts.get(i).nos!=null&&Feedpkts.get(i).sub_fileID.equals(fb.sub_fileID))
	                {		
	        			 System.out.println("miss_nos: "+Feedpkts.get(i).nos);
	        			 
	        			  if(fb.type==1&&Feedpkts.get(i).nos.get(0)<fb.nos.get(0))
	                      {
	        			      Feedpkts.get(i).nos=fb.nos;
	                      }
	        			  else if(fb.type==2&&Feedpkts.get(i).nos.size()<fb.nos.size())
		                   {
		        			   Feedpkts.get(i).nos=fb.nos;
		                   } 
	        			
	        		    
	        	       break;
	                }
	           }
	           if(i==Feedpkts.size())
	           {
	        	 Feedpkts.add(fb);
	        	 System.out.println("开始计时："+fb.sub_fileID+"\n");
	        	 Re_SendTask sendtask=null;
	        	 Timer feedtimer=null;
			     sendtask=new Re_SendTask(fb.sub_fileID);
			     feedtimer = new Timer(true);
			     feedtimer.schedule(sendtask,selfFeedPcks_time,selfFeedPcks_time); 
			     feedTimers.put(fb.sub_fileID, feedtimer);
	           }
            } 
	      }  
           else
           {
            synchronized(othersFeedpkt)
            {
         	int k=0;
        	for(;k<othersFeedpkt.size();k++)
	        {
		        if(othersFeedpkt.get(k).sub_fileID.equals(fb.sub_fileID))
	          	{ 
		        	 if(fb.type==1&&othersFeedpkt.get(k).nos.get(0)<fb.nos.get(0))
                     {
		        	     othersFeedpkt.get(k).nos=fb.nos;
				         othersFeedpkt.get(k).time=System.currentTimeMillis();
                     }
		        	 else if(fb.type==2&&othersFeedpkt.get(k).nos.size()<fb.nos.size())
		        	 {
		        		 othersFeedpkt.get(k).nos=fb.nos;
					     othersFeedpkt.get(k).time=System.currentTimeMillis();
		        	 }
		 
			      break;
		        }	
	        }
	       if(othersFeedpkt.size()==k)
	        {
	           otherFeedData ofd=new otherFeedData(fb.sub_fileID,fb.nos,System.currentTimeMillis(),fb.curr_time);
		       othersFeedpkt.add(ofd);
	        }
           }
          } 
	   System.out.println("收到反馈包 fb.type="+fb.type);
   }
   /**
    * 删除文件时，顺便删除相关的记录（发送文件列表，接收文件列表等）
    * @param path
    */
   public void deleteRecord(String path)
   {
	   boolean issend=false;
	   boolean issending=false;
	   boolean isexits=false;
	   System.out.println("删除相关的记录"); 
	   synchronized(FileSharing.recvFiles)
		{
		   if(FileSharing.recvFiles.containsKey(path))
		    {
		    	FileSharing.recvFiles.remove(path);  //从接收文件列表删除
		        issend=true;
		    }
		    else
		      issend=false;
		}
	   synchronized(FileSharing.sendFiles)
		{
		   if(FileSharing.sendFiles.size()>0)
		    {
		     Iterator it = FileSharing.sendFiles.keySet().iterator();
	         while (it.hasNext())
	           {
	             String key=null;
	             key=(String)it.next();  //第一次调用Iterator的next()方法时，它返回序列的第一个元素
                 if(FileSharing.sendFiles.get(key).equals(path)) 
                  {
    	              issending=true;
    	              FileSharing.sendFiles.remove(key); 
		              synchronized(FileSharing.nextseq)
		              {
			             FileSharing.nextseq.remove(key);     //对应文件的下一个子包序号列表也要删除
		              }
		             break;
	              }
	           }
		     }
		   }
		 synchronized(FileSharing.exitsFiles)
	      {
		     if(FileSharing.exitsFiles.contains(path))
		     {
		    	 FileSharing.exitsFiles.remove(path); 
			     isexits=true;
		      }
	      } 
		    if(issending==true|| issend==true||isexits==true)
		    {
		    	 sendInfo.init(path);
		         sendInfo.sendcontrolInfo();
		    }
   }
  /**
   * 监控文件目录的类
   * @author PC
   *
   */
   public class SDCardFileObserver extends FileObserver //是一个线程
    {
       //mask:指定要监听的事件类型，默认为FileObserver.ALL_EVENTS
	   public String directory=null;
	   List<SingleFileObserver> mObservers=null;
	   String mPath=null;
	   int mMask;
       public SDCardFileObserver(String path,int mask) 
       {
           super(path, mask);
       }
       public SDCardFileObserver(String path) 
       {
           super(path);
           directory=path;  //sdcard//Shared//...
           mPath=path;
      System.out.println("当前监听的目录："+directory);
       }
       @Override 
       public void startWatching()
       {
       if (mObservers != null)
          return ;
       mObservers = new ArrayList<SingleFileObserver>();
       Stack <String>  stack= new Stack <String>();
       stack.push(mPath);
       while (!stack.isEmpty())
       {
          String parent = stack.pop();
          mObservers.add(new SingleFileObserver(parent));
          File path = new File(parent);
          File[]files = path.listFiles();
          if (null == files)
           continue;
         
          for (File f: files)
          {
            if(f.isDirectory() && !f.getName().equals(".") && !f.getName() .equals(".."))
            {
            stack.push(f.getAbsolutePath());
            }
            exitsFiles.add(f.getAbsolutePath());
         }
      }
     for (SingleFileObserver sfo: mObservers)
     {
         sfo.startWatching();
     }
  }
    @Override 
   public void stopWatching()
   {
       if (mObservers == null)
         return ;
       synchronized(mObservers)
       {
       for (SingleFileObserver sfo: mObservers)
       {
           sfo.stopWatching();
       }
          mObservers.clear();
          mObservers = null;
       }
       super.stopWatching();
  }
       @Override
       public void onEvent(int event, String path) //此时的path就是绝对路径了
       {
           final int action = event & FileObserver.ALL_EVENTS; 
           long curr_time=System.currentTimeMillis();
           ArrayList<String[]> filelist=new ArrayList<String[]>();//0509
           switch (action) 
           {  
           case FileObserver.CREATE:
          		File f=new File(path);
          	    if(f.isDirectory())
          	    {
          	    	exitsFiles.add(path);
        	        System.out.println("----"+path+" CREATE:"); 
        	        handleDirectory(path);
          	   }    
   			break;
           case FileObserver.MODIFY:  //文件内容被修改时触发，如粘贴文件等
        	   System.out.println("----"+path + " MODIFY");
        	   break;
           case FileObserver.CLOSE_WRITE:  //编辑文件后，关闭
       	       System.out.println("----"+path + " CLOSE_WRITE");
       	       synchronized(recvFiles)
       	       {
       	         if(! recvFiles.containsKey(path))
       	        { 
       	    	 System.out.println("判断recvFiles的值："+recvFiles.containsKey(path));
       	    	  addFileModified(path,curr_time);	//放进文件列表，后期要改进的，因为放进去的时候如果以前有这个文件但时间旧的，要删除
       	    	//用于测试
       	       Map<String,Long> map=new HashMap<String,Long>();
     		   map=getFileModified();
     		   System.out.println("显示local表中的内容@@@@");
     		   BrowserActivity.show(map);
     		 //用于测试
       	    //FileSharing.writeLog("接收文件也可以在这里监听到");
       	      //对于上网结点启用的是seafile的监控，所有收到文件这里监控不到；对于不上网节点收文件这里也监控到，
        	   addToQueue(path,curr_time,false,false);}
       	       }
        	   break;
           case FileObserver.MOVED_TO:
      		    System.out.println("----"+path + " MOVED_TO");
      			addFileModified(path,curr_time);	//放进文件列表，后期要改进的，因为放进去的时候如果以前有这个文件但时间旧的，要删除
   	          //用于测试
        	   Map<String,Long> map=new HashMap<String,Long>();
      		   map=getFileModified();
      		   System.out.println("显示local表中的内容@@@@");
      		   BrowserActivity.show(map);
      		 //用于测试
      		    nonetupdate=true;
      		    addToQueue(path,curr_time,false,false);
              	break;
           case FileObserver.DELETE:
      		    System.out.println("----"+path + " DELETE");
      		//    deleteFileModified(path); //从数据库删除文件相关信息
      		    File file=new File(path);
        	    if(mObservers != null)
        	    {
        	    	synchronized(mObservers)
        	    	{
        	    	for(int i=0;i<mObservers.size();i++)
        	    	{
        	    		if(mObservers.get(i).mPath.equals(path))
        	    		{
        	    			System.out.println("停止监听该目录");
        	    			mObservers.get(i).stopWatching();
        	    			mObservers.remove(i);	
       	    			    break;
        	    		}
        	    	}
        	      }
        	    }
        	    deleteRecord(path);
      		    
        	   break;
           }
       }
       
   public class SingleFileObserver extends FileObserver
   {
       String mPath;
       String newPath=null;
       public SingleFileObserver(String path)
       {
         this(path, ALL_EVENTS);
         mPath = path;
       }
       public SingleFileObserver(String path, int mask)
       {
         super(path, mask);
         mPath = path;
       }
       @Override 
       public void onEvent(int event, String path)
       {
    	 newPath = mPath + "/" + path;
         SDCardFileObserver.this.onEvent(event, newPath);
       }
    
      @Override 
      public void stopWatching()
      {
    	   System.out.println("stoping watchinging: "+mPath);
    	   super.stopWatching();
       }
    }
  
   /**
    * 监控目录中有新建的目录
    * @param path
    */
   public synchronized void handleDirectory(String path)
   {
	   synchronized(recvFiles)
		 {
	   if(!recvFiles.containsKey(path)) //接收的的文件不再发送
	  	{
		 String filepath=path;  //要监听的子目录
		 System.out.println("开始监听子文件夹"+filepath);   
		 SingleFileObserver sfb=new SingleFileObserver(filepath);	 
		 sfb.startWatching();
		 mObservers.add(sfb);
		
	     File f=new File(filepath);
		 File[] files= f.listFiles();
		 
		 for(int i=0;i<files.length;i++)
		 {   
			 if(files[i].isDirectory())
			 {
				 handleDirectory(filepath+"/"+files[i].getName()); 
			 }
			 else
			 {
				 long curr_time=System.currentTimeMillis();
			//	 addFileModified(filepath+"/"+files[i].getName(),curr_time,"no");
		         addToQueue(filepath+"/"+files[i].getName(),curr_time,false,false);
			 }
		 }
	  }	
	   else
	   {
		 //接收的文件发送服务器上
		   
	   }
	}  
   }
  }
   /**
    * 在local中插入数据
    * @param filename
    */
  
   public static void addFileModified(String filename,long m_time)
   {
	   DatabaseHelper dbHelper;
	  dbHelper = DatabaseHelper.getDatabaseHelper();
	  dbHelper.saveFileModifiedTable(filename, m_time);
	   //FileSharing.writeLog("filename:  "+filename);
	  // FileSharing.writeLog(""+"\r\n");
	 //  FileSharing.writeLog("m_time:  "+m_time);
	 //  FileSharing.writeLog(""+"\r\n");
	  // FileSharing.writeLog("s_time:  "+s_time);
	 // dbHelper.close();//存完数据库已经关闭了
   }
   /**
    * 从local中读出数据
    * 
    */
   public static Map getFileModified()
   {
	   ArrayList<String[]> filelist=new ArrayList<String[]>();
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper();
	   filelist=dbHelper.getFileFromModifiedTable();
	   //System.out.println("从local中读出数据成功");
	   Map<String,Long> map=new LinkedHashMap<String,Long>(); 
	   if(filelist==null)
  	   {
  		   return map;
  	   }
	   for(int i=0;i<filelist.size();i++)
  	   {
		   
  		   String filename=filelist.get(i)[0];
  		   String time=filelist.get(i)[1];
  		  
         map.put(filename, Long.parseLong(time));
  	   }
	   return map;
  	  
   }
   /**
    * 从sync表中读出数据
    * 主动节点读，sync全网同步过程全网文件信息筛选阶段用到的表，
    */
   public static  ArrayList<String[]> getSyncTable()
   {
	   ArrayList<String[]> filelist=new ArrayList<String[]>();
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper();
	   filelist=dbHelper.getSynTable();
	   //System.out.println("从全网筛选文件信息表sync表中读出数据成功");
	   return filelist;
  	  
   }
   /**
    * 显示sync表中的内容
    * 
    */
   public static void showSync()
   {
	   ArrayList<String[]> filelist=new ArrayList<String[]>();
	   filelist=getSyncTable();
	   if(filelist!=null)
	   {
	       for(int i=0;i<filelist.size();i++)
	       {
		   System.out.println("显示sync表的内容，  文件名："+filelist.get(i)[0]+"     时间："+filelist.get(i)[1]+"IP："+filelist.get(i)[2]);
	       }
	   }
   }
   /**
    * 清空sync表中的内容
    */
   public static void clearSynTable()
   {
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper();
	   dbHelper.emptySynTable();
	   
   }
   
   
   
   
   
     
   /*
   public static void deleteFileModified(String filename)
   {
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper(); 
	   dbHelper.deleteFileModified(filename);
	   dbHelper.close();
   }
     */
   /**
    * 将文件加入到发送队列，准备发送
    * @param path
    * @param directSend若为true，不用考虑是否是收到的等一些条件，直接发送
    */
 public static synchronized void addToQueue(String path,long curr_time,boolean directSend,boolean islast)//*0522
 {
	 System.out.println("^^^^^^^进入addToQueue函数  "+path);
	// /*
	 FileMtimeData fileData=new FileMtimeData(path,curr_time,islast);//2.将要发送的文件封装成类0522
	 System.out.println("fileData.filename::    "+fileData.filename);
	 synchronized(recvFiles)
	 { 
		 boolean contain=false;
		 String file_id="";
		synchronized(SendFilequeue)
		{ 
		 if(directSend==false)
		 { //这里出现问题，比如一个节点从adhoc收到一个文件，会放进recvFiles中，当这个节点更新后不会发送出去
			 //收到更新文件会放进recvFiles中吗
	       if((!recvFiles.containsKey(path)&&!SendFilequeue.contains(fileData))||nonetupdate||netupdate)  //接收的的文件不再发送，发送队列中若已有，不再添加，在处理队列后，就会置为空
  	        {	
	    	   System.out.println("AAAAAAAAAAAAAAAAA    "+path);
	    	   contain=false;
	    	   
  	        }
	       else 
	         contain=true; 
		 }  
		 else if(SendFilequeue.contains(fileData)) //若directSend==true,不用上述的判断
		 {
			 contain=true;  
		 }
	      if(contain==false)
	     	{
			  String filename=path;  //绝对路径
			  System.out.println("BBBBBBBBBBBBB   "+path);
			  SendFilequeue.add(fileData);//3.将封装的文件放进发送文件列表
			  FileInputStream fis;
      	      long filelength=0;
      	 	  try 
      	 	  {
      	 	  fis = new FileInputStream(filename); 
      	 	  filelength= fis.available();
      	 	  } catch (Exception e)
      	 	  {
      	 	  e.printStackTrace();
      	 	  }
      	 	  file_number++;
      	 	  currentLength=currentLength+filelength;
      	 	  System.out.println("  ");   
      	 	  System.out.println("fileName: "+path);   
      	 	  System.out.println("SendFilequeue Information:"+file_number+" 个 "+currentLength+" Byte");
      		  if(currentLength>=maxQueueLength||file_number>=maxNumber) 
      		  {
      			
      			 synchronized(listenqueue.queueListen_List)
      			  {
      			  for(int i=0;i<listenqueue.queueListen_List.size();i++)
      				 listenqueue.queueListen_List.get(i).cancel();
      			     listenqueue.queueListen_List.clear();
      			  }
      			 Timer queueTimer=new Timer(true);
      			 QueueListenTask queueTask=new QueueListenTask();
      			 queueTimer.schedule(queueTask, FileSharing.sleeptime);
      			 listenqueue.queueListen_List.add(queueTimer);
      			    		    
      			 System.out.println("###队列超过了最大容量或是文件数量达到了最大数量,进行处理");
      			 System.out.println("重新计时："+System.currentTimeMillis()); 
    		    listenqueue.handleQueue(SendFilequeue); 
      		  }	
      		//}
	     }	
	      
  	   } //end 改变的是文件
	 }
	 nonetupdate=false;
	// */
 }
 
/**
 * 同步的触发函数
 * @param v
 */
	/*public void onsync() 
	{
	   requestSyncFunction rsf=new  requestSyncFunction();
	   System.out.println("***发起同步***");	
	   rsf.start();
	}*/
	public static String getAllLocalFiles()
	   {
		    String storedFiles="";
		    File f=new File(FileSharing.sharedPath);
			Stack <String>  stack= new Stack <String>();
			if(f.exists()&&f.isDirectory())
			  stack.push(f.getAbsolutePath());
			while(!stack.empty())
			{
				String filename=stack.pop();
				File file=new File(filename);
				File ff[]=file.listFiles();
				if(ff!=null)
				for(int i=0;i<ff.length;i++)
					if(ff[i].isDirectory()&& !ff[i].getName().equals(".") && !ff[i].getName() .equals(".."))
						stack.push(ff[i].getAbsolutePath());
					else 
						storedFiles+=ff[i].getAbsolutePath()+"|";
			}
			return storedFiles;
	   }
	/**
	 * 全网同步函数
	 */
	/*
	public void onAllsync() 
	{
		DatabaseHelper dbHelper;
		dbHelper = DatabaseHelper.getDatabaseHelper();
	    List<FileMtimeData> fileMtimes=null;
	    long curr_time=0;
	    String totalInfos="";
		String localFiles= getAllLocalFiles();
		String[]lofiles=localFiles.split("|");
		for(int i=0;i<lofiles.length;i++)
		{
			String filename=lofiles[i].trim();
			String [] fileinfo=dbHelper.getFileInfo(filename);
			if(fileinfo!=null)
			    curr_time=Long.parseLong(fileinfo[1]);	
			else
			{
		    	curr_time=System.currentTimeMillis();	
		    	dbHelper.saveFileModifiedTable(filename, curr_time, "no");
			}
			totalInfos+=filename+"*"+Long.toString(curr_time)+"|";
		}
		FileMtimeData fileData=new FileMtimeData(totalInfos,0,curr_time,4); 
		fileMtimes.add(fileData);
		dbHelper.close();
	   System.out.println("***发起全网同步***");	
	   SendServerTime sendtoAll =new SendServerTime(fileMtimes);
	   sendtoAll.start();
	}
   */
public class OtherTask extends java.util.TimerTask
{
	
	 public void run() 
  	   { 
         synchronized(othersFeedpkt)
         {
		   for(int k=0;k<othersFeedpkt.size();k++)
			 {
			 if(System.currentTimeMillis()-othersFeedpkt.get(k).time>3000) //3s后过期
				{
				 othersFeedpkt.remove(k);
				 k--;
				}
			 }
         }
  	   }
   }

public void destroy()
{
 writeLog("sending time:"+total_sending_timer+" ms"+"\r\n");
 writeLog("sending length:"+total_sending_length/1024+" k"+"\r\n");
 writeLog("encode time:"+total_encode_timer+" ms"+"\r\n");

 System.out.println("sending time:"+total_sending_timer+" ms");
 System.out.println("sending length:"+total_sending_length/1024+" k"+"\r\n");
 System.out.println("encode time:"+total_encode_timer+" ms");

 encodedPacket.clear();
 //if(mFileObserver!=null ) 
	 //mFileObserver.stopWatching(); //停止监听
 if(recvHandler!=null)
   recvHandler.getLooper().quit(); 
 synchronized(subfileTimers)
 {
 if( subfileTimers.size()>0)
	 {
	  Iterator it =  subfileTimers.keySet().iterator();
	  while (it.hasNext())
	   {
	   String key=null;
	   key=(String)it.next();
	   subfileTimers.get(key).cancel();  //防止还有计时器在运行
	   }
	  subfileTimers.clear();
	}
 }
 recvInfo.onDestrtoy();
 if(rThread!=null)
 rThread.destroy();
 //if(resp!=null)
 //resp.Destroy();
 if(othertimer!=null)
 othertimer.cancel();
 RecvSubFiles.clear();
 nextseq.clear();
 Feedpkts.clear();
 othersFeedpkt.clear();
 recvFiles.clear();
 //if(sThread!=null)
    //sThread.destroy(); 
 //if(listenqueue!=null)
 //listenqueue.ondestroy();
 SendFilequeue.clear();
 //if(rsf!=null)
  //rsf.destroy();
 try {
	   if(bw!=null)
	   bw.close();
  } catch (IOException e)
  {
	e.printStackTrace();
  }

}
}   