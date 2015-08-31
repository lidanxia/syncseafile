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
		System.out.println("��ЩС�ļ��ܹ���Ҫ�ķ��͵Ĵ�����" + number);
		int n = 0;
		int startblock = 0;
		int from = 0;
		int to = 0;
		String[] fileID_break = null;
		for (int ii = 0; ii < sfiles.size(); ii++) 
		{
			fileID_break = sfiles.get(ii).sub_fileid.split("--");//ȡ��ÿһ��С�ļ�
			SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
			Date curDate = new Date(System.currentTimeMillis());
			String m = formatter.format(curDate);
			String mm = "�����ļ� " + fileID_break[0] + "��ʱ��: " + m;
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
			System.out.println("���ļ������ݰ�:  " + data_blocks);
			//�Կ���б���
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
				if ((k - from)== data_blocks- 1)//����ļ����һ�����ĳ���
					send_blockLength = lastlength;
				else
					send_blockLength = FileSharing.blocklength;
				//�ҵ����һ���ļ������һ���������ڸ�����ļ��ְ�
				//��ʼ���а���islast��־����false�������һ������islast��־��Ϊtrue���������Ͱ���ʱ�򶼼��һ�£������1˵���������һ��
				//��������islast��true�İ����ͷ���end+nextip
				if(sfiles.get(ii).islast==true&&(k-from)==(data_blocks- 1))
				{
					System.out.println("@@@@ ��С�ļ������һ���ļ������һ���ļ�bao�ţ� "+(k-from));
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
					System.out.println("С�ļ������һ���ļ�,�������һ�������ļ��ţ� "+(k-from));
					p.data = filedata;
					p.filename = sfiles.get(ii).filename;
					p.totalsubFiles = 1;
					plist[n][k] = p;
					p.sub_fileID = sfiles.get(ii).sub_fileid;
				}
			}
			
			
			synchronized (FileSharing.nextseq) 
			{
				FileSharing.nextseq.put(sfiles.get(ii).sub_fileid, data_blocks); // ��һ�δ�plist[0]��ʼ��������ֻ�ŵ����������
			}
			if (to == maxPackets) 
			{
				 FileSharing.sThread.inital(plist[n],FileSharing.bcastaddress, FileSharing.port, 0, maxPackets, 1);
				 FileSharing.sThread.sending();
				 n++;
				System.out.println("�Ѿ���100�鿪ʼ����");
				for (int j = 0; j < startblock; j++)
				{
					byte[] filedata = new byte[FileSharing.blocklength];
					System.arraycopy(data, (j + maxPackets - from)* FileSharing.blocklength, filedata, 0,FileSharing.blocklength);
					int send_blockLength = 0;
					if ((maxPackets - from + j) == data_blocks - 1)
						send_blockLength = lastlength;
					else
						send_blockLength = FileSharing.blocklength;
					//�ҵ����һ���ļ������һ���������ڸ�����ļ��ְ�
					//��ʼ���а���islast��־����false�������һ������islast��־��Ϊtrue���������Ͱ���ʱ�򶼼��һ�£������1˵���������һ��
					//��������islast��true�İ����ͷ���end+nextip
					if(sfiles.get(ii).islast==true&&(maxPackets - from + j) ==( data_blocks - 1))
					{
						Packet p = new Packet(0,0,curr_time, sfiles.get(ii).filelength,
								coding_blocks, data_blocks, (maxPackets - from + j),
								send_blockLength, sfiles.get(ii).filelength,true);
						System.out.println("@@@@ С�ļ������һ���ļ����Ѿ���һ�ٿ飬���һ���ļ����ţ� "+(maxPackets - from + j));
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
						System.out.println("��С�ļ��������һ���ļ����Ѿ���һ�ٿ飬�ļ����ţ� "+(maxPackets - from + j));
						p.data = filedata;
						p.filename = sfiles.get(ii).filename;
						p.totalsubFiles = 1;
						plist[n][j] = p;
						p.sub_fileID = sfiles.get(ii).sub_fileid;
					}
				}
			}
		} // forѭ������
		if (startblock!=0) 
		{
			 FileSharing.sThread.inital(plist[n], FileSharing.bcastaddress,FileSharing.port, 0, startblock, 1);
			 FileSharing.sThread.sending();
		}
	}
/**
 * �õ��ļ������ݺ󣬽��б���ͷ��ͣ��������⿪��һ���̣߳����ͻ��ǵ�ǰ�߳�
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
		//�Կ���б���
		if(length>0)
		{
		  System.out.println("������ĳ��ȣ� "+length);
		  EncodingThread encodeThread=new EncodingThread(data,length,sub_fileid,filename,num,filelength,curr_time);
		  encodeThread.start();//ÿ����б���,���벻�ù��𣿣�������������
		  System.out.println("�������ִ����");
		  sendPacket = file_blocks(data, length, sub_fileid,filename, num,filelength,curr_time,islast);//ÿ����зְ�
		}
		if (sendPacket != null)
		{
			FileSharing.sThread.inital(sendPacket,FileSharing.bcastaddress,FileSharing.port,0,
					sendPacket[0].data_blocks, 0);
			FileSharing.sThread.sending(); 
			String message = "���͵İ��ĸ���: " + sendPacket[0].data_blocks;
			System.out.println(message);
	//		FileSharing.writeLog(message); 	
		}
		if (!FileSharing.sendFiles.containsKey(fileID_break[0]))
			FileSharing.sendFiles.put(fileID_break[0], sendPacket[0].filename);

	}
	/**
	 * ���ļ���ֳ�1K��С�����ݰ�
	 * @param filedata�� ������
	 * @param subFileLength�� ���ݳ���
	 * @param sub_fileID 
	 * @param filename
	 * @param totalsubFiles 
	 * @param FileLength 
	 * @param curr_time
	 * @return
	 */ 
	//7.������ʵ���˷ְ������͵�ʱ���û���ļ��ĸ����ˣ����ɰ��ˣ�����ÿ������ҪЯ���ļ�������
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
				System.out.println("���ļ��ְ����ļ����ţ� "+i);
				//��������һ��Ļ�,��������һ�����Ļ��������һ������islast��־��Ϊtrue
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
						i, send_blockLength, FileLength,false);//8.�Ѱ���װ����
				p.data = data;
				p.filename = filename;
				p.totalsubFiles = totalsubFiles;
				plist[i] = p;
				p.sub_fileID = sub_fileID;
			}
			
		}
		//�ж�Ҫ�ְ�������� �Ƿ����ҵ����Ǹ����һ���ļ������һ�飬����ǵ��ҵ����������һ����
		//if(sub_fileID==FileSharing.lastsub_id)
		//{
			//FileSharing.blockid=data_blocks-1;//������Ҫ�ְ��Ŀ������һ�飬�ͼ�¼�������һ�����ĺ�
		//}
		synchronized (FileSharing.nextseq)
		{
		  FileSharing.nextseq.put(plist[0].sub_fileID, data_blocks); // ��һ�δ�plist[0]��ʼ��������ֻ�ŵ����������
		}
		return plist;
	}
}
