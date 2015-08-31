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
	public static int control_port=40001; //���Ϳ�����Ϣ�˿ںţ����ļ�ɾ��
	public static int sync_port=40004;    //�¼���ڵ�Ҫ��ͬ���Ķ˿ں�
	public static int  FileMtimePort=40005;
	public static final int blocklength=1024;      //ÿ���Ĵ�С1k
	public static final int maxfilelength=102400;    //�ļ����С����Ϊ100k
	public static int sendFileID=0;  //�ļ���id��
	public static String sharedPath="/mnt/sdcard/Seafile";
	public static Map<String,Timer> recvTimers=new HashMap<String,Timer>();
	
	public static Handler myHandler=null;
    public static Handler recvHandler= null;
	public static sendThread sThread=new sendThread();
	public recvThread rThread=null;	
    public respondSync resp=null;

	public FileObserver mFileObserver=null;
	public static BufferedWriter bw=null;
	public static long total_encode_timer=0;  //�����ܹ����ѵ�ʱ��
	public static long total_sending_timer=0; //�����ܹ����ѵ�ʱ��
	public static long total_sending_length=0; 
	public static int stored_blocks=20;  //�洢�����Ŀ�
	public String message=null;
    public OtherTask othertask=null;
    public Timer othertimer=null;
    public int otherPcks_time=2000; //���˷������Ĺ���ʱ�� ms
    public int selfFeedPcks_time=1000; //�յ����Լ��������Ĺ���ʱ�� ms
	public static int sleeptime=3000;  //��ʱ3��֮���ڿ�ʼ��ض��С�
	
	public static ArrayList<String>exitsFiles=new ArrayList<String>();        //��ǰĿ¼�Ѿ����ڵ��ļ��б�
    public static HashMap<String,String>sendFiles=new HashMap<String,String>(); //�����ļ��б�
    public static Map<String,Long>recvFiles=new HashMap<String,Long>();        //�����ļ��б�
    public static Map<String,Integer>nextseq=new HashMap<String,Integer>(); //next packetID
    public static ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //���������ļ��ţ�������
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//�����˵ķ�������Ϣ
	public static Map<String,Timer> feedTimers=new HashMap<String,Timer>();//�յ�������ʱ��ʼ��ʱ
	
	
	
	public static Queue<FileMtimeData > SendFilequeue = new LinkedList<FileMtimeData>();   
	public static listenQueue listenqueue=null;     
	public static ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();//�����յ����ļ���
	public static Map<String,Integer>subFile_nums=new HashMap<String,Integer>(); //�յ���Ӧ�ļ���<10m���Ŀ���
	public static Map<String,Integer>sub_nums=new HashMap<String,Integer>();//����10m���ļ����յ��Ŀ���
	public static Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //�洢�յ��Ķ�Ӧ�ļ��Ŀ��
	public static Map<String,Timer> subfileTimers=new HashMap<String,Timer>();  //ÿ���յ�һ���ļ���ͻ��ʱ
	public static final int block_time=4000;  //�ļ����Ϳ鷴������ʱ��
	
	
	
	//public static final int syn_watie_time=4000;//li,��������ļ�ʱ��ʱ��
	//public static boolean Synced=false;//li,�Ѿ�ͬ���˵ı�־
	public static boolean nonetupdate=false;
	public static boolean netupdate=false;
	public static ArrayList<String>adhocreceive=new ArrayList<String>();//���������ڵ��ж��Ƿ��Ǵ�adhoc���յ���
	public static int ipCount=1;//li
	public static Map<String,ArrayList<String[]>> savedMap=new LinkedHashMap<String,ArrayList<String[]>>();//*0522��������ÿһ�����������
	public static boolean noteType=false;
	public static ArrayList<String> onlyInServer=new ArrayList<String>();//���ڴ��ֻ�з������б���û�е��ļ�
	//public Map<String,Timer> synTableTimers=new HashMap<String,Timer>(); //li,���ڷ���ÿ��IP�ļ�ʱ�� 
	//public Timer synTableTimer=null;
	//public recv_SyncTableTask syntask=null;
	
	//public static String lastsub_id=new String();//��������ҵ������һ���ļ������һ���ID��
	//public static int blockid=0;
	//���������ڵ�Ƚ�ʱ�ҵ��ķ������У��������ڵ�û�е��ļ���������б����Ϊ������󣬷�����Щ�ļ�����Ҫ���͵ģ�Ҫ��
	//�����ļ�������������
	
	
	
	public static long currentLength=0;  //��ǰ�ļ��б����ļ��ĳ���
	public static long  maxQueueLength=1024*10240; //���е��������
	public static int file_number=0;
	public static int maxNumber=20;  //�������������ļ���
	public static int maxStoredLength=1024*10240; //����10m���ļ�����յ��ļ���ֱ�����
	public static fecFuntion fecfunction=new fecFuntion(); //������ֵʵ��һ��fecFuntionʵ����������synchronized���ͣ������ڴ����
	public static ArrayList<Packet[]> encodedPacket=new  ArrayList<Packet[]>(); //������ɵĿ�
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
        total_encode_timer=0;  //�����ܹ����ѵ�ʱ��
    	total_sending_timer=0; //�����ܹ����ѵ�ʱ��
    	total_sending_length=0; 
        System.out.println("�ҵ�IP��ַ�ǣ�"+ipaddress+"\n");
        System.out.println("�ҵĹ㲥��ַ�ǣ�"+bcastaddress+"\n");
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
        		  if(s.equals("�����ļ�"))
        			  System.out.println("�����ļ�" );
        		}  
        		else if(Msg.arg1==1)
        		{
        		    Packet[] recvPacket=(Packet[]) Msg.obj;	  
        		    Packet p=recvPacket[0];
        	        if(p.type==0&&recvPacket.length>=p.data_blocks)
        		    {
        	        	System.out.println( "�յ��������ݰ�\n");	
        		       fecfunction.decode(recvPacket);	  //���ý��뺯��
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
        			 System.out.println("ɾ���ļ�");	
        			 if(f.isDirectory())
        			 {
        				 System.out.println("ɾ��Ŀ¼");	
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
        				 if(Utils.isNetworkOn()) //�Լ��ǿ������ӷ������Ľڵ�
        				 {
        					   //���������ͬ����Ȼ����������ڵ�ͬ�� 
             				   String mess ="�Լ��ǿ������ӷ������Ľڵ��յ�Ҫ��ͬ����Ϣ";
             			       System.out.println(mess); 
             			      BrowserActivity.showSynInfo(mess);
             				 BrowserActivity ba=new  BrowserActivity();
             	    		   map=ba.syncWithServer();//�����ڵ����Ⱥͷ��������б�ͬ��
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
             	    		   String infos1="IP��ַΪ"+BrowserActivity.getIp()+"�������������Լ����ļ���";
             	    		   requestSyncFunction rsf=new  requestSyncFunction(1,map1,infos1);
             	    		   rsf.start();
        				 }
        				 else//�������ڵ�
        				 {
        					  //ֱ�ӽ��Լ���local���ͳ�ȥ
           				      String mess ="�Լ��ǲ������ڵ� �յ�Ҫ��ͬ����Ϣ";
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
           				      String infos1="IP��ַΪ"+BrowserActivity.getIp()+"�������������Լ����ļ���";
           				      System.out.println(infos1); 
           				      requestSyncFunction rsf=new requestSyncFunction(1,map1,infos1);  
           				      rsf.start();
        				 }
        				 break;
        			 case 2:
        				//type=2��ʱ���յ��Ƿ���ͬ���������õı�
        				 System.out.println("******** �ڵ���յ���savedMap--"+syn.map);
    				     if(syn.map==null)//�������������������Ϊnull����Ϊ
    				     {
    					     System.out.println("û��Ҫͬ������Ϣ");
    				    	 String mess ="û��Ҫͬ������Ϣ";
    					     BrowserActivity.showSynInfo(mess);
    					     //û��Ҫͬ������Ϣ��Ӧ�ý����еı�־�ָ�
    				     }
    				     else
    				     {
        				      System.out.println("@@@@@@@�ڵ��յ������settlie�� ");
    					      FileSharing.savedMap=syn.map;//���յ���map�Ž���̬������Ϊ���Ժ�ʹ��
    					      Iterator it = syn.map.keySet().iterator(); 
    				          while(it.hasNext())
    				          {
    					          String key; 
    					          ArrayList<String[]> value=new ArrayList<String[]>();
    			    	          key=(String)it.next(); 
    			    	          System.out.println("�ڵ��յ������settle���ҵ����е�һ��IP "+key);
    			    	          if(BrowserActivity.getIp().equals(key))//�������ͬ������ǵ�һ���Ļ������ȷ�����Ҫ���͵��ļ�
    			    	          {
    			    	        	  System.out.println("@@@@@@ ���ڵ��ǵ�һ�����͵Ľڵ�-- "+BrowserActivity.getIp());
    			    	        	  System.out.println("respond----if �Ƿ�Ϊ��һ�� ");
    			    	        	   if(Utils.isNetworkOn())//����ǿ��������Ľ��
    			    		            {
    				    	    		    value=FileSharing.savedMap.get(key);
    					    		        for(int i=0;i<value.size();i++)
    					    		        {    
    					    			        String path=new String();
    					    			        String time=new String();
    					    		            path=value.get(i)[0]; 
    					    		            time=value.get(i)[1];
    					    		            Long curr_time=new Long(time);
    					    		            //�ǵ�һ������Ҫ�ж��������ǲ������ڵ㣬����ǵĻ����ж���Ҫͬ�����ļ������з������б���û�е�
    					    		            //Ҫ�ӷ���������
    					    		            if(FileSharing.onlyInServer.contains(path))//Ҫ�ӷ������ҵ������ص�
    					    		            {
    					    		            	if(i==(value.size()-1))
    					    		            	{
    					    		            		//���Ҫ���ص�����ļ��������һ���ļ��Ļ���������
    					    		            		BrowserActivity.downIsLast=true;
    					    		            		
    					    		            	}
    					    		            	//������ļ��ӷ��������أ������ͳ�ȥ
    					    		            	//Ҫ�ӷ��������أ��������߳��в��ܶ�UI�̵߳Ĳ���������
    					    		            	 Message msg = new Message();
    				                                 msg .obj = path;
    				                                 msg .arg1=4;
    				                                 FileSharing.myHandler.sendMessage(msg);
    				                                 System.out.println("�����ڵ�ͬ��---�ӷ����������ļ�ʱ��path��·��"+path);
    					    		            }
    					    		            else//�����е�
    					    		            {
    					    		            	 if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
    							    		            {
    							    			             FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
    							    		            }
    							    		            else
    							    		            {
    							    		            	FileSharing.addToQueue(path, curr_time, false,false);
    							    		            }
    					    		            }
    					    		         }//for��Ҫ���͵�ÿһ���ļ���ÿһ���ļ���onlyInServer�Ա����onlyInServerӦ�����
    					    		        FileSharing.onlyInServer.clear();
  					    		            BrowserActivity.reposInfo.clear();
    			    		            }
    			    	          else
    			    	          {
    			    	        	  System.out.println("�ڵ��յ������settle���ж��Լ��Ƿ�Ϊ������ ");
    			    		           value=syn.map.get(key);//�����key��IP
    			    		           for(int i=0;i<value.size();i++)
    			    		           {     
    			    			            String path=new String();
    			    			            String time=new String();
    			    		                path=value.get(i)[0]; 
    			    		                time=value.get(i)[1];
    			    		                Long curr_time=new Long(time);
    			    		                System.out.println("case  value.size()�� "+value.size());
    			    		                if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
    			    		                {
    			    		                	  System.out.println("case 2 �����һ���ļ� " );
    			    			                 FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
    			    			                 //���Լ�Ҫ���͵��ļ�������Ϻ󣬷���һ��������Ϣ��end������һ��Ҫ���͵�IP���������Ϣ�㲥��ȥ
    			    			                 //����յ�����㲥��Ϣ�󣬽�IPȡ�����Լ��ĵ�IP�Աȣ������ͬ��˵�����ǵڶ������Դ�����
    			    		                 }
    			    		                else
    			    		                {
    			    		                	  System.out.println("case 2 �������һ���ļ� ");
    			    		                      FileSharing.addToQueue(path, curr_time, false,false);
    			    		                 }
    			    		            
    			    		            }
    			    	          }
    			    	        }
    			    	        break;
    				           }
    				        //�����еĵ�һ����¼ȡ�����жϺ��Լ���IP�Ƿ���ȣ����˵�����IP�ǵ�һ��Ҫ���͵ģ����ҵ���һ����¼��IP
    		    				 //��һ��IP���Լ�Ҫ���͵��ļ����ηŽ����ķ��ͳ����У������������һ��IP�����Լ����ļ�
    		        				 
    				      }
    				     break;
        			 case 3:
        				//�յ�type����3��������յ����Ǵӵ�һ������IP��ʼ���ļ��������֪ͨ���㲥����ȥ���յ��Ľڵ����ip
    					 //���ַ����е�ip��ͬ��˵��������һ��Ҫ���͵�ip����ʼ����
    					   //FileSharing.savedMap=syn.map;//���յ���map�Ž���̬������Ϊ���Ժ�ʹ�ã�������������������Ծ�̬������ʹ�ã���������������    
        				     String nextip=syn.information;
        				      System.out.println("�ڵ��յ�nextIP��Ϣ��  FileSharing��case 3 ���д��� "+nextip );
					           if(BrowserActivity.getIp().equals(nextip))
					           {  
					        	   System.out.println("@@@@ ���ڵ��յ�nextIP�󣬷������Լ���IPһ�£����ڵ㿪ʼͬ���ļ�" );
					        	   synchronized(FileSharing.savedMap)
					               {
					        	     ArrayList<String[]> value=new ArrayList<String[]>();
					                 value=FileSharing.savedMap.get(nextip);
					        	    if(Utils.isNetworkOn())//����ǿ��������Ľ��
			    		             {
					    		        for(int i=0;i<value.size();i++)
					    		        {    
					    			        String path=new String();
					    			        String time=new String();
					    		            path=value.get(i)[0]; 
					    		            time=value.get(i)[1];
					    		            Long curr_time=new Long(time);
					    		            //�ǵ�һ������Ҫ�ж��������ǲ������ڵ㣬����ǵĻ����ж���Ҫͬ�����ļ������з������б���û�е�
					    		            //Ҫ�ӷ���������
					    		            if(FileSharing.onlyInServer.contains(path))//������ļ��Ƿ������ϲ��е��ļ�
					    		            {
					    		            	if(i==(value.size()-1))
					    		            	{
					    		            		//���Ҫ���ص�����ļ��������һ���ļ��Ļ���������
					    		            		BrowserActivity.downIsLast=true;
					    		            	}
					    		            	//������ļ��ӷ��������أ������ͳ�ȥ
					    		            	 sync_download(path);
					    		            }
					    		            else//�����е�
					    		            {
					    		            	 if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
							    		            {
							    			             FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
							    			             //���Լ�Ҫ���͵��ļ�������Ϻ󣬷���һ��������Ϣ��end������һ��Ҫ���͵�IP���������Ϣ�㲥��ȥ
							    			             //����յ�����㲥��Ϣ�󣬽�IPȡ�����Լ��ĵ�IP�Աȣ������ͬ��˵�����ǵڶ������Դ�����
							    			             //�������ļ������һ���Ļ��������һ���ļ�������Ҫ����end+nextIP
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
				    	    	   else//�����������Ļ�
				    	    	   {
				    	    		  
					    		        for(int i=0;i<value.size();i++)
					    		        {    
					    			        String path=new String();
					    			        String time=new String();
					    		            path=value.get(i)[0]; 
					    		            time=value.get(i)[1];
					    		            Long curr_time=new Long(time);
					    		            if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
					    		            {
					    			             FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
					    			             //���Լ�Ҫ���͵��ļ�������Ϻ󣬷���һ��������Ϣ��end������һ��Ҫ���͵�IP���������Ϣ�㲥��ȥ
					    			             //����յ�����㲥��Ϣ�󣬽�IPȡ�����Լ��ĵ�IP�Աȣ������ͬ��˵�����ǵڶ������Դ�����
					    			             //�������ļ������һ���Ļ��������һ���ļ�������Ҫ����end+nextIP
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
        			 }//switch����
        				 
       				 
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
     
       //��ʱ���±��˵ķ������б�
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,otherPcks_time,otherPcks_time);
   }
    /**
     * ȫ��ͬ��ʱ�������Ҫ�ӷ����������ļ�ʱ���õĺ���
     * @param path
     */
    public void sync_download(String path)
    {
    	 System.out.println("   ");
    	 System.out.println("������������������������������       ");
   	     System.out.println("sync_download�д��ݵĲ�����   "+path);
   	     String[]infos1=path.split("/");
    	 String reponame=infos1[5];
		 System.out.println("�ֿ�Ŀ¼�����ǣ�   "+reponame);
		 String repoid=null;
		 String dirpath="";
		 String filename=null;
		 System.out.println("BrowserActivity.reposInfo.size()"+BrowserActivity.reposInfo.size());
		/*
			Iterator it=BrowserActivity.reposInfo.keySet().iterator();
			while(it.hasNext())
			 {
				 System.out.println("BrowserActivity.reposInfo���м�¼��--filepath: "+it.next());
			 }
			 */
		 if(BrowserActivity.reposInfo.containsKey(path.trim()))
		 {
			 repoid=BrowserActivity.reposInfo.get(path);
			 String[]infos=path.split("/");
			// System.out.println("infos.length:  "+infos.length);
			 for(int i=6;i<infos.length;i++)
			   dirpath+="/"+infos[i];
			 System.out.println("�����ļ����ڵĲֿ�id��  "+"repoid�� "+repoid); 
			 System.out.println("�����ļ����ڵĲֿ�naem��  "+" repo_name:"+ reponame); 
			 System.out.println("�����ļ����ڵĲֿ�dirpath��  "+" dirpath: "+dirpath);
		     BrowserActivity.txService.addDownloadTask(BrowserActivity.account,reponame, repoid, dirpath);
		
		 }
		 //�����ļ�
		
    }
    
    /*
     * ��ʼ���������ļ�
     */
    public void MonitorDirentsAndfiles ()
    {
        //Ҫ�ȴ���һ���ļ���
        File SharedDir=new File(sharedPath);
        if (!SharedDir.exists())
        {
        	SharedDir.mkdirs();
        }
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //��ʼ����
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
  		           recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
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
 		       key=(String)it.next();  //��һ�ε���Iterator��next()����ʱ�����������еĵ�һ��Ԫ��
 	            if(sendFiles.get(key).equals(path)) 
 	             {
 			        sendFiles.remove(key); 
 			        synchronized(nextseq)
   		            {
 			           nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
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
     * �����յ��ķ������������鶪ʧ�������͵����İ���ʧ������
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
	        	 System.out.println("��ʼ��ʱ��"+fb.sub_fileID+"\n");
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
	   System.out.println("�յ������� fb.type="+fb.type);
   }
   /**
    * ɾ���ļ�ʱ��˳��ɾ����صļ�¼�������ļ��б������ļ��б�ȣ�
    * @param path
    */
   public void deleteRecord(String path)
   {
	   boolean issend=false;
	   boolean issending=false;
	   boolean isexits=false;
	   System.out.println("ɾ����صļ�¼"); 
	   synchronized(FileSharing.recvFiles)
		{
		   if(FileSharing.recvFiles.containsKey(path))
		    {
		    	FileSharing.recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
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
	             key=(String)it.next();  //��һ�ε���Iterator��next()����ʱ�����������еĵ�һ��Ԫ��
                 if(FileSharing.sendFiles.get(key).equals(path)) 
                  {
    	              issending=true;
    	              FileSharing.sendFiles.remove(key); 
		              synchronized(FileSharing.nextseq)
		              {
			             FileSharing.nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
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
   * ����ļ�Ŀ¼����
   * @author PC
   *
   */
   public class SDCardFileObserver extends FileObserver //��һ���߳�
    {
       //mask:ָ��Ҫ�������¼����ͣ�Ĭ��ΪFileObserver.ALL_EVENTS
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
      System.out.println("��ǰ������Ŀ¼��"+directory);
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
       public void onEvent(int event, String path) //��ʱ��path���Ǿ���·����
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
           case FileObserver.MODIFY:  //�ļ����ݱ��޸�ʱ��������ճ���ļ���
        	   System.out.println("----"+path + " MODIFY");
        	   break;
           case FileObserver.CLOSE_WRITE:  //�༭�ļ��󣬹ر�
       	       System.out.println("----"+path + " CLOSE_WRITE");
       	       synchronized(recvFiles)
       	       {
       	         if(! recvFiles.containsKey(path))
       	        { 
       	    	 System.out.println("�ж�recvFiles��ֵ��"+recvFiles.containsKey(path));
       	    	  addFileModified(path,curr_time);	//�Ž��ļ��б�����Ҫ�Ľ��ģ���Ϊ�Ž�ȥ��ʱ�������ǰ������ļ���ʱ��ɵģ�Ҫɾ��
       	    	//���ڲ���
       	       Map<String,Long> map=new HashMap<String,Long>();
     		   map=getFileModified();
     		   System.out.println("��ʾlocal���е�����@@@@");
     		   BrowserActivity.show(map);
     		 //���ڲ���
       	    //FileSharing.writeLog("�����ļ�Ҳ���������������");
       	      //��������������õ���seafile�ļ�أ������յ��ļ������ز��������ڲ������ڵ����ļ�����Ҳ��ص���
        	   addToQueue(path,curr_time,false,false);}
       	       }
        	   break;
           case FileObserver.MOVED_TO:
      		    System.out.println("----"+path + " MOVED_TO");
      			addFileModified(path,curr_time);	//�Ž��ļ��б�����Ҫ�Ľ��ģ���Ϊ�Ž�ȥ��ʱ�������ǰ������ļ���ʱ��ɵģ�Ҫɾ��
   	          //���ڲ���
        	   Map<String,Long> map=new HashMap<String,Long>();
      		   map=getFileModified();
      		   System.out.println("��ʾlocal���е�����@@@@");
      		   BrowserActivity.show(map);
      		 //���ڲ���
      		    nonetupdate=true;
      		    addToQueue(path,curr_time,false,false);
              	break;
           case FileObserver.DELETE:
      		    System.out.println("----"+path + " DELETE");
      		//    deleteFileModified(path); //�����ݿ�ɾ���ļ������Ϣ
      		    File file=new File(path);
        	    if(mObservers != null)
        	    {
        	    	synchronized(mObservers)
        	    	{
        	    	for(int i=0;i<mObservers.size();i++)
        	    	{
        	    		if(mObservers.get(i).mPath.equals(path))
        	    		{
        	    			System.out.println("ֹͣ������Ŀ¼");
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
    * ���Ŀ¼�����½���Ŀ¼
    * @param path
    */
   public synchronized void handleDirectory(String path)
   {
	   synchronized(recvFiles)
		 {
	   if(!recvFiles.containsKey(path)) //���յĵ��ļ����ٷ���
	  	{
		 String filepath=path;  //Ҫ��������Ŀ¼
		 System.out.println("��ʼ�������ļ���"+filepath);   
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
		 //���յ��ļ����ͷ�������
		   
	   }
	}  
   }
  }
   /**
    * ��local�в�������
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
	 // dbHelper.close();//�������ݿ��Ѿ��ر���
   }
   /**
    * ��local�ж�������
    * 
    */
   public static Map getFileModified()
   {
	   ArrayList<String[]> filelist=new ArrayList<String[]>();
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper();
	   filelist=dbHelper.getFileFromModifiedTable();
	   //System.out.println("��local�ж������ݳɹ�");
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
    * ��sync���ж�������
    * �����ڵ����syncȫ��ͬ������ȫ���ļ���Ϣɸѡ�׶��õ��ı�
    */
   public static  ArrayList<String[]> getSyncTable()
   {
	   ArrayList<String[]> filelist=new ArrayList<String[]>();
	   DatabaseHelper dbHelper;
	   dbHelper = DatabaseHelper.getDatabaseHelper();
	   filelist=dbHelper.getSynTable();
	   //System.out.println("��ȫ��ɸѡ�ļ���Ϣ��sync���ж������ݳɹ�");
	   return filelist;
  	  
   }
   /**
    * ��ʾsync���е�����
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
		   System.out.println("��ʾsync������ݣ�  �ļ�����"+filelist.get(i)[0]+"     ʱ�䣺"+filelist.get(i)[1]+"IP��"+filelist.get(i)[2]);
	       }
	   }
   }
   /**
    * ���sync���е�����
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
    * ���ļ����뵽���Ͷ��У�׼������
    * @param path
    * @param directSend��Ϊtrue�����ÿ����Ƿ����յ��ĵ�һЩ������ֱ�ӷ���
    */
 public static synchronized void addToQueue(String path,long curr_time,boolean directSend,boolean islast)//*0522
 {
	 System.out.println("^^^^^^^����addToQueue����  "+path);
	// /*
	 FileMtimeData fileData=new FileMtimeData(path,curr_time,islast);//2.��Ҫ���͵��ļ���װ����0522
	 System.out.println("fileData.filename::    "+fileData.filename);
	 synchronized(recvFiles)
	 { 
		 boolean contain=false;
		 String file_id="";
		synchronized(SendFilequeue)
		{ 
		 if(directSend==false)
		 { //����������⣬����һ���ڵ��adhoc�յ�һ���ļ�����Ž�recvFiles�У�������ڵ���º󲻻ᷢ�ͳ�ȥ
			 //�յ������ļ���Ž�recvFiles����
	       if((!recvFiles.containsKey(path)&&!SendFilequeue.contains(fileData))||nonetupdate||netupdate)  //���յĵ��ļ����ٷ��ͣ����Ͷ����������У�������ӣ��ڴ�����к󣬾ͻ���Ϊ��
  	        {	
	    	   System.out.println("AAAAAAAAAAAAAAAAA    "+path);
	    	   contain=false;
	    	   
  	        }
	       else 
	         contain=true; 
		 }  
		 else if(SendFilequeue.contains(fileData)) //��directSend==true,�����������ж�
		 {
			 contain=true;  
		 }
	      if(contain==false)
	     	{
			  String filename=path;  //����·��
			  System.out.println("BBBBBBBBBBBBB   "+path);
			  SendFilequeue.add(fileData);//3.����װ���ļ��Ž������ļ��б�
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
      	 	  System.out.println("SendFilequeue Information:"+file_number+" �� "+currentLength+" Byte");
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
      			    		    
      			 System.out.println("###���г�����������������ļ������ﵽ���������,���д���");
      			 System.out.println("���¼�ʱ��"+System.currentTimeMillis()); 
    		    listenqueue.handleQueue(SendFilequeue); 
      		  }	
      		//}
	     }	
	      
  	   } //end �ı�����ļ�
	 }
	 nonetupdate=false;
	// */
 }
 
/**
 * ͬ���Ĵ�������
 * @param v
 */
	/*public void onsync() 
	{
	   requestSyncFunction rsf=new  requestSyncFunction();
	   System.out.println("***����ͬ��***");	
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
	 * ȫ��ͬ������
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
	   System.out.println("***����ȫ��ͬ��***");	
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
			 if(System.currentTimeMillis()-othersFeedpkt.get(k).time>3000) //3s�����
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
	 //mFileObserver.stopWatching(); //ֹͣ����
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
	   subfileTimers.get(key).cancel();  //��ֹ���м�ʱ��������
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