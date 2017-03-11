/**
 * 
 * @author Marcel Seibel
 * @Version 040117
 * 
 */

package de.marcely.repair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.marcely.repair.ConfigManager.MultiKey.MultiKeyEntry;

import java.util.Map.Entry;

public class ConfigManager {
	
	private static final Random rand = new Random();
	
	private File configFile = null;
	private MultiKeyMap<String, Object> configs = new MultiKeyMap<String, Object>();
	
	public ConfigManager(String pluginName, String configName){
		setPath("plugins/" + pluginName + "/" + configName, true);
	}
	
	public ConfigManager(String pluginName, String configName, boolean createNewFile){
		setPath("plugins/" + pluginName + "/" + configName, createNewFile);
	}
	
	public void addConfig(String name){
		configs.put(name, null);
	}
	
	public void addConfig(String name, Object value){
		configs.put(name, value);
	}
	
	public void addConfig(String name, String value){
		configs.put(name, value);
	}
	
	public void addConfig(String name, boolean value){
		configs.put(name, value);
	}
	
	public void addConfig(String name, Double value){
		configs.put(name, value);
	}
	
	public void addConfig(String name, int value){
		configs.put(name, value);
	}
	
	public void addComment(String comment){
		addConfig("# " + comment, "");
	}
	
	public String getConfigString(String name){
		Object obj = getConfigObj(name);
		if(obj instanceof String)
			return (String) obj;
		
		return null;
	}
	
	public Boolean getConfigBoolean(String name){
		Object obj = getConfigObj(name);
		if(obj instanceof Boolean)
			return (boolean) obj;
		else if(obj instanceof String){
			String str = String.valueOf(obj);
			if(str.equalsIgnoreCase("true") ||
			   str.equalsIgnoreCase("false"))
				return Boolean.valueOf(str);
		}
		return null;
	}
	
	public Double getConfigDouble(String name){
		Object obj = getConfigObj(name);
		if(obj instanceof Double)
			return (double) obj;
		else if(obj instanceof Float)
			return (double)((float)obj);
		else if(obj instanceof String){
			return Double.valueOf((String) obj);
		}
		
		return null;
	}
	
	public Integer getConfigInt(String name){
		Object obj = getConfigObj(name);
		if(obj instanceof String && Util_IsInteger(String.valueOf(obj)))
			return Integer.valueOf(String.valueOf(obj));
		
		return null;
	}
	
	public void addEmptyLine(){
		addConfig("empty" + rand.nextInt(), (String) null);
	}
	
	/*public HashMap<String, String> getKeysWhichStartWith(String startsWith){
		HashMap<String, String> list = new HashMap<String, String>();
		for(MultiKeyEntry<String, Object> MultiKeyEntry:configs.entrySet()){
			String name = MultiKeyEntry.getKey();
			if(name.startsWith(startsWith)){
				if(MultiKeyEntry.getValue() != null){
					String value = MultiKeyEntry.getValue().toString();
					list.put(name, value);
				}else
					list.put(name, null);
			}
		}
		
		return list;
	}*/
	
	public MultiKeyMap<String, String> getKeysWhichStartWith(String startsWith){
		MultiKeyMap<String, String> list = new MultiKeyMap<String, String>();
		
		for(MultiKeyEntry<String, Object> MultiKeyEntry:configs.entrySet()){
			String name = MultiKeyEntry.getKey();
			if(name.startsWith(startsWith)){
				if(MultiKeyEntry.getValue() != null){
					String value = MultiKeyEntry.getValue().toString();
					list.put(name, value);
				}else
					list.put(name, null);
			}
		}
		
		return list;
	}
	
	public Object getConfigObj(String name){
		return configs.getFirst(name);
	}
	
	public boolean update(){
		if(!configFile.exists()){
			save();
			return false;
		}
		
		return true;
	}
	
	public MultiKeyMap<String, Object> getInside(int insideLvl){
		MultiKeyMap<String, Object> list = new MultiKeyMap<String, Object>();
		
	    for(MultiKeyEntry<String, Object> MultiKeyEntry:configs.entrySet()){
	    	String name = MultiKeyEntry.getKey();
	    	Object value = MultiKeyEntry.getValue();
	    	if(!String.valueOf(name).startsWith("# ")){
	    		if(name.split("\\.").length - 1 == insideLvl){
	    			String str = "";
	    			int i = 1;
	    			int max = name.split("\\.").length;
	    			
	    			for(String s:name.split("\\.")){
	    				while(s.startsWith("	"))
	    					s = s.substring(1, s.length());
	    				if(i >= max)
	    					str += s;
	    				else{
	    					str += s + ".";
	    					i++;
	    				}
	    			}
	    			if(value instanceof String){
	    				String v = (String) value;
	    				while(v.startsWith("	"))
	    					v = v.substring(1, v.length());
	    				value = v;
	    			}
	    			list.put(str, value);
	    		}
	    	}
	    }
	    
	    return list;
	}
	
