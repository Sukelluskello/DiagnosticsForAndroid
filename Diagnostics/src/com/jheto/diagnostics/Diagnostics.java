package com.jheto.diagnostics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

/*
 * Repository: https://github.com/JhetoX/DiagnosticsForAndroid
 * Creator: Jheto Xekri
 * License: LGPL v3
 * 
 * This a useful library to analyze Andoid OS
 * */
public final class Diagnostics {

	public static boolean ENABLE_LOGS = true;
	
	public static interface ILogcat {
		
		public void appendLine(String line);
		
	}

	public final static class REGEX {

		private REGEX(){}

		public final static String IPV4_REGEX = "\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z";
		public final static String DOMAIN_REGEX = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
		public final static String IPV6_REGEX = "\\A(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\z";
	}

	public final static class LOGCAT {
		
		private LOGCAT(){}
		
		private static Process logcatProcess = null;
		
		public final static String FILTER_ACTIVITY_MANAGER = "ActivityManager:I *:S";
		public final static String FILTER_SYSTEM_OUT = "System.out:I *:S";
		public final static String FILTER_DEBUG = "-d";
		
		public final static boolean startLogcat(StringBuilder log, String filter, ILogcat instance){
			boolean response = false;
			filter = (filter!= null && filter.length()>0)? filter:"";
			try{
				if(log != null && instance != null){
					Runtime r = Runtime.getRuntime();
					logcatProcess = r.exec("logcat " + filter);
					String separator = System.getProperty("line.separator"); 
					BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
				    String line;
					while ((line = reader.readLine()) != null) {
				        log.append(line);
				        log.append(separator);
				        instance.appendLine(line);
				    }
					reader.close();
					response = true;
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->startLogcat", e.toString());
				response = false;
			}
			return response;
		}
		
		public final static boolean stopLogcat(){
			boolean response = false;
			try{
				if(logcatProcess != null){
					logcatProcess.destroy();
					logcatProcess = null;
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->stopReadRawLogcat", e.toString());
				response = false;
			}
			return response;
		}
		
		public final static boolean cleanLogcat(){
			boolean response = false;
			try{
				Runtime r = Runtime.getRuntime();
				Process p = r.exec("logcat -d");
				int waitFor = p.waitFor();
				int exitValue = p.exitValue();
				if(exitValue == 0) response = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->cleanLogcat", e.toString());
				response = false;
			}
			return response;
		}
		
		public final static class PARSE {
			
			private PARSE(){}
			
			public final static boolean isSupportParse(String line){
				boolean response = false;
				if(isProcessDie(line)) response = true;
				else if(isDisplayed(line)) response = true;
				else if(isKilling(line)) response = true;
				else if(isUnableStartService(line)) response = true;
				else if(isSchedulingRestart(line)) response = true;
				else if(isStart(line)) response = true;
				else if(isStartIntent(line)) response = true;
				else if(isStartActivity(line)) response = true;
				else if(isStartProcess(line)) response = true;
				return response;
			}
			
			public final static Hashtable<String, Object> parse(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				if(isProcessDie(line)) response = parseProcessDie(line);
				else if(isDisplayed(line)) response = parseDisplayed(line);
				else if(isKilling(line)) response = parseKilling(line);
				else if(isUnableStartService(line)) response = parseUnableStartService(line);
				else if(isSchedulingRestart(line)) response = parseSchedulingRestart(line);
				else if(isStart(line)) response = parseStart(line);
				else if(isStartIntent(line)) response = parseStartIntent(line);
				else if(isStartActivity(line)) response = parseStartActivity(line);
				else if(isStartProcess(line)) response = parseStartProcess(line);
				return response;
			}
			
			private final static String getValueLogcat(String line){
				String response = null;
				try{
					response = line.substring(line.indexOf(":")+1).trim();
				}catch(Exception e){}
				return response;
			}
			
			private final static void fillKeyLogcat(Hashtable<String, Object> response, String line){
				try{
					line = line.substring(0, line.indexOf(":")).trim();
					String typeMessage = line.substring(0, 1).trim();
					String clss = line.substring(line.indexOf("/")+1);
					clss = clss.substring(0, clss.indexOf("(")).trim();
					line = line.substring(line.indexOf("(")+1);
					String id = line.substring(0, line.indexOf(")")).trim();
					
					if(typeMessage.equals("V")) typeMessage = "Verbose";
					else if(typeMessage.equals("D")) typeMessage = "Debug";
					else if(typeMessage.equals("I")) typeMessage = "Info"; 
					else if(typeMessage.equals("W")) typeMessage = "Warning";
					else if(typeMessage.equals("E")) typeMessage = "Error";
					else if(typeMessage.equals("F")) typeMessage = "Fatal";
					else if(typeMessage.equals("S")) typeMessage = "Silent";
					
					response.put("parseId", id);
					response.put("parseClass", clss);
					response.put("parseType", typeMessage);
					
				}catch(Exception e){}
			}
			
			//Process com.motorola.context (pid 30876) has died.
			private final static boolean isProcessDie(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Process") && line.endsWith("has died.")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
		
			//Process com.motorola.context (pid 30876) has died.
			private final static Hashtable<String, Object> parseProcessDie(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Process", endTag = "has died.";
					if(line.startsWith(initTag) && line.endsWith(endTag)){
						line = line.substring(initTag.length());
						line = line.substring(0, line.length() - endTag.length()).trim();
						line = line.replace("(pid ", "").replace(")", "").trim(); 
						String[] lines = line.split(" ");
						String pkg = lines[0].trim();
						String pid = lines[1].trim();
						response.put("parseMehtod", "ProcessDie");
						response.put("package", pkg);
						response.put("pid", pid);
					}
				}catch(Exception e){
				}
				return response;
			}
		
			//Displayed com.android.systemui/.recent.RecentsActivity: +150ms
			private final static boolean isDisplayed(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Displayed ")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
		
			//Displayed com.android.systemui/.recent.RecentsActivity: +150ms
			private final static Hashtable<String, Object> parseDisplayed(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Displayed ", middleTag = "+", endTag = "ms";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						line = line.substring(0, line.indexOf(endTag) + endTag.length());
						String[] lines = line.split(" ");
						String component = lines[0].replace(":", "").trim();
						String time = lines[1].replace("+", "").trim();
						response.put("parseMehtod", "Displayed");
						response.put("component", component);
						response.put("time", time);
					}
				}catch(Exception e){
				}
				return response;
			}
		
			//Killing 32570:com.outlook.Z7:engine/u0a103 (adj 0): kill background
			private final static boolean isKilling(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Killing ")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//Killing 32570:com.outlook.Z7:engine/u0a103 (adj 0): kill background
			private final static Hashtable<String, Object> parseKilling(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Killing", endTag = "/";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						line = line.substring(0, line.indexOf(endTag));
						String[] lines = line.split(":");
						String id = lines[0].trim();
						String pkg = lines[1].trim();
						response.put("parseMehtod", "Killing");
						response.put("package", pkg);
						response.put("id", id);
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//Unable to start service Intent { act=com.android.ussd.IExtendedNetworkService }: not found
			private final static boolean isUnableStartService(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Unable to start service ")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//Unable to start service Intent { act=com.android.ussd.IExtendedNetworkService }: not found
			private final static Hashtable<String, Object> parseUnableStartService(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Unable to start service Intent", endTag = ":";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						line = line.substring(0, line.indexOf(endTag)).trim();
						line = line.replace("{", "").replace("}", "").trim();
						String[] lines = line.split(" ");
						String action = "";
						if(lines != null && lines.length>0){
							for(int i=0; i<lines.length; i++){
								if(lines[i].startsWith("act=")){
									action = lines[i].substring(4);
									break;
								}
							}
						}
						response.put("parseMehtod", "UnableStartService");
						response.put("action", action);
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//Scheduling restart of crashed service com.outlook.Z7/com.seven.Z7.service.Z7Service in 335472ms
			private final static boolean isSchedulingRestart(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Scheduling restart of crashed service")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//Scheduling restart of crashed service com.outlook.Z7/com.seven.Z7.service.Z7Service in 335472ms
			private final static Hashtable<String, Object> parseSchedulingRestart(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Scheduling restart of crashed service", endTag = "ms";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						line = line.substring(0, line.indexOf(endTag)).trim();
						String[] lines = line.split(" ");
						if(lines != null && lines.length == 3){
							if(lines[1].equals("in")){
								String component = lines[0].trim();
								String time = lines[2].trim()+"ms";
								response.put("parseMehtod", "SchedulingRestart");
								response.put("component", component);
								response.put("time", time);
							}
						}
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 pkg=com.whatsapp cmp=com.whatsapp/.Main bnds=[64,395][212,559]} from pid 1499
			private final static boolean isStart(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("START u0")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 pkg=com.whatsapp cmp=com.whatsapp/.Main bnds=[64,395][212,559]} from pid 1499
			private final static Hashtable<String, Object> parseStart(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "START u0", endTag = "from pid";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						String fromPid = line.substring(line.lastIndexOf(endTag) + endTag.length()).trim();
						response.put("pid", fromPid);
						line = line.substring(0, line.indexOf(endTag)).trim();
						if(line.startsWith("{") && line.endsWith("}")){
							line = line.replace("{", "").replace("}", "").trim();
							String[] lines = line.split(" ");
							if(lines != null && lines.length > 0){
								for(int i=0; i<lines.length; i++){
									if(lines[i].indexOf("=") != -1){
										String key = lines[i].substring(0, lines[i].indexOf("="));
										String value = lines[i].substring(lines[i].indexOf("=") + 1);
										if(key.equals("act")) key = "action";
										else if(key.equals("cat")) key = "category";
										else if(key.equals("flg")) key = "flag";
										else if(key.equals("pkg")) key = "package";
										else if(key.equals("cmp")) key = "component";
										else key = null;
										if(key != null && key.length()>0) response.put(key, value);
									}
								}
								response.put("parseMehtod", "Start");
							}
						}
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.android.gallery/com.android.camera.GalleryPicker } from pid 151
			private final static boolean isStartIntent(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Starting: Intent")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}

			//Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.android.gallery/com.android.camera.GalleryPicker } from pid 151
			private final static Hashtable<String, Object> parseStartIntent(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Starting: Intent", endTag = "from pid";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						String fromPid = line.substring(line.lastIndexOf(endTag) + endTag.length()).trim();
						response.put("pid", fromPid);
						line = line.substring(0, line.indexOf(endTag)).trim();
						if(line.startsWith("{") && line.endsWith("}")){
							line = line.replace("{", "").replace("}", "").trim();
							String[] lines = line.split(" ");
							if(lines != null && lines.length > 0){
								for(int i=0; i<lines.length; i++){
									if(lines[i].indexOf("=") != -1){
										String key = lines[i].substring(0, lines[i].indexOf("="));
										String value = lines[i].substring(lines[i].indexOf("=") + 1);
										if(key.equals("act")) key = "action";
										else if(key.equals("cat")) key = "category";
										else if(key.equals("flg")) key = "flag";
										else if(key.equals("pkg")) key = "package";
										else if(key.equals("cmp")) key = "component";
										else key = null;
										if(key != null && key.length()>0) response.put(key, value);
									}
								}
								response.put("parseMehtod", "StartIntent");
							}
						}
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//Starting activity: Intent { cmp=im.tox.antox/.activities.CreateAcccountActivity } from pid 7219
			private final static boolean isStartActivity(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Starting activity: Intent")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//Starting activity: Intent { cmp=im.tox.antox/.activities.CreateAcccountActivity } from pid 7219
			private final static Hashtable<String, Object> parseStartActivity(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Starting activity: Intent ", endTag = "from pid";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						String fromPid = line.substring(line.lastIndexOf(endTag) + endTag.length()).trim();
						response.put("pid", fromPid);
						line = line.substring(0, line.indexOf(endTag)).trim();
						if(line.startsWith("{") && line.endsWith("}")){
							line = line.replace("{", "").replace("}", "").trim();
							String[] lines = line.split(" ");
							if(lines != null && lines.length > 0){
								for(int i=0; i<lines.length; i++){
									if(lines[i].indexOf("=") != -1){
										String key = lines[i].substring(0, lines[i].indexOf("="));
										String value = lines[i].substring(lines[i].indexOf("=") + 1);
										if(key.equals("act")) key = "action";
										else if(key.equals("cat")) key = "category";
										else if(key.equals("flg")) key = "flag";
										else if(key.equals("pkg")) key = "package";
										else if(key.equals("cmp")) key = "component";
										else key = null;
										if(key != null && key.length()>0) response.put(key, value);
									}
								}
								response.put("parseMehtod", "StartActivity");
							}
						}
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//Start proc com.linkedin.android for broadcast com.linkedin.android/.appwidget.ResponsiveWidgetService$ActiveUserListener: pid=1630 uid=10113 gids={50113, 3003, 1028, 1015}
			private final static boolean isStartProcess(String line){
				boolean response = false;
				try{
					line = getValueLogcat(line);
					if(line.startsWith("Start proc ")){
						response = true;
					}
				}catch(Exception e){
					response = false;
				}
				return response;
			}
			
			//Start proc com.linkedin.android for broadcast com.linkedin.android/.appwidget.ResponsiveWidgetService$ActiveUserListener: pid=1630 uid=10113 gids={50113, 3003, 1028, 1015}
			private final static Hashtable<String, Object> parseStartProcess(String line){
				Hashtable<String, Object> response = new Hashtable<String, Object>();
				try{
					fillKeyLogcat(response, line);
					line = getValueLogcat(line);
					String initTag = "Start proc ", middleTag = "for broadcast", endTag = ":";
					if(line.startsWith(initTag)){
						line = line.substring(initTag.length()).trim();
						String process =  line.substring(0, line.indexOf(middleTag)).trim();
						line = line.substring(line.indexOf(middleTag) + middleTag.length()).trim();
						String broadcast = line.substring(0, line.indexOf(endTag));
						String gids = null;
						response.put("parseMehtod", "StartProcess");
						response.put("process", process);
						response.put("broadcast", broadcast);
						int index_gids = line.indexOf("gids=");
						if(index_gids != -1){
							gids = line.substring(index_gids + 5);
							gids = gids.substring(0, gids.indexOf("}")+1);
							line = line.replace("gids=" + gids, "");
							response.put("gids", gids);
						}
						line = line.substring(line.indexOf(endTag)+1).trim();
						String[] lines = line.split(" ");
						if(lines != null && lines.length>0){
							for(int i=0; i<lines.length; i++){
								int index = lines[i].indexOf("=");
								if(index != -1){
									String key = lines[i].substring(0, index).trim();
									String value = lines[i].substring(index + 1).trim();
									response.put(key, value);
								}
							}
						}
					}
				}catch(Exception e){
				}
				return response;
			}
			
			//https://sites.google.com/site/pyximanew/blog/androidunderstandingddmslogcatmemoryoutputmessages
		}
		
	}
	
	public final static class SYSTEM {

		private SYSTEM(){}

		public final static boolean reboot(){
			boolean output = false;
			try{
				Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/su","-c","reboot now"});
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->reboot", e.toString());
				output = false;
			}
			return output;
		}

		public final static boolean isRooted() {
			return findBinary("su");
		}
		
		public final static String getBinaryPath(String binaryName) {
			String path = null;
			String[] places = { 
					"/sbin/", 
					"/system/bin/", 
					"/system/xbin/",
					"/data/local/xbin/", 
					"/data/local/bin/",
					"/system/sd/xbin/",
					"/system/bin/failsafe/",
					"/data/local/"
			};
			for (String where : places) {
				if (new File(where + binaryName).exists()) {
					path = where;
					break;
				}
			}
			return path;
		}

		public final static boolean findBinary(String binaryName) {
			boolean found = false;
			String[] places = { 
					"/sbin/", 
					"/system/bin/", 
					"/system/xbin/",
					"/data/local/xbin/", 
					"/data/local/bin/",
					"/system/sd/xbin/",
					"/system/bin/failsafe/",
					"/data/local/"
			};
			for (String where : places) {
				if (new File(where + binaryName).exists()) {
					found = true;
					break;
				}
			}
			return found;
		}

		public final static Vector<RunningAppProcessInfo> getProcesses(Context context){
			Vector<RunningAppProcessInfo> array = new Vector<RunningAppProcessInfo>();
			try{
				ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
					if(processInfo != null) array.add(processInfo);
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getProcesses", e.toString());
			}
			return array;
		}

		public final static Vector<RunningServiceInfo> getServices(Context context){
			Vector<RunningServiceInfo> array = new Vector<RunningServiceInfo>();
			try{
				ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
					if(serviceInfo != null) array.add(serviceInfo);
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getServices", e.toString());
			}
			return array;
		}

		public final static Vector<RunningTaskInfo> getTasks(Context context){
			Vector<RunningTaskInfo> array = new Vector<RunningTaskInfo>();
			try{
				ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				for (RunningTaskInfo taskInfo : manager.getRunningTasks(Integer.MAX_VALUE)){
					if(taskInfo != null) array.add(taskInfo);
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getTasks", e.toString());
			}
			return array;
		}

		public final static Vector<ApplicationInfo> getInstalledApplications(Context context){
			Vector<ApplicationInfo> array = new Vector<ApplicationInfo>();
			try{
				PackageManager pm = context.getPackageManager();
				List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
				for (ApplicationInfo packageInfo : packages) {
					if(packageInfo != null) array.add(packageInfo);
				}
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getInstalledApplications", e.toString());
			}
			return array;
		}

		public final static Process executeCmd(String args){
			Runtime runtime = Runtime.getRuntime();
			Process p = null;
			try{
				if(isRooted()){
					if(Build.VERSION.SDK_INT <= 16) p = runtime.exec("su " + args);
					else p = new ProcessBuilder().command("su", args).redirectErrorStream(true).start();
				}
				else{
					if(Build.VERSION.SDK_INT <= 16) p = runtime.exec(args);
					else p = new ProcessBuilder().command(args).redirectErrorStream(true).start();
				}
			}catch(Exception e){
				p = null;
			}
			return p;
		}

		public final static boolean isSystemApp(ApplicationInfo info){
			boolean isSystem = false;
			try{
				int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
				isSystem = (info.flags & mask) == 0;
			}catch(Exception e){
				isSystem = false;
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->isSystemApp", e.toString());
			}
			return isSystem;
		}

		public final static boolean isForegroundRuning(RunningAppProcessInfo info){
			boolean isForeground = false;
			try{
				if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) isForeground = true;
			}catch(Exception e){
				isForeground = false;
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->isForegroundRuning", e.toString());
			}
			return isForeground;
		}

		public final static ComponentName getCurrentApp(Context context){
			ComponentName componentInfo = null;
			try{
				ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			    List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
			    componentInfo = taskInfo.get(0).topActivity;
		    }catch(Exception e){
		    	componentInfo = null;
		    	if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getCurrentApp", e.toString());
		    }
			return componentInfo;
		}
		
		public final static boolean launchMarketApp(Context context, String packageName){
			boolean output = false;
			try{
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+packageName)));
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->launchMarketApp", e.toString());
				output = false;
			}
			return output;
		}

		public final static boolean isAppInstalled(Context context, String packageName){
			boolean output = false;
			try{
				PackageManager pm = context.getPackageManager();
				pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->isAppInstalled", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean isIntentCallable(Context context, Intent intent) {
			boolean output = false;
			try{
		        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);  
		        output = (list.size() > 0)? true:false;  
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->isIntentCallable", e.toString());
				output = false;
			}
			return output;
	}
		
	}

	public final static class NET {

		private NET(){}

		public final static boolean isWifiConnected(Context context){
			boolean connected = false;
			try{
				ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				SupplicantState supState = wifiInfo.getSupplicantState();
				if (mWifi.isConnected() && mWifi.getType() == ConnectivityManager.TYPE_WIFI && 
						mWifi.isAvailable() && supState.equals(SupplicantState.COMPLETED)) {
					connected = true;
				}
			}catch(Exception e){
				connected = false;
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->isWifiConnected", e.toString());
			}
			return connected;
		}

		public final static boolean isMobileConnected(Context context){
			boolean connected = false;
			try{
				ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				if (mMobile.isConnected() && mMobile.getType() == ConnectivityManager.TYPE_MOBILE && mMobile.isAvailable()) {
					connected = true;
				}
			}catch(Exception e){
				connected = false;
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->isWifiConnected", e.toString());
			}
			return connected;
		}

		public final static boolean ping(String ipOrDomain){
			boolean connected = false;
			try {
				if(ipOrDomain != null && (ipOrDomain.matches(REGEX.DOMAIN_REGEX) || ipOrDomain.matches(REGEX.IPV4_REGEX) || ipOrDomain.matches(REGEX.IPV6_REGEX))){
					Process  mIpAddrProcess = SYSTEM.executeCmd("/system/bin/ping -c 4 " + ipOrDomain);
					BufferedReader reader = new BufferedReader(new InputStreamReader(mIpAddrProcess.getInputStream()));
					StringBuffer output = new StringBuffer();
					String temp;
					while ( (temp = reader.readLine()) != null) {
						output.append(temp + "\n\r");
					}
					String[] lines = output.toString().split("\n\r");
					if(lines.length == 1 && lines[0].indexOf("unknown") != -1) connected = false;
					else{
						int counntSuccess = 0;
						for(int i=1; i<=5; i++){
							try{
								String pingLine = lines[i];
								if(pingLine != null){
									String[] pingInfo = pingLine.split(" ");
									String bytes = pingInfo[0];
									//String host = pingInfo[3];
									//String ip = pingInfo[4].replace("(", "").replace(")", "").replace(":", "");
									//String icmp_seq = pingInfo[5].replace("icmp_seq=", "");
									String ttl = pingInfo[6].replace("ttl=", "");
									//String time = pingInfo[7].replace("time=", "");
									//String ms = pingInfo[8];

									if(Integer.parseInt(bytes)>=0 && Integer.parseInt(ttl)>=0) counntSuccess++;
								}
							}catch(Exception e){}
						}
						if(counntSuccess >= 1) connected = true;
					}
				}
			}catch(Exception e){
				connected = false;
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->ping", e.toString());
			}
			return connected;
		}

		public final static Hashtable<String,String> getArpCache(){
			Hashtable<String,String> cache = new Hashtable<String,String>();
			try{
				BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
				String line; int c = 0;
				while ((line = br.readLine()) != null) {
					if(c >= 1){
						String[] lines = line.split(" +");
						if(lines != null && lines.length >= 4) cache.put(lines[0], lines[3]);
					}
					c++;
				}
				br.close();
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getArpCache", e.toString());
			}
			return cache;
		}

	}

	public final static class PERMISSIONS {

		private PERMISSIONS(){}

		public final static int getPermissions(File path)  {
			int output = 0;
			try{
				Class<?> fileUtils = Class.forName("android.os.FileUtils");
				int[] result = new int[1];
				Method getPermissions = fileUtils.getMethod("getPermissions", String.class, int[].class);
				getPermissions.invoke(null, path.getAbsolutePath(), result);
				output = result[0];
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getPermissions", e.toString());
				output = 0;
			}
			return output;
		}

		public final static int setPermissions(File path, int mode) {
			int output = 0;
			try{
				Class<?> fileUtils = Class.forName("android.os.FileUtils");
				Method setPermissions = fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
				output = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->setPermissions", e.toString());
				output = 0;
			}
			return output;
		}

		public final static String parsePermissions(int mode){
			return LinuxStatConstants.permString(mode);
		}

		public final static boolean checkPermission(Context context, String permName, String pkgName){
			boolean response = false;
			try{
				PackageManager pm = context.getPackageManager();
				response = (PackageManager.PERMISSION_GRANTED == pm.checkPermission(permName, pkgName))? true : false;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->checkPermission", e.toString());
				response = false;
			}
			return response;
		}

		public final static android.content.pm.PackageInfo getPermissions(Context context, String pkgName){
			android.content.pm.PackageInfo pi = null;
			try{
				PackageManager pm = context.getPackageManager();
				pi = pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS);
			}catch(Exception e){
				if(ENABLE_LOGS) Log.e(Diagnostics.class.getName() + "->getPermissions", e.toString());
				pi = null;
			}
			return pi;
		}

	}

	public final static class MANAGER {

		private MANAGER(){}

		public final static boolean forceStopPackage(Context context,String packageName){
			boolean output = false;
			try{
				ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				Method forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage", String.class);  
				forceStopPackage.setAccessible(true);  
				forceStopPackage.invoke(am, packageName);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->forceStopPackage", e.toString());
				output = false;
			}
			return output;
		}

		public final static boolean restartPackage(Context context,String packageName){
			boolean output = false;
			try{
				ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);    
				am.restartPackage(packageName);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->restartPackage", e.toString());
				output = false;
			}
			return output;
		}

		public final static boolean killBackgroundProcesses(Context context,String packageName){
			boolean output = false;
			try{
				ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);    
				am.killBackgroundProcesses(packageName);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->restartPackage", e.toString());
				output = false;
			}
			return output;
		}
		
		/*public final static boolean moveTaskToFront(Context context, int taskId, int flags){
			boolean output = false;
			try{
				ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);    
				am.moveTaskToFront(taskId, flags);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->moveTaskToFront", e.toString());
				output = false;
			}
			return output;
		}*/

		public final static boolean killPID(int pid){
			boolean output = false;
			try{
				android.os.Process.killProcess(pid);
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->killPID", e.toString());
				output = false;
			}
			return output;
		}

		public final static boolean kill(String packageName){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("am kill " + packageName);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->kill", e.toString());
				output = false;
			}
			return output;

		}
		
		public final static boolean foceStop(String packageName){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("am force-stop " + packageName);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->foceStop", e.toString());
				output = false;
			}
			return output;

		}
	
		//ojo revisar
		
		public final static boolean uninstallApp(String packageName){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm uninstall " + packageName);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->uninstallApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean installApp(String pathApp){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm install " + pathApp);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->installApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean enableApp(String packageNameOrComponent){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm enable " + packageNameOrComponent);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->enableApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean disableApp(String packageNameOrComponent){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm disable " + packageNameOrComponent);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->disableApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean startService(String packageNameOrComponent){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("am startservice " + packageNameOrComponent);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->startService", e.toString());
				output = false;
			}
			return output;
		}
		
		//<uses-permission android:name="android.permission.CLEAR_APP_USER_DATA"/>
		public final static boolean clearApp(String packageName){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm clear " + packageName);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->clearApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean grantApp(String packageName, String permission){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm grant " + packageName + " " + permission);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->grantApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static boolean revokeApp(String packageName, String permission){
			boolean output = false;
			try{
				Process p = SYSTEM.executeCmd("pm revoke " + packageName + " " + permission);
				p.waitFor();
				output = true;
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->revokeApp", e.toString());
				output = false;
			}
			return output;
		}
		
		public final static String pathApp(String packageName){
			String output = "";
			try{
				Process p = SYSTEM.executeCmd("pm path " + packageName);
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuffer buffer = new StringBuffer();
				String temp;
				while ( (temp = reader.readLine()) != null) {
					buffer.append(temp + "\n\r");
				}
				reader.close();
				p.waitFor();
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->pathApp", e.toString());
				output = "";
			}
			return output;
		}
		
		public final static String getPermissionsApp(String packageName){
			String output = "";
			try{
				Process p = SYSTEM.executeCmd("dumpsys package " + packageName);
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuffer buffer = new StringBuffer();
				String temp;
				while ( (temp = reader.readLine()) != null) {
					buffer.append(temp + "\n\r");
				}
				reader.close();
				p.waitFor();
				output = buffer.toString();
			}catch(Exception e){
				if(ENABLE_LOGS) Log.w(Diagnostics.class.getName() + "->getPermissionsApp", e.toString());
				output = "";
			}
			return output;
		}
		
	}
	
}
