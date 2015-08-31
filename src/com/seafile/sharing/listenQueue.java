package com.seafile.sharing;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Timer;


public class listenQueue extends Thread
{
	
	public boolean running=true;
	public ArrayList<SmallFileData >smallfiles=new ArrayList<SmallFileData >();
	public int small_total_length=0;
	public sendFileFunction sendFunction=new sendFileFunction();
	ArrayList <Timer>queueListen_List=new ArrayList<Timer>();
	public synchronized void run()
	{
	  while (running)
	  {
		 if(FileSharing.SendFilequeue.size()>0)
		 {
			 System.out.println("@@@@����listenQueue�߳�  "+System.currentTimeMillis());
			 handleQueue(FileSharing.SendFilequeue);
		 }
		 Timer queueTimer=new Timer(true);
		 QueueListenTask queueTask=new QueueListenTask();
		 queueTimer.schedule(queueTask, FileSharing.sleeptime);
		 queueListen_List.add(queueTimer);
		 try {
			wait();  
		 } catch (InterruptedException e) 
		 {
			e.printStackTrace();
		 }
	  }
	}
	public synchronized void notifyThread()
	{
		notify();
	}
	public synchronized void ondestroy()
	{
	   running=false;
	   synchronized(queueListen_List)
	   {
	    for(int i=0;i<queueListen_List.size();i++)
		    queueListen_List.get(i).cancel();
	     queueListen_List.clear();
      }
	}
	
