package com.seafile.sharing;

import java.io.Serializable;

public class Packet implements Serializable
{
	String sub_fileID; //文件ID号和文件的块号
	String filename;
	long init_time=0;
	long m_time=0;
	int totalsubFiles;  //该文件总共分为几块
	int type=0;  //0为数据，1为反馈包
	int coding_blocks;
	int data_blocks;
	int seqno;
	int data_length;
	byte[] data;
	int subFileLength=0; //文件块的大小
	long fileLength=0;
	//String ip;//0522
	boolean islast=false;//0522
	
	Packet(int type,long init_time,long m_time,int subFileLength, int coding_blocks, int data_blocks, int seqno, int data_length,long filelength,boolean islast)
	{
		this.type = type;
		this.init_time=init_time;
		this.m_time=m_time;
		this.subFileLength=subFileLength;
		this.coding_blocks = coding_blocks;
		this.data_blocks = data_blocks;
		this.seqno = seqno;
		this.data_length = data_length;
		this.fileLength=filelength;
		//this.ip=ip;//0522
		this.islast=islast;//0522
	}
}
