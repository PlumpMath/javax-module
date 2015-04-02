package javax.module.tools;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by robert on 4/2/15.
 */
public
class Convert
{
	public static
	Class[] getSupportedNonPrimitiveTypes()
	{
		return supportedNonPrimitiveTypes;
	}

	private static
	Class[] supportedNonPrimitiveTypes =
		new Class[]
			{
				String.class, File.class,
				InputStream.class, FileInputStream.class, PrintStream.class,
				OutputStream.class, FileOutputStream.class, BufferedReader.class,
				Enum.class, // <-- ???
				Short.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Character.class, Byte.class,
			};

	private static
	Set<Class> supportedNonPrimitiveTypeSet;

	public static
	boolean isSupportedType(Class aClass)
	{
		if (aClass.isPrimitive())
		{
			return true;
		}

		if (supportedNonPrimitiveTypeSet==null)
		{
			supportedNonPrimitiveTypeSet=new HashSet<>();

			for (Class supportedNonPrimitiveType : supportedNonPrimitiveTypes)
			{
				supportedNonPrimitiveTypeSet.add(supportedNonPrimitiveType);
			}
		}

		return supportedNonPrimitiveTypeSet.contains(aClass);
	}

	public static
	Object stringToBasicObject(String stringValue, Class targetType)
	{
		return stringToBasicObject(stringValue, targetType, "");
	}

	public static
	Object stringToBasicObject(String stringValue, Class targetType, String context)
	{
		//TODO: primitive types cannot be null, can probably give a much better message therefor.
		if (stringValue.equals("null")) return null;

		if (targetType == String.class) return stringValue;

		if (Enum.class.isAssignableFrom(targetType))
		{
			try
			{
				return Enum.valueOf(targetType, stringValue);
			}
			catch (IllegalArgumentException e)
			{
				throw new IllegalArgumentException(context+targetType.getSimpleName()+" parameter cannot be '"+stringValue+"', valid values are: "+ enumValuesToHumanReadableCsv(targetType));
			}
		}

		if (targetType==short   .class || targetType==Short    .class) return new Short(stringValue);
		if (targetType==int     .class || targetType==Integer  .class) return new Integer(stringValue);
		if (targetType==long    .class || targetType==Long     .class) return new Long(stringValue);
		if (targetType==float   .class || targetType==Float    .class) return new Float(stringValue);
		if (targetType==double  .class || targetType==Double   .class) return new Double(stringValue);
		if (targetType==boolean .class || targetType==Boolean  .class) return stringToBoolean(stringValue);

		if (targetType==char    .class || targetType==Character.class)
		{
			assert(stringValue.length()==1);
			return stringValue.charAt(0);
		}

		if (targetType==byte    .class || targetType==Byte     .class)
		{
			//Byte::decode() has sign issues... can't do 0xFF, for example.
			//return Byte.decode(stringValue);

			if (stringValue.startsWith("0x"))
			{
				stringValue=stringValue.substring(2);
			}

			//TODO: make this less ugly.
			assert(stringValue.length()<=2);
			return hexStringToByteArray(stringValue)[0];
		}

		if (targetType==byte[]  .class)
		{
			if (stringValue.startsWith("0x"))
			{
				stringValue=stringValue.substring(2);
			}

			//BigInteger drops leading zeros...
			//return new BigInteger(stringValue, 16).toByteArray();
			return hexStringToByteArray(stringValue);
		}

		if (targetType==File.class)
		{
			return new File(stringValue);
		}

		if (targetType==InputStream.class || targetType==FileInputStream.class)
		{
			try
			{
				if (stringValue.equals("-"))
				{
					if (targetType==FileInputStream.class)
					{
						//TODO: check to make sure this works (FileInputStream argument satisfied by stdin)
						return new FileInputStream("/dev/stdin");
					}
					else
					{
						return System.in;
					}
				}
				else
				{
					return new FileInputStream(stringValue);
				}
			}
			catch (FileNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}

		if (targetType == OutputStream.class || targetType == FileOutputStream.class)
		{
			try
			{
				if (stringValue.equals("-"))
				{
					if (targetType==FileOutputStream.class)
					{
						//TODO: check to make sure this works (FileOutputStream argument satisfied by stdout)
						return new FileOutputStream("/dev/stdout");
					}
					else
					{
						return System.out;
					}
				}
				else
				{
					return new FileOutputStream(stringValue);
				}
			}
			catch (FileNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}

		if (targetType==PrintStream.class)
		{
			if (stringValue.equals("-"))
			{
				return System.out;
			}
			else
			{
				try
				{
					return new PrintStream(stringValue);
				}
				catch (FileNotFoundException e)
				{
					throw new RuntimeException(e);
				}
			}
		}

		if (targetType==BufferedReader.class)
		{
			try
			{
				if (stringValue.equals("-"))
				{
					//TODO: check to make sure this works (BufferedReader argument satisfied by stdin)
					return new BufferedReader(new FileReader("/dev/stdin"));
				}
				else
				{
					return new BufferedReader(new FileReader(stringValue));
				}
			}
			catch (FileNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}

		throw new UnsupportedOperationException(targetType+" constructor parameters are not supported");
	}

	/*
	http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	 */
	public static
	byte[] hexStringToByteArray(String s)
	{
		int len = s.length();

		if (len%2==1)
		{
			s="0"+s;
			len++;
		}

		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
									  + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	public static
	Boolean stringToBoolean(String s)
	{
		if (s.length()==1)
		{
			char c=s.charAt(0);
			if (c=='0' ||c=='f' ||c=='F' ||c=='n' ||c=='N') return Boolean.FALSE;
			if (c=='1' ||c=='t' ||c=='T' ||c=='y' ||c=='Y') return Boolean.TRUE;
			if (c=='x' ||c=='u' ||c=='U') return null;
			throw new IllegalArgumentException("unable to interpret single-character boolean parameter: "+c);
		}
		else
		{
			//To avoid accidentally interpreting a misplaced string as a boolean, we require the full string (except for the one-char options above).
			s=s.toLowerCase();

			/*
			Matching this fine specification (missing 'undefined' strings, though):
			http://www.postgresql.org/docs/9.1/static/datatype-boolean.html
			 */

			if (s.equals("true" ) || s.equals("yes") || s.equals("on") ) return Boolean.TRUE;
			if (s.equals("false") || s.equals("no" ) || s.equals("off")) return Boolean.FALSE;
			if (s.equals("undefined") || s.equals("maybe")) return null;
			throw new IllegalArgumentException("unable to interpret boolean parameter: "+s);
		}
	}

	private static
	String enumValuesToHumanReadableCsv(Class<? extends Enum> enumClass)
	{
		try
		{
			Method method = enumClass.getMethod("values");
			Object[] enumValues = (Object[]) method.invoke(null);

			StringBuilder sb=new StringBuilder();

			for (Object o : enumValues)
			{
				sb.append(o);
				sb.append(',');
				sb.append(' ');
			}

			//clip off the last comma... and space...
			sb.deleteCharAt(sb.length()-1);
			sb.deleteCharAt(sb.length()-1);

			return sb.toString();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			return "<unable-to-determine>";
		}
	}

	private Convert() {}
}