	public void handleQueue(Queue<FileMtimeData> SendFilequeue)
	{
		 synchronized(SendFilequeue)
		   {
			 if(FileSharing.SendFilequeue.size()>0)
			 { 
				 System.out.println("@@@@@  ���еĴ�С��"+SendFilequeue.size());
			    while(FileSharing.SendFilequeue.size()>0)
				{
			     FileMtimeData fileData=SendFilequeue.peek();//4.��Ҫ���͵��ļ����δӷ��Ͷ�����ȡ��
				  String filename=fileData.filename;
				 
				  String id=Integer.toString(FileSharing.sendFileID);
	 			  String file_id =FileSharing.ipaddress+"-"+id;
	 			  int filelength=0;
	 			  FileInputStream fis=null;
	 			  File f=new File(filename);
	 			  System.out.println("�������ļ��ĳ��ȣ�AAAAA"+ f.getAbsolutePath());
	 			  if(f.exists())
	 			  {
	 				  System.out.println("�������ļ��ĳ��ȣ�BBBBB"+ f.getAbsolutePath());
	 				try {
	 					fis = new FileInputStream(filename);
	 					filelength = fis.available();
	 					System.out.println("�������ļ��ĳ��ȣ�"+filelength);
	 					System.out.println("@@@@@@�������ļ�"+filename);
	 				} catch (Exception e) 
	 				{
	 					e.printStackTrace();
	 				}
	 			  long curr_time=fileData.mtime;
	 			  //String ip=fileData.ip;//0522
	 			  boolean islast=fileData.islast;//0522
	 			  //long serv_time=fileData.stime;
	 			  //�������ݿ�
	 			  if(filelength>=FileSharing.maxfilelength)  
	 				{ 
	 				System.out.println("�ļ�>100k");
	 				SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 			    Date curDate = new Date(System.currentTimeMillis());
	 			    String m = formatter.format(curDate);
	 			    String mm="�����ļ� "+FileSharing.ipaddress+"-"+id+"��ʱ��: "+m; 
	 			    System.out.println(mm);
	 			    System.out.println("%%%From Adhoc sending largefiles--"+filename+",	");
	 			     FileSharing.writeLog("%%%From Adhoc sending largefiles--"+filename+",	");
	 			     FileSharing.writeLog(id+",	");
	 			     FileSharing.writeLog(filelength+",	");
	 			     FileSharing.writeLog(System.currentTimeMillis()+",	");
	 			     FileSharing.writeLog(m+",	"+"\r\n");
	 			     FileSharing.writeLog("\r\n");
	 					int num=0;
	 					if(filelength%FileSharing.maxfilelength==0)
	 						num=filelength/FileSharing.maxfilelength;
	 					else
	 						num=filelength/FileSharing.maxfilelength+1;
	 					
	 					System.out.println("###�ļ��ּ��η��ͣ�"+num);
	 				
	 					for(int i=0;i<num;i++)  //5.���ļ��ֿ�
	 					{
	 					  long start=0;
	 			          start=System.currentTimeMillis();
	 					  String sub_id =FileSharing.ipaddress+"-"+id+"--"+i;
	 					  if(islast==true)//�������ļ���islast��true��˵������ļ������һ���ļ�
	 					  {
	 						  if(i==(num-1))//��������һ��Ļ�
	 						  {
	 							 sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,true);
	 						  }
	 						  
	 					  }
	 					 else//�������һ���ļ������һ��Ļ���islast��ֵΪfalse
						  {
							 sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,false);
						  }
	 					 // sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,islast);//6.��ÿ���ļ�ȥ���룬�ְ�
	 			          long end=System.currentTimeMillis();
	 				  //    FileSharing.writeLog("&&&& one-block sending_SpentTime:"+id+"--"+i+",	"+(end-start)+"ms,	"+"\r\n"); 					 
	 					}
	 					
	 					
	 		        }	
	 			else if(filelength>0)	//���С�ļ��ճ�һ�����ļ�
	 			{
	 				System.out.println("���ļ�<100k");
	 				if(SendFilequeue.size()==1)  //��ǰ�ļ��Ƕ����е����һ��
	 				{
	 					if(smallfiles.size()==0)   //С�ļ�����Ϊ0
	 					{
	 					     SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 				         Date curDate = new Date(System.currentTimeMillis());
	 				         String m = formatter.format(curDate);
	 				         String mm="�����ļ� "+FileSharing.ipaddress+"-"+id+"��ʱ��: "+m; 
	 				         System.out.println(mm);
	 				         System.out.println("%%%From Adhoc sending smallfiles"+filename+",	");
	 				         FileSharing.writeLog("%%%From Adhoc sending smallfiles"+filename+",	");
	 				         FileSharing.writeLog(id+",	");
	 				         FileSharing.writeLog(filelength+",	");
	 				         FileSharing.writeLog(System.currentTimeMillis()+",	");
	 				         FileSharing.writeLog(m+",	"+"\r\n");
	 				         FileSharing. writeLog("\r\n");
	 					     String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					     //����Ҫ���С�ļ��ϲ�����ô����ļ����ֿ飬ֱ�ӷְ�
	 					     if(islast==true)//�������ļ������һ���Ļ�
	 					     {
	 						     //��Ϊ���С�ļ�ֻ��һ�������Բ��ֿ飬Ҳ����ֻ��һ����
	 					    	 sendFunction.sendToAll(filename,filelength, sub_id,1,curr_time,true);
	 						     //FileSharing.lastsub_id =sub_id;
	 					     }
	 					     else
	 					     {
	 					    	 sendFunction.sendToAll(filename,filelength, sub_id,1,curr_time,false);//6.1һ���ļ�ȥ���룬�ְ�
	 					     }
	 					    
	 					     
	 					 
	 				    }
	 					else  
	 					{  //6.2���С�ļ�
	 					    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id,curr_time,islast);
	 	   					smallfiles.add(sf);
	 	   			     	small_total_length=small_total_length+filelength;
	 	   			     	System.out.println("С�ļ��б��е��ļ������� "+smallfiles.size());
	 	   				    sendFunction.sendSmallFiles(smallfiles ,small_total_length);
	 	   				    smallfiles.clear();
	 					}
	 				}
	 				else  //�����л�ʣ�¶���ļ���>1��
	 				{
	 				    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 				    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id,curr_time,islast);
	 					smallfiles.add(sf);	
	 					small_total_length=small_total_length+filelength;
	 				}
	 			} // end else if(filelength>0)
	 			 FileSharing.sendFiles.put(FileSharing.ipaddress+"-"+id, filename);
	 			 FileSharing.sendFileID++;  
	 			 SendFilequeue.remove();
	 		} //end if(f.exists())
		   }  //�ڲ�whileѭ������
			    FileSharing.currentLength=0; 
			    FileSharing.file_number=0;
			    if(smallfiles.size()>0) 
			    {
			     	System.out.println("С�ļ��б��е��ļ������� "+smallfiles.size());
			    	sendFunction.sendSmallFiles(smallfiles,small_total_length);
		   		    smallfiles.clear();
			    }	
			    small_total_length=0;
		   }
		 } //end synchronized queue	
	 }
}
