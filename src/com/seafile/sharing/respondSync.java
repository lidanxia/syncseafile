package com.seafile.sharing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;

import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.Utils;
import com.seafile.sharing.recvThread.recvTask;

import android.os.Message;

//接收同步信息
public class respondSync extends Thread
{
	public String recvIP=null;
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public byte[]messages=null;
	//public String filenames =null;
	public boolean running=true;
	private String localIP="";
	public int port=0;
	public recv_SyncTableTask syntask=null;
	public Timer synTableTimer=null;

	public int COUNT=1;
	int cc=1;
	//public ArrayList<String> duplist=new ArrayList<String>();
	
	respondSync(String localIP,int port)
	{
		
		this.localIP=localIP;
		this.port=port;
	}
	public void init()
	{
		try {
			socket= new DatagramSocket(null); 
			socket.setReuseAddress(true); 
			socket.bind(new InetSocketAddress(FileSharing.sync_port)); 
			socket.setBroadcast(true);
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
		
	}
	@Override
	public void run() 
	{
		 init();

	  while(running)
	  {
		 messages=new byte[1024];
		 packet = new DatagramPacket(messages, messages.length);
		 try {
			socket.receive(packet);
			System.out.println("收到packet ");
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		 recvIP=packet.getAddress().toString();
		 String[]rr=recvIP.split("/");
		 recvIP=rr[1];
		 System.out.println("recvIP:  "+recvIP);
		 if (!recvIP.equals(localIP)&& packet.getData().length != 0)//  接收不为空，且不是自己的包  
		 {
			 SynPacket syn=null;//定义变量都在try{}外面定义,出了{}就看不到了
			 try
			 {
			       ByteArrayInputStream ba=new ByteArrayInputStream(packet.getData());
			       ObjectInputStream oi=new ObjectInputStream(ba);
			       syn=(SynPacket)oi.readObject();
			       ba.close();
			       oi.close();
			 }catch(Exception e)
			 {
				   System.out.println(e.toString());
				    e.printStackTrace();
			 }
			 
			 System.out.println(" SynPacket syn "+syn);
			 System.out.println(" SynPacket syn.type "+syn.type);
		     if(syn.type==1) //收到其他节点发来的表，因为是广播发送，普通节点也会收到，所以这里要判断节点类型是true的节点，才将这 	        //表收下来，普通节点收到什么也不做，
			 {
		    	 if(FileSharing.noteType==true)//只有节点类型是发起节点的才收别人发来的表
				    {
		    			    //不相等的话，放进sync表
		    			    int syn_watie_time=4000;//li,等其他表的计时器时间
		    			 synchronized(FileSharing.recvTimers)
		    			 {
		    			    if(FileSharing.recvTimers.containsKey(recvIP))
		    			    {
		    			    	FileSharing.recvTimers.get(recvIP).cancel();
		    			    	FileSharing.recvTimers.remove(recvIP);
		    			    	
		    			    	syntask=new recv_SyncTableTask(recvIP);
		    			        synTableTimer = new Timer(true);
								synTableTimer.schedule(syntask,syn_watie_time);
								FileSharing.recvTimers.put(recvIP, synTableTimer);
		    			    	System.out.println("*******收到相同的IP，recvIP——"+recvIP);
		    			    }
		    			    else
		    			    {
		    			    	 Iterator it=FileSharing.recvTimers.keySet().iterator();
		    			    	 while(it.hasNext())
		    			    	 {
		    			    		String key=(String)it.next(); 
		    			    		FileSharing.recvTimers.get(key).cancel();
		    			    	 }
		    			    	 FileSharing.recvTimers.clear();
		    			    	 syntask=new recv_SyncTableTask(recvIP);
						    	 synTableTimer = new Timer(true);
						    	 System.out.println("*********没有收到相同的IP,加载计时器    第   "+cc+" 次");
						    	 cc++;
								 synTableTimer.schedule(syntask,syn_watie_time);
								 FileSharing.recvTimers.put(recvIP, synTableTimer);
								 
								  	FileSharing.ipCount=FileSharing.ipCount+1;
							        //添加计时器
							        //将收到的表放进sync表里
							        Map<String, Long> map1=new HashMap<String, Long>();
							        Iterator it1 = syn.map.keySet().iterator(); 
			     				      while (it1.hasNext())
			     				      { 
			     				    	 String key; 
			     			    	     key=(String)it1.next(); 
			     			    	     ArrayList<String[]> value=new ArrayList<String[]>();
			     			    	     value=syn.map.get(key);
			     			    	     for(int i=0;i<value.size();i++)
			     			    	     {
			     			    	    	 String filename=value.get(i)[0];
			     			    	    	 Long time=Long.parseLong(value.get(i)[1]);
			     			    	    	 map1.put(filename,time);
			     			    	     }
			     				      }
							        BrowserActivity.copyToSynTable(map1,recvIP); 
		    			    }
		    		   }
				    }
			 }
		     else
		     {
		    	   Message msg = new Message();
		           msg .obj = syn;
		           msg .arg1=3;
		           FileSharing.myHandler.sendMessage(msg);	
		     }
		 }
		 
	 }
	  
	}
	
	public class recv_SyncTableTask extends java.util.TimerTask
	{
		public String recvIP=null;
		recv_SyncTableTask(String recvip)
		{
			this.recvIP=recvip;
		}
		
		 public void run()
		 {
			 synchronized(FileSharing.recvTimers)
			 {
			   Iterator itt=FileSharing.recvTimers.keySet().iterator();
	    	   while(itt.hasNext())
	    	   {
	    		String key=(String)itt.next(); 
	    		FileSharing.recvTimers.get(key).cancel();
	    	   }
	    	   FileSharing.recvTimers.clear();
			 }
			   System.out.println("第   "+COUNT+"  次调用计时器超时函数,超时的IP是 ： "+recvIP);
			    COUNT++;
			 //应该在同步完成后再变回来，也就是说在结点正在同步的时候，别人发起同步
			 //或者当自己刚按了同步，正在发送文件的时候，又按了同步，那么又会发送数据
			 //也就是说不管谁发起同步，只要同步没有完成，接收方或者发起方，都不能按再按同步按钮了，
			   FileSharing.noteType=false;//当计时器到后，这个结点表为普通结点，对迟到的表不再接收
			   System.out.println("@@@@@@ 参与同步的节点数目："+FileSharing.ipCount);
			   //显示一下当前放在sync表里的内容
			   System.out.println("显示计时器到时后同步结点最终收到的表");
			   FileSharing.showSync();
			   System.out.println("以上是计时器到时后，sync表的内容");
			 
			 
			 if(FileSharing.getSyncTable()!=null)//sync不为空的话，普通节点没有所以不整理
			 {
				 synchronized(FileSharing.savedMap)
				 {
				 FileSharing.savedMap=BrowserActivity.settleSynTable4();//整理sync表 saveMap中放的就是settle4中的整理结果
				 if(FileSharing.savedMap!=null)
				 {
					 System.out.println("***************************");
					 System.out.println("******** 显示settle4的最终整理结果");
					 BrowserActivity.showSettle4(FileSharing.savedMap);//显示settle4的结果，也就是整理后的结果
				
				
				//在这里将同步的节点数量改为初始值1，因为不在收表了 
				   FileSharing.ipCount=1;
				 //sync表整理好后，也就是用完了，清空
				 FileSharing.clearSynTable();
				 //将最终整理好的map发送出去
				 String infos="！！！！发起同步IP为："+BrowserActivity.getIp()+"的节点发送了整理表";
				 requestSyncFunction rsf=new  requestSyncFunction(2,FileSharing.savedMap,infos);
				 rsf.start();
				 
				 System.out.println("******** FileSharing.savedMap--"+FileSharing.savedMap);
				if(FileSharing.savedMap!=null)
				{
				Iterator it = FileSharing.savedMap.keySet().iterator(); 
				 while(it.hasNext())
			     {
				       
			    	   String key; 
				       ArrayList<String[]> value=new ArrayList<String[]>();
		    	       key=(String)it.next(); 
		    	       if(BrowserActivity.getIp().equals(key))//如果发起同步结点是第一个的话，首先发送他要发送的文件
		    	       {
		    	    	   System.out.println("@@@@@@ 本节点是第一个发送的节点-- "+BrowserActivity.getIp());
		    	    	   if(Utils.isNetworkOn())//如果是可以上网的结点
	    		            {
		    	    		   
		    	    		    value=FileSharing.savedMap.get(key);
		    	    		    System.out.println("@@@@@@ value.size() "+value.size());
			    		        for(int i=0;i<value.size();i++)
			    		        {    
			    			        String path=new String();
			    			        String time=new String();
			    		            path=value.get(i)[0]; 
			    		            time=value.get(i)[1];
			    		            Long curr_time=new Long(time);
			    		            System.out.println("@@@@@@ 整理完表，发送时path "+path);			    		            //是第一个，还要判断这个结点是不联网节点，如果是的话，判断他要同步的文件里，如果有服务器有本地没有的
			    		            //要从服务器下载
			    		            if(FileSharing.onlyInServer.contains(path))//要从服务器找到并下载的
			    		            {
			    		            	if(i==(value.size()-1))
			    		            	{
			    		            		//如果要下载的这个文件还是最后一个文件的话，记下来,
			    		            		BrowserActivity.downIsLast=true;
			    		            			
			    		            	}
			    		            	//将这个文件从服务器下载，并发送出去
			    		            	//要从服务器下载，这里子线程中不能对UI线程的操作，所以
			    		            	System.out.println("FileSharing.onlyInServer.contains(path) "+path);
			    		            	 Message msg = new Message();
		                                 msg .obj = path;
		                                 msg .arg1=4;
		                                 FileSharing.myHandler.sendMessage(msg);	 
			    		            	
			    		            }
			    		            else//本地有的
			    		            {
			    		            	     if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
					    		            {
			    		            		     System.out.println("@@@@@@ 此文件是最后一个文件，发送时path "+path);
					    			             FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
					    		            }
					    		            else
					    		            {
					    		            	  System.out.println("@@@@@@ 本地节点有的表，发送时path "+path);
					    		                  FileSharing.addToQueue(path, curr_time, false,false);
					    		            }
			    		            }
			    		         }//for找要发送的每一个文件，每一个文件和onlyInServer对比完后，onlyInServer应该清空
			    		        FileSharing.onlyInServer.clear();
			    		          BrowserActivity.reposInfo.clear();
	    		            }
		    	    	   else//不可以上网的话
		    	    	   {
		    	    		   value=FileSharing.savedMap.get(key);
			    		        for(int i=0;i<value.size();i++)
			    		        {    
			    			        String path=new String();
			    			        String time=new String();
			    		            path=value.get(i)[0]; 
			    		            time=value.get(i)[1];
			    		            Long curr_time=new Long(time);
			    		            if(i==(value.size()-1))//如果此文件是最后一个的话，在addToQueue中加最后一个标志，此标志的值为true
			    		            {
			    		            	System.out.println("@@@@@@ i==(value.size()-1)  path "+path);
			    			             FileSharing.addToQueue(path, curr_time, false,true);//后期可以将addToQueue的long类型改为String
			    			             //将自己要发送的文件发送完毕后，发送一个控制信息，end加上下一个要发送的IP，将这个消息广播出去
			    			             //结点收到这个广播消息后，将IP取出和自己的的IP对比，如果相同，说明他是第二个，以此类推
			    			             //如果这个文件是最后一个的话，在最后一个文件发送完要发送end+nextIP
			    		            }
			    		            else
			    		            {
			    		            	System.out.println("@@@@@@  不可以上网 else  path "+path);
			    		                 FileSharing.addToQueue(path, curr_time, false,false);
			    		            }
			    		         }
		    	    	   }
		    	       }
		    	        break;//只找第一个
			     } //结束WHILE
				}
				 } //if
				 else
				 {
					 //savedmao如果为空的话，就没必要把这个空值穿过去了，而是广播一个消息，没有要同步的内容，同步结束，为以后准备
					 //0530今天就什么也不穿了为了程序的调试
					 //savedmap如果为空的话，说明没有要同步的信息，所以同步就结束了,将用来统计个数的归1；放收数据的也归1，可能大家都一样，不需要同步
					 //String mess ="没有要同步的信息";
				     //BrowserActivity.showSynInfo(mess);
					 //这里就不显示了，以后要改成当一次同步都结束后，也就是最后一个结点发完后，发送一个广播，收到这个同步结束的广播后
					 //,不管是发起结点还是普通结点，才可以按同步按钮，否则一次同步没有结束，再一次同步，同步的过程中收到上一次同步
					 //的文件，还是没同步，那么做法是：主动同步结点按了同步按钮后，就将这个按钮置为灰色的不可用，收到最后一个普通结点
					 //发来的同步结束通知，按钮才激活；对于普通结点也是可以设计为收到别人发起同步的通知后，这里设为俩个按钮，一个叫
					 //发起同步，一个叫接受同步；如果你按了接受同步那妞，那么你的发起同步按钮就变成灰色的了，不可用，因为你接受了同步
					 //这次同步没有完的话，你就不可以发起同步，当收到最后一个结点发完的广播后，才将发起同步按钮启用，也就是说你发现发起
					 //同步按钮不可用的话是在同步过程中
				   //在这里将同步的节点数量改为初始值1，因为不在收表了 
					   FileSharing.ipCount=1;
					 //sync表整理好后，也就是用完了，清空
					 FileSharing.clearSynTable();
				 }
				}//synchronized
			 }
		 }
	}
	
	
	public void onDestrtoy()
	{
		running=false;
		socket.close();
	}
}
