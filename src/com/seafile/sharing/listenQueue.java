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
			 System.out.println("@@@@进入listenQueue线程  "+System.currentTimeMillis());
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
				 System.out.println("@@@@@  队列的大小："+SendFilequeue.size());
			    while(FileSharing.SendFilequeue.size()>0)
				{
			     FileMtimeData fileData=SendFilequeue.peek();//4.将要发送的文件依次从发送队列中取出
				  String filename=fileData.filename;
				 
				  String id=Integer.toString(FileSharing.sendFileID);
	 			  String file_id =FileSharing.ipaddress+"-"+id;
	 			  int filelength=0;
	 			  FileInputStream fis=null;
	 			  File f=new File(filename);
	 			  System.out.println("待发送文件的长度：AAAAA"+ f.getAbsolutePath());
	 			  if(f.exists())
	 			  {
	 				  System.out.println("待发送文件的长度：BBBBB"+ f.getAbsolutePath());
	 				try {
	 					fis = new FileInputStream(filename);
	 					filelength = fis.available();
	 					System.out.println("待发送文件的长度："+filelength);
	 					System.out.println("@@@@@@待发送文件"+filename);
	 				} catch (Exception e) 
	 				{
	 					e.printStackTrace();
	 				}
	 			  long curr_time=fileData.mtime;
	 			  //String ip=fileData.ip;//0522
	 			  boolean islast=fileData.islast;//0522
	 			  //long serv_time=fileData.stime;
	 			  //存入数据库
	 			  if(filelength>=FileSharing.maxfilelength)  
	 				{ 
	 				System.out.println("文件>100k");
	 				SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 			    Date curDate = new Date(System.currentTimeMillis());
	 			    String m = formatter.format(curDate);
	 			    String mm="发送文件 "+FileSharing.ipaddress+"-"+id+"的时间: "+m; 
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
	 					
	 					System.out.println("###文件分几次发送："+num);
	 				
	 					for(int i=0;i<num;i++)  //5.大文件分块
	 					{
	 					  long start=0;
	 			          start=System.currentTimeMillis();
	 					  String sub_id =FileSharing.ipaddress+"-"+id+"--"+i;
	 					  if(islast==true)//如果这个文件的islast是true，说明这个文件是最后一个文件
	 					  {
	 						  if(i==(num-1))//如果是最后一块的话
	 						  {
	 							 sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,true);
	 						  }
	 						  
	 					  }
	 					 else//不是最后一个文件的最后一块的话，islast的值为false
						  {
							 sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,false);
						  }
	 					 // sendFunction.sendToAll(filename,filelength, sub_id,num,curr_time,islast);//6.将每块文件去编码，分包
	 			          long end=System.currentTimeMillis();
	 				  //    FileSharing.writeLog("&&&& one-block sending_SpentTime:"+id+"--"+i+",	"+(end-start)+"ms,	"+"\r\n"); 					 
	 					}
	 					
	 					
	 		        }	
	 			else if(filelength>0)	//多个小文件凑成一个大文件
	 			{
	 				System.out.println("该文件<100k");
	 				if(SendFilequeue.size()==1)  //当前文件是队列中的最后一个
	 				{
	 					if(smallfiles.size()==0)   //小文件链表为0
	 					{
	 					     SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 				         Date curDate = new Date(System.currentTimeMillis());
	 				         String m = formatter.format(curDate);
	 				         String mm="发送文件 "+FileSharing.ipaddress+"-"+id+"的时间: "+m; 
	 				         System.out.println(mm);
	 				         System.out.println("%%%From Adhoc sending smallfiles"+filename+",	");
	 				         FileSharing.writeLog("%%%From Adhoc sending smallfiles"+filename+",	");
	 				         FileSharing.writeLog(id+",	");
	 				         FileSharing.writeLog(filelength+",	");
	 				         FileSharing.writeLog(System.currentTimeMillis()+",	");
	 				         FileSharing.writeLog(m+",	"+"\r\n");
	 				         FileSharing. writeLog("\r\n");
	 					     String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					     //不需要多个小文件合并，那么这个文件不分块，直接分包
	 					     if(islast==true)//如果这个文件是最后一个的话
	 					     {
	 						     //因为这个小文件只有一个，所以不分块，也就是只有一个块
	 					    	 sendFunction.sendToAll(filename,filelength, sub_id,1,curr_time,true);
	 						     //FileSharing.lastsub_id =sub_id;
	 					     }
	 					     else
	 					     {
	 					    	 sendFunction.sendToAll(filename,filelength, sub_id,1,curr_time,false);//6.1一个文件去编码，分包
	 					     }
	 					    
	 					     
	 					 
	 				    }
	 					else  
	 					{  //6.2多个小文件
	 					    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id,curr_time,islast);
	 	   					smallfiles.add(sf);
	 	   			     	small_total_length=small_total_length+filelength;
	 	   			     	System.out.println("小文件列表中的文件个数： "+smallfiles.size());
	 	   				    sendFunction.sendSmallFiles(smallfiles ,small_total_length);
	 	   				    smallfiles.clear();
	 					}
	 				}
	 				else  //队列中还剩下多个文件（>1）
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
		   }  //内层while循环结束
			    FileSharing.currentLength=0; 
			    FileSharing.file_number=0;
			    if(smallfiles.size()>0) 
			    {
			     	System.out.println("小文件列表中的文件个数： "+smallfiles.size());
			    	sendFunction.sendSmallFiles(smallfiles,small_total_length);
		   		    smallfiles.clear();
			    }	
			    small_total_length=0;
		   }
		 } //end synchronized queue	
	 }
}
