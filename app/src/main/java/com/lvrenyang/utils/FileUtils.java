package com.lvrenyang.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.Environment;

public class FileUtils {

	public static final String external_sd_dump_txt = Environment.getExternalStorageDirectory() + 
			 File.separator + "dump.txt";
	public static final String sdcard_dump_txt = "sdcard" + File.separator
			+ "dump.txt";

	public static final boolean debug = true;
	
	public static void DebugAddToFile(byte[] buffer, int byteOffset, int byteCount,
			String dumpfile){
		if(debug)
			AddToFile(buffer, byteOffset, byteCount , dumpfile);
	}
	
	public static void DebugAddToFile(String text, String dumpfile)
	{
		if(debug)
			AddToFile(text, dumpfile);
	}
	
	public static void DebugSaveToFile(String text, String dumpfile) {
		if(debug)
			SaveToFile(text,dumpfile);
	}
	
	/**
	 * 将数据存到文件
	 */
	public static void AddToFile(byte[] buffer, int byteOffset, int byteCount,
			String dumpfile) {
		if (null == dumpfile)
			return;
		if (null == buffer)
			return;
		if (byteOffset < 0 || byteCount <= 0)
			return;

		try {
			File file = new File(dumpfile);
			if (!file.exists()) {
				file.createNewFile();
			}
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(file.length());
			raf.write(buffer, byteOffset, byteCount);
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void AddToFile(String text, String dumpfile) {
		if (null == dumpfile)
			return;
		if (null == text)
			return;
		if ("".equals(text))
			return;

		try {
			File file = new File(dumpfile);
			if (!file.exists()) {
				file.createNewFile();
			}
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(file.length());
			raf.write(text.getBytes());
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void SaveToFile(String text, String dumpfile) {
		if (null == dumpfile)
			return;
		if (null == text)
			return;

		try {
			File file = new File(dumpfile);
			if (file.exists())
				file.delete();

			file.createNewFile();
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(0);
			raf.write(text.getBytes());
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String ReadToString(String filePathName) {

		File file = new File(filePathName);
		if (!file.exists())
			return null;

		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
			return new String(filecontent);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] ReadToMem(String filePathName) {
		File file = new File(filePathName);
		if (!file.exists())
			return null;

		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
			return filecontent;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
