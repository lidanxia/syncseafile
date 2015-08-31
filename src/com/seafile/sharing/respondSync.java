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

//����ͬ����Ϣ
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
			System.out.println("�յ�packet ");
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		 recvIP=packet.getAddress().toString();
		 String[]rr=recvIP.split("/");
		 recvIP=rr[1];
		 System.out.println("recvIP:  "+recvIP);
		 if (!recvIP.equals(localIP)&& packet.getData().length != 0)//  ���ղ�Ϊ�գ��Ҳ����Լ��İ�  
		 {
			 SynPacket syn=null;//�����������try{}���涨��,����{}�Ϳ�������
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
		     if(syn.type==1) //�յ������ڵ㷢���ı���Ϊ�ǹ㲥���ͣ���ͨ�ڵ�Ҳ���յ�����������Ҫ�жϽڵ�������true�Ľڵ㣬�Ž��� 	        //������������ͨ�ڵ��յ�ʲôҲ������
			 {
		    	 if(FileSharing.noteType==true)//ֻ�нڵ������Ƿ���ڵ�Ĳ��ձ��˷����ı�
				    {
		    			    //����ȵĻ����Ž�sync��
		    			    int syn_watie_time=4000;//li,��������ļ�ʱ��ʱ��
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
		    			    	System.out.println("*******�յ���ͬ��IP��recvIP����"+recvIP);
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
						    	 System.out.println("*********û���յ���ͬ��IP,���ؼ�ʱ��    ��   "+cc+" ��");
						    	 cc++;
								 synTableTimer.schedule(syntask,syn_watie_time);
								 FileSharing.recvTimers.put(recvIP, synTableTimer);
								 
								  	FileSharing.ipCount=FileSharing.ipCount+1;
							        //��Ӽ�ʱ��
							        //���յ��ı�Ž�sync����
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
			   System.out.println("��   "+COUNT+"  �ε��ü�ʱ����ʱ����,��ʱ��IP�� �� "+recvIP);
			    COUNT++;
			 //Ӧ����ͬ����ɺ��ٱ������Ҳ����˵�ڽ������ͬ����ʱ�򣬱��˷���ͬ��
			 //���ߵ��Լ��հ���ͬ�������ڷ����ļ���ʱ���ְ���ͬ������ô�ֻᷢ������
			 //Ҳ����˵����˭����ͬ����ֻҪͬ��û����ɣ����շ����߷��𷽣������ܰ��ٰ�ͬ����ť�ˣ�
			   FileSharing.noteType=false;//����ʱ�������������Ϊ��ͨ��㣬�Գٵ��ı��ٽ���
			   System.out.println("@@@@@@ ����ͬ���Ľڵ���Ŀ��"+FileSharing.ipCount);
			   //��ʾһ�µ�ǰ����sync���������
			   System.out.println("��ʾ��ʱ����ʱ��ͬ����������յ��ı�");
			   FileSharing.showSync();
			   System.out.println("�����Ǽ�ʱ����ʱ��sync�������");
			 
			 
			 if(FileSharing.getSyncTable()!=null)//sync��Ϊ�յĻ�����ͨ�ڵ�û�����Բ�����
			 {
				 synchronized(FileSharing.savedMap)
				 {
				 FileSharing.savedMap=BrowserActivity.settleSynTable4();//����sync�� saveMap�зŵľ���settle4�е�������
				 if(FileSharing.savedMap!=null)
				 {
					 System.out.println("***************************");
					 System.out.println("******** ��ʾsettle4������������");
					 BrowserActivity.showSettle4(FileSharing.savedMap);//��ʾsettle4�Ľ����Ҳ���������Ľ��
				
				
				//�����ｫͬ���Ľڵ�������Ϊ��ʼֵ1����Ϊ�����ձ��� 
				   FileSharing.ipCount=1;
				 //sync������ú�Ҳ���������ˣ����
				 FileSharing.clearSynTable();
				 //����������õ�map���ͳ�ȥ
				 String infos="������������ͬ��IPΪ��"+BrowserActivity.getIp()+"�Ľڵ㷢���������";
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
		    	       if(BrowserActivity.getIp().equals(key))//�������ͬ������ǵ�һ���Ļ������ȷ�����Ҫ���͵��ļ�
		    	       {
		    	    	   System.out.println("@@@@@@ ���ڵ��ǵ�һ�����͵Ľڵ�-- "+BrowserActivity.getIp());
		    	    	   if(Utils.isNetworkOn())//����ǿ��������Ľ��
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
			    		            System.out.println("@@@@@@ �����������ʱpath "+path);			    		            //�ǵ�һ������Ҫ�ж��������ǲ������ڵ㣬����ǵĻ����ж���Ҫͬ�����ļ������з������б���û�е�
			    		            //Ҫ�ӷ���������
			    		            if(FileSharing.onlyInServer.contains(path))//Ҫ�ӷ������ҵ������ص�
			    		            {
			    		            	if(i==(value.size()-1))
			    		            	{
			    		            		//���Ҫ���ص�����ļ��������һ���ļ��Ļ���������,
			    		            		BrowserActivity.downIsLast=true;
			    		            			
			    		            	}
			    		            	//������ļ��ӷ��������أ������ͳ�ȥ
			    		            	//Ҫ�ӷ��������أ��������߳��в��ܶ�UI�̵߳Ĳ���������
			    		            	System.out.println("FileSharing.onlyInServer.contains(path) "+path);
			    		            	 Message msg = new Message();
		                                 msg .obj = path;
		                                 msg .arg1=4;
		                                 FileSharing.myHandler.sendMessage(msg);	 
			    		            	
			    		            }
			    		            else//�����е�
			    		            {
			    		            	     if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
					    		            {
			    		            		     System.out.println("@@@@@@ ���ļ������һ���ļ�������ʱpath "+path);
					    			             FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
					    		            }
					    		            else
					    		            {
					    		            	  System.out.println("@@@@@@ ���ؽڵ��еı�����ʱpath "+path);
					    		                  FileSharing.addToQueue(path, curr_time, false,false);
					    		            }
			    		            }
			    		         }//for��Ҫ���͵�ÿһ���ļ���ÿһ���ļ���onlyInServer�Ա����onlyInServerӦ�����
			    		        FileSharing.onlyInServer.clear();
			    		          BrowserActivity.reposInfo.clear();
	    		            }
		    	    	   else//�����������Ļ�
		    	    	   {
		    	    		   value=FileSharing.savedMap.get(key);
			    		        for(int i=0;i<value.size();i++)
			    		        {    
			    			        String path=new String();
			    			        String time=new String();
			    		            path=value.get(i)[0]; 
			    		            time=value.get(i)[1];
			    		            Long curr_time=new Long(time);
			    		            if(i==(value.size()-1))//������ļ������һ���Ļ�����addToQueue�м����һ����־���˱�־��ֵΪtrue
			    		            {
			    		            	System.out.println("@@@@@@ i==(value.size()-1)  path "+path);
			    			             FileSharing.addToQueue(path, curr_time, false,true);//���ڿ��Խ�addToQueue��long���͸�ΪString
			    			             //���Լ�Ҫ���͵��ļ�������Ϻ󣬷���һ��������Ϣ��end������һ��Ҫ���͵�IP���������Ϣ�㲥��ȥ
			    			             //����յ�����㲥��Ϣ�󣬽�IPȡ�����Լ��ĵ�IP�Աȣ������ͬ��˵�����ǵڶ������Դ�����
			    			             //�������ļ������һ���Ļ��������һ���ļ�������Ҫ����end+nextIP
			    		            }
			    		            else
			    		            {
			    		            	System.out.println("@@@@@@  ���������� else  path "+path);
			    		                 FileSharing.addToQueue(path, curr_time, false,false);
			    		            }
			    		         }
		    	    	   }
		    	       }
		    	        break;//ֻ�ҵ�һ��
			     } //����WHILE
				}
				 } //if
				 else
				 {
					 //savedmao���Ϊ�յĻ�����û��Ҫ�������ֵ����ȥ�ˣ����ǹ㲥һ����Ϣ��û��Ҫͬ�������ݣ�ͬ��������Ϊ�Ժ�׼��
					 //0530�����ʲôҲ������Ϊ�˳���ĵ���
					 //savedmap���Ϊ�յĻ���˵��û��Ҫͬ������Ϣ������ͬ���ͽ�����,������ͳ�Ƹ����Ĺ�1���������ݵ�Ҳ��1�����ܴ�Ҷ�һ��������Ҫͬ��
					 //String mess ="û��Ҫͬ������Ϣ";
				     //BrowserActivity.showSynInfo(mess);
					 //����Ͳ���ʾ�ˣ��Ժ�Ҫ�ĳɵ�һ��ͬ����������Ҳ�������һ����㷢��󣬷���һ���㲥���յ����ͬ�������Ĺ㲥��
					 //,�����Ƿ����㻹����ͨ��㣬�ſ��԰�ͬ����ť������һ��ͬ��û�н�������һ��ͬ����ͬ���Ĺ������յ���һ��ͬ��
					 //���ļ�������ûͬ������ô�����ǣ�����ͬ����㰴��ͬ����ť�󣬾ͽ������ť��Ϊ��ɫ�Ĳ����ã��յ����һ����ͨ���
					 //������ͬ������֪ͨ����ť�ż��������ͨ���Ҳ�ǿ������Ϊ�յ����˷���ͬ����֪ͨ��������Ϊ������ť��һ����
					 //����ͬ����һ���н���ͬ��������㰴�˽���ͬ����椣���ô��ķ���ͬ����ť�ͱ�ɻ�ɫ���ˣ������ã���Ϊ�������ͬ��
					 //���ͬ��û����Ļ�����Ͳ����Է���ͬ�������յ����һ����㷢��Ĺ㲥�󣬲Ž�����ͬ����ť���ã�Ҳ����˵�㷢�ַ���
					 //ͬ����ť�����õĻ�����ͬ��������
				   //�����ｫͬ���Ľڵ�������Ϊ��ʼֵ1����Ϊ�����ձ��� 
					   FileSharing.ipCount=1;
					 //sync������ú�Ҳ���������ˣ����
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
