package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

public class Configuration {
	private static Configuration instance;
	private JSONObject confMapping;
	
	private Configuration() {
		configure();
	}
	
	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		
		return instance;
	}
	
	private void configure() {
		InputStream confFile = Configuration.class.getResourceAsStream("/resources/conf.json");
		BufferedReader br = new BufferedReader(new InputStreamReader(confFile));
		String confLine = "";
		String totalLines = "";
		try {
			while ((confLine = br.readLine()) != null) {
				totalLines += confLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Parse json
		confMapping = new JSONObject(totalLines);
		for (String key : confMapping.keySet()) {
			System.out.println(key);
		}
	}
	
	public Object getValue(String key) {
		Object returnVal = new Object();
		try {
			returnVal = confMapping.get(key);
		} catch (JSONException ee) {
			return null;
		}
		return returnVal;
	}
}
