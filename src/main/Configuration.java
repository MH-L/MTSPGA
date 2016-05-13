package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Configuration {
	private static Configuration instance;
	public Map<String, Object> confMapping;
	
	private Configuration() {
		confMapping = new HashMap<String, Object>();
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
		
		System.out.println(totalLines);
	}
}
