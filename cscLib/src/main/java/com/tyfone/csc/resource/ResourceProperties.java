/* 
 * © 2004-2014, Tyfone, Inc. All rights reserved.  
 * Patented and other Patents pending.  
 * All trademarks are property of their respective owners. 
 * SideTap, SideSafe, SideKey, The Connected Smart Card,  
 * CSC are trademarks of Tyfone, Inc. 
 * For questions visit: www.tyfone.com 
 */
package com.tyfone.csc.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import android.content.Context;

import com.tyfone.csc.exception.CSCException;

/**
 * This class is used to get the resource strings based on {@link Locale} from
 * properties file.
 * 
 * @author <b>suryaprakash</b> on Jul 31, 2014 12:19:19 PM </br> Project: CSC
 *         Library </br>
 */
public class ResourceProperties {

	private static final String TAG = "Resource Properties";
	private static Locale defaultLocale = new Locale("en", "US");
	private static Locale locale = defaultLocale;

	private Properties properties;
	private static ResourceProperties resourceProperties;
	private HashMap<String, String> propetiesMap = null;

	/**
	 * This method instantiates {@link ResourceProperties}.
	 * 
	 * @return {@link ResourceProperties}.
	 * @throws CSCException
	 */
	public static ResourceProperties getInstance() {
		return getInstance(locale);
	}

	/**
	 * This method instantiate {@link ResourceProperties}.
	 * 
	 * @param locale
	 *            Locale to get the resource information.
	 * @return {@link ResourceProperties}.
	 * @throws CSCException
	 */
	private synchronized static ResourceProperties getInstance(
			final Locale locale) {
		if (resourceProperties == null) {
			resourceProperties = new ResourceProperties();
			ResourceProperties.locale = locale;
			resourceProperties.properties = new Properties();
		}
		return resourceProperties;
	}

	/**
	 * It gets the properties from the resource properties file.
	 * 
	 * @return returns the properties from the resource properties file.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap<String, String> getproperties() {
		if (propetiesMap == null) {
			String libFileName = constructFileName(defaultLocale, true);
			Properties propertiesLibMap = loadProperties(libFileName);
//			Logger.info("ResourceProperties (getPropeties)",
//					"Library Propeties Count:" + propertiesLibMap.size());

			String appFileName = constructFileName(locale, false);
			Properties propertiesMap = loadProperties(appFileName);
			propetiesMap = new HashMap<String, String>((Map) propertiesMap);
//			Logger.info("Total App Propeties Count=" + propertiesMap.size()
//					+ ";Propeties==>" + propetiesMap.toString());
		}
		return propetiesMap;
	}

	/**
	 * Gets the properties of an error key.
	 * 
	 * @param key
	 *            It is a unique key (error key) to get the property (Error
	 *            description) value.
	 * @return error description (Property value).
	 */
	public String getProperty(String key) {
		return getproperties().get(key);
	}

	/**
	 * This method constructs the Resource file name based on {@link Locale}
	 * 
	 * @param locale
	 * @param useLibProperties
	 * @return file name
	 */
	private static String constructFileName(Locale locale,
			boolean useLibProperties) {

		StringBuffer fileName;
		if (useLibProperties)
			fileName = new StringBuffer("csc-");
		else
			fileName = new StringBuffer("resources/csc-");

		String language = locale.getLanguage();
		String country = locale.getCountry();
		fileName.append(language).append("_").append(country)
				.append(".properties");
//		Logger.info(fileName.toString());
		return fileName.toString();
	}

	/**
	 * This method sets device locale to resource properties.
	 * 
	 * @param context
	 *            Application Context.
	 * @return resource properties.
	 * @throws CSCException
	 */
	public ResourceProperties setSystemLocale(Context context)
			throws CSCException {
		setLocale(context);
		return reload();
	}

	/**
	 * This method sets resource locale based on resource instance.
	 * 
	 * @param context
	 *            Context of the Resources.
	 */
	private static void setLocale(final Context context) {
		ResourceProperties.locale = context.getResources().getConfiguration().locale;
	}

	/**
	 * It loads the properties after a change in locale and returns resources
	 * relevant to new locale.
	 * 
	 * @throws CSCException
	 */
	public ResourceProperties reload() throws CSCException {
		return resourceProperties = getInstance(ResourceProperties.locale);
	}

	/**
	 * This method returns current locale from resource instance.
	 * 
	 * @return locale from resource instance.
	 */
	public static Locale getLocale() {
		return locale;
	}

	/**
	 * It loads all the properties from properties file.
	 * 
	 * @param fileName file name of properties file.
	 * @return all properties present in file.
	 */
	private synchronized Properties loadProperties(String fileName) {
		URL url = ResourceProperties.class.getClassLoader().getResource(
				fileName);
		if (url != null) {
			InputStream inputStream = ResourceProperties.class.getClassLoader()
					.getResourceAsStream(fileName);
			if (inputStream != null) {
				try {
					propetiesMap = null;
					resourceProperties.properties.load(inputStream);
				} catch (IOException e) {
//					Logger.error(TAG, e.getMessage());
				}
				return resourceProperties.properties;
			}
		}
		return resourceProperties.properties;
	}
}
