package com.jheto.diagnostics;

import java.io.File;
import java.lang.reflect.Method;

public final class LinuxStatConstants {

	private LinuxStatConstants(){}

	public static final int S_IFMT = 0170000; /* type of file */
	public static final int S_IFLNK = 0120000; /* symbolic link */
	public static final int S_IFREG = 0100000; /* regular */
	public static final int S_IFBLK = 0060000; /* block special */ 

	public static final int S_IFDIR = 0040000; /* directory */
	public static final int S_IFCHR = 0020000; /* character special */
	public static final int S_IFIFO = 0010000; /* this is a FIFO */
	public static final int S_ISUID = 0004000; /* set user id on execution */
	public static final int S_ISGID = 0002000; /* set group id on execution */

	private static String permRwx(int perm) {
		String result;
		result = ((perm & 04) != 0 ? "r" : "-") + ((perm & 02) != 0 ? "w" : "-") + ((perm & 1) != 0 ? "x" : "-");
		return result;
	}

	private static String permFileType(int perm) {
		String result = "?";
		switch (perm & S_IFMT) {
		case S_IFLNK:
			result = "s";
			break; /* symbolic link */
		case S_IFREG:
			result = "-";
			break; /* regular */
		case S_IFBLK:
			result = "b";
			break; /* block special */
		case S_IFDIR:
			result = "d";
			break; /* directory */
		case S_IFCHR:
			result = "c";
			break; /* character special */
		case S_IFIFO:
			result = "p";
			break; /* this is a FIFO */
		}
		return result;
	}

	public final static String permString(int perms) {
		String result;
		result = permFileType(perms) + permRwx(perms >> 6) + permRwx(perms >> 3) + permRwx(perms);
		return result;
	}
	
	public final static FileStatus getFileStatus(File path) {
		FileStatus result = new FileStatus();
		try{
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Class<?> fileStatus = Class.forName("android.os.FileUtils$FileStatus");
			Method getOsFileStatus = fileUtils.getMethod("getFileStatus", String.class, fileStatus);
			Object fs = fileStatus.newInstance();
			if ((Boolean) getOsFileStatus.invoke(null, path.getAbsolutePath(), fs)) {
				result.atime = fileStatus.getField("atime").getLong(fs);
				result.blksize = fileStatus.getField("blksize").getInt(fs);
				result.blocks = fileStatus.getField("blocks").getLong(fs);
				result.ctime = fileStatus.getField("ctime").getLong(fs);
				result.dev = fileStatus.getField("dev").getInt(fs);
				result.gid = fileStatus.getField("gid").getInt(fs);
				result.ino = fileStatus.getField("ino").getInt(fs);
				result.mode = fileStatus.getField("mode").getInt(fs);
				result.mtime = fileStatus.getField("mtime").getLong(fs);
				result.nlink = fileStatus.getField("nlink").getInt(fs);
				result.rdev = fileStatus.getField("rdev").getInt(fs);
				result.size = fileStatus.getField("size").getLong(fs);
				result.uid = fileStatus.getField("uid").getInt(fs);
			}
		}catch(Exception e){}
		return result;
	}

}
