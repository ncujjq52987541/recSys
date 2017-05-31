package com.dataprocess;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class Netflix {

	public static void main(String[] args) throws IOException {
		File files = new File("D:\\tmp\\recommend\\netflix\\temp");
		File[] listFiles = files.listFiles();
		
		FileInputStream fis =null;
    	BufferedInputStream bis = null;
    	DataInputStream dis = null;
    	
//    	FileOutputStream fos = new FileOutputStream("D:\\tmp\\recommend\\netflixprocess\\netflix.txt",true);
//    	BufferedOutputStream bos = new BufferedOutputStream(fos);
//    	DataOutputStream dos = new DataOutputStream(bos);
    	FileWriter fw = new FileWriter("D:\\tmp\\recommend\\netflixprocess\\netflix.txt",true);
    	BufferedWriter bw = new BufferedWriter(fw);
    	
    	String str = null;
		for (int i = 0; i < listFiles.length; i++) {
			fis = new FileInputStream(listFiles[i]);
			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);
			String num=null;
			String output=null;
			while((str=dis.readLine())!=null){
				if(str.contains(":")){
					num = str.substring(0,str.indexOf(":"));
				}else{
					String[] split = str.split(",");
					output=split[0]+","+num+","+split[1];
					bw.write(output);
					bw.write("\r\n");
				}
			}
			dis.close();bis.close();fis.close();
		}
//		dos.close();bos.close();fos.close();

		bw.close();fw.close();
	}

}