	public void save(){
		try {
			savee();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void savee() throws IOException {
		if(configFile.exists()) configFile.delete();
		
	    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF8"));
	    
	    List<String> doneInsideConfigs = new ArrayList<String>();
	    
	    for(MultiKeyEntry<String, Object> MultiKeyEntry:configs.entrySet()){
	    	String name = MultiKeyEntry.getKey();
	    	Object value = MultiKeyEntry.getValue();
	    	
	    	// create empty lines
	    	if(name.startsWith("empty") && value == null){
	    		bw.write("");
	    		bw.newLine();
	    	
	    	// isn't annotated
	    	}else if(!String.valueOf(name).startsWith("# ")){
	    		
	    		// prepare configs with multiple names
		    	if(name.split("\\.").length >= 2){
		    		String insideConfig = "";
		    		for(int i=0; i<name.split("\\.").length - 1; i++){
		    			if(i <= name.split("\\.").length - 2)
		    				insideConfig += name.split("\\.")[i];
		    			else
		    				insideConfig += name.split("\\.")[i] + ".";
		    		}
		    		
		    		// write object
		    		if(!doneInsideConfigs.contains(insideConfig)){
		    			bw.write(insideConfig + " {");
		    			bw.newLine();
		    			for(MultiKeyEntry<String, Object> MultiKeyEntry1:configs.entrySet()){
		    		    	String name1 = MultiKeyEntry1.getKey();
		    		    	Object value1 = MultiKeyEntry1.getValue();
		    		    	if(!String.valueOf(name1).startsWith("# ") && name1.split("\\.").length >= 2){
		    		    		String insideConfig1 = "";
		    		    		for(int i=0; i<name1.split("\\.").length - 1; i++){
		    		    			if(i <= name1.split("\\.").length - 2)
		    		    				insideConfig1 += name1.split("\\.")[i];
		    		    			else
		    		    				insideConfig1 += name1.split("\\.")[i] + ".";
		    		    		}
		    		    		// write config
		    		    		if(insideConfig.equals(insideConfig1)){
		    		    			String s = "";
		    		    			for(int i=0; i<name1.split("\\.").length - 1; i++)
		    		    				s += "	";
		    		    			if(value1 != null)
		    		    				bw.write(s + name1.replace(insideConfig, "").substring(1) + ": " + String.valueOf(value1));
		    		    			else
		    		    				bw.write(s + name1.replace(insideConfig, "").substring(1));
		    		    			bw.newLine();
		    		    		}
		    		    	}
		    			}
		    			doneInsideConfigs.add(insideConfig);
		    			bw.write("}");
		    			bw.newLine();
		    		}
		    	}else{
		    		// write config
		    		if(value != null)
		    			bw.write(name + ": " + String.valueOf(value));
		    		else
		    			bw.write(name);;
		    		bw.newLine();
		    	}
	    	}else{
		    	bw.write(name);
		    	bw.newLine();
	    	}
	    }
	    bw.close();
	}
	
	public void load(){
		load(true);
	}
	
	public void load(boolean utf8){
		// prepare
		BufferedReader br = null;
		try{
			if(!utf8)
				br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
			else
				br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), "UTF8"));
		}catch(Exception e){
			System.err.println("[MBedwars] There was an issue with loading a config with utf8.");
			if(!utf8)
				e.printStackTrace();
			else
				load(false);
		}
		String line = null;
		String currentLooking = "";
		
