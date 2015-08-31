package com.seafile.sharing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;

public class sendFileFunction {
	public final int maxPackets=FileSharing.maxfilelength/FileSharing.blocklength;

	public void sendSmallFiles(ArrayList<SmallFileData > sfiles,int total_length) 
	{
		int number = 0;
		if (total_length % FileSharing.maxfilelength == 0)
			number = total_length / FileSharing.maxfilelength;
		else
			number = total_length / FileSharing.maxfilelength + 1;
		Packet[][] plist = new Packet[number+1][FileSharing.maxfilelength/ FileSharing.blocklength];
		System.out.println("这些小文件总共需要的发送的次数：" + number);
		int n = 0;
		int startblock = 0;
		int from = 0;
		int to = 0;
		String[] fileID_break = null;
		for (int ii = 0; ii < sfiles.size(); ii++) 
		{
			fileID_break = sfiles.get(ii).sub_fileid.split("--");//取出每一个小文件
			SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
			Date curDate = new Date(System.currentTimeMillis());
			String m = formatter.format(curDate);
			String mm = "发送文件 " + fileID_break[0] + "的时间: " + m;
			System.out.println(mm);
            long curr_time=sfiles.get(ii).curr_time;
           // String ip=sfiles.get(ii).ip;//0522
            boolean islast=sfiles.get(ii).islast;//0522
			FileSharing.writeLog("###start sending small_files" + sfiles.get(ii).filename + ",	");
			FileSharing.writeLog(fileID_break[0].split("-")[1] + ",	");
			FileSharing.writeLog(sfiles.get(ii).filelength + ",	");
			FileSharing.writeLog(System.currentTimeMillis() + ",	");
			FileSharing.writeLog(m + ",	" + "\r\n");
			FileSharing.writeLog("\r\n");
			FileInputStream fis = null;
			BufferedInputStream in = null;
			int sub_no = Integer.parseInt(fileID_break[1]);
			if (!FileSharing.sendFiles.containsKey(fileID_break[0]))
				FileSharing.sendFiles.put(fileID_break[0], sfiles.get(ii).filename);
			String file = sfiles.get(ii).filename;
			int data_blocks;
			int coding_blocks = 0;

			int lastlength = FileSharing.blocklength;
			byte[] data = new byte[FileSharing.maxfilelength];
			File f=new File(file);
			int length=0;
			if(f.exists())
			{
			try {
				fis = new FileInputStream(file);
				in = new BufferedInputStream(fis);
				in.skip(FileSharing.maxfilelength * sub_no);
				length=in.read(data, 0, FileSharing.maxfilelength);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			}
			if (sfiles.get(ii).filelength % FileSharing.blocklength == 0) 
			{
				data_blocks = sfiles.get(ii).filelength/ FileSharing.blocklength;
			} 
			else 
			{
				data_blocks = sfiles.get(ii).filelength/ FileSharing.blocklength + 1;
				lastlength = sfiles.get(ii).filelength% FileSharing.blocklength;
			}
			System.out.println("该文件的数据包:  " + data_blocks);
			//对块进行编码
			  EncodingThread encodeThread=new EncodingThread(data,length,sfiles.get(ii).sub_fileid,sfiles.get(ii).filename,1,sfiles.get(ii).filelength, curr_time);
			  encodeThread.start();
			if (startblock == 0) 
			{
				startblock = data_blocks;
				from = 0;
				to = data_blocks;
			} 
			else
			{
				from = startblock;
				to = from + data_blocks;
				if (to >= maxPackets)
				{
					startblock = to - maxPackets;
					to = maxPackets;
				}
				else
				  startblock=to;
			}
			for (int k = from; k < to; k++) 
			{
				byte[] filedata = new byte[FileSharing.blocklength];
				System.arraycopy(data, (k - from) * FileSharing.blocklength,filedata, 0, FileSharing.blocklength);
				int send_blockLength = 0;
				if ((k - from)== data_blocks- 1)//这个文件最后一个包的长度
					send_blockLength = lastlength;
				else
					send_blockLength = FileSharing.blocklength;
				//找到最后一个文件的最后一个包，是在给这个文件分包
				//初始所有包的islast标志都是false；将最后一个包的islast标志置为true；这样发送包的时候都检测一下，如果是1说明就是最后一个
				//当发送完islast是true的包，就发送end+nextip
				if(sfiles.get(ii).islast==true&&(k-from)==(data_blocks- 1))
				{
					System.out.println("@@@@ 该小文件是最后一个文件，最后一个文件bao号： "+(k-from));
					Packet p = new Packet(0, 0,curr_time,sfiles.get(ii).filelength,
							coding_blocks, data_blocks, (k - from),
							send_blockLength, sfiles.get(ii).filelength,true);
					p.data = filedata;
					p.filename = sfiles.get(ii).filename;
					p.totalsubFiles = 1;
					plist[n][k] = p;
					p.sub_fileID = sfiles.get(ii).sub_fileid;
				}
				else
				{
					Packet p = new Packet(0, 0,curr_time,sfiles.get(ii).filelength,
							coding_blocks, data_blocks, (k - from),
							send_blockLength, sfiles.get(ii).filelength,false);
					System.out.println("小文件是最后一个文件,不是最后一个包，文件号： "+(k-from));
					p.data = filedata;
					p.filename = sfiles.get(ii).filename;
					p.totalsubFiles = 1;
					plist[n][k] = p;
					p.sub_fileID = sfiles.get(ii).sub_fileid;
				}
			}
			
			
			synchronized (FileSharing.nextseq) 
			{
				FileSharing.nextseq.put(sfiles.get(ii).sub_fileid, data_blocks); // 下一次从plist[0]开始发，其中只放的是冗余包。
			}
			if (to == maxPackets) 
			{
				 FileSharing.sThread.inital(plist[n],FileSharing.bcastaddress, FileSharing.port, 0, maxPackets, 1);
				 FileSharing.sThread.sending();
				 n++;
				System.out.println("已经够100块开始发送");
				for (int j = 0; j < startblock; j++)
				{
					byte[] filedata = new byte[FileSharing.blocklength];
					System.arraycopy(data, (j + maxPackets - from)* FileSharing.blocklength, filedata, 0,FileSharing.blocklength);
					int send_blockLength = 0;
					if ((maxPackets - from + j) == data_blocks - 1)
						send_blockLength = lastlength;
					else
						send_blockLength = FileSharing.blocklength;
					//找到最后一个文件的最后一个包，是在给这个文件分包
					//初始所有包的islast标志都是false；将最后一个包的islast标志置为true；这样发送包的时候都检测一下，如果是1说明就是最后一个
					//当发送完islast是true的包，就发送end+nextip
					if(sfiles.get(ii).islast==true&&(maxPackets - from + j) ==( data_blocks - 1))
					{
						Packet p = new Packet(0,0,curr_time, sfiles.get(ii).filelength,
								coding_blocks, data_blocks, (maxPackets - from + j),
								send_blockLength, sfiles.get(ii).filelength,true);
						System.out.println("@@@@ 小文件是最后一个文件，已经够一百块，最后一个文件包号： "+(maxPackets - from + j));
						p.data = filedata;
						p.filename = sfiles.get(ii).filename;
						p.totalsubFiles = 1;
						plist[n][j] = p;
						p.sub_fileID = sfiles.get(ii).sub_fileid;
					}
					else
					{
						Packet p = new Packet(0,0,curr_time, sfiles.get(ii).filelength,
								coding_blocks, data_blocks, (maxPackets - from + j),
								send_blockLength, sfiles.get(ii).filelength,false);
						System.out.println("该小文件不是最后一个文件，已经够一百块，文件包号： "+(maxPackets - from + j));
						p.data = filedata;
						p.filename = sfiles.get(ii).filename;
						p.totalsubFiles = 1;
						plist[n][j] = p;
						p.sub_fileID = sfiles.get(ii).sub_fileid;
					}
				}
			}
		} // for循环结束
		if (startblock!=0) 
		{
			 FileSharing.sThread.inital(plist[n], FileSharing.bcastaddress,FileSharing.port, 0, startblock, 1);
			 FileSharing.sThread.sending();
		}
	}
/**
 * 拿到文件块数据后，进行编码和发送，编码另外开了一个线程，发送还是当前线程
 * @param filename
 * @param filelength
 * @param sub_fileid
 * @param num
 * @param curr_time
 */
	public void sendToAll(String filename, int filelength, String sub_fileid,int num,long curr_time,boolean islast) 
	{
		Packet[] sendPacket = null;
		FileInputStream fis = null;
		BufferedInputStream in = null;
		String[] fileID_break = null;
		fileID_break = sub_fileid.split("--");
		int sub_no = Integer.parseInt(fileID_break[1]);
		byte[] data = new byte[FileSharing.maxfilelength];
		int length = 0;
		File f=new File(filename);
		if(f.exists())
		{
		try {
			fis = new FileInputStream(filename);
			in = new BufferedInputStream(fis);
			in.skip(FileSharing.maxfilelength * sub_no);
			length = in.read(data, 0, FileSharing.maxfilelength);
		   } catch (IOException e)
		   {
			e.printStackTrace();
		   }
		}
		//对块进行编码
		if(length>0)
		{
		  System.out.println("待编码的长度： "+length);
		  EncodingThread encodeThread=new EncodingThread(data,length,sub_fileid,filename,num,filelength,curr_time);
		  encodeThread.start();//每块进行编码,编码不用管吗？？？？？？？？
		  System.out.println("编码可以执行吗？");
		  sendPacket = file_blocks(data, length, sub_fileid,filename, num,filelength,curr_time,islast);//每块进行分包
		}
		if (sendPacket != null)
		{
			FileSharing.sThread.inital(sendPacket,FileSharing.bcastaddress,FileSharing.port,0,
					sendPacket[0].data_blocks, 0);
			FileSharing.sThread.sending(); 
			String message = "发送的包的个数: " + sendPacket[0].data_blocks;
			System.out.println(message);
	//		FileSharing.writeLog(message); 	
		}
		if (!FileSharing.sendFiles.containsKey(fileID_break[0]))
			FileSharing.sendFiles.put(fileID_break[0], sendPacket[0].filename);

	}
	/**
	 * 将文件块分成1K大小的数据包
	 * @param filedata： 块数据
	 * @param subFileLength： 数据长度
	 * @param sub_fileID 
	 * @param filename
	 * @param totalsubFiles 
	 * @param FileLength 
	 * @param curr_time
	 * @return
	 */ 
	//7.在这里实现了分包，传送的时候就没有文件的概念了，都成包了，所以每个包里要携带文件的属性
	public Packet[] file_blocks(byte[] filedata, int subFileLength,String sub_fileID, String filename, int totalsubFiles,long FileLength,long curr_time,boolean islast) 
	{
		int data_blocks;
		int coding_blocks = 0;
		Packet[] plist = null;
		int lastlength = FileSharing.blocklength;	
		if (subFileLength % FileSharing.blocklength == 0) 
		{
			data_blocks = subFileLength / FileSharing.blocklength;
		}
		else 
		{
			data_blocks = subFileLength / FileSharing.blocklength + 1;
			lastlength = subFileLength % FileSharing.blocklength;
		}
		plist = new Packet[data_blocks];
		for (int i = 0; i < data_blocks; i++) 
		{
			byte[] data = new byte[FileSharing.blocklength];
			System.arraycopy(filedata, i * FileSharing.blocklength, data, 0,
					FileSharing.blocklength);
			int send_blockLength = 0;
			if (i == data_blocks - 1)
				send_blockLength = lastlength;
			else
				{send_blockLength = FileSharing.blocklength;}
			if(islast==true&&i == data_blocks - 1)
			{
				System.out.println("大文件分包，文件包号： "+i);
				//如果是最后一块的话,如果是最后一个包的话，将最后一个包的islast标志置为true
				Packet p = new Packet(0, 0,curr_time,subFileLength, coding_blocks, data_blocks,
						i, send_blockLength, FileLength,true);
				p.data = data;
				p.filename = filename;
				p.totalsubFiles = totalsubFiles;
				plist[i] = p;
				p.sub_fileID = sub_fileID;
				
			}
			else
			{
				Packet p = new Packet(0, 0,curr_time,subFileLength, coding_blocks, data_blocks,
						i, send_blockLength, FileLength,false);//8.把包封装成类
				p.data = data;
				p.filename = filename;
				p.totalsubFiles = totalsubFiles;
				plist[i] = p;
				p.sub_fileID = sub_fileID;
			}
			
		}
		//判断要分包的这个块 是否是找到的那个最后一个文件的最后一块，如果是的找到这个块的最后一个包
		//if(sub_fileID==FileSharing.lastsub_id)
		//{
			//FileSharing.blockid=data_blocks-1;//如果这个要分包的块是最后一块，就记录他的最后一个包的号
		//}
		synchronized (FileSharing.nextseq)
		{
		  FileSharing.nextseq.put(plist[0].sub_fileID, data_blocks); // 下一次从plist[0]开始发，其中只放的是冗余包。
		}
		return plist;
	}
}
