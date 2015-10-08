package javax.module.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Created by robert on 2015-10-08 01:07.
 */
public abstract
class AbstractPropertiesAdapter implements InvocationHandler
{
	private final
	String contextBit;

	protected final
	Properties lowerCasedDefaults;

	protected
	AbstractPropertiesAdapter(String contextBit, Properties mixedCaseDefaults)
	{
		this.contextBit = contextBit;

		if (mixedCaseDefaults==null)
		{
			this.lowerCasedDefaults=null;
		}
		else
		{
			this.lowerCasedDefaults = new Properties();

			for (String key : mixedCaseDefaults.stringPropertyNames())
			{
				lowerCasedDefaults.setProperty(key.toLowerCase(), mixedCaseDefaults.getProperty(key));
			}
		}
	}

	@Override
	public
	Object invoke(Object o, Method method, Object[] objects) throws Throwable
	{
		final
		String methodName = method.getName();

		final
		Object[] context = new Object[]{contextBit, ": ", methodName};

		final
		boolean readMode;

		final
		String propertyName;
		{
			final
			boolean capitalMatch=methodName.length()>3 && Character.isUpperCase(methodName.charAt(3));

			if (capitalMatch && methodName.startsWith("get"))
			{
				readMode=true;
				propertyName=methodName.substring(3);
			}
			else
			if (capitalMatch && methodName.startsWith("set"))
			{
				readMode=false;
				propertyName=methodName.substring(3);
			}
			else
			{
				readMode=true;
				propertyName=methodName;
			}
		}

		if (readMode)
		{
			if (objects!=null && objects.length>0)
			{
				throw new UnsupportedOperationException("PropertiesFileAdapter cannot handle read methods that take arguments: "+methodName);
			}

			//TODO: check for void return type? what would that even mean?!
			final
			Class<?> returnType = method.getReturnType();

			final
			String stringValue = getPropertyForLowerCaseKey(propertyName.toLowerCase());

			//System.err.println(String.format("get(%s) -> %s", propertyName, stringValue));
			if (returnType.isPrimitive() && stringValue==null)
			{
				throw new IllegalStateException(propertyName+" cannot be null/empty/missing due to primitive return type");
			}

			return Convert.stringToBasicObject(stringValue, returnType, context);
		}
		else
		{
			throw new UnsupportedOperationException("write methods are not yet implemented (in this version)");
		}
	}

	protected abstract
	String getPropertyForLowerCaseKey(String key);
}