		// read
	    try{
		    clear();
			while((line = br.readLine()) != null){ 
				
				boolean b = false;
				
				// prepare configs inside objects and objects
				if(line.endsWith("{")){
					String z = line.substring(0, line.length() - 1);	
					
					while(z.startsWith("	"))
						z = z.substring(1, z.length());
					while(z.endsWith(" "))
						z = z.substring(0, z.length() - 1);
					
					if(currentLooking.equals(""))
						currentLooking += z;
					else
						currentLooking += "." + z;
					
					b = true;
					
				}else if(line.startsWith("}")){
					if(!line.equals("")){
						String[] strs = currentLooking.split("\\.");
						currentLooking = currentLooking.substring(0, currentLooking.length() - strs[strs.length - 1].length());
						b = true;
					}
				}
				
				// differentiate name and config
				if(b == false){
					String[] strs = line.split(":");
					String name = strs[0];
					String value = "";
					int i=1;
					if(strs.length >= 2){
						while(i < strs.length){
							if(i > 1)
								value += ":" + strs[i];
							else
								value += strs[i];
							i++;
						}
					}else
						value = null;
					
					// fix spaces
					if(value != null){
						while(value.startsWith("	") || value.startsWith(" "))
							value = value.substring(1, value.length());
					}while(name.startsWith("	") || name.startsWith(" "))
						name = name.substring(1, name.length());
					
					// save
					if(currentLooking.equals(""))
						configs.put(name, value);
					else
						configs.put(currentLooking + "." + name, value);
				}
			}
			
			br.close();
		}catch (IOException e){
			e.printStackTrace();
		}	
	}
	
	public List<String> getInsideAsStringList(String insideName){
		List<String> list = new ArrayList<String>();
		
		for(MultiKeyEntry<String, String> e:getKeysWhichStartWith(insideName).entrySet()){
			String str = e.getKey().replaceFirst(insideName.endsWith(".") ? insideName : insideName + ".", "") // key
					+ (e.getValue() != null ? ": " + e.getValue() : ""); // value
			
			while(str.startsWith("	") || str.startsWith(" "))
				str = str.substring(1, str.length());
			
			list.add(str);
		}
		
		return list;
	}
	
	public void clear(){
		configs = new MultiKeyMap<String, Object>();
	}
	
	public boolean exists(){
		return configFile.exists();
	}
	
	public boolean isEmpty(){
		return configFile.length() == 0;
	}
	
	public void setPath(String path){
		setPath(path, true);
	}
	
	public void setPath(String path, boolean createNewFile){
		configFile = new File(path);
		File dir = configFile.getParentFile();
		
		if(!dir.exists())
			dir.mkdirs();
		if(createNewFile){
			if(!configFile.exists()){
				try{
					configFile.createNewFile();
				}catch (IOException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public String getPath(){
		return this.configFile.getPath();
	}
	
	public File getFile(){
		return this.configFile;
	}
	
	public MultiKeyMap<String, Object> getCache(){
		return this.configs;
	}
	
	private static boolean Util_IsInteger(String str){
		try{
			Integer.valueOf(str);
			
			return true;
		}catch(Exception e){
			return false;
		}
	}
	
	
	
	public static class MultiKey {
		public static class MultiKeyEntry<obj1, obj2> implements Entry<obj1, obj2> {
			private obj1 o1;
			private obj2 o2;
			
			public MultiKeyEntry(obj1 key, obj2 value){
				this.o1 = key;
				this.o2 = value;
			}
			
			@Override
			public obj1 getKey() {
				return this.o1;
			}

			@Override
			public obj2 getValue() {
				return this.o2;
			}

			@Override
			public obj2 setValue(obj2 value) {
				this.o2 = value;
				return value;
			}
		}
	}
	
	public static class MultiKeyMap<obj1, obj2>{
		List<obj1> l1 = new ArrayList<obj1>();
		List<obj2> l2 = new ArrayList<obj2>();
		
		public MultiKeyMap(){ }
		
		public int size(){
			return l1.size();
		}
		
		public void put(obj1 o1, obj2 o2){
			l1.add(o1);
			l2.add(o2);
		}
		
		public obj2 getFirst(obj1 o1){
			if(get(o1).size() >= 1)
				return get(o1).get(0);
			else
				return null;
		}
		
		public List<obj2> get(obj1 o1){
			List<obj2> list = new ArrayList<obj2>();
			int i=0;
			for(obj1 o:l1){
				if(o1.equals(o))
					list.add(l2.get(i));
				i++;
			}
			return list;
		}
		
		public ArrayList<MultiKeyEntry<obj1, obj2>> entrySet(){
			ArrayList<MultiKeyEntry<obj1, obj2>> list = new ArrayList<MultiKeyEntry<obj1, obj2>>();
			int i=0;
			for(obj1 o:this.l1){
				list.add(new MultiKeyEntry<obj1, obj2>(o, l2.get(i)));
				i++;
			}
			return list;
		}
		
		public void remove(obj1 o1){
			int i=0;
			for(obj1 o:l1){
				if(o.equals(o1)){
					l1.remove(i);
					l2.remove(i);
				}
				i++;
			}
		}
		
		public void remove(obj1 o1, obj2 o2){
			int i=0;
			for(obj1 o:new ArrayList<obj1>(l1)){
				if(o.equals(o1) && l2.get(i).equals(o2)){
					l1.remove(i);
					l2.remove(i);
					i--;
				}
				i++;
			}
		}
		
		public boolean containsKey(obj1 o1){
			return l1.contains(o1);
		}
		
		public boolean containsValue(obj2 o2){
			return l2.contains(o2);
		}
		
		public void replace(obj1 o1, obj2 o2){
			remove(o1);
			put(o1, o2);
		}
		
		public List<obj1> keySet(){
			return new ArrayList<obj1>(l1);
		}
	}
}
