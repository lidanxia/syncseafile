package com.seafile.sharing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Timer;
/**
 * 发送整个文件块线程
 * @author PC
 *
 */

public class sendFile_blockThread extends Thread
{
	public  ArrayList<Integer> nos=null; //要发送的文件块号，也是收到的对方发送的丢掉的文件块
	public  String fileid=null;
	public  int num=0;
	public  String fileName=null;
	long  curr_time=0;
	public sendFileFunction sendFunction=new sendFileFunction();
	
	sendFile_blockThread(String fileid,long curr_time,ArrayList<Integer> nos,int num)
	{
	  this.nos=new ArrayList<Integer>();
	  this.nos=nos;
	  this.fileid=fileid;
	  this.num=num;
	  this.curr_time=curr_time;
	}
	public void run()
	{
		 int length=0;
		 long filelength=0;
	     Timer subfiletimer = null;
		 fileName=FileSharing.sendFiles.get(fileid);
	     String file=fileName;
	     File ff=new File(file);
	    if(ff.exists())
		{
	     RandomAccessFile raf=null;	
         try {
			raf = new RandomAccessFile(ff,"r");
			filelength=raf.length();
		  } catch (Exception e) 
		  {
			e.printStackTrace();
		  }
	
			for(int l=0;l<nos.size();l++)
			{
				System.out.println("sendFile_blockThread wait SendFilequeue");
				     byte[] data = new byte[FileSharing.maxfilelength];
				     try {
					  raf.seek(0);
					  raf.skipBytes(FileSharing.maxfilelength*nos.get(l));
				 	  length=raf.read(data,0,FileSharing.maxfilelength); 
	 			     } catch (IOException e) 
	 			    {
	 				e.printStackTrace();
	 			    }
				System.out.println(fileid+" 发送wenjian的快号： "+nos.get(l));	
		 		System.out.println("发送数据长度：  "+length);	
		 		String sub_id =fileid+"--"+nos.get(l);
		 		EncodingThread encodeThread=new EncodingThread(data,length,sub_id ,fileName,num,filelength,curr_time );
				encodeThread.start();
		 		Packet[]sendPacket=null;
		 	 	sendPacket=sendFunction.file_blocks(data,length,sub_id,fileName,num,filelength,curr_time,false);
		 	 	if(sendPacket!=null)
		 		 {
		 		  FileSharing.sThread.inital(sendPacket,FileSharing.bcastaddress,FileSharing.port,0,sendPacket[0].data_blocks,0);
				  FileSharing.sThread.sending(); 
		 		  String message = "发送的包的个数: "+sendPacket[0].data_blocks;
		 		  System.out.println(message);	
		 		 }
		} //end for
	  
	      synchronized(FileSharing.Feedpkts)
	         {
		           for(int kk=0;kk<FileSharing.Feedpkts.size();kk++)
		           {
		             if(FileSharing.Feedpkts.get(kk).sub_fileID.equals(fileid))
		 	          {
		 	             FileSharing.Feedpkts.remove(kk);
		 	             break;
		 	           }
		             }
	            }
	     }		 
	    }
	
}
