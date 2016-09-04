package com.github.spirylics.web2app;

import java.util.HashMap;
import java.util.Map;

public class NpmModel {
	public static class Package {
		private String name;
		private String version;
		final private Map<String, Object> dependencies = new HashMap<>();
		final private Map<String, Object> config = new HashMap<>();
		final private Map<String, Object> scripts = new HashMap<>();

		public String getName() {
			return name;
		}

		public Map<String, Object> getConfig() {
			return config;
		}

		public Map<String, Object> getScripts() {
			return scripts;
		}

		public Package setName(String name) {
			this.name = name;
			return this;
		}

		public String getVersion() {
			return version;
		}

		public Package setVersion(String version) {
			this.version = version;
			return this;
		}

		public Map<String, Object> getDependencies() {
			return dependencies;
		}

		public Package addDependency(String name, Object version) {
			getDependencies().put(name, version);
			return this;
		}

		public Package addConfig(String key, Object value) {
			getConfig().put(key, value);
			return this;
		}

		public Package addScript(String name, Object cmd) {
			getScripts().put(name, cmd);
			return this;
		}
	}
}
